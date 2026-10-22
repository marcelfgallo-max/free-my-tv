package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Your team (team= in config.properties, its ESPN id; São Paulo is 2026): the last result and the next game, or the
 * score while it's on, from ESPN's free API (no key; every competition at once). Every 15 minutes, every minute from
 * 15 minutes before kick-off to the end. The crests are kept on disk. Only fetched while a screen that shows it asks.
 */
final class Team {
    private static final String API = "https://site.api.espn.com/apis/site/v2/sports/soccer/all/teams/";
    private static final long EVERY = 15 * 60_000L, LIVE_EVERY = 60_000L;
    private static final int CREST_PX = 64;

    interface Listener { void onTeam(); }

    /** One side of a game: its short name in caps, its score ("" before the game) and crest (null until loaded). */
    static final class Side {
        String id = "", name = "", score = "";
        volatile Bitmap crest;
    }

    /** A game: home and away, live or not, and what to say under it. */
    static final class Game {
        final Side home = new Side(), away = new Side();
        boolean live, ours;     // ours: our team plays at home
        String score = "";      // "1–0" (made once, not every frame)
        String tag = "", rest = "";   // "NEXT" + "SAT 24/10 16:30 · @ MIRASSOL", or "LIVE" + "67'"
    }

    private static Team instance;

    static synchronized Team get(Context c) {
        if (instance == null) instance = new Team(c.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<Listener> listeners = new ArrayList<>();
    private final Map<String, Bitmap> crests = new HashMap<>();
    /** The last result and the next game (or the one on now); null until fetched or without a team. */
    volatile Game last, next;
    private volatile long fetchedAt, kickoff;
    private volatile boolean fetching;

    private Team(Context c) {
        context = c;
    }

    static boolean on() {
        return !Config.TEAM.trim().isEmpty();
    }

    void addListener(Listener l) { if (!listeners.contains(l)) listeners.add(l); }
    void removeListener(Listener l) { listeners.remove(l); }

    /** Fetches again if stale: every 15 minutes, or every minute around a game. */
    void refreshIfStale() {
        if (!on() || fetching) return;
        long now = System.currentTimeMillis();
        Game n = next;
        boolean close = n != null && (n.live || (kickoff > 0 && now > kickoff - 15 * 60_000L && now < kickoff + 3 * 3600_000L));
        if (now - fetchedAt < (close ? LIVE_EVERY : EVERY) && now >= fetchedAt) return;
        fetching = true;
        io.execute(() -> {
            try {
                String id = Config.TEAM.trim();
                Game played = lastGame(new JSONObject(get(API + id + "/schedule")), id);
                Game coming = nextGame(new JSONObject(get(API + id + "/schedule?fixture=true")), id);
                for (Game g : new Game[] {played, coming}) {
                    if (g == null) continue;
                    g.home.crest = crest(g.home.id);
                    g.away.crest = crest(g.away.id);
                }
                last = played;
                next = coming;
                fetchedAt = System.currentTimeMillis();
                main.post(() -> { for (Listener l : new ArrayList<>(listeners)) l.onTeam(); });
            } catch (Exception e) {
                fetchedAt = System.currentTimeMillis() - EVERY + 2 * 60_000L;   // try again in 2 minutes
                AppLog.w("team: " + e.getMessage());
            } finally {
                fetching = false;
            }
        });
    }

    /** The most recent game that ended. */
    private static Game lastGame(JSONObject o, String id) throws Exception {
        JSONArray events = o.optJSONArray("events");
        JSONObject best = null;
        String bestDate = "";
        for (int i = 0; events != null && i < events.length(); i++) {
            JSONObject e = events.getJSONObject(i);
            JSONObject type = competition(e).getJSONObject("status").getJSONObject("type");
            if (!type.optBoolean("completed")) continue;
            String date = e.optString("date");
            if (date.compareTo(bestDate) > 0) {
                bestDate = date;
                best = e;
            }
        }
        return best == null ? null : game(best, id);
    }

    /** The next game, or the one on now. */
    private Game nextGame(JSONObject o, String id) throws Exception {
        JSONArray events = o.optJSONArray("events");
        if (events == null || events.length() == 0) {
            kickoff = 0;
            return null;
        }
        JSONObject e = events.getJSONObject(0);
        Game g = game(e, id);
        JSONObject status = competition(e).getJSONObject("status");
        long at = parseDate(e.optString("date"));
        kickoff = at;
        g.live = "in".equals(status.getJSONObject("type").optString("state"));
        if (g.live) {
            g.tag = Ui.tr("AO VIVO", "LIVE", "EN VIVO");
            g.rest = status.optString("displayClock");
        } else {
            Side other = g.ours ? g.away : g.home;
            String when = new SimpleDateFormat(Ui.h12() ? "EEE dd/MM  h:mm a" : "EEE dd/MM  HH:mm", Ui.locale()).format(new Date(at));
            g.tag = Ui.tr("PRÓXIMO", "NEXT", "PRÓXIMO");
            g.rest = when.replace(".", "").toUpperCase(Ui.locale()) + "  ·  " + (g.ours ? "VS " : "@ ") + other.name;
        }
        return g;
    }

    private static JSONObject competition(JSONObject e) throws Exception {
        return e.getJSONArray("competitions").getJSONObject(0);
    }

    private static Game game(JSONObject e, String id) throws Exception {
        Game g = new Game();
        JSONArray sides = competition(e).getJSONArray("competitors");
        for (int i = 0; i < sides.length(); i++) {
            JSONObject c = sides.getJSONObject(i);
            JSONObject team = c.getJSONObject("team");
            boolean home = "home".equals(c.optString("homeAway"));
            Side s = home ? g.home : g.away;
            s.id = team.optString("id");
            s.name = team.optString("shortDisplayName", team.optString("displayName")).toUpperCase(Ui.locale());
            // The score is an object in results ({"displayValue": "1"}) and a plain string in a live game.
            Object score = c.opt("score");
            s.score = score instanceof JSONObject ? ((JSONObject) score).optString("displayValue")
                    : score == null || score == JSONObject.NULL ? "" : String.valueOf(score);
            if (home && id.equals(s.id)) g.ours = true;
        }
        g.score = g.home.score.isEmpty() ? "×" : g.home.score + "–" + g.away.score;
        return g;
    }

    private static long parseDate(String iso) {
        try {
            SimpleDateFormat f = new SimpleDateFormat(iso.length() > 17 ? "yyyy-MM-dd'T'HH:mm:ss'Z'" : "yyyy-MM-dd'T'HH:mm'Z'", Locale.US);
            f.setTimeZone(TimeZone.getTimeZone("UTC"));
            return f.parse(iso).getTime();
        } catch (Exception e) {
            return 0;
        }
    }

    /** A team's crest at 64 px: from memory, from disk, or downloaded once (ESPN's 500 px logo, reduced). */
    private Bitmap crest(String id) {
        if (id.isEmpty()) return null;
        synchronized (crests) {
            if (crests.containsKey(id)) return crests.get(id);
        }
        File f = new File(new File(context.getCacheDir(), "crests"), id + ".png");
        Bitmap b = BitmapFactory.decodeFile(f.getPath());
        if (b == null) {
            try {
                HttpURLConnection c = (HttpURLConnection) new URL("https://a.espncdn.com/i/teamlogos/soccer/500/" + id + ".png").openConnection();
                c.setConnectTimeout(6000);
                c.setReadTimeout(10000);
                Bitmap big;
                try (InputStream in = c.getInputStream()) {
                    big = BitmapFactory.decodeStream(in);
                } finally {
                    c.disconnect();
                }
                if (big != null) {
                    float s = (float) CREST_PX / Math.max(big.getWidth(), big.getHeight());
                    b = HomeView.downscale(big, Math.round(big.getWidth() * s), Math.round(big.getHeight() * s));
                    //noinspection ResultOfMethodCallIgnored
                    f.getParentFile().mkdirs();
                    try (FileOutputStream out = new FileOutputStream(f)) {
                        b.compress(Bitmap.CompressFormat.PNG, 100, out);
                    }
                }
            } catch (IOException e) {
                AppLog.w("team: crest " + id + ": " + e.getMessage());
            }
        }
        if (b != null) {
            synchronized (crests) {
                crests.put(id, b);   // a failed download isn't kept: the next refresh tries again
            }
        }
        return b;
    }

    /**
     * Draws the block right-aligned at `right`: on the first line the game on now (or the last result) as
     * [crest] HOME 1–0 AWAY [crest]; on the second the tag in `tag`'s color ("NEXT", "LIVE") and the rest in `small`'s,
     * then a hairline under both. Returns the block's left edge, or `right` with nothing to show.
     */
    static float draw(Canvas c, float right, float y1, float y2, float k, Paint big, Paint small, Paint tag, Paint image, Paint rule) {
        Team t = instance;
        if (t == null || !on()) return right;
        Game next = t.next, top = next != null && next.live ? next : t.last;
        float left = right, crest = 32 * k, gap = 12 * k, cap = Ui.cap(big);
        if (top != null) {
            float x = right;
            x = crestAt(c, top.away.crest, x, y1 - cap / 2, crest, image) - gap;
            x -= big.measureText(top.away.name);
            c.drawText(top.away.name, x, y1, big);
            x -= 18 * k + big.measureText(top.score);
            c.drawText(top.score, x, y1, big);
            x -= 18 * k + big.measureText(top.home.name);
            c.drawText(top.home.name, x, y1, big);
            x = crestAt(c, top.home.crest, x - gap, y1 - cap / 2, crest, image);
            left = Math.min(left, x);
        }
        if (next != null && !next.tag.isEmpty()) {
            float x = right - small.measureText(next.rest);
            c.drawText(next.rest, x, y2, small);
            x -= 16 * k + tag.measureText(next.tag);
            c.drawText(next.tag, x, y2, tag);
            left = Math.min(left, x);
        }
        if (left < right) {
            float y = Math.round(y2 + 14 * k);
            c.drawRect(Math.round(left), y, Math.round(right), y + Math.max(1, Math.round(k)), rule);
        }
        return left;
    }

    private static final android.graphics.RectF CREST_BOX = new android.graphics.RectF();   // drawing is on the UI thread

    /** A crest `size` tall, its right edge at `right`, centered on `cy`; returns its left edge. */
    private static float crestAt(Canvas c, Bitmap b, float right, float cy, float size, Paint image) {
        if (b == null) return right;
        float w = size * b.getWidth() / b.getHeight();
        CREST_BOX.set(right - w, cy - size / 2, right, cy + size / 2);
        c.drawBitmap(b, null, CREST_BOX, image);
        return right - w;
    }

    private static String get(String url) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(6000);
        c.setReadTimeout(10000);
        c.setRequestProperty("User-Agent", "FreeMyTV");
        try {
            if (c.getResponseCode() != 200) throw new IOException("HTTP " + c.getResponseCode());
            try (InputStream in = c.getInputStream()) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
                return out.toString(StandardCharsets.UTF_8.name());
            }
        } finally {
            c.disconnect();
        }
    }
}
