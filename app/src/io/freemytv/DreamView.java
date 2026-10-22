package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Choreographer;
import android.view.View;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * The screensaver, in the same style as the home screen:
 *  - the recommendations' photos in full screen, one every 30 s, with a slow zoom and pan (the "Ken Burns" effect of
 *    the Apple TV photos) and a crossfade between them; each photo is loaded 10% larger than the screen, so the zoom
 *    never upscales it;
 *  - top right, in Modern 80s: the date and the weather in caps on a hairline, the big tight clock and the compact
 *    prices;
 *  - bottom left, the photo's caption: the time-of-day label ("TONIGHT") on a hairline, the title in caps, and
 *    genre · runtime · service.
 * Dark scrims only where there is text, stronger where the photo is bright (Modern 80s has no text shadows). Any key
 * exits (as on Apple TV). Without photos (no TMDB key, or before the first fetch): the big clock on the Ember field.
 */
final class DreamView extends View implements Choreographer.FrameCallback, HomeData.Listener, Recs.Listener {
    private static final long SHOW_MS = 30_000, FADE_MS = 3_000, FIRST_MS = 1_600;
    private static final float ZOOM = 0.1f;
    /** Photos 10% larger than the screen (the zoom goes up to 1.1 without upscaling). */
    private static final int PW = 2112, PH = 1188;
    private static final float MARGIN = 96, RIGHT = 1824, TITLE_W = 900, META_BASE = 1000;
    /** The corner block: a caps label row on a hairline (date, weather), the clock under it. */
    private static final float CORNER_W = 720, DATE_BASE = 96, RULE_Y = 120, CLOCK_TOP = 150, CLOCK_SIZE = 220, QUOTES_GAP = 56;
    private static final int INK = 0xFFFFFFFF, MUTED = 0xB8FFFFFF, HAIRLINE = 0x8CFFFFFF;
    private static final double DRIFT_PERIOD_S = 600;

    /** One photo on screen: the image, its motion (zooming in or out, toward a corner) and its caption. */
    private static final class Slide {
        final Recs.Pick pick;
        final Bitmap image;
        final float lightCaption, lightClock;
        final boolean zoomIn;
        final float panX, panY;
        final Recording caption = Recording.create("caption");
        long startedAt;

        Slide(Recs.Pick pick, Bitmap image, float lightCaption, float lightClock, int n) {
            this.pick = pick;
            this.image = image;
            this.lightCaption = lightCaption;
            this.lightClock = lightClock;
            zoomIn = n % 2 == 0;
            // Toward one of the corners, alternating, without repeating the previous one.
            panX = (n / 2) % 2 == 0 ? 0.8f : -0.8f;
            panY = n % 3 == 0 ? -0.6f : 0.6f;
        }
    }

    private final Scene scene = new Scene(false);
    private final HomeData data;
    private final Recs recs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Recording content = Recording.create("clock");     // without photos: the clock over the aurora
    private final Recording chrome = Recording.create("corner");     // date, weather, prices and the clock's shadow
    private final float timeOffset = (float) (Math.random() * 300);
    private final Matrix matrix = new Matrix();
    private final float[] placeA = new float[3], placeB = new float[3];
    private final Paint photoPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint black = new Paint();
    private final Paint rule = new Paint();
    private final Paint crestPaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
    private Paint teamBig, teamSmall, teamTag;
    private final Team team;
    private final Team.Listener teamChanged = () -> {
        chromeDirty = true;
        invalidate();
    };
    private final Set<String> failed = new HashSet<>();     // Recs.key: a movie and a series can have the same ID
    private boolean running, dirty = true, chromeDirty = true;
    private boolean fallback;       // the aurora with the clock (no photos)
    private boolean decoding;
    private int generation, slides;
    private Slide current, incoming, prepared;
    private Bitmap field;           // the Ember field, without photos
    private final android.graphics.RectF fieldRect = new android.graphics.RectF();
    private long startedAt, firstAt, lastFrame, lastSky;
    private int w, h;
    private float k = 1;
    private long minute = -1;
    private String clock = "", date = "";
    private Paint datePaint, clockPaint, weatherPaint, weatherDim, quotePaint, stalePaint;
    private Paint cornerDate, cornerQuote, cornerStale, cornerClock, labelPaint, metaPaint;
    private TextPaint titlePaint;

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            data.refreshIfStale();
            recs.refreshIfStale();
            recs.prefetchAll();
            team.refreshIfStale();
            dirty = true;
            chromeDirty = true;
            prepareNext();
            handler.postDelayed(this, 60_000);
        }
    };

    DreamView(Context c) {
        super(c);
        data = HomeData.get(c);
        recs = Recs.get(c);
        team = Team.get(c);
        Shaders.dream(scene.palette);
        scene.setMotion(0.4f, 0.32f);
        scene.applyPalette();
        content.setUseCompositingLayer(true, null);
        black.setColor(0xFF000000);
        rule.setColor(HAIRLINE);
    }

    void start() {
        if (running) return;
        running = true;
        if (worker.isShutdown()) worker = Executors.newSingleThreadExecutor();
        startedAt = clock();
        lastFrame = 0;
        failed.clear();
        fallback = recs.showcase().isEmpty();
        loadField();
        data.addListener(this);
        recs.addListener(this);
        team.addListener(teamChanged);
        team.refreshIfStale();
        data.refreshIfStale();
        recs.refreshIfStale();
        recs.prefetchAll();
        prepareNext();
        handler.postDelayed(tick, 60_000);
        Choreographer.getInstance().postFrameCallback(this);
        invalidate();
    }

    void stop() {
        if (!running) return;
        running = false;
        generation++;
        decoding = false;
        Choreographer.getInstance().removeFrameCallback(this);
        handler.removeCallbacks(tick);
        data.removeListener(this);
        recs.removeListener(this);
        team.removeListener(teamChanged);
        // The photos stay in GPU memory until the garbage collector runs: release them now (after the last frame).
        final java.util.Set<Bitmap> gone = new java.util.HashSet<>();
        for (Slide s : new Slide[] {current, incoming, prepared}) {
            if (s == null) continue;
            gone.add(s.image);
        }
        if (!gone.isEmpty()) handler.postDelayed(() -> { for (Bitmap b : gone) b.recycle(); }, 1000);
        current = incoming = prepared = null;
        slides = 0;
    }

    /** The Ember field for the clock without photos, decoded off the UI thread once. */
    private void loadField() {
        if (field != null) return;
        android.content.res.AssetManager assets = getContext().getAssets();
        worker.execute(() -> {
            Bitmap b = null;
            try (java.io.InputStream in = assets.open("fields/ember.png")) {
                b = android.graphics.BitmapFactory.decodeStream(in);
            } catch (java.io.IOException | OutOfMemoryError e) {
                AppLog.w("screensaver: field: " + e);
            }
            Bitmap ready = b;
            handler.post(() -> {
                field = ready;
                invalidate();
            });
        });
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        worker.shutdownNow();
    }

    @Override public void onData() {
        dirty = true;
        chromeDirty = true;
        invalidate();
    }

    @Override public void onRecs() {
        if (running) prepareNext();
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        w = width;
        h = height;
        k = w / 1920f;
        scene.setSize(w, h);
        content.setPosition(0, 0, w, h);
        chrome.setPosition(0, 0, w, h);
        // Without photos: the clock on the Ember field.
        datePaint = Ui.text(Ui.displayMedium, 30 * k, INK);
        datePaint.setLetterSpacing(-0.01f);
        datePaint.setTextAlign(Paint.Align.CENTER);
        clockPaint = Ui.text(Ui.displayMedium, 300 * k, INK);
        clockPaint.setTextAlign(Paint.Align.CENTER);
        clockPaint.setLetterSpacing(-0.05f);
        weatherPaint = Ui.numbers(Ui.displayMedium, 30 * k, INK);
        weatherDim = Ui.numbers(Ui.regular, 28 * k, MUTED);
        quotePaint = Ui.numbers(Ui.regular, 24 * k, MUTED);
        stalePaint = Ui.text(Ui.regular, 20 * k, 0x80FFFFFF);
        stalePaint.setTextAlign(Paint.Align.CENTER);
        // With photos: the corner (date, weather, clock, prices) and the caption.
        cornerDate = Ui.numbers(Ui.displayMedium, 26 * k, INK);
        cornerDate.setLetterSpacing(-0.01f);
        cornerQuote = Ui.numbers(Ui.regular, 24 * k, INK);
        cornerStale = Ui.text(Ui.regular, 21 * k, MUTED);
        cornerClock = Ui.text(Ui.displayMedium, CLOCK_SIZE * k, INK);
        cornerClock.setLetterSpacing(-0.05f);
        labelPaint = Ui.text(Ui.displayMedium, 26 * k, INK);
        labelPaint.setLetterSpacing(-0.01f);
        titlePaint = new TextPaint(Ui.text(Ui.displayMedium, 76 * k, INK));
        titlePaint.setLetterSpacing(-0.035f);
        metaPaint = Ui.text(Ui.regular, 26 * k, MUTED);
        teamBig = Ui.text(Ui.displayMedium, 26 * k, INK);
        teamBig.setLetterSpacing(-0.01f);
        teamSmall = Ui.numbers(Ui.regular, 21 * k, MUTED);
        teamTag = Ui.text(Ui.regular, 21 * k, 0xFF5EDCFF);
        fieldRect.set(0, 0, w, h);
        dirty = true;
        chromeDirty = true;
        for (Slide s : new Slide[] {current, incoming, prepared}) if (s != null) recordCaption(s);
    }

    // MARK: - Photos

    /** Loads the next photo in the background (one at a time); once ready, it comes in at the right moment. */
    private void prepareNext() {
        if (!running || decoding || prepared != null) return;
        List<Recs.Pick> list = recs.showcase();
        if (list.isEmpty()) return;
        Slide last = incoming != null ? incoming : current;
        int start = 0;
        if (last != null) {
            for (int i = 0; i < list.size(); i++) {
                if (Recs.key(list.get(i)).equals(Recs.key(last.pick))) {
                    start = i + 1;
                    break;
                }
            }
        }
        Recs.Pick pick = null;
        for (int i = 0; i < list.size() && pick == null; i++) {
            Recs.Pick p = list.get((start + i) % list.size());
            if (!failed.contains(Recs.key(p))) pick = p;
        }
        if (pick == null) {
            if (current == null) fallback = true;
            return;
        }
        if (last != null && Recs.key(pick).equals(Recs.key(last.pick))) {
            // Only one photo: the same one again, with a different motion.
            prepared = newSlide(pick, last.image, last.lightCaption, last.lightClock);
            return;
        }
        File file = recs.readyImage(pick);
        if (file == null) {
            failed.add(Recs.key(pick));   // the image is gone from disk: try the next one
            prepareNext();
            return;
        }
        decoding = true;
        final int gen = generation;
        final Recs.Pick chosen = pick;
        worker.execute(() -> {
            Bitmap image = null;
            float[] light = new float[2];
            try {
                image = Backdrop.gpu(withScrims(Backdrop.photo(file, PW, PH, light), light[0], light[1]));
            } catch (Exception | OutOfMemoryError e) {
                AppLog.w("screensaver: photo for “" + chosen.title + "”: " + e.getMessage());
            }
            final Bitmap ready = image;
            handler.post(() -> {
                if (gen != generation) {
                    // The screensaver closed while the photo was loading: free the memory right away.
                    if (ready != null) ready.recycle();
                    return;
                }
                decoding = false;
                if (ready == null) {
                    failed.add(Recs.key(chosen));
                    prepareNext();
                    return;
                }
                prepared = newSlide(chosen, ready, light[0], light[1]);
                invalidate();
            });
        });
    }

    private Slide newSlide(Recs.Pick pick, Bitmap image, float lightCaption, float lightClock) {
        Slide s = new Slide(pick, image, lightCaption, lightClock, slides++);
        if (w > 0) recordCaption(s);
        return s;
    }

    /** The prepared photo goes on screen when its turn comes (the first one right away; the others every 30 s). */
    private void advance(long up) {
        if (current == null) {
            if (prepared == null) return;
            current = prepared;
            prepared = null;
            current.startedAt = up;
            firstAt = up;
            prepareNext();
            return;
        }
        if (incoming == null && prepared != null && up >= current.startedAt + SHOW_MS) {
            incoming = prepared;
            prepared = null;
            incoming.startedAt = up;
            return;
        }
        if (incoming != null && up >= incoming.startedAt + FADE_MS) {
            Slide old = current;
            current = incoming;
            incoming = null;
            if (old.image != current.image) {
                // Once the last frame that uses it has been drawn.
                Bitmap gone = old.image;
                handler.postDelayed(gone::recycle, 1000);
            }
            prepareNext();
        }
    }

    /** Photo position and scale now: zoom from 1 to 1.1 (or back) and a pan toward a corner, never showing an edge. */
    private void place(Slide s, long up, float[] out) {
        float u = Ui.clamp((up - s.startedAt) / (float) (SHOW_MS + FADE_MS));
        float f = s.zoomIn ? u : 1 - u;
        float z = 1 + ZOOM * f;
        float rx = w * (z - 1) / 2, ry = h * (z - 1) / 2;
        out[0] = rx * (s.panX * f - 1);
        out[1] = ry * (s.panY * f - 1);
        out[2] = (float) w / s.image.getWidth() * z;
    }

    private void drawPhoto(Canvas c, Slide s, float[] place, float alpha) {
        if (alpha <= 0.004f) return;
        matrix.setScale(place[2], place[2]);
        matrix.postTranslate(place[0], place[1]);
        photoPaint.setAlpha(Math.round(255 * Math.min(1, alpha)));
        c.drawBitmap(s.image, matrix, photoPaint);
    }

    // MARK: - Frames: 20 per second, locked to every 3rd display frame (60 Hz)
    // Motion is slow (under half a pixel per frame). Measured on the TV: ~4 ms of GPU per frame of ours, but drawing
    // every frame, it only reaches ~50 fps (the system also composites the UI on the GPU); at 20 there's room to spare.

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        if (frameTimeNanos - lastFrame >= 45_000_000L) {
            lastFrame = frameTimeNanos;
            invalidate();
        }
        Choreographer.getInstance().postFrameCallback(this);
    }

    /** The clock that drives the animations. */
    private long clock() {
        long up = SystemClock.uptimeMillis();
        return up;
    }

    @Override protected void onDraw(Canvas canvas) {
        if (w == 0) return;
        long up = clock();
        long now = System.currentTimeMillis();
        if (now / 60_000 != minute) {
            minute = now / 60_000;
            clock = Ui.clock(now);
            date = Ui.date(now);
            dirty = true;
            chromeDirty = true;
        }
        if (running) advance(up);

        float first = current == null ? 0 : fadeIn(up - firstAt, FIRST_MS);
        if (current == null || first < 1) {
            if (fallback) drawFallback(canvas, up, current == null ? 1 : 1 - first);
            else canvas.drawRect(0, 0, w, h, black);
        }
        if (current == null) return;
        if (fallback && first >= 1) fallback = false;

        place(current, up, placeA);
        float e = incoming == null ? 0 : fadeIn(up - incoming.startedAt, FADE_MS);
        drawPhoto(canvas, current, placeA, first);
        if (incoming != null) {
            place(incoming, up, placeB);
            drawPhoto(canvas, incoming, placeB, e);
        }

        if (chromeDirty) recordChrome();
        chrome.setAlpha(first);
        chrome.draw(canvas);
        drawCaptions(canvas, up);
    }

    private static float fadeIn(long elapsed, long duration) {
        float t = Ui.clamp(elapsed / (float) duration);
        return t * t * (3 - 2 * t);
    }

    /**
     * Black scrims where there is text, stronger where the photo is bright, painted once onto the photo itself (in the
     * background): each frame draws only the photo (the TV's GPU feels every full-screen layer). With the slow zoom,
     * they move a little along with the photo, which makes no difference to the text.
     */
    private static Bitmap withScrims(Bitmap photo, float lightCaption, float lightClock) {
        int w = photo.getWidth(), h = photo.getHeight();
        Canvas c = new Canvas(photo);
        Paint p = new Paint(Paint.DITHER_FLAG);
        float caption = scrimStrength(lightCaption, 0.5f), corner = scrimStrength(lightClock, 0.42f);
        p.setShader(new LinearGradient(0, h, 0, h * 0.42f,
                new int[] {black(0.55f * caption), black(0.33f * caption), black(0.11f * caption), 0},
                new float[] {0, 0.35f, 0.7f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, h * 0.42f, w, h, p);
        RadialGradient around = new RadialGradient(0, h, w * 0.7f,
                new int[] {black(caption), black(0.55f * caption), black(0.15f * caption), 0},
                new float[] {0, 0.4f, 0.72f, 1}, Shader.TileMode.CLAMP);
        Matrix m = new Matrix();
        m.setScale(1, 0.62f, 0, h);
        around.setLocalMatrix(m);
        p.setShader(around);
        c.drawRect(0, h - w * 0.7f * 0.62f, w * 0.7f, h, p);
        p.setShader(new LinearGradient(0, 0, 0, h * 0.3f, new int[] {black(0.3f), black(0.075f), 0},
                new float[] {0, 0.45f, 1}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h * 0.3f, p);
        float cx = w * 0.86f, cy = h * 0.15f;
        RadialGradient clockSide = new RadialGradient(cx, cy, w * 0.36f,
                new int[] {black(corner), black(0.6f * corner), black(0.15f * corner), 0},
                new float[] {0, 0.42f, 0.75f, 1}, Shader.TileMode.CLAMP);
        Matrix mc = new Matrix();
        mc.setScale(1, 0.72f, cx, cy);
        clockSide.setLocalMatrix(mc);
        p.setShader(clockSide);
        c.drawRect(w * 0.5f, 0, w, cy + w * 0.36f * 0.72f, p);
        return photo;
    }

    private static int black(float a) {
        return Math.round(Math.max(0, Math.min(1, a)) * 255) << 24;
    }

    private static float scrimStrength(float light, float base) {
        return Math.max(base - 0.1f, Math.min(0.78f, base + (light - 0.3f) * 0.9f));
    }

    // MARK: - Corner: date and weather on a hairline, the clock, prices

    private void recordChrome() {
        Canvas c = chrome.beginRecording(w, h);
        try {
            float right = RIGHT * k, left = right - CORNER_W * k, base = DATE_BASE * k;
            // The label row: the date at the left of the block, the weather at its right.
            String day = date.toUpperCase(Ui.locale());
            c.drawText(day, left, base, cornerDate);
            HomeData.Weather wx = data.weather;
            if (!Double.isNaN(wx.temp) && Prefs.weather()) {
                String temp = HomeData.degrees(wx.temp);
                float tw = cornerDate.measureText(temp);
                c.drawText(temp, right - tw, base, cornerDate);
                String symbol = Symbols.weather(wx.code, wx.isDay);
                float size = 28 * k, sw = Symbols.width(getContext(), symbol, size);
                Symbols.drawSharp(getContext(), c, symbol, right - tw - 12 * k - sw / 2, base - Ui.halfCap(cornerDate), size, INK);
            }
            float y = Math.round(RULE_Y * k);
            c.drawRect(Math.round(left), y, Math.round(right), y + Math.max(1, Math.round(k)), rule);

            Rect ink = new Rect();
            cornerClock.getTextBounds(clock, 0, clock.length(), ink);
            c.drawText(clock, right - ink.right, CLOCK_TOP * k - ink.top, cornerClock);

            // Compact prices under the clock, right-aligned.
            float qbase = (CLOCK_TOP + Ui.cap(cornerClock) / k + QUOTES_GAP) * k;
            float cap = Ui.cap(cornerQuote), gap = 8 * k, spacing = 28 * k;
            int[] coins = HomeData.active();
            int n = HomeData.coinCount();
            String[] values = new String[n];
            float[] glyphs = new float[n];
            float total = spacing * (n - 1);
            for (int i = 0; i < n; i++) {
                values[i] = HomeData.compact(data.prices[coins[i]]);
                glyphs[i] = Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap);
                total += glyphs[i] + gap + cornerQuote.measureText(values[i]);
            }
            float qx = right - total;
            for (int i = 0; i < n; i++) {
                Glyphs.coin(getContext(), c, HomeData.COINS[coins[i]], qx + glyphs[i] / 2, qbase - cap / 2, cap, MUTED);
                qx += glyphs[i] + gap;
                c.drawText(values[i], qx, qbase, cornerQuote);
                qx += cornerQuote.measureText(values[i]) + spacing;
            }
            String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
            if (!stale.isEmpty() && n > 0) c.drawText(stale, right - total - 24 * k - cornerStale.measureText(stale), qbase, cornerStale);
            // Your team under the clock (and under the prices when they're shown).
            float ty = n > 0 ? qbase + 56 * k : (CLOCK_TOP + 20) * k + Ui.cap(cornerClock) + 50 * k;
            Team.draw(c, right, ty, ty + 38 * k, k, teamBig, teamSmall, teamTag, crestPaint, rule);
        } finally {
            chrome.endRecording();
        }
        chromeDirty = false;
    }

    private void recordCaption(Slide s) {
        Canvas c = s.caption.beginRecording(w, h);
        try {
            float x = MARGIN * k, metaBase = META_BASE * k, titleBase = metaBase - 52 * k;
            String title = s.pick.title == null ? "" : s.pick.title.toUpperCase(Ui.locale());
            StaticLayout t = StaticLayout.Builder.obtain(title, 0, title.length(), titlePaint, Math.round(TITLE_W * k))
                    .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false).build();
            float originY = titleBase - t.getLineBaseline(t.getLineCount() - 1);
            float capTop = originY + t.getLineBaseline(0) - Ui.cap(titlePaint);
            // The time-of-day label in caps on a hairline as wide as the title's column.
            float ruleY = Math.round(capTop - 32 * k);
            c.drawText(Recs.periodLabel().toUpperCase(Ui.locale()), x, ruleY - 14 * k, labelPaint);
            c.drawRect(Math.round(x), ruleY, Math.round(x + TITLE_W * k), ruleY + Math.max(1, Math.round(k)), rule);
            c.save();
            c.translate(x - Ui.leftBearing(titlePaint, title), originY);
            t.draw(c);
            c.restore();
            if (s.pick.meta != null && !s.pick.meta.isEmpty()) c.drawText(s.pick.meta, x, metaBase, metaPaint);
        } finally {
            s.caption.endRecording();
        }
        s.caption.setPosition(0, 0, w, h);
    }

    /** The caption fades out fast; the new one rises in once the photo has partly changed (like the home screen). */
    private void drawCaptions(Canvas c, long up) {
        if (incoming == null) {
            float a = current.startedAt == firstAt ? fadeIn(up - firstAt - 700, 900) : 1;
            current.caption.setAlpha(a);
            current.caption.setTranslationY(14 * k * (1 - a));
            current.caption.draw(c);
            return;
        }
        long t = up - incoming.startedAt;
        float out = 1 - Ui.clamp(t / 500f);
        if (out > 0.004f) {
            current.caption.setAlpha(out);
            current.caption.setTranslationY(0);
            current.caption.draw(c);
        }
        float in = fadeIn(t - 1300, 900);
        if (in > 0.004f) {
            incoming.caption.setAlpha(in);
            incoming.caption.setTranslationY(14 * k * (1 - in));
            incoming.caption.draw(c);
        }
    }

    // MARK: - Without photos: the big clock over the aurora

    private void drawFallback(Canvas canvas, long up, float alpha) {
        float t = (up - startedAt) / 1000f;
        if (up - lastSky >= 100) {
            lastSky = up;
            scene.advance(timeOffset + t);
        }
        if (field != null) canvas.drawBitmap(field, null, fieldRect, photoPaint);
        else scene.drawBackground(canvas);
        if (dirty) {
            Canvas rc = content.beginRecording(w, h);
            try {
                drawContent(rc);
            } finally {
                content.endRecording();
            }
            dirty = false;
        }
        double phase = 2 * Math.PI * t / DRIFT_PERIOD_S;
        content.setTranslationX((float) (40 * k * Math.sin(phase)));
        content.setTranslationY((float) (26 * k * Math.sin(2 * phase + 0.7)));
        content.setAlpha(alpha);
        content.draw(canvas);
    }

    private void drawContent(Canvas c) {
        float cx = w / 2f;
        float clockCap = Ui.cap(clockPaint);
        float clockBase = h * 0.5f + clockCap / 2 - 10 * k;
        c.drawText(date.toUpperCase(Ui.locale()), cx, clockBase - clockCap - 40 * k, datePaint);
        c.drawText(clock, cx, clockBase, clockPaint);

        // Weather: symbol, temperature, high and low, centered under the clock.
        HomeData.Weather wx = data.weather;
        if (!Double.isNaN(wx.temp) && Prefs.weather()) {
            float base = clockBase + 76 * k;
            String symbol = Symbols.weather(wx.code, wx.isDay);
            String now = HomeData.degrees(wx.temp);
            String range = Ui.ru ? "Макс. " + HomeData.degrees(wx.max) + "  Мин. " + HomeData.degrees(wx.min)
                    : Ui.en ? "H:" + HomeData.degrees(wx.max) + "  L:" + HomeData.degrees(wx.min)
                    : Ui.de ? "H:" + HomeData.degrees(wx.max) + "  T:" + HomeData.degrees(wx.min)
                    : "Máx. " + HomeData.degrees(wx.max) + "  Mín. " + HomeData.degrees(wx.min);
            float sw = Symbols.width(getContext(), symbol, 32 * k);
            float width = sw + 12 * k + weatherPaint.measureText(now) + 18 * k + weatherDim.measureText(range);
            float x = cx - width / 2;
            Symbols.drawSharp(getContext(), c, symbol, x + sw / 2, base - Ui.halfCap(weatherPaint), 32 * k, INK);
            x += sw + 12 * k;
            c.drawText(now, x, base, weatherPaint);
            x += weatherPaint.measureText(now) + 18 * k;
            c.drawText(range, x, base, weatherDim);
        }

        // Prices near the bottom edge: small white logo + value, all four on one line.
        float cap = Ui.cap(quotePaint), spacing = 56 * k, gap = 10 * k;
        int[] coins = HomeData.active();
        String[] values = new String[HomeData.coinCount()];
        float total = spacing * (values.length - 1);
        for (int i = 0; i < values.length; i++) {
            values[i] = HomeData.price(data.prices[coins[i]]);
            total += Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap) + gap + quotePaint.measureText(values[i]);
        }
        float x = cx - total / 2, base = h - 92 * k;
        for (int i = 0; i < values.length; i++) {
            float gw = Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap);
            Glyphs.coin(getContext(), c, HomeData.COINS[coins[i]], x + gw / 2, base - cap / 2, cap, 0x80FFFFFF);
            x += gw + gap;
            c.drawText(values[i], x, base, quotePaint);
            x += quotePaint.measureText(values[i]) + spacing;
        }
        String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
        if (!stale.isEmpty() && values.length > 0) c.drawText(Ui.tr("cotações de ", "prices from ") + stale, cx, base + 38 * k, stalePaint);
    }
}
