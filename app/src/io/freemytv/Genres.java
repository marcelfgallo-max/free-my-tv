package io.freemytv;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Explore's showcase: genres two at a time (a pair per page), each the most popular titles of that genre on the
 * streaming services the TV has (TMDB's watch providers for the region, one service and kind at a time so each title
 * knows its app), the services taking turns. A genre's list is fetched when its page (or the one before it) shows,
 * then kept a day. The posters (TMDB's w342) are reduced once to the cell's exact size and kept on disk.
 */
final class Genres {
    /** Label (pt, en, es), the movie query and the show query (null: none of that kind). TMDB genre ids. */
    private static final String[][] GENRES = {
        {"Explorar Comédias", "Explore Comedies", "Explorar Comedias", "with_genres=35", "with_genres=35"},
        {"Explorar Doramas Coreanos", "Explore Korean Drama", "Explorar Doramas Coreanos", null, "with_genres=18&with_original_language=ko"},
        {"Explorar Documentários", "Explore Documentaries", "Explorar Documentales", "with_genres=99", "with_genres=99"},
        {"Explorar Aventura", "Explore Adventure", "Explorar Aventura", "with_genres=12", "with_genres=10759"},
        {"Explorar Suspense", "Explore Thrillers", "Explorar Suspenso", "with_genres=53", "with_genres=9648"},
        {"Explorar Drama", "Explore Drama", "Explorar Drama", "with_genres=18", "with_genres=18"},
        {"Explorar Ação", "Explore Action", "Explorar Acción", "with_genres=28", "with_genres=10759"},
        {"Explorar Animação", "Explore Animation", "Explorar Animación", "with_genres=16&without_genres=10751", "with_genres=16&without_original_language=ja"},
        {"Explorar Romance", "Explore Romance", "Explorar Romance", "with_genres=10749", null},
        {"Explorar Anime", "Explore Anime", "Explorar Anime", "with_genres=16&with_original_language=ja", "with_genres=16&with_original_language=ja"},
        {"Explorar Ficção Científica", "Explore Science Fiction", "Explorar Ciencia Ficción", "with_genres=878", "with_genres=10765"},
        {"Explorar Crime", "Explore Crime", "Explorar Crimen", "with_genres=80", "with_genres=80"},
    };
    /** TMDB provider id, HomeApps key, package (the same services as the catalog). */
    private static final Object[][] SERVICES = {
        {8, "netflix", "com.netflix.ninja"},
        {119, "prime", "com.amazon.amazonvideo.livingroom"},
        {337, "disney", "com.disney.disneyplus"},
        {1899, "max", "com.wbd.stream"},
        {350, "appletv", "com.apple.atve.androidtv.appletv"},
    };
    /** Titles kept per genre (a row shows up to 11). */
    static final int PER_GENRE = 14;
    private static final long EVERY = 24 * 3600_000L;
    private static final String REGION = Config.TMDB_REGION.trim().toUpperCase(Locale.ROOT);
    private static final String POSTERS = "https://image.tmdb.org/t/p/w342";

    private static Genres instance;

    static synchronized Genres get(Context c) {
        if (instance == null) instance = new Genres(c.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final SharedPreferences cache;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final ExecutorService images = Executors.newFixedThreadPool(3);
    private final Handler main = new Handler(Looper.getMainLooper());
    @SuppressWarnings("unchecked")
    private final List<Recs.Pick>[] lists = new List[GENRES.length];
    private final long[] fetchedAt = new long[GENRES.length];
    private final Set<Integer> fetching = new HashSet<>();
    private volatile long failedAt;
    /** The posters at their cell size, the most recent last (a page and the next one fit). */
    private final Map<String, Bitmap> posters = new LinkedHashMap<String, Bitmap>(64, 0.75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Bitmap> eldest) { return size() > 60; }
    };
    private final Set<String> loading = new HashSet<>();
    private int posterW, posterH;
    private Runnable listener;

    private Genres(Context c) {
        context = c;
        cache = c.getSharedPreferences("genres", Context.MODE_PRIVATE);
        for (int g = 0; g < GENRES.length; g++) {
            lists[g] = new ArrayList<>();
            try {
                if (Ui.language.equals(cache.getString("language", ""))) {
                    lists[g] = parse(new JSONArray(cache.getString("g" + g, "[]")));
                    fetchedAt[g] = cache.getLong("at" + g, 0);
                }
            } catch (Exception ignored) {
            }
        }
    }

    void setListener(Runnable l) { listener = l; }

    static int pages() { return GENRES.length / 2; }

    /** "Explore Comedies", in the app's language. */
    static String label(int genre) {
        String[] g = GENRES[genre];
        return Ui.tr(g[0], g[1], g[2]);
    }

    List<Recs.Pick> items(int genre) {
        return genre < GENRES.length ? lists[genre] : new ArrayList<>();
    }

    boolean loading(int page) {
        for (int g = 2 * page; g < 2 * page + 2 && g < GENRES.length; g++) if (fetching.contains(g)) return true;
        return false;
    }

    /** The page's two genres and the next page's, fetched if they're missing or a day old. */
    void refresh(int page) {
        if (!Recs.hasKey(context)) return;
        long now = System.currentTimeMillis();
        if (failedAt != 0 && now - failedAt >= 0 && now - failedAt < 10 * 60_000L) return;
        for (int g = 2 * page; g < 2 * page + 4 && g < GENRES.length; g++) {
            boolean stale = lists[g].isEmpty() || now - fetchedAt[g] > EVERY || now < fetchedAt[g];
            if (!stale || fetching.contains(g)) continue;
            fetching.add(g);
            final int genre = g;
            io.execute(() -> {
                try {
                    List<Recs.Pick> list = fetch(genre);
                    if (list.isEmpty()) throw new IOException("no titles");
                    lists[genre] = list;
                    fetchedAt[genre] = System.currentTimeMillis();
                    cache.edit().putString("g" + genre, save(list).toString()).putLong("at" + genre, fetchedAt[genre])
                            .putString("language", Ui.language).apply();
                } catch (Exception e) {
                    failedAt = System.currentTimeMillis();
                    AppLog.w("genres: " + GENRES[genre][1] + ": " + e.getMessage());
                } finally {
                    main.post(() -> {
                        fetching.remove(genre);
                        if (listener != null) listener.run();
                    });
                }
            });
        }
    }

    /** Per installed service and kind, the genre's most popular titles; then they take turns, without repeats. */
    private List<Recs.Pick> fetch(int genre) throws Exception {
        Recs recs = Recs.get(context);
        String[] spec = GENRES[genre];
        List<List<Recs.Pick>> sources = new ArrayList<>();
        for (Object[] s : SERVICES) {
            if (HomeApps.launchIntent(context.getPackageManager(), (String) s[2]) == null) continue;
            String common = "&language=" + Recs.lang() + "&watch_region=" + REGION + "&with_watch_providers=" + s[0]
                    + "&with_watch_monetization_types=flatrate&include_adult=false&sort_by=popularity.desc";
            for (int k = 0; k < 2; k++) {
                String query = spec[3 + k];
                if (query == null) continue;
                String kind = k == 0 ? "movie" : "tv";
                JSONArray results = recs.get("/discover/" + kind + "?" + query + common
                        + (k == 0 ? "&vote_count.gte=50" : "&vote_count.gte=20")).getJSONArray("results");
                List<Recs.Pick> list = new ArrayList<>();
                for (int i = 0; i < results.length(); i++) {
                    Recs.Pick p = pick(results.getJSONObject(i), kind);
                    if (p == null) continue;
                    p.app = (String) s[1];
                    p.pkg = (String) s[2];
                    p.meta = p.meta + Recs.serviceName(p);
                    list.add(p);
                }
                sources.add(list);
            }
        }
        List<Recs.Pick> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; out.size() < PER_GENRE && i < 20; i++) {
            for (List<Recs.Pick> list : sources) {
                if (i < list.size() && out.size() < PER_GENRE && seen.add(Recs.key(list.get(i)))) out.add(list.get(i));
            }
        }
        return out;
    }

    /** A title with a poster; meta starts as "2025 · " (the service is added after). */
    private static Recs.Pick pick(JSONObject o, String kind) {
        String poster = o.optString("poster_path", "");
        if (poster.isEmpty() || "null".equals(poster)) return null;
        Recs.Pick p = new Recs.Pick();
        p.id = o.optInt("id");
        p.kind = kind;
        p.title = o.optString(kind.equals("movie") ? "title" : "name");
        p.poster = poster;
        String date = o.optString(kind.equals("movie") ? "release_date" : "first_air_date", "");
        p.meta = date.length() >= 4 ? date.substring(0, 4) + "  ·  " : "";
        p.detailed = true;
        return p;
    }

    private static JSONArray save(List<Recs.Pick> list) throws Exception {
        JSONArray a = new JSONArray();
        for (Recs.Pick p : list) {
            a.put(new JSONObject().put("id", p.id).put("kind", p.kind).put("title", p.title).put("poster", p.poster)
                    .put("meta", p.meta).put("app", p.app).put("pkg", p.pkg));
        }
        return a;
    }

    private static List<Recs.Pick> parse(JSONArray a) {
        List<Recs.Pick> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.optJSONObject(i);
            if (o == null) continue;
            Recs.Pick p = new Recs.Pick();
            p.id = o.optInt("id");
            p.kind = o.optString("kind");
            p.title = o.optString("title");
            p.poster = o.optString("poster");
            p.meta = o.optString("meta");
            p.app = o.optString("app");
            p.pkg = o.optString("pkg");
            p.detailed = true;
            out.add(p);
        }
        return out;
    }

    // MARK: - Posters

    /** The poster at w × h if it's ready (null: it loads in the background and the listener runs when it's in). */
    Bitmap poster(Recs.Pick p, int w, int h) {
        if (p == null || p.poster == null || p.poster.isEmpty()) return null;
        synchronized (posters) {
            if (w != posterW || h != posterH) {
                posters.clear();
                posterW = w;
                posterH = h;
            }
            Bitmap b = posters.get(p.poster);
            if (b != null) return b;
            if (!loading.add(p.poster)) return null;
        }
        final String path = p.poster;
        images.execute(() -> {
            Bitmap b = null;
            try {
                b = load(path, w, h);
            } catch (IOException | RuntimeException | OutOfMemoryError e) {
                AppLog.w("poster " + path + ": " + e.getMessage());
            }
            Bitmap ready = b;
            synchronized (posters) {
                loading.remove(path);
                if (ready != null && w == posterW && h == posterH) posters.put(path, ready);
            }
            if (ready != null) main.post(() -> { if (listener != null) listener.run(); });
        });
        return null;
    }

    /** From disk (or TMDB, kept on disk), cropped to w:h and reduced to exactly w × h. */
    private Bitmap load(String path, int w, int h) throws IOException {
        File dir = new File(context.getCacheDir(), "posters"), f = new File(dir, path.replace("/", ""));
        if (!f.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
            File part = new File(f.getPath() + "." + Thread.currentThread().getId() + ".part");
            HttpURLConnection c = (HttpURLConnection) new URL(POSTERS + path).openConnection();
            c.setConnectTimeout(8000);
            c.setReadTimeout(15000);
            try (InputStream in = c.getInputStream(); FileOutputStream out = new FileOutputStream(part)) {
                byte[] buffer = new byte[16384];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            } finally {
                c.disconnect();
            }
            if (!part.renameTo(f) && !f.exists()) throw new IOException("couldn't save the poster");
            prune(dir);
        }
        Bitmap src = BitmapFactory.decodeFile(f.getPath());
        if (src == null) {
            //noinspection ResultOfMethodCallIgnored
            f.delete();
            throw new IOException("unreadable poster");
        }
        float ratio = (float) w / h;
        int cw = Math.min(src.getWidth(), Math.round(src.getHeight() * ratio));
        int ch = Math.min(src.getHeight(), Math.round(cw / ratio));
        Bitmap crop = Bitmap.createBitmap(src, (src.getWidth() - cw) / 2, (src.getHeight() - ch) / 2, cw, ch);
        if (crop != src) src.recycle();
        Bitmap out = HomeView.downscale(crop, w, h);
        if (out != crop) crop.recycle();
        return Backdrop.gpu(out);
    }

    /** Keeps the 160 most recent posters on disk (a few pages of every genre). */
    private static void prune(File dir) {
        File[] files = dir.listFiles();
        if (files == null || files.length <= 160) return;
        Arrays.sort(files, (a, b) -> Long.compare(a.lastModified(), b.lastModified()));
        for (int i = 0; i < files.length - 160; i++) {
            //noinspection ResultOfMethodCallIgnored
            files[i].delete();
        }
    }
}
