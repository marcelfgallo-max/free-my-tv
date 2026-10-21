package io.freemytv;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Continue watching: what the streaming apps publish to Android TV's Play Next row (watch_next_program in the TV
 * provider: Disney+, Max, Prime Video), plus Netflix's continue-watching title, which Netflix puts first in its own
 * channel instead. Other apps' rows are readable with READ_TV_LISTINGS (granted through the TV's ADB when switching),
 * and only the ones they mark as searchable. Each item opens with the app's own link.
 */
final class WatchNext {
    private static final Uri WATCH_NEXT = Uri.parse("content://android.media.tv/watch_next_program");
    private static final Uri PREVIEW = Uri.parse("content://android.media.tv/preview_program");
    private static final String NETFLIX = "com.netflix.ninja";
    private static final int WATCHLIST = 3, NEW = 2;     // TvContract.WatchNextPrograms.WATCH_NEXT_TYPE_*
    private static final long MAX_AGE = 365L * 24 * 3600_000L, EVERY = 30_000L;
    static final int MAX = 10;

    private static WatchNext instance;

    static synchronized WatchNext get(Context c) {
        if (instance == null) instance = new WatchNext(c.getApplicationContext());
        return instance;
    }

    private final Context context;
    private final SharedPreferences seen;
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());
    private volatile List<Recs.Pick> items = new ArrayList<>();
    private volatile long readAt;
    private volatile boolean reading;
    private Runnable listener;

    private WatchNext(Context c) {
        context = c;
        seen = c.getSharedPreferences("watchnext", Context.MODE_PRIVATE);
    }

    /** The home screen, notified on the main thread when the list changes. */
    void setListener(Runnable l) { listener = l; }

    /** Most recent first, up to MAX. */
    List<Recs.Pick> items() { return items; }

    static boolean allowed(Context c) {
        return c.checkSelfPermission("android.permission.READ_TV_LISTINGS") == PackageManager.PERMISSION_GRANTED;
    }

    /** Reads the provider again, in the background, at most every 30 s. */
    void refresh() {
        long now = System.currentTimeMillis();
        if (reading || !allowed(context) || (now - readAt < EVERY && now >= readAt)) return;
        reading = true;
        readAt = now;
        io.execute(() -> {
            try {
                List<Recs.Pick> list = read();
                if (!same(list, items)) {
                    items = list;
                    main.post(() -> { if (listener != null) listener.run(); });
                }
            } catch (RuntimeException e) {
                AppLog.w("continue watching: " + e);
            } finally {
                reading = false;
            }
        });
    }

    private List<Recs.Pick> read() {
        long now = System.currentTimeMillis();
        List<Object[]> found = new ArrayList<>();     // {time, pick}
        Set<String> titles = new HashSet<>();
        PackageManager pm = context.getPackageManager();
        // The provider takes no selection from other apps (SecurityException): everything is read and filtered here.
        String[] columns = {"package_name", "title", "episode_title", "season_display_number", "episode_display_number",
                "watch_next_type", "last_engagement_time_utc_millis", "last_playback_position_millis", "duration_millis",
                "intent_uri", "poster_art_uri"};
        try (Cursor c = context.getContentResolver().query(WATCH_NEXT, columns, null, null, null)) {
            while (c != null && c.moveToNext()) {
                String pkg = c.getString(0), title = c.getString(1), intent = c.getString(9);
                long at = c.isNull(6) ? 0 : c.getLong(6);
                int type = c.isNull(5) ? 0 : c.getInt(5);
                if (pkg == null || title == null || intent == null || type == WATCHLIST || at <= 0 || now - at > MAX_AGE) continue;
                if (pm.getLaunchIntentForPackage(pkg) == null && pm.getLeanbackLaunchIntentForPackage(pkg) == null) continue;
                if (!titles.add(title.toLowerCase(Locale.ROOT))) continue;
                Recs.Pick p = pick(pkg, title, intent, c.getString(10));
                p.meta = meta(p, c.getString(2), c.getString(3), c.getString(4), type);
                long position = c.isNull(7) ? 0 : c.getLong(7), duration = c.isNull(8) ? 0 : c.getLong(8);
                if (position > 0 && duration > 0) p.progress = Math.min(1, (float) position / duration);
                found.add(new Object[] {at, p});
            }
        }
        // Netflix: its channel's titles that it lists as continue watching. Each one is dated by when it first showed
        // up there (Netflix gives no date), so a title you just started moves to the front.
        Set<String> netflix = new HashSet<>();
        try (Cursor c = context.getContentResolver().query(PREVIEW, new String[] {"package_name", "title", "intent_uri", "poster_art_uri"}, null, null, null)) {
            while (c != null && c.moveToNext()) {
                String pkg = c.getString(0), title = c.getString(1), intent = c.getString(2);
                if (!NETFLIX.equals(pkg) || title == null || intent == null || !intent.contains("listContext%3DcontinueWatching")) continue;
                netflix.add(title);
                if (!titles.add(title.toLowerCase(Locale.ROOT))) continue;
                long at = seen.getLong(title, 0);
                if (at == 0) {
                    at = now;
                    seen.edit().putLong(title, at).apply();
                }
                Recs.Pick p = pick(pkg, title, intent, c.getString(3));
                p.meta = meta(p, null, null, null, 0);
                found.add(new Object[] {at, p});
            }
        }
        // Netflix's titles that left its list are forgotten (watching one again later dates it anew).
        SharedPreferences.Editor gone = seen.edit();
        for (String title : seen.getAll().keySet()) if (!netflix.contains(title)) gone.remove(title);
        gone.apply();
        Collections.sort(found, (a, b) -> Long.compare((Long) b[0], (Long) a[0]));
        List<Recs.Pick> out = new ArrayList<>();
        for (int i = 0; i < found.size() && i < MAX; i++) out.add((Recs.Pick) found.get(i)[1]);
        return out;
    }

    private static Recs.Pick pick(String pkg, String title, String intent, String image) {
        Recs.Pick p = new Recs.Pick();
        p.kind = "next";
        p.id = (pkg + "|" + title).hashCode();
        p.title = title;
        p.pkg = pkg;
        p.app = Recs.serviceKey(pkg);
        p.intent = intent;
        p.detailed = true;
        // Disney+ publishes a 544 px image; its image server makes the size asked for.
        if (image != null && image.contains("bamgrid.com")) image = image.replaceAll("width=\\d+", "width=1920").replaceAll("height=\\d+", "height=1080");
        p.backdrop = image;
        return p;
    }

    /** "T3:E8 · Ice Chips · Disney+", "New episodes · Prime Video", "Netflix". */
    private static String meta(Recs.Pick p, String episode, String season, String number, int type) {
        List<String> parts = new ArrayList<>();
        if (season != null && number != null) {
            parts.add(Ui.ru ? season + " сезон, " + number + " серия"
                    : (Ui.en || Ui.de ? "S" : "T") + season + ":E" + number);
        }
        if (episode != null && !episode.isEmpty()) parts.add(episode);
        if (parts.isEmpty() && type == NEW) parts.add(Ui.tr("Episódios novos", "New episodes", "Episodios nuevos"));
        String service = Recs.serviceName(p);
        if (service != null && !service.isEmpty()) parts.add(service);
        return String.join(" · ", parts);
    }

    private static boolean same(List<Recs.Pick> a, List<Recs.Pick> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            Recs.Pick x = a.get(i), y = b.get(i);
            if (!Recs.key(x).equals(Recs.key(y)) || !x.meta.equals(y.meta) || x.progress != y.progress
                    || !String.valueOf(x.backdrop).equals(String.valueOf(y.backdrop))) return false;
        }
        return true;
    }

    /**
     * Opens the title in its app with the link the app published: an "intent:" (Netflix, Prime Video) as it is, a web
     * link (Disney+, Max) in that app. False if the app didn't take it.
     */
    static boolean open(Activity a, Recs.Pick p) {
        try {
            Intent i;
            if (p.intent.startsWith("intent:") || p.intent.contains("#Intent;")) {
                i = Intent.parseUri(p.intent, Intent.URI_INTENT_SCHEME);
            } else {
                i = new Intent(Intent.ACTION_VIEW, Uri.parse(p.intent));
            }
            i.setPackage(p.pkg);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            a.startActivity(i);
            return true;
        } catch (URISyntaxException | ActivityNotFoundException | SecurityException e) {
            AppLog.w("continue watching: couldn't open " + p.title + ": " + e);
            return false;
        }
    }
}
