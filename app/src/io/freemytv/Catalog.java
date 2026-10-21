package io.freemytv;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The catalog tab: the most popular movies and shows on each streaming service the TV has (TMDB's watch providers for
 * the region), the services taking turns. A title opens on its own page where the service's id is known: Netflix and
 * Apple TV, through Wikidata (TMDB → Wikidata item → the service's id). The others (Disney+, Prime Video, Max) only
 * take ids that Wikidata doesn't have: their titles open the app.
 */
final class Catalog {
    /** TMDB provider id, HomeApps key, package, brand color (the badge on the card). */
    private static final Object[][] SERVICES = {
        {8, "netflix", "com.netflix.ninja", 0xFFE50914},
        {119, "prime", "com.amazon.amazonvideo.livingroom", 0xFF1A98FF},
        {337, "disney", "com.disney.disneyplus", 0xFF1838C8},
        {1899, "max", "com.wbd.stream", 0xFF6C3BE0},
        {350, "appletv", "com.apple.atve.androidtv.appletv", 0xFF1C1C1E},
    };
    static final int MAX = 30;
    private static final long EVERY = 24 * 3600_000L;
    private static final String REGION = Config.TMDB_REGION.trim().toUpperCase(Locale.ROOT);

    private static Catalog instance;

    static synchronized Catalog get(Context c) {
        if (instance == null) instance = new Catalog(c.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final SharedPreferences cache;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile List<Recs.Pick> items = new ArrayList<>();
    private volatile long fetchedAt, failedAt;
    private volatile boolean fetching;
    private Runnable listener;

    private Catalog(Context c) {
        context = c;
        cache = c.getSharedPreferences("catalog", Context.MODE_PRIVATE);
        try {
            if (Ui.language.equals(cache.getString("language", ""))) {
                items = parse(new JSONArray(cache.getString("items", "[]")));
                fetchedAt = cache.getLong("at", 0);
            }
        } catch (Exception ignored) {
        }
    }

    void setListener(Runnable l) { listener = l; }

    List<Recs.Pick> items() { return items; }

    boolean loading() { return fetching || (items.isEmpty() && failedAt == 0); }

    /** Fetches the list again once a day (or in another language); not again for 10 min after a failure. */
    void refresh() {
        if (!Recs.hasKey(context) || fetching) return;
        long now = System.currentTimeMillis();
        boolean stale = items.isEmpty() || now - fetchedAt > EVERY || now < fetchedAt || !Ui.language.equals(cache.getString("language", ""));
        if (!stale || (failedAt != 0 && now - failedAt >= 0 && now - failedAt < 10 * 60_000L)) return;
        fetching = true;
        io.execute(() -> {
            try {
                List<Recs.Pick> list = fetch();
                if (list.isEmpty()) throw new IOException("no titles");
                items = list;
                fetchedAt = System.currentTimeMillis();
                failedAt = 0;
                cache.edit().putString("items", save(list).toString()).putLong("at", fetchedAt).putString("language", Ui.language).apply();
            } catch (Exception e) {
                failedAt = System.currentTimeMillis();
                AppLog.w("catalog: " + e.getMessage());
            } finally {
                fetching = false;
                main.post(() -> { if (listener != null) listener.run(); });
            }
        });
    }

    /** Per installed service, its most popular movies and shows; then the services take turns, movie and show alternating. */
    private List<Recs.Pick> fetch() throws Exception {
        Recs recs = Recs.get(context);
        List<List<Recs.Pick>> perService = new ArrayList<>();
        for (Object[] s : SERVICES) {
            if (HomeApps.launchIntent(context.getPackageManager(), (String) s[2]) == null) continue;
            String common = "?language=" + Recs.lang() + "&watch_region=" + REGION + "&with_watch_providers=" + s[0]
                    + "&with_watch_monetization_types=flatrate&include_adult=false&sort_by=popularity.desc";
            List<Recs.Pick> movies = new ArrayList<>(), shows = new ArrayList<>(), mixed = new ArrayList<>();
            JSONArray m = recs.get("/discover/movie" + common + "&vote_count.gte=50").getJSONArray("results");
            JSONArray t = recs.get("/discover/tv" + common + "&vote_count.gte=20").getJSONArray("results");
            for (int i = 0; i < m.length(); i++) Recs.add(movies, m.getJSONObject(i), "movie");
            for (int i = 0; i < t.length(); i++) Recs.add(shows, t.getJSONObject(i), "tv");
            for (int i = 0; i < Math.max(movies.size(), shows.size()); i++) {
                if (i < shows.size()) mixed.add(shows.get(i));
                if (i < movies.size()) mixed.add(movies.get(i));
            }
            for (Recs.Pick p : mixed) {
                p.app = (String) s[1];
                p.pkg = (String) s[2];
                p.meta = (p.kind.equals("movie") ? Ui.tr("Filme", "Movie") : Ui.tr("Série", "Series")) + " · " + Recs.serviceName(p);
                p.detailed = true;
            }
            perService.add(mixed);
        }
        List<Recs.Pick> out = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (int i = 0; out.size() < MAX && i < 40; i++) {
            for (List<Recs.Pick> list : perService) {
                if (i < list.size() && out.size() < MAX && seen.add(Recs.key(list.get(i)))) out.add(list.get(i));
            }
        }
        return out;
    }

    /** The badge color of the title's service. */
    static int color(Recs.Pick p) {
        for (Object[] s : SERVICES) if (s[1].equals(p.app)) return (Integer) s[3];
        return 0xFF1C1C1E;
    }

    // MARK: - Opening a title

    /** Looks up the links of these titles ahead of time (the visible page), so OK opens at once. */
    void prefetch(List<Recs.Pick> list) {
        for (Recs.Pick p : list) {
            if (!exact(p) || cache.contains("link:" + Recs.key(p))) continue;
            io.execute(() -> link(p));
        }
    }

    /** The intent that opens the title's own page, or null (open the app): handed over on the main thread. */
    void open(Recs.Pick p, Consumer<Intent> done) {
        if (!exact(p)) {
            done.accept(null);
            return;
        }
        io.execute(() -> {
            Intent i = link(p);
            main.post(() -> done.accept(i));
        });
    }

    private static boolean exact(Recs.Pick p) {
        return "netflix".equals(p.app) || "appletv".equals(p.app);
    }

    /** Wikidata properties: Netflix ID, Apple TV movie ID, Apple TV show ID. */
    private Intent link(Recs.Pick p) {
        String key = "link:" + Recs.key(p);
        String id = cache.getString(key, null);
        if (id == null) {
            id = "";
            try {
                String item = Recs.get(context).get("/" + p.kind + "/" + p.id + "/external_ids").optString("wikidata_id", "");
                String property = p.app.equals("netflix") ? "P1874" : p.kind.equals("movie") ? "P9586" : "P9751";
                if (item.startsWith("Q")) id = claim(item, property);
                cache.edit().putString(key, id).apply();    // "" = Wikidata has none: the app opens
            } catch (Exception e) {
                AppLog.w("catalog link “" + p.title + "”: " + e.getMessage());
                return null;    // not saved: tried again next time
            }
        }
        if (id.isEmpty()) return null;
        Intent i;
        if (p.app.equals("netflix")) {
            // As Netflix's own links: its main screen and a source, or it only comes to the front without the title.
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("http://www.netflix.com/title/" + id));
            i.setClassName(p.pkg, p.pkg + ".MainActivity");
            i.addFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            i.putExtra("source_type", 27);
        } else {
            i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://tv.apple.com/" + REGION.toLowerCase(Locale.ROOT) + "/"
                    + (p.kind.equals("movie") ? "movie" : "show") + "/" + slug(p.title) + "/" + id));
            i.setPackage(p.pkg);
        }
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return i;
    }

    /** The first value of a Wikidata item's property, or "". */
    private static String claim(String item, String property) throws Exception {
        String url = "https://www.wikidata.org/w/api.php?action=wbgetclaims&format=json&entity=" + item + "&property=" + property;
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(8000);
        c.setReadTimeout(8000);
        c.setRequestProperty("User-Agent", "FreeMyTV (https://freemytv.io)");
        try {
            if (c.getResponseCode() != 200) throw new IOException("Wikidata returned HTTP " + c.getResponseCode());
            JSONObject all = new JSONObject(read(c.getInputStream())).optJSONObject("claims");
            JSONArray claims = all == null ? null : all.optJSONArray(property);
            if (claims == null || claims.length() == 0) return "";
            JSONObject value = claims.getJSONObject(0).getJSONObject("mainsnak").optJSONObject("datavalue");
            return value == null ? "" : value.optString("value", "");
        } finally {
            c.disconnect();
        }
    }

    /** "Ruptura: Temporada 1" → "ruptura-temporada-1" (Apple only reads the id; the name just has to be there). */
    private static String slug(String title) {
        String s = Normalizer.normalize(title == null ? "" : title, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        s = s.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        return s.isEmpty() ? "title" : s;
    }

    private static String read(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        return out.toString(StandardCharsets.UTF_8.name());
    }

    // MARK: - Cache

    private static JSONArray save(List<Recs.Pick> list) throws Exception {
        JSONArray a = new JSONArray();
        for (Recs.Pick p : list) {
            a.put(new JSONObject().put("id", p.id).put("kind", p.kind).put("title", p.title).put("backdrop", p.backdrop)
                    .put("meta", p.meta).put("app", p.app).put("pkg", p.pkg));
        }
        return a;
    }

    private static List<Recs.Pick> parse(JSONArray a) throws Exception {
        List<Recs.Pick> out = new ArrayList<>();
        for (int i = 0; i < a.length(); i++) {
            JSONObject o = a.getJSONObject(i);
            Recs.Pick p = new Recs.Pick();
            p.id = o.getInt("id");
            p.kind = o.getString("kind");
            p.title = o.getString("title");
            p.backdrop = o.getString("backdrop");
            p.meta = o.optString("meta");
            p.app = o.optString("app");
            p.pkg = o.optString("pkg");
            p.detailed = true;
            out.add(p);
        }
        return out;
    }
}
