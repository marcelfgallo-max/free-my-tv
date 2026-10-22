package io.freemytv;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.BitmapShader;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
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

/** Cinematic photos, dotted clock and quiet Day/Night panels. Any key still exits. */
final class DreamView extends View implements Choreographer.FrameCallback, HomeData.Listener, Recs.Listener {
    private static final long SHOW_MS = Theme.DREAM_SHOW_MS, FADE_MS = Theme.DREAM_FADE_MS, FIRST_MS = Theme.DREAM_FIRST_MS;
    private static final float ZOOM = 0.1f;
    private static final int PW = 2112, PH = 1188;
    private static final float MARGIN = 96, RIGHT = 1824, TITLE_W = Theme.DREAM_TITLE_WIDTH, META_BASE = 952;
    private static final double DRIFT_PERIOD_S = 600;

    /** One photo on screen: the image, its motion (zooming in or out, toward a corner) and its caption. */
    private static final class Slide {
        final Recs.Pick pick;
        final Bitmap image;
        final BitmapShader shader;
        final boolean zoomIn;
        final float panX, panY;
        final Recording caption = Recording.create("caption");
        long startedAt;

        Slide(Recs.Pick pick, Bitmap image, int n) {
            this.pick = pick;
            this.image = image;
            shader = new BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            zoomIn = n % 2 == 0;
            // Toward one of the corners, alternating, without repeating the previous one.
            panX = (n / 2) % 2 == 0 ? 0.8f : -0.8f;
            panY = n % 3 == 0 ? -0.6f : 0.6f;
        }
    }

    private final HomeData data;
    private final Recs recs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Recording content = Recording.create("clock");     // without photos: the dotted clock
    private final Recording chrome = Recording.create("corner");     // date, weather, prices and dotted clock
    private final Matrix matrix = new Matrix();
    private final float[] placeA = new float[3], placeB = new float[3];
    private final Paint photoPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint background = new Paint(), surface = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rule = new Paint(Paint.ANTI_ALIAS_FLAG), accent = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF photoRect = new RectF(), panelRect = new RectF();
    private final DotDisplay cornerDots = new DotDisplay(), fallbackDots = new DotDisplay();
    private boolean night;
    private int prefsVersion = -1, ink, muted;
    private final Set<String> failed = new HashSet<>();     // Recs.key: a movie and a series can have the same ID
    private boolean running, dirty = true, chromeDirty = true;
    private boolean decoding;
    private int generation, slides;
    private Slide current, incoming, prepared;
    private long firstAt, lastFrame;
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
        content.setUseCompositingLayer(true, null);
        night = Theme.MODE_NIGHT.equals(Prefs.themeMode());
    }

    void start() {
        if (running) return;
        running = true;
        if (worker.isShutdown()) worker = Executors.newSingleThreadExecutor();
        lastFrame = 0;
        failed.clear();
        updateTheme();
        data.addListener(this);
        recs.addListener(this);
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
        content.setPosition(0, 0, w, h);
        chrome.setPosition(0, 0, w, h);
        photoRect.set(Theme.DREAM_PHOTO_INSET * k, Theme.DREAM_PHOTO_INSET * k,
                w - Theme.DREAM_PHOTO_INSET * k, h - Theme.DREAM_PHOTO_INSET * k);
        datePaint = Ui.text(Typeface.MONOSPACE, 26 * k, 0);
        datePaint.setTextAlign(Paint.Align.CENTER);
        clockPaint = Ui.text(Ui.displayMedium, Theme.DREAM_FALLBACK_CLOCK_HEIGHT * k, 0);
        weatherPaint = Ui.numbers(Typeface.MONOSPACE, 30 * k, 0);
        weatherDim = Ui.numbers(Typeface.MONOSPACE, 24 * k, 0);
        quotePaint = Ui.numbers(Typeface.MONOSPACE, 24 * k, 0);
        stalePaint = Ui.text(Typeface.MONOSPACE, 20 * k, 0);
        stalePaint.setTextAlign(Paint.Align.CENTER);
        cornerDate = Ui.numbers(Typeface.MONOSPACE, 23 * k, 0);
        cornerQuote = Ui.numbers(Typeface.MONOSPACE, 22 * k, 0);
        cornerStale = Ui.text(Typeface.MONOSPACE, 18 * k, 0);
        cornerClock = Ui.text(Ui.displayMedium, Theme.DREAM_CLOCK_HEIGHT * k, 0);
        labelPaint = Ui.text(Typeface.MONOSPACE, 22 * k, 0);
        titlePaint = new TextPaint(Ui.text(Ui.displayMedium, Theme.DREAM_TITLE_SIZE * k, 0));
        titlePaint.setLetterSpacing(-0.02f);
        metaPaint = Ui.text(Typeface.MONOSPACE, 22 * k, 0);
        updateTheme();
        dirty = true;
        chromeDirty = true;
    }

    private void updateTheme() {
        prefsVersion = Prefs.version;
        night = Theme.MODE_NIGHT.equals(Prefs.themeMode());
        ink = Theme.color(Theme.TEXT_PRIMARY, night);
        muted = Theme.color(Theme.TEXT_SECONDARY, night);
        background.setColor(Theme.color(Theme.BACKGROUND, night));
        surface.setColor(Theme.color(Theme.SURFACE, night));
        rule.setColor(Theme.color(Theme.SOFT_DIVIDER, night));
        accent.setColor(Theme.ACCENT_ORANGE);
        ColorMatrix color = new ColorMatrix();
        color.setSaturation(night ? Theme.DREAM_PHOTO_SATURATION_NIGHT : Theme.DREAM_PHOTO_SATURATION_DAY);
        float brightness = night ? Theme.DREAM_PHOTO_BRIGHTNESS_NIGHT : 1;
        ColorMatrix dim = new ColorMatrix(new float[] {
                brightness,0,0,0,0, 0,brightness,0,0,0, 0,0,brightness,0,0, 0,0,0,1,0});
        color.postConcat(dim);
        photoPaint.setColorFilter(new ColorMatrixColorFilter(color));
        if (clockPaint == null) return;
        for (Paint paint : new Paint[] {datePaint, clockPaint, weatherPaint, cornerDate, cornerClock, titlePaint})
            paint.setColor(ink);
        for (Paint paint : new Paint[] {weatherDim, quotePaint, stalePaint, cornerQuote, cornerStale, labelPaint, metaPaint})
            paint.setColor(muted);
        dirty = chromeDirty = true;
        for (Slide slide : new Slide[] {current, incoming, prepared}) if (slide != null) recordCaption(slide);
    }

    private void dottedRule(Canvas c, float left, float right, float y) {
        for (float x = left; x <= right; x += Theme.DOTTED_RULE_STEP * k)
            c.drawCircle(x, y, Theme.DOTTED_RULE_RADIUS * k, rule);
    }

    private void weatherIcon(Canvas c, String symbol, float x, float y, float size) {
        if (!DrawerSymbols.draw(c, symbol, x, y, size, ink))
            Symbols.drawSharp(getContext(), c, symbol, x, y, size, ink);
    }

    private String fit(String text, Paint paint, float width) {
        return TextUtils.ellipsize(text, new TextPaint(paint), Math.max(0, width), TextUtils.TruncateAt.END).toString();
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
            return;
        }
        if (last != null && Recs.key(pick).equals(Recs.key(last.pick))) {
            // Only one photo: the same one again, with a different motion.
            prepared = newSlide(pick, last.image);
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
            try {
                image = Backdrop.gpu(Backdrop.photo(file, PW, PH, null));
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
                prepared = newSlide(chosen, ready);
                invalidate();
            });
        });
    }

    private Slide newSlide(Recs.Pick pick, Bitmap image) {
        Slide s = new Slide(pick, image, slides++);
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
        DreamGeometry.place(photoRect.left, photoRect.top, photoRect.width(), photoRect.height(),
                s.image.getWidth(), s.image.getHeight(), z, s.panX * f, s.panY * f, out);
    }

    private void drawPhoto(Canvas c, Slide s, float[] place, float alpha) {
        if (alpha <= 0.004f) return;
        matrix.setScale(place[2], place[2]);
        matrix.postTranslate(place[0], place[1]);
        photoPaint.setAlpha(Math.round(255 * Math.min(1, alpha)));
        s.shader.setLocalMatrix(matrix);
        photoPaint.setShader(s.shader);
        c.drawRoundRect(photoRect, Theme.DREAM_PHOTO_RADIUS * k, Theme.DREAM_PHOTO_RADIUS * k, photoPaint);
        photoPaint.setShader(null);
    }

    // MARK: - Frames: 20 per second, locked to every 3rd display frame (60 Hz)
    // Preserve the existing low-frequency photo animation. This design has not been profiled on the TV yet.

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
        if (prefsVersion != Prefs.version) updateTheme();
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

        canvas.drawRect(0, 0, w, h, background);
        float first = current == null ? 0 : fadeIn(up - firstAt, FIRST_MS);
        if (current == null || first < 1) drawFallback(canvas, up, 1 - first);
        if (current == null) return;

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

    // Chrome is recorded only on minute/data/theme changes, never rebuilt for photo motion.
    private void recordChrome() {
        Canvas c = chrome.beginRecording(w, h);
        try {
            float right = RIGHT * k, left = right - Theme.DREAM_CLOCK_CARD_WIDTH * k;
            float x = left + 32 * k, edge = right - 32 * k;
            panelRect.set(left, 96 * k, right, 448 * k);
            c.drawRoundRect(panelRect, Theme.DREAM_PANEL_RADIUS * k, Theme.DREAM_PANEL_RADIUS * k, surface);
            float dateWidth = edge - x;
            HomeData.Weather wx = data.weather;
            if (!Double.isNaN(wx.temp) && Prefs.weather()) {
                String temp = HomeData.degrees(wx.temp);
                float tw = cornerDate.measureText(temp);
                c.drawText(temp, edge - tw, 156 * k, cornerDate);
                weatherIcon(c, Symbols.weather(wx.code, wx.isDay), edge - tw - 28 * k, 146 * k, 32 * k);
                dateWidth -= tw + 76 * k;
            }
            c.drawText(fit(date.toUpperCase(Ui.locale()), cornerDate, dateWidth), x, 156 * k, cornerDate);
            dottedRule(c, x, edge, 180 * k);
            float height = DotMatrix.fittedHeight(clock, Theme.DREAM_CLOCK_HEIGHT * k, edge - x);
            float width = DotMatrix.widthUnits(clock) * height / DotMatrix.HEIGHT;
            cornerDots.draw(c, clock, x + (edge - x - width) / 2, 344 * k, height, edge - x, cornerClock);

            int[] coins = HomeData.active();
            int n = HomeData.coinCount();
            float cell = (edge - x) / Math.max(1, n), cap = Ui.cap(cornerQuote);
            for (int i = 0; i < n; i++) {
                float qx = x + i * cell;
                float gw = Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap);
                Glyphs.coin(getContext(), c, HomeData.COINS[coins[i]], qx + gw / 2, 394 * k - cap / 2, cap, muted);
                c.drawText(fit(HomeData.compact(data.prices[coins[i]]), cornerQuote, cell - gw - 16 * k),
                        qx + gw + 8 * k, 394 * k, cornerQuote);
            }
            String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
            if (!stale.isEmpty() && n > 0) c.drawText(fit(Ui.tr("cotações de ", "prices from ") + stale,
                    cornerStale, edge - x), x, 425 * k, cornerStale);
        } finally {
            chrome.endRecording();
        }
        chromeDirty = false;
    }

    private void recordCaption(Slide s) {
        Canvas c = s.caption.beginRecording(w, h);
        try {
            float x = (MARGIN + 32) * k, metaBase = META_BASE * k, titleBase = metaBase - 44 * k;
            String title = s.pick.title == null ? "" : s.pick.title;
            StaticLayout t = StaticLayout.Builder.obtain(title, 0, title.length(), titlePaint, Math.round(TITLE_W * k))
                    .setMaxLines(2).setEllipsize(TextUtils.TruncateAt.END).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                    .setIncludePad(false).build();
            float originY = titleBase - t.getLineBaseline(t.getLineCount() - 1);
            float capTop = originY + t.getLineBaseline(0) - Ui.cap(titlePaint);
            float ruleY = Math.round(capTop - 24 * k);
            panelRect.set(MARGIN * k, ruleY - 66 * k, (MARGIN + TITLE_W + 64) * k, metaBase + 32 * k);
            c.drawRoundRect(panelRect, Theme.DREAM_PANEL_RADIUS * k, Theme.DREAM_PANEL_RADIUS * k, surface);
            c.drawCircle(x + 4 * k, ruleY - 27 * k, 4 * k, accent);
            c.drawText(Recs.periodLabel().toUpperCase(Ui.locale()), x + 22 * k, ruleY - 18 * k, labelPaint);
            dottedRule(c, x, x + TITLE_W * k, ruleY);
            c.save();
            c.translate(x - Ui.leftBearing(titlePaint, title), originY);
            t.draw(c);
            c.restore();
            if (s.pick.meta != null && !s.pick.meta.isEmpty()) c.drawText(fit(s.pick.meta, metaPaint, TITLE_W * k), x, metaBase, metaPaint);
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

    // Without photos: the same theme and dotted clock, with a slow positional drift.
    private void drawFallback(Canvas canvas, long up, float alpha) {
        float t = up / 1000f;
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
        float clockCap = DotMatrix.fittedHeight(clock, Theme.DREAM_FALLBACK_CLOCK_HEIGHT * k, 1120 * k);
        float clockBase = h * 0.5f + clockCap / 2 - 10 * k;
        panelRect.set(cx - 660 * k, clockBase - clockCap - 130 * k, cx + 660 * k, clockBase + 144 * k);
        c.drawRoundRect(panelRect, Theme.DREAM_PANEL_RADIUS * k, Theme.DREAM_PANEL_RADIUS * k, surface);
        c.drawText(fit(date.toUpperCase(Ui.locale()), datePaint, 1120 * k), cx, clockBase - clockCap - 48 * k, datePaint);
        float clockWidth = DotMatrix.widthUnits(clock) * clockCap / DotMatrix.HEIGHT;
        fallbackDots.draw(c, clock, cx - clockWidth / 2, clockBase, clockCap, 1120 * k, clockPaint);

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
            weatherIcon(c, symbol, x + sw / 2, base - Ui.halfCap(weatherPaint), 32 * k);
            x += sw + 12 * k;
            c.drawText(now, x, base, weatherPaint);
            x += weatherPaint.measureText(now) + 18 * k;
            c.drawText(range, x, base, weatherDim);
        }

        // Prices near the bottom edge, using the active theme ink.
        int[] coins = HomeData.active();
        int count = HomeData.coinCount();
        float available = 1640 * k, cell = available / Math.max(1, count);
        float left = cx - available / 2, base = h - 108 * k;
        float cap = Ui.cap(quotePaint);
        for (int i = 0; i < count; i++) {
            float gw = Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap);
            String value = fit(HomeData.price(data.prices[coins[i]]), quotePaint, cell - gw - 38 * k);
            float total = gw + 10 * k + quotePaint.measureText(value);
            float x = left + cell * (i + .5f) - total / 2;
            Glyphs.coin(getContext(), c, HomeData.COINS[coins[i]], x + gw / 2, base - cap / 2, cap, muted);
            c.drawText(value, x + gw + 10 * k, base, quotePaint);
        }
        String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
        if (!stale.isEmpty() && count > 0) c.drawText(fit(Ui.tr("cotações de ", "prices from ") + stale,
                stalePaint, available), cx, base + 38 * k, stalePaint);
    }
}
