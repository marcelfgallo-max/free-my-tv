package io.freemytv;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.media.tv.TvInputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.Choreographer;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * The home screen, styled after the Apple TV app with the macOS 27 glass:
 *  - at the top left, the time (large) and the tabs (Explore, Movies, Shows, Continue, Catalog); at the top right, icon-only glass
 *    buttons (Live TV, HDMI Inputs, Settings), with the focused one's name below it, and the small crypto quotes
 *    (logo + price) at the right end of the tab row;
 *  - three cards with the titles recommended for right now in the selected tab (Recs, from TMDB), or the last ones
 *    watched in your apps (Continue: WatchNext), or what's on each service (Catalog); behind everything,
 *    a flat graphite background;
 *  - at the bottom, the glass shelf with the apps (icons in each brand's color, like the tvOS Dock; the focused one
 *    grows);
 *  - ↓ on the shelf: everything moves up and the bottom drawer appears (inputs, shortcuts, weather for the next hours
 *    and quotes with the 24 h change); the background lightens (darkens at night). ↑ or Back closes it;
 *  - from sunset to sunrise, a night look: dark background, smoked glass and white text.
 * Measurements are in a 1920×1080 space (k = width / 1920). The screen is split into layers so the TV's weak GPU only
 * redraws what changed.
 */
final class HomeView extends ViewGroup implements Choreographer.FrameCallback, HomeData.Listener, Recs.Listener {
    // tvOS grid (Human Interface Guidelines): content inside the safe area, 80 px from the sides and 60 from top and
    // bottom; 40 px between items; 7 columns of 217 px (the apps). The shelf glass extends 36 px past the tiles (the
    // same padding on all four sides) and the tile corners are concentric with it (glass radius = radius + padding).
    private static final float MARGIN = Theme.Legacy.HomeLayout.MARGIN, CONTENT_R = Theme.Legacy.HomeLayout.CONTENT_R;
    // Hero, anchored at the bottom (a two-line title grows upward).
    private static final float HERO_TITLE_W = Theme.Legacy.HomeLayout.HERO_TITLE_W;
    // Shelf: tiles with corners concentric with the glass (glass radius = tile radius + padding).
    // Shelf like the tvOS 27 Dock: 5:3 icons (210 × 126) in each brand's color, 48 apart, and the glass bar hugging
    // them with 40 of padding (the focused icon grows 20% without touching its edge).
    private static final float SHELF_PAD = Theme.Legacy.HomeLayout.SHELF_PAD, TILE_GAP = Theme.Legacy.HomeLayout.TILE_GAP;
    private static final float TILE_RADIUS = Theme.Legacy.HomeLayout.TILE_RADIUS, TILE_MAX_W = Theme.Legacy.HomeLayout.TILE_MAX_W;
    // Tile focus (Apple TV style): it grows from the center and its shadow drops (it "rises" toward the viewer).
    private static final float FOCUS_GROW = Theme.Legacy.HomeMotion.FOCUS_GROW;
    // Corner capsule.
    // Corner capsule: the height and top offset of the tvOS tab bar (68 and 46).
    private static final float CAPSULE_TOP = Theme.Legacy.HomeLayout.CAPSULE_TOP, CAPSULE_H = Theme.Legacy.HomeLayout.CAPSULE_H, BUTTON = Theme.Legacy.HomeLayout.BUTTON;
    // Bottom drawer (measured with it open; everything moves up by SHIFT).
    private static final float SHIFT = Theme.Legacy.HomeLayout.SHIFT, ROW_TITLE = Theme.Legacy.HomeLayout.ROW_TITLE, ROW_TOP = Theme.Legacy.HomeLayout.ROW_TOP, ROW_H = Theme.Legacy.HomeLayout.ROW_H, CARDS_TOP = Theme.Legacy.HomeLayout.CARDS_TOP, CARDS_H = Theme.Legacy.HomeLayout.CARDS_H;
    // "All Apps" sheet.
    private static final int COLUMNS = Theme.Legacy.HomeLayout.COLUMNS, VISIBLE_ROWS = Theme.Legacy.HomeLayout.VISIBLE_ROWS;
    private static final float GRID_W = Theme.Legacy.HomeLayout.GRID_W, GRID_H = Theme.Legacy.HomeLayout.GRID_H, GRID_GAP = Theme.Legacy.HomeLayout.GRID_GAP, GRID_ROW = Theme.Legacy.HomeLayout.GRID_ROW, GRID_TOP = Theme.Legacy.HomeLayout.GRID_TOP, GRID_RADIUS = Theme.Legacy.HomeLayout.GRID_RADIUS;
    private static final int SMOKE = Theme.Legacy.HomeColor.SMOKE, INK = Theme.Legacy.HomeColor.INK, WHITE = Theme.Legacy.HomeColor.WHITE;
    private static final int UP_COLOR = Theme.Legacy.HomeColor.UP_COLOR, DOWN_COLOR = Theme.Legacy.HomeColor.DOWN_COLOR;
    // Resolved Home ink colors; Night preserves the previously validated values.
    private int M_INK = Theme.Legacy.HomeColor.M_INK, M_MUTED = Theme.Legacy.HomeColor.M_MUTED, M_HAIRLINE = Theme.Legacy.HomeColor.M_HAIRLINE;
    private int M_UP = Theme.Legacy.HomeColor.M_UP, M_DOWN = Theme.Legacy.HomeColor.M_DOWN;
    private static final long TOAST_MS = Theme.Legacy.HomeMotion.TOAST_MS, FADE_MS = Theme.Legacy.HomeMotion.FADE_MS;
    /** Blur radii of the shadow and shadowDeep paints (Shadows needs them to draw the same blur on older TVs). */
    private static final float SHADOW_BLUR = Theme.Legacy.HomeEffect.SHADOW_BLUR, DEEP_BLUR = Theme.Legacy.HomeEffect.DEEP_BLUR;
    private static final int SKY = 0, HERO = 1, STATUS = 2, PLATTER = 3, TILES = 4, DRAWER = 5, SHEET = 6, TOAST = 7;
    private static final int Z_STATUS = 0, Z_HERO = 1, Z_SHELF = 2, Z_DRAWER = 3;
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor();
    // Light design (the L_ constants): title, tab bar, three recommendation cards and the shelf.
    // The tab bar is a 68 px capsule; the cards sit on the 5-column grid (the wide one spans 3).
    private static final float L_TITLE_BASE = Theme.Legacy.HomeLayout.L_TITLE_BASE, L_TABBAR_TOP = Theme.Legacy.HomeLayout.L_TABBAR_TOP, L_TABBAR_H = Theme.Legacy.HomeLayout.L_TABBAR_H, L_CARDS_TOP = Theme.Legacy.HomeLayout.L_CARDS_TOP, L_CARD_H = Theme.Legacy.HomeLayout.L_CARD_H;
    // Modern 80s tab strip: caps labels on a baseline, a hairline under the row, a 3 px segment under the selected one.
    private static final float L_TAB_BASE = Theme.Legacy.HomeLayout.L_TAB_BASE, L_TAB_RULE = Theme.Legacy.HomeLayout.L_TAB_RULE, L_TAB_GAP = Theme.Legacy.HomeLayout.L_TAB_GAP;
    private static final float L_TALL_W = Theme.Legacy.HomeLayout.L_TALL_W, L_CARD_GAP = Theme.Legacy.HomeLayout.L_CARD_GAP, L_BUTTON = Theme.Legacy.HomeLayout.L_BUTTON, L_BUTTON_GAP = Theme.Legacy.HomeLayout.L_BUTTON_GAP, L_BUTTON_CY = Theme.Legacy.HomeLayout.L_BUTTON_CY;
    private static final float L_PLATTER_TOP = Theme.Legacy.HomeLayout.L_PLATTER_TOP, L_TILE_H = Theme.Legacy.HomeLayout.L_TILE_H;
    private static final float L_DATE_BASE = Theme.Legacy.HomeLayout.L_DATE_BASE, L_CLOCK_BASE = Theme.Legacy.HomeLayout.L_CLOCK_BASE, L_WEATHER_BASE = Theme.Legacy.HomeLayout.L_WEATHER_BASE, L_QUOTES_BASE = Theme.Legacy.HomeLayout.L_QUOTES_BASE;
    private static final int L_INK = Theme.Legacy.HomeColor.L_INK, L_DIM = Theme.Legacy.HomeColor.L_DIM, L_FAINT = Theme.Legacy.HomeColor.L_FAINT;
    private static final String[] TABS = {"Explorar", "Filmes", "Séries", "Continuar", "Catálogo"};
    private static final String[] TABS_RU = {"Обзор", "Фильмы", "Сериалы", "Продолжить", "Каталог"};
    private static final String[] TABS_EN = {"Explore", "Movies", "Shows", "Continue", "Catalog"};
    private static final String[] TABS_ES = {"Explorar", "Películas", "Series", "Continuar", "Catálogo"};
    private static final String[] TABS_DE = {"Entdecken", "Filme", "Serien", "Weiterschauen", "Katalog"};
    private static final String[] TAB_KIND = {null, "movie", "tv", "next", "catalog"};
    /** The tabs come first in the top row (0 to TAB_COUNT - 1), then the three corner buttons. */
    private static final int TAB_COUNT = TABS.length, NEXT_TAB = 3, CATALOG_TAB = 4;
    private static final int Z_TOP = 4, Z_CARDS = 5;
    // Explore's showcase: two genres, 11 posters (2:3) each, filling the margins exactly (11 × 138 + 10 × 21 = 1728).
    private static final int SHOW_COLS = Theme.Legacy.HomeLayout.SHOW_COLS;
    private static final float SHOW_W = Theme.Legacy.HomeLayout.SHOW_W, SHOW_H = Theme.Legacy.HomeLayout.SHOW_H, SHOW_GAP = Theme.Legacy.HomeLayout.SHOW_GAP, SHOW_LABEL_1 = Theme.Legacy.HomeLayout.SHOW_LABEL_1, SHOW_LABEL_2 = Theme.Legacy.HomeLayout.SHOW_LABEL_2;

    private final Activity activity;
    private final Scene scene = new Scene();
    private final HomeData data;
    private final Recs recs;
    private final WatchNext watchNext;
    private final Catalog catalog;
    private final Click click;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences ui;
    private final TvInputManager tvInputs;
    private final Spoken spoken = new Spoken(this, new Spoken.Source() {
        @Override public void items(List<Integer> out) { a11yItems(out); }
        @Override public String text(int id) { return a11yText(id); }
        @Override public boolean bounds(int id, Rect out) { return a11yBounds(id, out); }
        @Override public int focused() { return a11yFocused(); }
        @Override public void focus(int id) { a11yFocus(id); }
        @Override public void click(int id) { activate(); }
        @Override public boolean clickable(int id) { return (id - 1) / 1000 != WIDGETS; }
    });
    private final RectF a11yRect = new RectF();
    private final Layer sky, heroLayer, status, platter, tiles, drawerLayer, sheetLayer, toastLayer;
    private int zone = Z_SHELF;

    // Night look (like Apple's "Automatic" appearance): darker from sunset to sunrise, with a 2 s transition.
    private boolean nightTarget, themeInitialized;
    private float night = 1;            // Restored from the saved appearance preference before drawing.
    private final int[] neutralGlassPx = new int[Backdrop.GW * Backdrop.GH], neutralFrostPx = new int[Backdrop.FW * Backdrop.FH];

    // At the top: tabs (0 to 2) and icon-only buttons (3 to 5); the three cards
    private int tab, topIndex, cardIndex;
    private int page;       // Continue and Catalog: which three of the list the cards show (← → past the ends turn the page)
    private int widgetIndex;    // with TalkBack on, the drawer's weather (0) or quotes (1) card can take the focus
    private List<Recs.Pick> cards = new ArrayList<>();
    private final Spring[] topFocus = new Spring[TAB_COUNT + 3];
    private final RectF[] topRects = new RectF[TAB_COUNT + 3];
    // The three cards, or in Explore's showcase one per poster (row 1 first).
    private final Spring[] cardFocus = new Spring[2 * SHOW_COLS];
    private final RectF[] cardRects = new RectF[2 * SHOW_COLS];
    private final Genres genres;
    private int genrePage, showRow1;    // the pair of genres on screen; how many posters the first row has
    private final List<HomeApps.Item> lightButtons = new ArrayList<>();
    private Paint badgePaint;
    private Paint mBigDegree, teamBig, teamSmall, teamTag, teamImage;
    private final Team team;
    private final Team.Listener teamChanged = () -> {
        heroDirty = true;
        wake();
    };
    private String nowDigits = "";
    private Paint mLabel, mLabelRight, mCell, mName, mPrice, mChange, mMuted, mMutedRight, mHour, mHourTemp, mBig;
    private final java.util.HashMap<String, String> capsCache = new java.util.HashMap<>();
    private String appsLine = "";
    private Paint lGreeting;
    private String greetingText = "";
    private Paint lTitle, lTab, lTabOn, lCardTitle, lCardMeta, lQuote, lTip, lDate, lClock, lWeather, lClockQuote;

    // Shelf
    private List<HomeApps.Item> shelf = new ArrayList<>();
    private Spring[] tileFocus = new Spring[0];
    private RectF[] tileRects = new RectF[0];
    private final Map<String, Bitmap> crispLogos = new HashMap<>();   // logos already sized for the focused tile
    private final android.graphics.PorterDuffColorFilter nightLogo =
            new android.graphics.PorterDuffColorFilter(Theme.LOGO_NIGHT, android.graphics.PorterDuff.Mode.SRC_IN);
    private final android.graphics.PorterDuffColorFilter dayLogo =
            new android.graphics.PorterDuffColorFilter(Theme.LOGO_DAY, android.graphics.PorterDuff.Mode.SRC_IN);
    private final android.graphics.ColorMatrixColorFilter grayscale = new android.graphics.ColorMatrixColorFilter(
            new float[] {0.213f, 0.715f, 0.072f, 0, 0, 0.213f, 0.715f, 0.072f, 0, 0,
                    0.213f, 0.715f, 0.072f, 0, 0, 0, 0, 0, 1, 0});
    private final RectF platterRect = new RectF();
    private int shelfIndex;

    // Moving apps on the shelf (as on Apple TV): hold OK on a tile, then ← → move it, OK drops it, Back cancels. The
    // lifted tile wiggles; the others slide to their new places (each slot's x offset is a spring that returns to 0).
    private static final long HOLD_MS = Theme.Legacy.HomeMotion.HOLD_MS, WIGGLE_MS = Theme.Legacy.HomeMotion.WIGGLE_MS;
    private static final float LIFT_GROW = Theme.Legacy.HomeMotion.LIFT_GROW, WIGGLE_DEG = Theme.Legacy.HomeMotion.WIGGLE_DEG;
    private boolean reordering, reorderAnim, okHeld, holdArmed;
    private int liftIndex = -1;
    private long reorderAt;
    private float wiggle;
    private Spring[] tileSlide = new Spring[0];
    private final Spring moveLift = new Spring(Theme.Legacy.HomeMotion.MOVE_LIFT_RESPONSE_S, Theme.Legacy.HomeMotion.MOVE_LIFT_DAMPING, 0);
    private List<HomeApps.Item> reorderOrigin;
    private final Runnable holdStart = () -> { if (this.okDown && this.holdArmed) startReorder(); };

    // Corner capsule
    private List<HomeApps.Item> statusItems = new ArrayList<>();
    private Spring[] statusFocus = new Spring[0];
    private RectF[] statusRects = new RectF[0];
    private final RectF capsuleRect = new RectF();
    private float capsuleInfoW;
    private int statusIndex;

    // Hero and background
    private List<Recs.Pick> picks = new ArrayList<>();
    private int pickIndex, heroSlide = 1;
    private Hero hero, heroFrom;
    private long heroFadeAt;
    private final Spring heroFocus = new Spring(Theme.Legacy.HomeMotion.HERO_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.HERO_FOCUS_DAMPING, 0);
    private final RectF heroButton = new RectF();

    // Bottom drawer
    private List<HomeApps.Item> drawerItems = new ArrayList<>();
    private int drawerInputs;          // how many leading items are inputs (the rest are shortcuts)
    private Spring[] drawerFocus = new Spring[0];
    private RectF[] drawerRects = new RectF[0];
    private final RectF weatherCard = new RectF(), quotesCard = new RectF();
    private final Spring drawer = new Spring(Theme.Legacy.HomeMotion.DRAWER_RESPONSE_S, Theme.Legacy.HomeMotion.DRAWER_DAMPING, 0);
    private boolean drawerOpen;
    private int drawerIndex;

    // "All Apps" sheet
    private List<HomeApps.Item> others = new ArrayList<>();
    private Spring[] gridFocus = new Spring[0];
    private String[] gridLabels;
    private final Spring sheet = new Spring(Theme.Legacy.HomeMotion.SHEET_RESPONSE_S, Theme.Legacy.HomeMotion.SHEET_DAMPING, 0);
    private final Spring scroll = new Spring(Theme.Legacy.HomeMotion.SCROLL_RESPONSE_S, Theme.Legacy.HomeMotion.SCROLL_DAMPING, 0);
    private final RectF focusedCell = new RectF();
    private boolean sheetOpen;
    private int gridIndex;

    // Toasts
    private final ArrayDeque<String> toasts = new ArrayDeque<>();
    private StaticLayout toast;
    private long toastAt;

    // Frames
    private boolean running, tilesDirty, statusDirty, heroDirty, drawerDirty, gridDirty, motionDirty = true;
    private boolean drawerWasMoving, idle;
    private long lastNanos, resumedAt, lastStep;
    private int w, h;
    private float k = 1;
    private String clockText = "", dateText = "", weatherLine = "", sunLine = "";
    private long weatherLineAt = -1;
    private int weatherLinePrefs = -1;
    private String weatherLineLanguage = "";
    private long clockMinute = -1;
    private boolean okDown, backDown;

    private Paint labelPaint, metaPaint, clockPaint, buttonPaint, capsuleText, tipPaint, quotePaint, staleQuote;
    private Paint rowTitle, drawerLabel, cardTitle, cardText, cardDim, cardDimRight, bigTemp, hourPaint, hourTemp;
    private Paint pricePaint, changePaint, sheetTitle;
    private TextPaint titlePaint, gridLabel, toastPaint;
    private final Paint bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint imagePaint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
    private final Paint shadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowSoft = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint shadowDeep = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint halo = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Shadows shadows = new Shadows();
    private final Map<Bitmap, BitmapShader> imageShaders = new WeakHashMap<>();
    private final Matrix imageMatrix = new Matrix();
    private final Paint imageFill = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint veil = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF tmp = new RectF(), cell = new RectF(), screen = new RectF();
    private final TextPaint fitPaint = new TextPaint();
    private final RectF gridCell = new RectF();
    private LinearGradient tileGlow, cardSheen;
    private float cardSheenH;
    private LinearGradient[] lightSheens = new LinearGradient[0], lightRims = new LinearGradient[0];
    private float[] lightShaderW = new float[0];
    private HomeData.Weather textWeather;
    private long textPricesAt = -1, textMinute = -1;
    private int textPrefs = -1;
    private String textLanguage;
    private String weatherTitle, nowTemp, highLow, sunriseText, sunsetText, staleText;
    private final String[] hourText = new String[6], hourTempText = new String[6];
    private final String[] priceText = new String[HomeData.COINS.length], changeText = new String[HomeData.COINS.length];
    /** The quotes card's widest name and price (where the mini charts fit), and the chart's points (no allocation). */
    private float nameColumn, priceColumn;
    private final float[] sparkPoints = new float[4 * 48];
    private final Paint sparkPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    /** What the hero shows: a recommendation or, without any, the date, the time and the weather. */
    private static final class Hero {
        String label = "", title = "", meta = "";
        boolean clock, button;
        int dots, dot;
        StaticLayout titleLayout;

        boolean sameText(Hero o) {
            return o != null && clock == o.clock && label.equals(o.label) && title.equals(o.title) && meta.equals(o.meta);
        }
    }

    /** One layer of the screen. It draws in full-screen coordinates: the glass needs each pixel's real position. */
    private final class Layer extends View {
        private final int kind;

        Layer(Context c, int kind) {
            super(c);
            this.kind = kind;
        }

        @Override protected void onDraw(Canvas c) {
            if (labelPaint == null) return;
            c.translate(-getLeft(), -getTop());
            // The layer may be shifted (bottom drawer): the glass reads the background from where it really is. The
            // drawer and the top buttons are always drawn as if resting in place (they are only visible there and are
            // not redrawn while they move).
            scene.setOffset(0, kind == DRAWER || kind == STATUS ? 0 : getTranslationY());
            switch (kind) {
                case SKY: drawSky(c); break;
                case HERO: drawLightTop(c); break;
                case STATUS: drawLightButtons(c); break;
                case PLATTER: drawPlatter(c); break;
                case TILES: drawTiles(c); break;
                case DRAWER: drawDrawer(c); break;
                case SHEET: drawSheet(c); break;
                default: drawToast(c); break;
            }
            scene.setOffset(0, 0);
        }
    }

    private final Runnable reloadSoon = this::reload;

    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override public void onInputStateChanged(String inputId, int state) { reloadSoon(); }
        @Override public void onInputAdded(String inputId) { reloadSoon(); }
        @Override public void onInputRemoved(String inputId) { reloadSoon(); }
    };

    /** A device on an HDMI port turned on or off: the drawer's green dots. */
    private final Runnable cecChanged = () -> {
        if (!running) return;
        drawerDirty = true;
        wake();
    };

    private void reloadSoon() {
        handler.removeCallbacks(reloadSoon);
        handler.postDelayed(reloadSoon, 500);
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (!running) return;
            wake();
            data.refreshIfStale();
            recs.refreshIfStale();
            watchNext.refresh();
            catalog.refresh();
            genres.refresh(genrePage);
            team.refreshIfStale();
            Cec.refresh(activity, false, cecChanged);
            checkNotices();
            updatePalette();
            checkNight(true);
            updatePicks();      // three new ones every hour
            handler.postDelayed(this, 15_000);
        }
    };

    HomeView(Activity a) {
        super(a);
        activity = a;
        data = HomeData.get(a);
        recs = Recs.get(a);
        watchNext = WatchNext.get(a);
        team = Team.get(a);
        catalog = Catalog.get(a);
        genres = Genres.get(a);
        click = new Click(a);
        ui = a.getSharedPreferences("home-ui", Context.MODE_PRIVATE);
        tvInputs = (TvInputManager) a.getSystemService(Context.TV_INPUT_SERVICE);
        setFocusable(true);
        sky = new Layer(a, SKY);
        heroLayer = new Layer(a, HERO);
        status = new Layer(a, STATUS);
        platter = new Layer(a, PLATTER);
        tiles = new Layer(a, TILES);
        drawerLayer = new Layer(a, DRAWER);
        sheetLayer = new Layer(a, SHEET);
        toastLayer = new Layer(a, TOAST);
        // Hardware layers on the GPU: only the one that changed is redrawn; moving, fading and scaling stay cheap.
        // Not on Android 9 to 11: on older GPUs (a Mi Box 3's Mali-450) redrawing and compositing a big layer on every
        // frame cost more than drawing straight to the screen (measured: focus on the picks 33 → 12 ms a frame, the
        // drawer 19 → 17 ms).
        for (View v : new View[] {heroLayer, status, platter, tiles, drawerLayer, sheetLayer}) v.setLayerType(Ui.LEGACY ? LAYER_TYPE_NONE : LAYER_TYPE_HARDWARE, null);
        for (View v : new View[] {sky, heroLayer, status, platter, tiles, drawerLayer, sheetLayer, toastLayer}) {
            v.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);   // TalkBack reads the items (Spoken)
            addView(v);
        }
        setContentDescription("Free My TV");
        drawerLayer.setVisibility(INVISIBLE);
        for (int i = 0; i < topFocus.length; i++) {
            topFocus[i] = new Spring(Theme.Legacy.HomeMotion.TOP_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.TOP_FOCUS_DAMPING, 0);
            topRects[i] = new RectF();
        }
        for (int i = 0; i < cardFocus.length; i++) {
            cardFocus[i] = new Spring(Theme.Legacy.HomeMotion.CARD_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.CARD_FOCUS_DAMPING, 0);
            cardRects[i] = new RectF();
        }
        makeLightButtons();
        recs.setLight(true);
        checkNight(false);
        sheetLayer.setVisibility(INVISIBLE);
        scene.setMotion(0.6f, 0f);
        updatePalette();
        // First time: right away (a cold start opens with the apps already there); missing logos come from reload().
        setShelf(HomeApps.shelf(a, false));
        setStatusItems(statusList(a));
        setDrawerItems(drawerList(a));
    }

    // MARK: - Lifecycle (called by HomeActivity)

    void resume() {
        if (running) return;
        running = true;
        checkNight(false);
        resumedAt = SystemClock.uptimeMillis();
        lastNanos = 0;
        lastStep = 0;
        data.addListener(this);
        recs.addListener(this);
        watchNext.setListener(this::onRecs);
        catalog.setListener(this::onRecs);
        genres.setListener(this::onRecs);
        data.refreshIfStale();
        recs.refreshIfStale();
        watchNext.refresh();
        catalog.refresh();
        team.addListener(teamChanged);
        team.refreshIfStale();
        Cec.refresh(activity, true, cecChanged);
        if (tvInputs != null) tvInputs.registerCallback(inputCallback, handler);
        handler.postDelayed(tick, 15_000);
        checkNotices();
        reload();
        updatePalette();
        updatePicks();
        if (sheetOpen) closeSheet();
        sheet.snap(sheet.target);
        motionDirty = true;
        invalidateAll();
        idle = false;
        Choreographer.getInstance().postFrameCallback(this);
        spoken.readAgain();
    }

    void pause() {
        if (!running) return;
        running = false;
        cancelReorder(true);
        handler.removeCallbacks(holdStart);
        okHeld = holdArmed = false;
        Choreographer.getInstance().removeFrameCallback(this);
        handler.removeCallbacks(tick);
        handler.removeCallbacks(reloadSoon);
        data.removeListener(this);
        recs.removeListener(this);
        watchNext.setListener(null);
        catalog.setListener(null);
        genres.setListener(null);
        team.removeListener(teamChanged);
        if (tvInputs != null) tvInputs.unregisterCallback(inputCallback);
    }

    void destroy() {
        pause();
        click.release();
    }

    /** Starting state (Home pressed while the home screen is showing): first app focused, drawer and sheet closed. */
    void reset(boolean animated) {
        wake();
        cancelReorder(!animated);
        zone = Z_SHELF;
        shelfIndex = 0;
        sheetOpen = false;
        drawerOpen = false;
        if (animated) {
            sheet.target = 0;
            drawer.target = 0;
        } else {
            sheet.snap(0);
            drawer.snap(0);
        }
        if (pickIndex != 0 && !picks.isEmpty()) {
            pickIndex = 0;
            updatePicks();
        }
        if (tab != 0) selectTab(0);
        page = 0;
        if (genrePage != 0) {
            genrePage = 0;
            updateCards();
        }
        retarget(!animated);
        motionDirty = true;
    }

    /** The three corner buttons, with their names in the current language. */
    private void makeLightButtons() {
        lightButtons.clear();
        lightButtons.add(HomeApps.action(HomeApps.LIVE_TV, "live", Ui.tr("TV aberta", "Live TV"), "live_tv"));
        lightButtons.add(HomeApps.action(HomeApps.INPUTS, "inputs", Ui.tr("Entradas HDMI", "HDMI Inputs"), "hdmi"));
        lightButtons.add(HomeApps.action(HomeApps.SETTINGS, "settings", Ui.tr("Configurações", "Settings"), "settings"));
    }

    private static String[] tabs() {
        return Ui.ru ? TABS_RU : Ui.es ? TABS_ES : Ui.de ? TABS_DE : Ui.en ? TABS_EN : TABS;
    }

    @Override public void onData() {
        wake();
        updatePalette();
        checkNight(true);
        layoutStatus();
        heroDirty = statusDirty = drawerDirty = true;
        platter.invalidate();
    }

    @Override public void onRecs() {
        wake();
        updatePicks();
    }

    private void invalidateAll() {
        for (View v : new View[] {sky, heroLayer, status, platter, tiles, drawerLayer}) v.invalidate();
    }

    // MARK: - Size and layers

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        int width = MeasureSpec.getSize(widthSpec), height = MeasureSpec.getSize(heightSpec);
        float kk = width / Theme.Legacy.Canvas.WIDTH;
        measure(sky, width, height);
        measure(heroLayer, width, Math.round(850 * kk));   // down to the showcase's second row
        measure(status, Math.round(1160 * kk), Math.round(240 * kk));
        // Sized to what they draw (the glass and its shadow; the grown tiles and their shadows): smaller layers are
        // cheaper for the GPU to composite on every frame of the drawer animation.
        measure(platter, width, Math.round(320 * kk));
        measure(tiles, width, Math.round(270 * kk));
        // From the apps' label (with the drawer open, the shelf sits under it) to the bottom.
        measure(drawerLayer, width, Math.round(900 * kk));
        measure(sheetLayer, width, height);
        measure(toastLayer, Math.round(1000 * kk), Math.round(220 * kk));
        setMeasuredDimension(width, height);
    }

    private static void measure(View v, int width, int height) {
        v.measure(MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY));
    }

    @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l, height = b - t;
        float kk = width / Theme.Legacy.Canvas.WIDTH;
        sky.layout(0, 0, width, height);
        place(heroLayer, 0, 0);
        place(status, width - status.getMeasuredWidth(), 0);
        place(platter, 0, Math.round(750 * kk));
        place(tiles, 0, Math.round(790 * kk));
        place(drawerLayer, 0, Math.round(180 * kk));
        sheetLayer.layout(0, 0, width, height);
        sheetLayer.setPivotX(width / 2f);
        sheetLayer.setPivotY(height / 2f);
        place(toastLayer, (width - toastLayer.getMeasuredWidth()) / 2, 0);
    }

    private static void place(View v, int x, int y) {
        v.layout(x, y, x + v.getMeasuredWidth(), y + v.getMeasuredHeight());
    }

    @Override protected void onSizeChanged(int width, int height, int oldWidth, int oldHeight) {
        w = width;
        h = height;
        k = w / Theme.Legacy.Canvas.WIDTH;
        screen.set(0, 0, w, h);
        scene.setSize(w, h);
        makePaints();
        gridLabels = null;
        clockMinute = -1;
        updateClock();
        layoutShelf();
        layoutStatus();
        layoutDrawer();
        layoutLight();
        hero = null;
        heroFrom = null;
        updatePicks();
        motionDirty = true;
        wake();
    }

    private void makePaints() {
        labelPaint = Ui.text(Ui.medium, Theme.Legacy.HomeText.LABEL_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AC7);
        labelPaint.setLetterSpacing(Theme.Legacy.HomeText.LABEL_PAINT_TRACKING);
        titlePaint = new TextPaint(Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.TITLE_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AFF));
        titlePaint.setLetterSpacing(Theme.Legacy.HomeText.TITLE_PAINT_TRACKING);
        metaPaint = Ui.text(Ui.medium, Theme.Legacy.HomeText.META_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AD1);
        metaPaint.setLetterSpacing(Theme.Legacy.HomeText.META_PAINT_TRACKING);
        clockPaint = Ui.numbers(Ui.thin, Theme.Legacy.HomeText.CLOCK_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AFF);
        clockPaint.setLetterSpacing(Theme.Legacy.HomeText.CLOCK_PAINT_TRACKING);
        for (Paint p : new Paint[] {labelPaint, titlePaint, metaPaint, clockPaint}) p.setShadowLayer(Theme.Legacy.HomeEffect.HERO_TEXT_SHADOW_BLUR * k, 0, Theme.Legacy.HomeEffect.HERO_TEXT_SHADOW_DY * k, Theme.Legacy.Palette.BLACK_A59);
        // Modern 80s: the clock big and tight, caps tabs and titles in Inter Display at 500.
        lTitle = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.L_TITLE_SIZE * k, M_INK);
        lTitle.setLetterSpacing(Theme.Legacy.HomeText.L_TITLE_TRACKING);
        lGreeting = Ui.text(Ui.regular, Theme.Legacy.HomeText.L_GREETING_SIZE * k, M_MUTED);
        lGreeting.setLetterSpacing(Theme.Legacy.HomeText.L_GREETING_TRACKING);
        lTab = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.L_TAB_SIZE * k, M_MUTED);
        lTab.setLetterSpacing(Theme.Legacy.HomeText.L_TAB_TRACKING);
        lTabOn = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.L_TAB_ON_SIZE * k, M_INK);
        lTabOn.setLetterSpacing(Theme.Legacy.HomeText.L_TAB_ON_TRACKING);
        lCardTitle = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.L_CARD_TITLE_SIZE * k, M_INK);
        lCardTitle.setLetterSpacing(Theme.Legacy.HomeText.L_CARD_TITLE_TRACKING);
        lCardMeta = Ui.text(Ui.regular, Theme.Legacy.HomeText.L_CARD_META_SIZE * k, M_MUTED);
        badgePaint = Ui.text(Ui.semibold, Theme.Legacy.HomeText.BADGE_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AFF);
        lQuote = Ui.numbers(Ui.regular, Theme.Legacy.HomeText.L_QUOTE_SIZE * k, M_INK);
        lTip = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.L_TIP_SIZE * k, M_INK);
        lTip.setTextAlign(Paint.Align.CENTER);
        lDate = Ui.text(Ui.semibold, Theme.Legacy.HomeText.L_DATE_SIZE * k, L_DIM);
        lClock = Ui.numbers(Ui.light, Theme.Legacy.HomeText.L_CLOCK_SIZE * k, L_INK);
        lClock.setLetterSpacing(Theme.Legacy.HomeText.L_CLOCK_TRACKING);
        lWeather = Ui.text(Ui.medium, Theme.Legacy.HomeText.L_WEATHER_SIZE * k, L_DIM);
        lClockQuote = Ui.numbers(Ui.medium, Theme.Legacy.HomeText.L_CLOCK_QUOTE_SIZE * k, L_DIM);
        buttonPaint = Ui.text(Ui.semibold, Theme.Legacy.HomeText.BUTTON_PAINT_SIZE * k, INK);
        capsuleText = Ui.numbers(Ui.medium, Theme.Legacy.HomeText.CAPSULE_TEXT_SIZE * k, Theme.Legacy.Palette.WHITE_AF2);
        tipPaint = Ui.text(Ui.semibold, Theme.Legacy.HomeText.TIP_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AF2);
        tipPaint.setTextAlign(Paint.Align.CENTER);
        tipPaint.setShadowLayer(Theme.Legacy.HomeEffect.TOOLTIP_SHADOW_BLUR * k, 0, Theme.Legacy.HomeEffect.TOOLTIP_SHADOW_DY * k, Theme.Legacy.Palette.BLACK_A99);
        // Quotes: a subtle detail, smaller and dimmer than the rest.
        quotePaint = Ui.numbers(Ui.regular, Theme.Legacy.HomeText.QUOTE_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AB8);
        quotePaint.setShadowLayer(Theme.Legacy.HomeEffect.QUOTE_SHADOW_BLUR * k, 0, Theme.Legacy.HomeEffect.QUOTE_SHADOW_DY * k, Theme.Legacy.Palette.BLACK_A99);
        staleQuote = Ui.text(Ui.medium, Theme.Legacy.HomeText.STALE_QUOTE_SIZE * k, Theme.Legacy.Palette.WHITE_A99);
        staleQuote.setTextAlign(Paint.Align.RIGHT);
        rowTitle = Ui.text(Ui.semibold, Theme.Legacy.HomeText.ROW_TITLE_SIZE * k, Theme.Legacy.Palette.WHITE_AB3);
        drawerLabel = Ui.text(Ui.medium, Theme.Legacy.HomeText.DRAWER_LABEL_SIZE * k, Theme.Legacy.Palette.WHITE_AEB);
        drawerLabel.setTextAlign(Paint.Align.CENTER);
        cardTitle = Ui.text(Ui.semibold, Theme.Legacy.HomeText.CARD_TITLE_SIZE * k, Theme.Legacy.Palette.WHITE_AB3);
        cardText = Ui.text(Ui.medium, Theme.Legacy.HomeText.CARD_TEXT_SIZE * k, Theme.Legacy.Palette.WHITE_AF2);
        cardDim = Ui.numbers(Ui.medium, Theme.Legacy.HomeText.CARD_DIM_SIZE * k, Theme.Legacy.Palette.WHITE_AAD);
        cardDimRight = Ui.text(Ui.medium, Theme.Legacy.HomeText.CARD_DIM_RIGHT_SIZE * k, Theme.Legacy.Palette.WHITE_A99);
        cardDimRight.setTextAlign(Paint.Align.RIGHT);
        bigTemp = Ui.numbers(Ui.light, Theme.Legacy.HomeText.BIG_TEMP_SIZE * k, Theme.Legacy.Palette.WHITE_AFF);
        hourPaint = Ui.numbers(Ui.medium, Theme.Legacy.HomeText.HOUR_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AAD);
        hourPaint.setTextAlign(Paint.Align.CENTER);
        hourTemp = Ui.numbers(Ui.medium, Theme.Legacy.HomeText.HOUR_TEMP_SIZE * k, Theme.Legacy.Palette.WHITE_AF2);
        hourTemp.setTextAlign(Paint.Align.CENTER);
        pricePaint = Ui.numbers(Ui.medium, Theme.Legacy.HomeText.PRICE_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AF2);
        pricePaint.setTextAlign(Paint.Align.RIGHT);
        changePaint = Ui.numbers(Ui.semibold, Theme.Legacy.HomeText.CHANGE_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AFF);
        changePaint.setTextAlign(Paint.Align.CENTER);
        sheetTitle = Ui.text(Ui.displaySemibold, Theme.Legacy.HomeText.SHEET_TITLE_SIZE * k, Theme.Legacy.Palette.WHITE_AFF);
        sheetTitle.setTextAlign(Paint.Align.CENTER);
        gridLabel = new TextPaint(Ui.text(Ui.medium, Theme.Legacy.HomeText.GRID_LABEL_SIZE * k, Theme.Legacy.Palette.WHITE_A99));
        gridLabel.setTextAlign(Paint.Align.CENTER);
        toastPaint = new TextPaint(Ui.text(Ui.medium, Theme.Legacy.HomeText.TOAST_PAINT_SIZE * k, Theme.Legacy.Palette.WHITE_AF2));
        shadow.setMaskFilter(new BlurMaskFilter(SHADOW_BLUR * k, BlurMaskFilter.Blur.NORMAL));
        shadowSoft.setMaskFilter(new BlurMaskFilter(Theme.Legacy.HomeEffect.SOFT_BLUR * k, BlurMaskFilter.Blur.NORMAL));
        shadowDeep.setMaskFilter(new BlurMaskFilter(DEEP_BLUR * k, BlurMaskFilter.Blur.NORMAL));
        halo.setMaskFilter(new BlurMaskFilter(Theme.Legacy.HomeEffect.HALO_BLUR * k, BlurMaskFilter.Blur.NORMAL));
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(Theme.Legacy.HomeEffect.BORDER_STROKE * k);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(Theme.Legacy.HomeEffect.RIM_STROKE * k);
        // Modern 80s: Inter Display at 500, tight; labels in caps.
        mLabel = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.M_LABEL_SIZE * k, M_INK);
        mLabel.setLetterSpacing(Theme.Legacy.HomeText.M_LABEL_TRACKING);
        mLabelRight = new Paint(mLabel);
        mLabelRight.setTextAlign(Paint.Align.RIGHT);
        mCell = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.M_CELL_SIZE * k, M_INK);
        mCell.setLetterSpacing(Theme.Legacy.HomeText.M_CELL_TRACKING);
        mCell.setTextAlign(Paint.Align.CENTER);
        mName = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.M_NAME_SIZE * k, M_INK);
        mName.setLetterSpacing(Theme.Legacy.HomeText.M_NAME_TRACKING);
        mPrice = Ui.numbers(Ui.displayMedium, Theme.Legacy.HomeText.M_PRICE_SIZE * k, M_INK);
        mPrice.setLetterSpacing(Theme.Legacy.HomeText.M_PRICE_TRACKING);
        mPrice.setTextAlign(Paint.Align.RIGHT);
        mChange = Ui.numbers(Ui.displayMedium, Theme.Legacy.HomeText.M_CHANGE_SIZE * k, M_UP);
        mChange.setLetterSpacing(Theme.Legacy.HomeText.M_CHANGE_TRACKING);
        mChange.setTextAlign(Paint.Align.RIGHT);
        mMuted = Ui.numbers(Ui.regular, Theme.Legacy.HomeText.M_MUTED_SIZE * k, M_MUTED);
        mMutedRight = new Paint(mMuted);
        mMutedRight.setTextAlign(Paint.Align.RIGHT);
        mHour = Ui.numbers(Ui.regular, Theme.Legacy.HomeText.M_HOUR_SIZE * k, M_MUTED);
        mHour.setTextAlign(Paint.Align.CENTER);
        mHourTemp = Ui.numbers(Ui.displayMedium, Theme.Legacy.HomeText.M_HOUR_TEMP_SIZE * k, M_INK);
        mHourTemp.setLetterSpacing(Theme.Legacy.HomeText.M_HOUR_TEMP_TRACKING);
        mHourTemp.setTextAlign(Paint.Align.CENTER);
        mBig = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.M_BIG_SIZE * k, M_INK);   // proportional digits: a "1" isn't as wide as an "8"
        mBig.setLetterSpacing(Theme.Legacy.HomeText.M_BIG_TRACKING);
        mBigDegree = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.M_BIG_DEGREE_SIZE * k, M_INK);
        teamBig = Ui.text(Ui.displayMedium, Theme.Legacy.HomeText.TEAM_BIG_SIZE * k, M_INK);
        teamBig.setLetterSpacing(Theme.Legacy.HomeText.TEAM_BIG_TRACKING);
        teamSmall = Ui.numbers(Ui.regular, Theme.Legacy.HomeText.TEAM_SMALL_SIZE * k, M_MUTED);
        teamTag = Ui.text(Ui.regular, Theme.Legacy.HomeText.TEAM_TAG_SIZE * k, M_UP);
        teamImage = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        capsCache.clear();
        applyTone();
    }

    /** `s` in capitals (in the app's language), made once: the drawing code asks every frame. */
    private String caps(String s) {
        if (s == null) return "";
        String up = capsCache.get(s);
        if (up == null) {
            if (capsCache.size() > 300) capsCache.clear();
            up = s.toUpperCase(Ui.locale());
            capsCache.put(s, up);
        }
        return up;
    }

    private final Paint rulePaint = new Paint();

    /** The hairline paint (Team draws its rule with it). */
    private Paint rule() {
        rulePaint.setColor(M_HAIRLINE);
        return rulePaint;
    }

    /** The bottom edge of the tab row's hairline (whole pixels). */
    private int tabRuleBottom() {
        return Math.round(L_TAB_RULE * k) + Math.max(1, Math.round(k));
    }

    /** A 1 px hairline across [x0, x1] at y. */
    private void hairline(Canvas c, float x0, float x1, float y) {
        fill.setShader(null);
        fill.setColor(M_HAIRLINE);
        y = Math.round(y);   // on a whole pixel: one sharp row, not two half-lit ones
        c.drawRect(Math.round(x0), y, Math.round(x1), y + Math.max(1, Math.round(k)), fill);
    }

    /** A 1 px vertical hairline at x across [y0, y1]. */
    private void vline(Canvas c, float x, float y0, float y1) {
        fill.setShader(null);
        fill.setColor(M_HAIRLINE);
        x = Math.round(x);
        c.drawRect(x, Math.round(y0), x + Math.max(1, Math.round(k)), Math.round(y1), fill);
    }

    // MARK: - Frames: 60 per second while something moves; nothing is redrawn when still

    @Override public void doFrame(long frameTimeNanos) {
        if (!running) return;
        float dt = lastNanos == 0 ? 1 / Theme.Legacy.Frame.FPS : Math.min(Theme.Legacy.Frame.MAX_STEP_S, (frameTimeNanos - lastNanos) / 1e9f);
        lastNanos = frameTimeNanos;
        long up = SystemClock.uptimeMillis();

        boolean tilesMoving = step(tileFocus, dt);
        if (reorderAnim) tilesMoving |= stepReorder(dt, up);
        boolean statusMoving = step(statusFocus, dt);
        boolean heroMoving = heroFocus.step(dt);
        boolean topMoving = step(topFocus, dt), cardsMoving = step(cardFocus, dt);
        if (topMoving) status.invalidate();
        heroMoving |= topMoving | cardsMoving;
        boolean drawerItemsMoving = step(drawerFocus, dt);
        boolean gridMoving = step(gridFocus, dt) | scroll.step(dt);
        boolean drawerMoving = drawer.step(dt);
        boolean sheetMoving = sheet.step(dt);
        boolean toastFading = tickToast(up);
        boolean heroFading = tickHeroFade(up);

        if (drawerMoving || sheetMoving || motionDirty) updateMotion();
        // Opening and closing the drawer only moves the cached layers: redrawing the glass on every frame made the TV
        // drop frames (measured: ~50 ms of GPU per frame). The app tiles have no glass; the top buttons fade out as
        // they rise; the drawer is drawn resting in place; the glass shelf is redrawn once the drawer stops, so the
        // glass shows the background where it ended up.
        if (tilesMoving || tilesDirty) tiles.invalidate();
        if (statusMoving || statusDirty) status.invalidate();
        if (heroMoving || heroDirty || heroFading) heroLayer.invalidate();
        if (drawerItemsMoving || drawerDirty) drawerLayer.invalidate();
        if (gridMoving || gridDirty) sheetLayer.invalidate();
        if (drawerWasMoving && !drawerMoving) platter.invalidate();
        drawerWasMoving = drawerMoving;
        tilesDirty = statusDirty = heroDirty = drawerDirty = gridDirty = motionDirty = false;
        if (updateClock()) {
            updateSleepLabel();
            layoutStatus();
            status.invalidate();
            if (hero != null && hero.clock) {
                refreshHero(false);
                heroLayer.invalidate();
            }
            heroLayer.invalidate();   // the time at the top left (or the big clock without picks)
        }

        // Busy: something is moving, or the sheet or the drawer is open.
        boolean busy = tilesMoving || statusMoving || heroMoving || drawerItemsMoving || gridMoving || drawerMoving
                || sheetMoving || toastFading || heroFading || sheetOpen || sheet.value > 0.001f
                || drawerOpen || drawer.value > 0.001f || toast != null || !toasts.isEmpty();
        idle = !busy;
        if (busy) Choreographer.getInstance().postFrameCallback(this);
        else Choreographer.getInstance().postFrameCallbackDelayed(this, Theme.Legacy.Frame.IDLE_MS);
    }

    private void wake() {
        if (!running || !idle) return;
        idle = false;
        lastNanos = 0;
        Choreographer.getInstance().removeFrameCallback(this);
        Choreographer.getInstance().postFrameCallback(this);
    }

    private static boolean step(Spring[] springs, float dt) {
        boolean moving = false;
        for (Spring s : springs) moving |= s.step(dt);
        return moving;
    }

    /** Drawer and sheet: the layers' positions and opacity (without redrawing their content). */
    private void updateMotion() {
        float d = drawer.value, dc = Ui.clamp(d), s = Ui.clamp(sheet.value);
        float home = 1 - s, top = home * (1 - smoothstep(0, 0.32f, dc));
        float shift = SHIFT * k;
        float lift = -d * shift;
        show(heroLayer, top, lift);
        show(status, top, lift);
        show(platter, home, lift);
        show(tiles, home, lift);
        float da = home * smoothstep(0.1f, 0.6f, dc);
        drawerLayer.setTranslationY((1 - d) * shift);
        drawerLayer.setAlpha(da);
        drawerLayer.setVisibility(da > 0.002f ? VISIBLE : INVISIBLE);
        sheetLayer.setVisibility(s > 0.001f || sheetOpen ? VISIBLE : INVISIBLE);
        sheetLayer.setAlpha(s);
        float scale = 1.04f - 0.04f * s;
        sheetLayer.setScaleX(scale);
        sheetLayer.setScaleY(scale);
    }

    private static void show(View v, float alpha, float translationY) {
        v.setAlpha(alpha);
        v.setTranslationY(translationY);
        v.setVisibility(alpha > 0.002f ? VISIBLE : INVISIBLE);
    }

    private boolean updateClock() {
        long now = System.currentTimeMillis();
        long minute = now / 60_000;
        if (minute == clockMinute || clockPaint == null) return false;
        clockMinute = minute;
        clockText = Ui.clock(now);
        greetingText = greeting(now);
        dateText = Ui.date(now);
        return true;
    }

    /** "Good evening, Ana" (the name from config.properties, if any): morning from 5, afternoon from 12, evening from 18. */
    private static String greeting(long now) {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(now);
        int h = cal.get(java.util.Calendar.HOUR_OF_DAY);
        String hello = h >= 5 && h < 12 ? Ui.tr("Bom dia", "Good morning")
                : h >= 12 && h < 18 ? Ui.tr("Boa tarde", "Good afternoon") : Ui.tr("Boa noite", "Good evening");
        String name = Config.NAME.trim();
        return name.isEmpty() ? hello : hello + ", " + name;
    }

    private void updatePalette() {
        HomeData.Weather wx = data.weather;
        Shaders.sky(scene.palette, System.currentTimeMillis(), wx.sunrise, wx.sunset);
        scene.applyPalette();
    }

    // MARK: - Recommendations: hero and background

    private Recs.Pick currentPick() {
        return picks.isEmpty() ? null : picks.get(Math.min(pickIndex, picks.size() - 1));
    }

    /** This hour's picks: the cards and the background image (called when Recs notifies and every 15 s). */
    private void updatePicks() {
        if (labelPaint == null) return;
        updateCards();
    }

    private static boolean samePicks(List<Recs.Pick> a, List<Recs.Pick> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!Recs.key(a.get(i)).equals(Recs.key(b.get(i)))) return false;
        return true;
    }

    /**
     * Rebuilds the hero. With `animate`, a different title slides in while the previous one slides out; details that
     * arrive later (genre, runtime) just appear.
     */
    private void refreshHero(boolean animate) {
        Hero next = makeHero();
        if (next.sameText(hero) && hero.dots == next.dots && hero.dot == next.dot && hero.button == next.button) return;
        boolean other = hero != null && (hero.clock != next.clock || !hero.title.equals(next.title));
        if (animate && other) {
            heroFrom = hero;
            heroFadeAt = SystemClock.uptimeMillis();
        }
        boolean layoutChanged = hero == null || hero.clock != next.clock;
        hero = next;
        heroDirty = true;
        if (layoutChanged) layoutStatus();
    }

    private Hero makeHero() {
        Hero h = new Hero();
        Recs.Pick p = currentPick();
        if (p == null) {
            h.clock = true;
            h.label = Ui.date(System.currentTimeMillis()).toUpperCase(Ui.locale());
            return h;
        }
        h.label = Recs.periodLabel();
        h.title = p.title == null ? "" : p.title;
        h.meta = p.meta == null ? "" : p.meta;
        h.button = p.pkg != null;
        h.dots = picks.size();
        h.dot = pickIndex;
        h.titleLayout = StaticLayout.Builder.obtain(h.title, 0, h.title.length(), titlePaint, Math.max(1, Math.round(HERO_TITLE_W * k)))
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setLineSpacing(0, 0.94f)
                .setIncludePad(false)
                .build();
        return h;
    }

    /** ←/→ on the hero: the next (or previous) of this hour's picks. */
    private boolean browse(int dir) {
        if (picks.size() < 2) return false;
        int next = pickIndex;
        for (int i = 0; i < picks.size(); i++) {
            next = (next + dir + picks.size()) % picks.size();
            if (zone != Z_HERO || picks.get(next).pkg != null) break;
        }
        if (next == pickIndex) return false;
        pickIndex = next;
        heroSlide = dir;
        refreshHero(true);
        spoken.focusMoved();
        return true;
    }

    /** The glass the toasts and the All Apps sheet are drawn with: a plain dark tone (there's no photo behind). */
    private void neutralGlass() {
        int color = Theme.Legacy.Palette.GLASS_NIGHT;
        java.util.Arrays.fill(neutralFrostPx, color);
        java.util.Arrays.fill(neutralGlassPx, color);
        scene.setFrost(neutralFrostPx);
        scene.setGlassImage(neutralGlassPx);
    }

    /** Apply saved Home appearance on resume; no animation or repeated disk reads while drawing. */
    private void checkNight(boolean unusedAnimate) {
        boolean target = Theme.MODE_NIGHT.equals(Prefs.themeMode());
        if (themeInitialized && nightTarget == target) return;
        themeInitialized = true;
        nightTarget = target;
        night = target ? 1 : 0;
        M_INK = Theme.color(Theme.TEXT_PRIMARY, target);
        M_MUTED = Theme.color(Theme.TEXT_SECONDARY, target);
        M_HAIRLINE = Theme.color(Theme.DIVIDER, target);
        M_UP = Theme.color(Theme.POSITIVE, target);
        M_DOWN = Theme.color(Theme.NEGATIVE, target);
        if (lTitle != null) applyTone();
        neutralGlass();
        heroDirty = statusDirty = tilesDirty = drawerDirty = gridDirty = true;
        invalidateAll();
    }

    /** Current endpoint: 0 for Day, 1 for Night. */
    private float dark() {
        return night;
    }

    /** The day color or the night color, depending on the night value (blended during the transition). */
    private int tone(int onLight, int onDark) {
        return blend(onLight, onDark, dark());
    }

    private float tone(float onLight, float onDark) {
        return onLight + (onDark - onLight) * dark();
    }

    /** Text colors for the current night value (dark by day, white at night). */
    private void applyTone() {
        lTitle.setColor(M_INK);
        lGreeting.setColor(M_MUTED);
        lTab.setColor(M_MUTED);
        lTabOn.setColor(M_INK);
        lCardTitle.setColor(M_INK);
        lCardMeta.setColor(M_MUTED);
        lQuote.setColor(M_INK);
        lTip.setColor(tone(L_INK, WHITE));
        lDate.setColor(tone(L_DIM, Theme.Legacy.Palette.TEXT_DIM_NIGHT));
        lClock.setColor(tone(L_INK, WHITE));
        lWeather.setColor(tone(L_DIM, Theme.Legacy.Palette.TEXT_DIM_NIGHT));
        lClockQuote.setColor(tone(L_DIM, Theme.Legacy.Palette.TEXT_DIM_NIGHT));
        staleQuote.setColor(tone(L_FAINT, Theme.Legacy.Palette.WHITE_A99));
        mLabel.setColor(M_INK);
        mLabelRight.setColor(M_INK);
        mCell.setColor(M_INK);
        mName.setColor(M_INK);
        mPrice.setColor(M_INK);
        mMuted.setColor(M_MUTED);
        mMutedRight.setColor(M_MUTED);
        mHour.setColor(M_MUTED);
        mHourTemp.setColor(M_INK);
        mBig.setColor(M_INK);
        mBigDegree.setColor(M_INK);
        teamBig.setColor(M_INK);
        teamSmall.setColor(M_MUTED);
        teamTag.setColor(M_UP);
    }

    private boolean tickHeroFade(long up) {
        if (heroFadeAt == 0) return false;
        if (fadeEase(heroFadeAt, up) >= 1) {
            heroFadeAt = 0;
            heroFrom = null;
        }
        return true;
    }

    private float fadeEase(long start, long up) {
        return fadeEase(start, up, FADE_MS);
    }

    private static float fadeEase(long start, long up, float duration) {
        float t = Ui.clamp((up - start) / duration);
        return t * t * (3 - 2 * t);
    }

    private static int mix(int a, int b, float f) {
        int r = Math.round(((a >> 16) & 255) * (1 - f) + ((b >> 16) & 255) * f);
        int g = Math.round(((a >> 8) & 255) * (1 - f) + ((b >> 8) & 255) * f);
        int bl = Math.round((a & 255) * (1 - f) + (b & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // MARK: - Background

    private void drawSky(Canvas c) {
        drawLightSky(c);
    }

    private void drawImage(Canvas c, Bitmap image, float alpha) {
        if (image == null || alpha <= 0.004f) return;
        imagePaint.setAlpha(Math.round(255 * Math.min(1, alpha)));
        c.drawBitmap(image, null, screen, imagePaint);
    }

    // MARK: - Hero

    private static void text(Canvas c, String s, float x, float y, Paint p, int color, float alpha) {
        if (s == null || s.isEmpty()) return;
        p.setColor(Ui.alpha(color, alpha));
        c.drawText(s, x, y, p);
    }

    // MARK: - Corner capsule: time and weather (when the hero shows a recommendation) and icon-only buttons

    /** Live TV, the inputs with a device on, All Apps and Settings. */
    private static List<HomeApps.Item> statusList(Context c) {
        List<HomeApps.Item> list = new ArrayList<>();
        list.addAll(HomeApps.shortcuts(c));
        list.add(HomeApps.settings());
        return list;
    }

    private void setStatusItems(List<HomeApps.Item> list) {
        boolean same = list.size() == statusItems.size();
        for (int i = 0; same && i < list.size(); i++) same = list.get(i).key.equals(statusItems.get(i).key);
        String focusedKey = statusIndex < statusItems.size() ? statusItems.get(statusIndex).key : null;
        statusItems = list;
        if (!same || statusFocus.length != list.size()) {
            statusFocus = new Spring[list.size()];
            statusRects = new RectF[list.size()];
            int index = Math.min(statusIndex, Math.max(0, list.size() - 1));
            for (int i = 0; i < statusFocus.length; i++) {
                statusFocus[i] = new Spring(Theme.Legacy.HomeMotion.STATUS_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.STATUS_FOCUS_DAMPING, 0);
                statusRects[i] = new RectF();
                if (list.get(i).key.equals(focusedKey)) index = i;   // a device came on: keep focus on the same button
            }
            statusIndex = index;
            if (zone == Z_STATUS && list.isEmpty()) zone = Z_SHELF;
            aim(statusFocus, zone == Z_STATUS ? statusIndex : -1, true);   // only the capsule's springs
            statusDirty = true;
        }
        layoutStatus();
    }

    private boolean showInfo() {
        return hero != null && !hero.clock;
    }

    private void layoutStatus() {
        if (capsuleText == null) return;
        int n = statusItems.size();
        float b = BUTTON * k, gap = 4 * k, pad = 6 * k;
        float info = 0;
        if (showInfo()) {
            info = 26 * k + capsuleText.measureText(clockText);
            HomeData.Weather wx = data.weather;
            if (!Double.isNaN(wx.temp) && Prefs.weather()) {
                info += 22 * k + Symbols.width(getContext(), Symbols.weather(wx.code, wx.isDay), 24 * k) + 8 * k
                        + capsuleText.measureText(HomeData.degrees(wx.temp));
            }
            info += 22 * k;
        }
        capsuleInfoW = info;
        float buttons = n * b + Math.max(0, n - 1) * gap;
        float right = CONTENT_R * k, top = CAPSULE_TOP * k;
        float left = right - pad - buttons - (info > 0 ? info + 12 * k : pad);
        capsuleRect.set(left, top, right, top + CAPSULE_H * k);
        float x = right - pad - buttons, cy = capsuleRect.centerY();
        for (int i = 0; i < n; i++) {
            statusRects[i].set(x, cy - b / 2, x + b, cy + b / 2);
            x += b + gap;
        }
        statusDirty = true;
    }

    // MARK: - Quotes and shelf

    private void setShelf(List<HomeApps.Item> list) {
        wake();
        cancelReorder(true);    // the list changed under the move (an app installed or removed)
        boolean same = list.size() == shelf.size();
        for (int i = 0; same && i < list.size(); i++) same = list.get(i).key.equals(shelf.get(i).key);
        shelf = list;
        if (!same || tileFocus.length != list.size()) {
            tileFocus = new Spring[list.size()];
            tileSlide = new Spring[list.size()];
            tileRects = new RectF[list.size()];
            for (int i = 0; i < tileFocus.length; i++) {
                tileFocus[i] = new Spring(Theme.Legacy.HomeMotion.TILE_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.TILE_FOCUS_DAMPING, 0);
                tileSlide[i] = new Spring(Theme.Legacy.HomeMotion.TILE_SLIDE_RESPONSE_S, Theme.Legacy.HomeMotion.TILE_SLIDE_DAMPING, 0);
                tileRects[i] = new RectF();
            }
            shelfIndex = Math.min(shelfIndex, Math.max(0, list.size() - 1));
            retarget(true);
            layoutShelf();
        }
        tilesDirty = true;
        platter.invalidate();
    }

    /** "NETFLIX · YOUTUBE · …": the shelf's names, at the right of the drawer's apps label. */
    private void updateAppsLine() {
        StringBuilder s = new StringBuilder();
        for (HomeApps.Item it : shelf) {
            if (it.kind != HomeApps.APP || it.label == null || it.label.isEmpty()) continue;
            if (s.length() > 0) s.append(" · ");
            s.append(it.label);
        }
        appsLine = s.toString().toUpperCase(Ui.locale());
        if (mLabelRight != null) {
            fitPaint.set(mLabelRight);
            appsLine = TextUtils.ellipsize(appsLine, fitPaint, (CONTENT_R - MARGIN - 300) * k, TextUtils.TruncateAt.END).toString();
        }
    }

    private void layoutShelf() {
        updateAppsLine();
        int n = shelf.size();
        float pad = SHELF_PAD * k, gap = TILE_GAP * k, th = L_TILE_H * k;
        float full = (CONTENT_R - MARGIN) * k;
        float tw = Math.min(TILE_MAX_W * k, (full - Math.max(0, n - 1) * gap) / Math.max(1, n));
        float width = n * tw + Math.max(0, n - 1) * gap, left = (w - width) / 2f;
        float top = L_PLATTER_TOP * k;
        platterRect.set(left - pad, top, left + width + pad, top + th + 2 * pad);
        float x = left, y = top + pad;
        // Whole pixels: an edge between two pixels is drawn as two half-lit rows, which reads as blur.
        for (int i = 0; i < n; i++) {
            tileRects[i].set(Math.round(x), Math.round(y), Math.round(x + tw), Math.round(y + th));
            x += tw + gap;
        }
        platterRect.set(Math.round(platterRect.left), Math.round(platterRect.top), Math.round(platterRect.right), Math.round(platterRect.bottom));
        crispLogos.clear();
    }

    /** One opaque surface behind the existing app tiles, moving with the drawer. */
    private void drawPlatter(Canvas c) {
        if (shelf.isEmpty()) return;
        fill.setShader(null);
        fill.setColor(Theme.color(Theme.SURFACE, night >= 0.5f));
        float radius = Theme.DOCK_RADIUS * k;
        c.drawRoundRect(platterRect, radius, radius, fill);
    }

    /** The quotes row's text and sizes, for one set of coins, prices and text size. */
    private final class Quotes {
        final int[] coins;
        final double[] prices;
        final float size, cap, halfCap;
        final String[] values;
        final float[] glyphs, widths;

        Quotes(int[] coins, double[] all, Paint text) {
            this.coins = coins;
            size = text.getTextSize();
            cap = Ui.cap(text);
            halfCap = Ui.halfCap(text);
            int n = coins.length;
            prices = new double[n];
            values = new String[n];
            glyphs = new float[n];
            widths = new float[n];
            for (int i = 0; i < n; i++) {
                prices[i] = all[coins[i]];
                values[i] = HomeData.compact(prices[i]);
                glyphs[i] = Glyphs.width(getContext(), HomeData.COINS[coins[i]], cap);
                widths[i] = text.measureText(values[i]);
            }
        }

        boolean matches(int[] now, double[] all, float textSize) {
            if (now != coins || textSize != size) return false;
            for (int i = 0; i < coins.length; i++) if (Double.compare(all[coins[i]], prices[i]) != 0) return false;
            return true;
        }
    }

    private Quotes quotes;

    /** Coin logo + price in the configured currency, in a row at the right end of the tab bar. */
    private void drawQuotes(Canvas c) {
        boolean clock = !picksOn();
        float right = platterRect.isEmpty() ? CONTENT_R * k : platterRect.right - SHELF_PAD * k;
        applyTone();
        Paint text = clock ? lClockQuote : lQuote;
        // The corner buttons' names appear in this row: the quotes fade out while one of them has focus.
        float fade = 1;
        for (int i = TAB_COUNT; i < TAB_COUNT + 3 && !clock; i++) fade = Math.min(fade, 1 - Ui.clamp(topFocus[i].value));
        if (fade <= 0.01f) return;
        int textColor = text.getColor();
        text.setColor(Ui.alpha(textColor, fade));
        int n = HomeData.coinCount();
        if (n == 0) { text.setColor(textColor); return; }
        // Measured once per set of prices: this row is redrawn on every frame of the cards' focus animation, and
        // measuring text there cost ~3 ms a frame on a Mi Box 3.
        int[] coins = HomeData.active();
        Quotes q = quotes;
        if (q == null || !q.matches(coins, data.prices, text.getTextSize())) {
            q = quotes = new Quotes(coins, data.prices, text);
        }
        // On the tab row, at the right.
        float base = clock ? L_QUOTES_BASE * k : L_TAB_BASE * k;
        float cap = q.cap, gap = (clock ? 8 : 6) * k, spacing = (clock ? 32 : 24) * k;
        float total = spacing * (n - 1);
        for (int i = 0; i < n; i++) total += q.glyphs[i] + gap + q.widths[i];
        float x = clock ? MARGIN * k : right - total;
        int coinColor = text.getColor();
        for (int i = 0; i < n; i++) {
            Glyphs.coin(getContext(), c, HomeData.COINS[coins[i]], x + q.glyphs[i] / 2, base - cap / 2, cap, coinColor);
            x += q.glyphs[i] + gap;
            c.drawText(q.values[i], x, base, text);
            x += q.widths[i] + spacing;
        }
        text.setColor(textColor);
        String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
        if (stale.isEmpty()) return;
        if (clock) c.drawText(stale, x - spacing + 24 * k + staleQuote.measureText(stale), base, staleQuote);
        else c.drawText(stale, right - total - 24 * k, base, staleQuote);
    }

    private void drawTiles(Canvas c) {
        // The focused one last: it grows over its neighbors without being covered by them.
        int top = -1;
        float most = 0.02f;
        for (int i = 0; i < shelf.size(); i++) {
            if (tileFocus[i].value > most) {
                most = tileFocus[i].value;
                top = i;
            }
        }
        if (liftIndex >= 0 && liftIndex < shelf.size() && moveLift.value > 0.02f) top = liftIndex;
        for (int pass = 0; pass < 2; pass++) {
            for (int i = 0; i < shelf.size(); i++) {
                if ((pass == 0) == (i == top)) continue;
                // Moving apps: the tile slides from where it was and the lifted one wiggles (only then: no extra work
                // otherwise).
                float dx = i < tileSlide.length ? tileSlide[i].value : 0;
                float angle = i == liftIndex ? wiggle * moveLift.value : 0;
                if (dx == 0 && angle == 0) {
                    drawShelfTile(c, i);
                    continue;
                }
                c.save();
                c.translate(dx, 0);
                c.rotate(angle, tileRects[i].centerX(), tileRects[i].centerY());
                drawShelfTile(c, i);
                c.restore();
            }
        }
    }

    /**
     * App tile: monochrome logo on the continuous dock. Focused: a rounded orange outline outside it
     * (legible from across the room, without relying on color). Lifted to be moved: a little bigger.
     */
    private void drawShelfTile(Canvas c, int i) {
        HomeApps.Item it = shelf.get(i);
        float l = i == liftIndex ? Math.max(0, moveLift.value) : 0;
        float f = Ui.clamp(tileFocus[i].value), s = 1 + LIFT_GROW * l;
        RectF base = tileRects[i];
        float bw = base.width(), bh = base.height(), cx = base.centerX(), cy = base.centerY();
        cell.set(cx - bw * s / 2, cy - bh * s / 2, cx + bw * s / 2, cy + bh * s / 2);
        if (it.logo != null) {
            drawLogo(c, it, cell, s);
        } else if (it.tile != null) {
            // If extraction is unavailable, retain recognizable TV art in grayscale.
            bitmapPaint.setColorFilter(grayscale);
            imageFill.setColorFilter(grayscale);
            roundImage(c, it.tile, cell, Theme.POSTER_RADIUS * k);
            bitmapPaint.setColorFilter(null);
            imageFill.setColorFilter(null);
        }
        if (f > 0.01f) {
            float out = Theme.Legacy.HomeEffect.FOCUS_GAP * k + Theme.Legacy.HomeEffect.FOCUS_HALF_STROKE * k;
            tmp.set(cell.left - out, cell.top - out, cell.right + out, cell.bottom + out);
            rim.setShader(null);
            rim.setColor(Ui.alpha(Theme.color(Theme.FOCUS, night >= 0.5f), f));
            rim.setStrokeWidth(Theme.Legacy.HomeEffect.FOCUS_STROKE * k);
            float radius = Theme.APP_FOCUS_RADIUS * k * s;
            c.drawRoundRect(tmp, radius, radius, rim);
        }
    }

    /**
     * `image` stretched over `cell`, with corners of radius `r`. Android 12+ clips to the rounded shape; on Android 9 to
     * 11 that anti-aliased clip costs the GPU a mask on every frame of a focus animation (measured on a Mi Box 3: most of
     * a 49 ms frame), so there the image is the shader of a rounded rectangle instead, which looks the same.
     */
    private void roundImage(Canvas c, Bitmap image, RectF cell, float r) {
        if (!Ui.LEGACY) {
            clip.reset();
            clip.addRoundRect(cell, r, r, Path.Direction.CW);
            c.save();
            c.clipPath(clip);
            c.drawBitmap(image, null, cell, bitmapPaint);
            c.restore();
            return;
        }
        BitmapShader shader = imageShaders.get(image);
        if (shader == null) {
            shader = new BitmapShader(image, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            imageShaders.put(image, shader);
        }
        imageMatrix.setScale(cell.width() / image.getWidth(), cell.height() / image.getHeight());
        imageMatrix.postTranslate(cell.left, cell.top);
        shader.setLocalMatrix(imageMatrix);
        imageFill.setShader(shader);
        c.drawRoundRect(cell, r, r, imageFill);
        imageFill.setShader(null);
    }

    /**
     * Logo at its "optical" size (all with the same amount of ink, plus each one's own adjustment, never past 80% × 62%
     * of the tile), centered and already filtered down to the focused tile size: when focused it is drawn pixel for
     * pixel, without resampling.
     */
    private void drawLogo(Canvas c, HomeApps.Item it, RectF box, float s) {
        Bitmap logo = crispLogo(it);
        if (logo == null) return;
        float scale = s;   // made at the tile's own size: drawn pixel for pixel at rest
        float dw = logo.getWidth() * scale, dh = logo.getHeight() * scale;
        float left = box.centerX() - dw / 2, top = box.centerY() - dh / 2;
        if (scale > 0.998f) {
            left = Math.round(left);
            top = Math.round(top);
        }
        tmp.set(left, top, left + dw, top + dh);
        bitmapPaint.setColorFilter(it.monochromeLogo == null ? grayscale : (night >= 0.5f ? nightLogo : dayLogo));
        c.drawBitmap(logo, null, tmp, bitmapPaint);
        bitmapPaint.setColorFilter(null);
    }

    private Bitmap crispLogo(HomeApps.Item it) {
        Bitmap src = it.monochromeLogo != null ? it.monochromeLogo : it.logo;
        if (tileRects.length == 0) return src;
        String key = it.key + "@" + System.identityHashCode(src) + "@" + Math.round(tileRects[0].width());
        Bitmap cached = crispLogos.get(key);
        if (cached != null) return cached;
        float bw = tileRects[0].width(), bh = tileRects[0].height();
        float lw = src.getWidth(), lh = src.getHeight();
        float scale = (float) Math.sqrt(bw * bh * 0.085f / (lw * lh * Math.max(0.08f, it.logoInk))) * it.logoScale;
        scale = Math.min(scale, Math.min(bw * 0.76f / lw, bh * 0.58f / lh));
        Bitmap out = downscale(src, Math.round(lw * scale), Math.round(lh * scale));
        crispLogos.put(key, out);
        return out;
    }

    /** Halves the size while it can (a 2×2 average, no aliasing), then finishes at the exact size. */
    static Bitmap downscale(Bitmap src, int w, int h) {
        w = Math.max(1, w);
        h = Math.max(1, h);
        Bitmap b = src;
        while (b.getWidth() / 2 >= w && b.getHeight() / 2 >= h) {
            Bitmap half = Bitmap.createScaledBitmap(b, b.getWidth() / 2, b.getHeight() / 2, true);
            if (b != src) b.recycle();
            b = half;
        }
        Bitmap out = Bitmap.createScaledBitmap(b, w, h, true);
        if (b != src && b != out) b.recycle();
        return out;
    }

    // MARK: - Bottom drawer

    private static List<HomeApps.Item> drawerList(Context c) {
        List<HomeApps.Item> list = HomeApps.allInputs(c);
        list.addAll(HomeApps.drawerShortcuts());
        return list;
    }

    /** The sleep timer's tile counts down the minutes. */
    private void updateSleepLabel() {
        for (HomeApps.Item it : drawerItems) {
            if (it.kind != HomeApps.SLEEP) continue;
            String label = SleepTimer.label(activity);
            if (!label.equals(it.label)) {
                it.label = label;
                drawerDirty = true;
            }
        }
    }

    private void setDrawerItems(List<HomeApps.Item> list) {
        int inputs = 0;
        while (inputs < list.size() && (list.get(inputs).kind == HomeApps.INPUT || list.get(inputs).kind == HomeApps.LIVE_TV)) inputs++;
        boolean same = list.size() == drawerItems.size();
        for (int i = 0; same && i < list.size(); i++) {
            same = list.get(i).key.equals(drawerItems.get(i).key) && list.get(i).connected == drawerItems.get(i).connected;
        }
        drawerItems = list;
        drawerInputs = inputs;
        updateSleepLabel();
        if (drawerFocus.length != list.size()) {
            drawerFocus = new Spring[list.size()];
            drawerRects = new RectF[list.size()];
            for (int i = 0; i < drawerFocus.length; i++) {
                drawerFocus[i] = new Spring(Theme.Legacy.HomeMotion.DRAWER_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.DRAWER_FOCUS_DAMPING, 0);
                drawerRects[i] = new RectF();
            }
            drawerIndex = Math.min(drawerIndex, Math.max(0, list.size() - 1));
            retarget(true);
        }
        layoutDrawer();
        if (!same) drawerDirty = true;
    }

    private void layoutDrawer() {
        int n = drawerItems.size();
        float left = MARGIN * k, right = CONTENT_R * k;
        // Inputs and shortcuts: two equal halves 48 px apart, each split evenly into cells (no gaps: hairlines).
        float groupGap = Theme.Legacy.HomeLayout.DRAWER_GROUP_GAP * k, top = ROW_TOP * k;
        int groups = drawerInputs > 0 && drawerInputs < n ? 2 : 1;
        float half = groups == 2 ? (right - left - groupGap) / 2 : right - left;
        for (int i = 0; i < n; i++) {
            boolean second = groups == 2 && i >= drawerInputs;
            int count = groups == 2 ? (second ? n - drawerInputs : drawerInputs) : n, at = second ? i - drawerInputs : i;
            float x0 = left + (second ? half + groupGap : 0), cw = half / Math.max(1, count);
            drawerRects[i].set(Math.round(x0 + at * cw), Math.round(top), Math.round(x0 + (at + 1) * cw), Math.round(top + ROW_H * k));
        }
        float cardsTop = CARDS_TOP * k, split = left + (right - left) * 0.585f;
        float weatherRight = split - 16 * k, quotesLeft = split + 16 * k;
        if (groups == 2) {
            // The cards line up with the rows above: the weather under the inputs, the quotes under the shortcuts.
            weatherRight = drawerRects[drawerInputs - 1].right;
            quotesLeft = drawerRects[drawerInputs].left;
            right = drawerRects[n - 1].right;
        }
        boolean wx = Prefs.weather(), qt = HomeData.quotesCard();
        // A card that is off gives its room to the other one.
        weatherCard.set(Math.round(left), Math.round(cardsTop), Math.round(qt ? weatherRight : right), Math.round(cardsTop + CARDS_H * k));
        quotesCard.set(Math.round(wx ? quotesLeft : left), Math.round(cardsTop), Math.round(right), Math.round(cardsTop + CARDS_H * k));
        drawerDirty = true;
    }

    private void openDrawer() {
        if (drawerItems.isEmpty()) return;
        drawerOpen = true;
        drawer.target = 1;
        zone = Z_DRAWER;
        drawerIndex = shelf.isEmpty() ? 0 : nearest(drawerRects, tileRects[shelfIndex].centerX());
        for (int i = 0; i < drawerFocus.length; i++) drawerFocus[i].snap(i == drawerIndex ? 1 : 0);
        drawerDirty = true;
    }

    private void closeDrawer() {
        drawerOpen = false;
        drawer.target = 0;
        zone = Z_SHELF;
        for (Spring f : drawerFocus) f.snap(0);   // the drawer goes away: its focus doesn't need to animate
        if (!shelf.isEmpty() && drawerIndex < drawerRects.length) shelfIndex = nearest(tileRects, drawerRects[drawerIndex].centerX());
    }

    /** Color for the drawer's text and symbols: dark by day, white at night. */
    private int drawerInk(float alpha) {
        return Ui.alpha(tone(L_INK, Theme.Legacy.Palette.WHITE_AFF), alpha);
    }

    /** The drawer's sections have no box: only their content and hairlines. */
    private void drawerCard(Canvas c, RectF r) {
    }

    private void drawDrawer(Canvas c) {
        applyTone();
        if (drawerItems.isEmpty()) return;
        // The apps (the shelf, moved up with the drawer open): a caps label and their names over its top hairline.
        if (!shelf.isEmpty()) {
            float base = tileRects[0].top - SHIFT * k - 54 * k;
            c.drawText(caps(Ui.tr("Apps", "Apps")), MARGIN * k, base, mLabel);
            c.drawText(appsLine, CONTENT_R * k, base, mLabelRight);
        }
        float titleBase = ROW_TITLE * k;
        if (drawerInputs > 0) {
            c.drawText(caps(Ui.tr("Entradas", "Inputs")), drawerRects[0].left, titleBase, mLabel);
            drawCellRow(c, 0, drawerInputs);
        }
        if (drawerInputs < drawerItems.size()) {
            c.drawText(caps(Ui.tr("Atalhos", "Shortcuts")), drawerRects[drawerInputs].left, titleBase, mLabel);
            drawCellRow(c, drawerInputs, drawerItems.size());
        }
        for (int i = 0; i < drawerItems.size(); i++) drawDrawerTile(c, i);
        if (Prefs.weather()) drawWeatherCard(c, weatherCard);
        if (HomeData.quotesCard()) drawQuotesCard(c, quotesCard);
    }

    /** A group of cells: hairlines above and below it and between the cells. */
    private void drawCellRow(Canvas c, int from, int to) {
        RectF a = drawerRects[from], b = drawerRects[to - 1];
        hairline(c, a.left, b.right, a.top);
        hairline(c, a.left, b.right, a.bottom);
        for (int i = from + 1; i < to; i++) vline(c, drawerRects[i].left, a.top, a.bottom);
    }

    /** A drawer cell: symbol and caps name; an input with no device is dimmed. Focused: it inverts (white, black type). */
    private void drawDrawerTile(Canvas c, int i) {
        HomeApps.Item it = drawerItems.get(i);
        float f = Ui.clamp(drawerFocus[i].value);
        RectF cellRect = drawerRects[i];
        float cx = cellRect.centerX();
        if (f > 0.01f) {
            fill.setShader(null);
            fill.setColor(Ui.alpha(Theme.color(Theme.FOCUS, night >= 0.5f), f));
            c.drawRect(cellRect, fill);
        }
        boolean off = it.kind == HomeApps.INPUT && !it.connected;
        int ink = blend(off ? Theme.color(Theme.DISABLED, night >= 0.5f) : M_INK, Theme.color(Theme.ON_ACCENT_SMALL, night >= 0.5f), f);
        Symbols.drawSharp(getContext(), c, it.symbol, cx, cellRect.top + cellRect.height() * 0.36f, 34 * k, ink);
        String label = caps(it.label);
        mCell.setColor(ink);
        float room = cellRect.width() - 16 * k, wide = mCell.measureText(label);
        // A long name shrinks a little before it gets cut.
        if (wide > room) mCell.setTextSize(Math.max(Theme.Legacy.HomeText.M_CELL_MIN_SIZE * k, Theme.Legacy.HomeText.M_CELL_SIZE * k * room / wide));
        c.drawText(label, cx, cellRect.top + cellRect.height() * 0.74f, mCell);
        mCell.setTextSize(Theme.Legacy.HomeText.M_CELL_SIZE * k);
        if (it.kind == HomeApps.INPUT && it.number != null && Cec.on(Integer.parseInt(it.number))) {
            // The device on this port is on (HDMI-CEC): a dot at the top right (cyan: the system has no green).
            fill.setShader(null);
            fill.setColor(f > 0.5f ? Theme.Legacy.Palette.CEC_FOCUSED : Theme.color(Theme.CEC_ACTIVE, night >= 0.5f));
            c.drawCircle(cellRect.right - Theme.Legacy.HomeLayout.CEC_INSET_X * k, cellRect.top + Theme.Legacy.HomeLayout.CEC_INSET_Y * k, Theme.Legacy.HomeLayout.CEC_DOT_RADIUS * k, fill);
        }
    }

    /** Weather for your location: now (large), the next 6 hours, and sunrise and sunset. */
    private void drawWeatherCard(Canvas c, RectF r) {
        drawerCard(c, r);
        if (Ui.LEGACY) weatherSnapshot.draw(c, r, drawerCardsKey(), cc -> drawWeather(cc, r));
        else drawWeather(c, r);
    }

    private void drawWeather(Canvas c, RectF r) {
        refreshDrawerText();
        float x = r.left, labelBase = r.top + Theme.Legacy.HomeLayout.WIDGET_LABEL_BASE * k, rule = r.top + Theme.Legacy.HomeLayout.WIDGET_RULE_Y * k;
        c.drawText(caps(weatherTitle), x, labelBase, mLabel);
        hairline(c, r.left, r.right, rule);
        HomeData.Weather wx = textWeather;
        if (Double.isNaN(wx.temp)) {
            c.drawText(Ui.tr("Sem dados do clima agora.", "No weather data right now."), x, r.top + 120 * k, mMuted);
            return;
        }
        // The condition at the right of the label row, with its symbol after it.
        String condition = caps(HomeData.condition(wx.code, wx.isDay)), symbol = Symbols.weather(wx.code, wx.isDay);
        float sw = Symbols.width(getContext(), symbol, 26 * k);
        Symbols.drawSharp(getContext(), c, symbol, r.right - sw / 2, labelBase - Ui.halfCap(mLabel), 26 * k, M_INK);
        c.drawText(condition, r.right - sw - 12 * k, labelBase, mLabelRight);
        // The number big and tight, its degree sign small at the top (like a superscript).
        float tempBase = r.top + Theme.Legacy.HomeLayout.TEMPERATURE_BASE * k, tx = x - Ui.leftBearing(mBig, nowDigits);
        c.drawText(nowDigits, tx, tempBase, mBig);
        if (nowDigits.length() < nowTemp.length()) {
            c.drawText("°", tx + mBig.measureText(nowDigits) + 4 * k, tempBase - Ui.cap(mBig) + Ui.cap(mBigDegree) * 0.9f, mBigDegree);
        }
        c.drawText(highLow, x, r.top + Theme.Legacy.HomeLayout.WEATHER_FOOTER_BASE * k, mMuted);
        int hours = weatherHours();
        float hx = r.left + Theme.Legacy.HomeLayout.WEATHER_HOURS_X * k, hw = (r.right - hx) / hours;
        for (int i = 0; i < wx.hours.size() && i < hours; i++) {
            float hc = hx + hw * (i + 0.5f);
            if (i > 0) vline(c, hx + hw * i, r.top + 76 * k, r.top + 251 * k);
            c.drawText(hourText[i], hc, r.top + Theme.Legacy.HomeLayout.WEATHER_HOUR_BASE * k, mHour);
            Symbols.drawSharp(getContext(), c, Symbols.weather(wx.hourCode.get(i), wx.hourDay.get(i)), hc, r.top + Theme.Legacy.HomeLayout.WEATHER_ICON_Y * k, 32 * k, M_INK);
            c.drawText(hourTempText[i], hc, r.top + Theme.Legacy.HomeLayout.WEATHER_HOUR_TEMP_BASE * k, mHourTemp);
        }
        c.drawText(sunLine, hx, r.top + Theme.Legacy.HomeLayout.WEATHER_FOOTER_BASE * k, mMuted);
    }

    /** Next hours in the weather card: five beside the quotes card, six with the whole width. */
    private static int weatherHours() {
        return HomeData.quotesCard() ? 5 : 6;
    }

    private void refreshDrawerText() {
        HomeData.Weather wx = data.weather;
        boolean settings = Prefs.version != textPrefs || !Ui.language.equals(textLanguage);
        textPrefs = Prefs.version;
        textLanguage = Ui.language;
        if (wx != textWeather || settings) {
            textWeather = wx;
            weatherTitle = Prefs.city().isEmpty() ? Ui.tr("Clima", "Weather") : Prefs.city();
            nowTemp = HomeData.degrees(wx.temp);
            nowDigits = nowTemp.endsWith("°") ? nowTemp.substring(0, nowTemp.length() - 1) : nowTemp;
            highLow = Ui.ru ? "Макс. " + HomeData.degrees(wx.max) + "  ·  Мин. " + HomeData.degrees(wx.min)
                    : Ui.en ? "H " + HomeData.degrees(wx.max) + "  ·  L " + HomeData.degrees(wx.min)
                    : Ui.de ? "H " + HomeData.degrees(wx.max) + "  ·  T " + HomeData.degrees(wx.min)
                    : "Máx. " + HomeData.degrees(wx.max) + "  ·  Mín. " + HomeData.degrees(wx.min);
            for (int i = 0; i < wx.hours.size() && i < 6; i++) {
                hourText[i] = HomeData.hour(wx.hours.get(i));
                hourTempText[i] = HomeData.degrees(wx.hourTemp.get(i));
            }
            sunriseText = Ui.tr("Nascer ", "Sunrise ") + Ui.time(wx.sunrise);
            sunsetText = Ui.tr("Pôr do sol ", "Sunset ") + Ui.time(wx.sunset);
            sunLine = wx.sunrise == 0 ? "" : wx.sunset == 0 ? sunriseText : sunriseText + "  ·  " + sunsetText;
        }
        long minute = System.currentTimeMillis() / 60_000;
        if (data.pricesAt != textPricesAt || minute != textMinute || settings) {
            textPricesAt = data.pricesAt;
            textMinute = minute;
            String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
            staleText = !stale.isEmpty() ? Ui.tr("de ", "", "de ") + stale : changeLabel();
            for (int i = 0; i < HomeData.COINS.length; i++) {
                priceText[i] = HomeData.quote(i, data.prices[i]);
                changeText[i] = HomeData.change(i, data.changes[i]);
            }
            nameColumn = priceColumn = 0;
            int[] coins = HomeData.active();
            for (int j = 0; j < coins.length && j < HomeData.CARD_ROWS; j++) {
                nameColumn = Math.max(nameColumn, mName.measureText(caps(HomeData.coinName(coins[j]))));
                priceColumn = Math.max(priceColumn, mPrice.measureText(priceText[coins[j]]));
            }
        }
    }

    /** The coins' change is over 24 h; the dollar's, the euro's and the Ibovespa's, since the last close. */
    private static String changeLabel() {
        return HomeData.anyMarket() ? Ui.tr("variação", "change") : Ui.tr("variação em 24 h", "24h change");
    }

    private static String quotesTitle() {
        return HomeData.anyMarket() ? Ui.tr("Cotações", "Markets") : Ui.tr("Cotações", "Crypto");
    }

    private float sunItem(Canvas c, String symbol, String label, float x, float base) {
        float size = 22 * k, sw = Symbols.width(getContext(), symbol, size);
        Symbols.draw(getContext(), c, symbol, x + sw / 2, base - Ui.halfCap(cardDim), size, tone(L_DIM, Theme.Legacy.Palette.WHITE_AB3));
        c.drawText(label, x + sw + 10 * k, base, cardDim);
        return x + sw + 10 * k + cardDim.measureText(label);
    }

    /** Prices in the configured currency with the 24 h change (green/red, like Apple's Stocks app). */
    private void drawQuotesCard(Canvas c, RectF r) {
        drawerCard(c, r);
        if (Ui.LEGACY) quotesSnapshot.draw(c, r, drawerCardsKey(), cc -> drawPrices(cc, r));
        else drawPrices(c, r);
    }

    /**
     * What the drawer cards show depends on: the data, the settings, the language, day or night, and the minute (the
     * sunrise and sunset, "12 min ago").
     */
    private String drawerCardsKey() {
        return data.weatherAt + "|" + data.pricesAt + "|" + data.sparkAt + "|" + Prefs.version + "|" + Ui.language + "|"
                + Float.floatToIntBits(dark()) + "|" + System.currentTimeMillis() / 60_000;
    }

    /**
     * Android 9 to 11: a drawer card's content (its text and symbols, not its glass) kept as a bitmap. The drawer is
     * redrawn on every frame of its focus animation, and laying that text out again took ~15 ms a frame on a Mi Box 3.
     */
    private static final class Snapshot {
        private Bitmap bitmap;
        private String key = "";

        void draw(Canvas c, RectF r, String key, Consumer<Canvas> content) {
            int left = Math.round(r.left), top = Math.round(r.top);
            int w = Math.max(1, Math.round(r.right) - left), h = Math.max(1, Math.round(r.bottom) - top);
            if (bitmap == null || bitmap.getWidth() != w || bitmap.getHeight() != h) {
                bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                this.key = "";
            }
            if (!key.equals(this.key)) {
                bitmap.eraseColor(0);
                Canvas cc = new Canvas(bitmap);
                cc.translate(-left, -top);
                content.accept(cc);
                this.key = key;
            }
            c.drawBitmap(bitmap, left, top, null);
        }
    }

    private final Snapshot weatherSnapshot = new Snapshot(), quotesSnapshot = new Snapshot();

    private void drawPrices(Canvas c, RectF r) {
        float x = r.left, right = r.right;
        refreshDrawerText();
        float labelBase = r.top + Theme.Legacy.HomeLayout.WIDGET_LABEL_BASE * k;
        c.drawText(caps(quotesTitle()), x, labelBase, mLabel);
        c.drawText(caps(staleText), right, labelBase, mLabelRight);
        hairline(c, r.left, r.right, r.top + 53 * k);
        float cap = Ui.cap(mName);
        float priceRight = right - 150 * k;
        // The mini charts sit between the names and the prices, as wide as that room allows (up to 170 px).
        float sparkRight = priceRight - priceColumn - 32 * k;
        float sparkLeft = Math.max(x + 44 * k + nameColumn + 32 * k, sparkRight - 170 * k);
        int[] coins = HomeData.active();
        for (int j = 0; j < coins.length && j < HomeData.CARD_ROWS; j++) {
            int i = coins[j];
            float rule = r.top + (117 + 64 * j) * k, base = rule - 21 * k, mid = base - cap / 2;
            Glyphs.coin(getContext(), c, HomeData.COINS[i], x + 8 * k, mid, cap * 0.85f, M_MUTED);
            c.drawText(caps(HomeData.coinName(i)), x + 44 * k, base, mName);
            c.drawText(priceText[i], priceRight, base, mPrice);
            double change = data.changes[i];
            int color = Double.isNaN(change) || HomeData.flat(i, change) ? M_MUTED : change >= 0 ? M_UP : M_DOWN;
            float[] line = data.spark[i];
            if (line != null && sparkRight - sparkLeft > 48 * k) drawSpark(c, line, sparkLeft, sparkRight, mid, color);
            if (!Double.isNaN(change)) {
                mChange.setColor(color);
                c.drawText(changeText[i], right, base, mChange);
            }
            hairline(c, r.left, r.right, rule);
        }
    }

    /** A quote's mini chart: its line from the lowest to the highest point, 28 px tall, centered on the row. */
    private void drawSpark(Canvas c, float[] line, float left, float right, float mid, int color) {
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (float v : line) {
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        float h = 28 * k, span = hi - lo;
        int points = Math.min(line.length, sparkPoints.length / 4 + 1);
        float step = (line.length - 1) / (float) (points - 1);
        int n = 0;
        float px = 0, py = 0;
        for (int p = 0; p < points; p++) {
            float v = line[Math.round(p * step)];
            float cx = left + (right - left) * p / (points - 1);
            float cy = span > 0 ? mid + h / 2 - (v - lo) / span * h : mid;
            if (p > 0) {
                sparkPoints[n++] = px;
                sparkPoints[n++] = py;
                sparkPoints[n++] = cx;
                sparkPoints[n++] = cy;
            }
            px = cx;
            py = cy;
        }
        sparkPaint.setStyle(Paint.Style.STROKE);
        sparkPaint.setStrokeWidth(Theme.Legacy.HomeEffect.SPARK_STROKE * k);
        sparkPaint.setStrokeCap(Paint.Cap.ROUND);
        sparkPaint.setColor(color);
        c.drawLines(sparkPoints, 0, n, sparkPaint);
    }

    // MARK: - Focus

    /** Sets the springs' targets from the current focus (snap: no animation). */
    private void retarget(boolean snap) {
        // Only the group whose focus changed is redrawn (moving along the shelf doesn't redraw the hero or capsule).
        boolean tilesChanged = aim(tileFocus, !sheetOpen && zone == Z_SHELF ? shelfIndex : -1, snap);
        boolean statusChanged = aim(statusFocus, !sheetOpen && zone == Z_STATUS ? statusIndex : -1, snap);
        boolean drawerChanged = aim(drawerFocus, !sheetOpen && zone == Z_DRAWER ? drawerIndex : -1, snap);
        boolean heroChanged = heroFocus.target != (!sheetOpen && zone == Z_HERO ? 1 : 0);
        heroFocus.target = !sheetOpen && zone == Z_HERO ? 1 : 0;
        if (snap) heroFocus.snap(heroFocus.target);
        boolean topChanged = aim(topFocus, !sheetOpen && zone == Z_TOP ? topIndex : -1, snap);
        heroChanged |= aim(cardFocus, !sheetOpen && zone == Z_CARDS ? cardIndex : -1, snap) | topChanged;
        statusChanged |= topChanged;
        tilesDirty |= snap || tilesChanged;
        statusDirty |= snap || statusChanged;
        heroDirty |= snap || heroChanged;
        drawerDirty |= snap || drawerChanged;
        spoken.focusMoved();
    }

    /** Aims a group's springs (only `focused` at 1); returns true if any target changed. */
    private static boolean aim(Spring[] springs, int focused, boolean snap) {
        boolean changed = false;
        for (int i = 0; i < springs.length; i++) {
            float target = i == focused ? 1 : 0;
            changed |= springs[i].target != target;
            springs[i].target = target;
            if (snap) springs[i].snap(target);
        }
        return changed;
    }

    // MARK: - "All Apps" sheet

    private void setOthers(List<HomeApps.Item> list) {
        others = list;
        gridFocus = new Spring[list.size()];
        for (int i = 0; i < gridFocus.length; i++) gridFocus[i] = new Spring(Theme.Legacy.HomeMotion.GRID_FOCUS_RESPONSE_S, Theme.Legacy.HomeMotion.GRID_FOCUS_DAMPING, 0);
        gridLabels = null;
        gridIndex = Math.min(gridIndex, Math.max(0, list.size() - 1));
        retargetGrid(true);
        gridDirty = true;
    }

    private void openSheet() {
        sheetOpen = true;
        gridIndex = 0;
        scroll.snap(0);
        retargetGrid(true);
        retarget(false);
        sheet.target = 1;
        gridDirty = true;
    }

    private void closeSheet() {
        sheetOpen = false;
        sheet.target = 0;
        retarget(false);
    }

    private void retargetGrid(boolean snap) {
        for (int i = 0; i < gridFocus.length; i++) {
            gridFocus[i].target = sheetOpen && i == gridIndex ? 1 : 0;
            if (snap) gridFocus[i].snap(gridFocus[i].target);
        }
        int rows = Math.max(1, (others.size() + COLUMNS - 1) / COLUMNS);
        int visible = Math.min(rows, VISIBLE_ROWS);
        int r = gridIndex / COLUMNS;
        float target = scroll.target;
        if (r < target) target = r;
        else if (r > target + visible - 1) target = r - visible + 1;
        scroll.target = Math.max(0, Math.min(rows - visible, target));
        if (snap) scroll.snap(scroll.target);
        spoken.focusMoved();
    }

    private void drawSheet(Canvas c) {
        c.drawColor(Theme.Legacy.Palette.BLACK_AA8);
        float shift = scroll.value * GRID_ROW * k;
        c.drawText(Ui.tr("Todos os Apps", "All Apps"), w / 2f, 124 * k - shift, sheetTitle);
        int n = others.size();
        if (gridLabels == null || gridLabels.length != n) {
            gridLabels = new String[n];
            for (int i = 0; i < n; i++) {
                gridLabels[i] = TextUtils.ellipsize(others.get(i).label, gridLabel, (GRID_W - 10) * k, TextUtils.TruncateAt.END).toString();
            }
        }
        float gridWidth = COLUMNS * GRID_W + (COLUMNS - 1) * GRID_GAP;
        float left = (1920 - gridWidth) / 2 * k;
        float gridTop = GRID_TOP * k;
        for (int i = 0; i < n; i++) {
            int r = i / COLUMNS, col = i % COLUMNS;
            float y = gridTop + r * GRID_ROW * k - shift;
            if (y > h || y + GRID_ROW * k < 0) continue;
            float x = left + col * (GRID_W + GRID_GAP) * k;
            gridCell.set(x, y, x + GRID_W * k, y + GRID_H * k);
            float f = gridFocus[i].value;
            drawCard(c, others.get(i).tile, gridCell, GRID_RADIUS * k, f, 0.08f);
            if (sheetOpen && i == gridIndex) focusedCell.set(cell);
            gridLabel.setColor(blend(Theme.Legacy.Palette.WHITE_A99, Theme.Legacy.Palette.WHITE_AFF, Ui.clamp(f)));
            c.drawText(gridLabels[i], gridCell.centerX(), gridCell.bottom + (40 + 6 * f) * k, gridLabel);
        }
    }

    /** 16:9 sheet card. Focused (the shelf's subtle focus): it grows, with a shadow, a soft sheen and a thin border. */
    private void drawCard(Canvas c, Bitmap image, RectF base, float radius, float f, float grow) {
        float s = 1 + grow * f;
        float cx = base.centerX(), cy = base.centerY();
        cell.set(cx - base.width() * s / 2, cy - base.height() * s / 2, cx + base.width() * s / 2, cy + base.height() * s / 2);
        float r = radius * s;
        float fc = Ui.clamp(f);
        if (f > 0.01f) {
            tmp.set(cell);
            tmp.offset(0, 16 * k * f);
            shadow.setColor(Ui.alpha(Theme.Legacy.Palette.BLACK_AFF, Theme.Legacy.HomeEffect.GRID_SHADOW_ALPHA * fc));
            shadows.draw(c, tmp, r, SHADOW_BLUR * k, shadow);
        }
        if (image != null) roundImage(c, image, cell, r);
        if (f > 0.01f) {
            float bh = base.height();
            if (cardSheen == null || cardSheenH != bh) {
                cardSheen = new LinearGradient(0, -bh / 2, 0, 0, Theme.Legacy.Palette.WHITE_A1A, Theme.Legacy.Palette.WHITE_A00, Shader.TileMode.CLAMP);
                cardSheenH = bh;
            }
            c.save();
            c.translate(cx, cy);
            c.scale(s, s);
            tmp.set(-base.width() / 2, -bh / 2, base.width() / 2, bh / 2);
            sheen.setShader(cardSheen);
            sheen.setAlpha(Math.round(255 * fc));
            c.drawRoundRect(tmp, radius, radius, sheen);
            sheen.setAlpha(255);
            c.restore();
        }
        if (fc < 0.99f) {
            veil.setColor(Ui.alpha(Theme.Legacy.Palette.BLACK_AFF, Theme.Legacy.HomeEffect.GRID_VEIL_ALPHA * (1 - fc)));
            c.drawRoundRect(cell, r, r, veil);
        }
        border.setColor(Theme.Legacy.Palette.WHITE_A14);
        c.drawRoundRect(cell, r, r, border);
        if (f > 0.01f) {
            rim.setShader(null);
            rim.setColor(Ui.alpha(Theme.color(Theme.FOCUS, night >= 0.5f), fc));
            rim.setStrokeWidth(Theme.Legacy.HomeEffect.GRID_FOCUS_STROKE * k);
            c.drawRoundRect(cell, r, r, rim);
            rim.setStrokeWidth(Theme.Legacy.HomeEffect.RIM_STROKE * k);
        }
    }

    // MARK: - Toasts

    void showToast(String text) {
        toasts.add(text);
        announceForAccessibility(text);
        wake();
    }

    /** Advances the toast queue; returns true while a toast fades in or out (animation). */
    private boolean tickToast(long up) {
        if (toast != null && up - toastAt > TOAST_MS) {
            toast = null;
            toastLayer.invalidate();
        }
        if (toast == null && toastPaint != null) {
            String next = toasts.poll();
            if (next != null) {
                toast = layout(next);
                toastAt = up;
            }
        }
        if (toast == null) return false;
        long age = up - toastAt;
        boolean fading = age < Theme.Legacy.HomeMotion.TOAST_ENTER_REDRAW_MS || age > TOAST_MS - Theme.Legacy.HomeMotion.TOAST_EXIT_REDRAW_MS;
        if (fading) toastLayer.invalidate();
        return fading;
    }

    private void drawToast(Canvas c) {
        if (toast == null) return;
        float age = (SystemClock.uptimeMillis() - toastAt) / 1000f;
        float a = Math.min(Ui.clamp(age / Theme.Legacy.HomeMotion.TOAST_ENTER_S), Ui.clamp((TOAST_MS / 1000f - age) / Theme.Legacy.HomeMotion.TOAST_EXIT_S));
        if (a <= 0) return;
        float padX = 28 * k, padY = 18 * k, icon = 26 * k, iconW = Symbols.width(getContext(), "bell", icon);
        float tw = toast.getWidth() + iconW + 16 * k, th = toast.getHeight();
        float toastTop = 64 * k - (1 - a) * 16 * k;
        tmp.set((w - tw) / 2f - padX, toastTop, (w + tw) / 2f + padX, toastTop + th + 2 * padY);
        int save = c.saveLayerAlpha(tmp.left - 4, tmp.top - 4, tmp.right + 4, tmp.bottom + 4, Math.round(255 * a));
        scene.glass(c, tmp, Math.min(tmp.height() / 2, 32 * k), SMOKE, 0.55f, 0, 0, 0.02f, 1);
        Symbols.draw(getContext(), c, "bell", tmp.left + padX + iconW / 2, tmp.centerY(), icon, Theme.Legacy.Palette.WHITE_AEB);
        c.translate(tmp.left + padX + iconW + 16 * k, tmp.top + padY);
        toast.draw(c);
        c.restoreToCount(save);
    }

    private StaticLayout layout(String text) {
        int max = Math.round(720 * k);
        int width = (int) Math.min(max, Math.ceil(Layout.getDesiredWidth(text, toastPaint)));
        return StaticLayout.Builder.obtain(text, 0, text.length(), toastPaint, Math.max(1, width))
                .setMaxLines(2)
                .setEllipsize(TextUtils.TruncateAt.END)
                .build();
    }

    /** New notices from the main process (mode switches, the check after the TV boots): those from the last 24 h. */
    private void checkNotices() {
        final Context c = getContext().getApplicationContext();
        LOADER.execute(() -> {
            JSONArray list = HomeMode.state(c).optJSONArray("notices");
            if (list == null) return;
            long seen = ui.getLong("seenNotice", 0), newest = seen;
            long cutoff = System.currentTimeMillis() - 24 * 3600_000L;
            final List<String> fresh = new ArrayList<>();
            for (int i = list.length() - 1; i >= 0; i--) {   // the list comes newest first
                JSONObject o = list.optJSONObject(i);
                if (o == null) continue;
                long at = o.optLong("at");
                if (at <= seen || at < cutoff) continue;
                fresh.add(o.optString("text"));
                newest = Math.max(newest, at);
            }
            if (newest > seen) ui.edit().putLong("seenNotice", newest).apply();
            if (!fresh.isEmpty()) handler.post(() -> { for (String t : fresh) showToast(t); });
        });
    }

    // MARK: - Lists (in the background, so returning to the home screen isn't delayed)

    private void reload() {
        final Context c = getContext().getApplicationContext();
        LOADER.execute(() -> {
            final List<HomeApps.Item> s = HomeApps.shelf(c, true);
            final List<HomeApps.Item> st = statusList(c);
            final List<HomeApps.Item> d = drawerList(c);
            final List<HomeApps.Item> o = HomeApps.others(c);
            handler.post(() -> {
                setShelf(s);
                setStatusItems(st);
                setDrawerItems(d);
                if (!sheetOpen) setOthers(o);
            });
        });
    }

    // MARK: - Keys: only the arrows, OK and Back (system keys pass through to the system)

    boolean onKey(KeyEvent e) {
        wake();
        boolean down = e.getAction() == KeyEvent.ACTION_DOWN;
        if (reordering) return reorderKey(e, down);
        int code = e.getKeyCode();
        if (down && code != KeyEvent.KEYCODE_DPAD_CENTER && code != KeyEvent.KEYCODE_ENTER
                && code != KeyEvent.KEYCODE_NUMPAD_ENTER) {
            holdArmed = false;      // another key while OK is held: no move mode
            handler.removeCallbacks(holdStart);
        }
        switch (code) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
                if (down) move(-1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (down) move(1, 0);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
                if (down) move(0, -1);
                return true;
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (down) move(0, 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                // Opens on release, and only if the press happened here: the release of an OK pressed inside another
                // app opens nothing.
                // Held on a shelf app (~0.5 s): move mode. A timer, since not every remote repeats a held key, or the
                // long-press repeat, whichever comes first; the release after a hold opens nothing.
                if (down) {
                    if (e.getRepeatCount() == 0) {
                        okDown = true;
                        okHeld = false;
                        handler.removeCallbacks(holdStart);
                        holdArmed = canReorder();
                        if (holdArmed) handler.postDelayed(holdStart, HOLD_MS);
                    } else if (okDown && holdArmed && e.isLongPress()) {
                        startReorder();
                    }
                } else {
                    holdArmed = false;
                    handler.removeCallbacks(holdStart);
                    if (okDown && !okHeld && !e.isCanceled() && spoken.keyClick()) activate();
                    okDown = okHeld = false;
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (down) {
                    if (e.getRepeatCount() == 0) backDown = true;
                } else {
                    if (backDown && !e.isCanceled()) back();
                    backDown = false;
                }
                return true;
            default:
                return false;
        }
    }

    private void back() {
        if (sheetOpen) {
            closeSheet();
        } else if (zone == Z_DRAWER || zone == WIDGETS) {
            closeDrawer();
            retarget(false);
        } else if (zone != Z_SHELF) {
            zone = Z_SHELF;
            retarget(false);
        } else {
            return;
        }
        click.move();
    }

    private void move(int dx, int dy) {
        if (sheetOpen) {
            moveGrid(dx, dy);
            return;
        }
        if (moveLight(dx, dy)) return;
        switch (zone) {
            case Z_SHELF:
                if (dx != 0) {
                    int next = Math.max(0, Math.min(shelf.size() - 1, shelfIndex + dx));
                    if (next == shelfIndex) return;
                    shelfIndex = next;
                } else if (dy < 0) {
                    if (hero != null && hero.button) {
                        zone = Z_HERO;
                    } else if (!statusItems.isEmpty()) {
                        zone = Z_STATUS;
                        statusIndex = shelf.isEmpty() ? 0 : nearest(statusRects, tileRects[shelfIndex].centerX());
                    } else {
                        return;
                    }
                } else {
                    if (drawerItems.isEmpty()) return;
                    openDrawer();
                }
                break;
            case Z_HERO:
                if (dx != 0) {
                    if (!browse(dx)) return;
                } else if (dy < 0) {
                    if (statusItems.isEmpty()) return;
                    zone = Z_STATUS;
                    statusIndex = 0;
                } else {
                    zone = Z_SHELF;
                }
                break;
            case Z_STATUS:
                if (dx != 0) {
                    int next = Math.max(0, Math.min(statusItems.size() - 1, statusIndex + dx));
                    if (next == statusIndex) return;
                    statusIndex = next;
                } else if (dy > 0) {
                    if (hero != null && hero.button) {
                        zone = Z_HERO;
                    } else {
                        zone = Z_SHELF;
                        if (!shelf.isEmpty()) shelfIndex = nearest(tileRects, statusRects[statusIndex].centerX());
                    }
                } else {
                    return;
                }
                break;
            case WIDGETS: {
                List<Integer> on = widgets();
                int at = on.indexOf(widgetIndex);
                if (dy < 0 || at < 0) {
                    zone = Z_DRAWER;
                } else if (dx != 0 && at + dx >= 0 && at + dx < on.size()) {
                    widgetIndex = on.get(at + dx);
                } else {
                    return;
                }
                break;
            }
            default:
                if (dx != 0) {
                    int next = Math.max(0, Math.min(drawerItems.size() - 1, drawerIndex + dx));
                    if (next == drawerIndex) return;
                    drawerIndex = next;
                } else if (dy < 0) {
                    closeDrawer();
                } else if (talkBack() && !widgets().isEmpty()) {
                    // Only with TalkBack: the widgets are there to be read, and this is how it gets to them.
                    zone = WIDGETS;
                    widgetIndex = widgets().get(0);
                } else {
                    return;
                }
                break;
        }
        retarget(false);
        click.move();
    }

    private static int nearest(RectF[] rects, float x) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < rects.length; i++) {
            float d = Math.abs(rects[i].centerX() - x);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    private void moveGrid(int dx, int dy) {
        int n = others.size();
        if (n == 0) return;
        int r = gridIndex / COLUMNS, col = gridIndex % COLUMNS;
        int rows = (n + COLUMNS - 1) / COLUMNS;
        int next = gridIndex;
        if (dx != 0) {
            int candidate = gridIndex + dx;
            if (candidate >= 0 && candidate < n && candidate / COLUMNS == r) next = candidate;
        } else {
            int targetRow = r + dy;
            if (targetRow >= 0 && targetRow < rows) next = Math.min(n - 1, targetRow * COLUMNS + col);
        }
        if (next == gridIndex) return;
        gridIndex = next;
        retargetGrid(false);
        click.move();
        gridDirty = true;
    }

    private void activate() {
        wake();
        if (reordering) {   // TalkBack's click while moving an app: drop it
            dropReorder();
            return;
        }
        HomeApps.Item it;
        RectF r;
        if (!sheetOpen && zone == WIDGETS) return;   // a widget is only read
        if (sheetOpen) {
            if (others.isEmpty()) return;
            it = others.get(gridIndex);
            r = focusedCell;
        } else if (zone == Z_HERO) {
            Recs.Pick p = currentPick();
            if (p == null || p.pkg == null) return;
            it = HomeApps.app(p.pkg, Recs.serviceName(p));
            r = heroButton;
        } else if (zone == Z_STATUS) {
            if (statusItems.isEmpty()) return;
            it = statusItems.get(statusIndex);
            r = statusRects[statusIndex];
        } else if (zone == Z_DRAWER) {
            if (drawerItems.isEmpty()) return;
            it = drawerItems.get(drawerIndex);
            r = drawerRects[drawerIndex];
        } else if (zone == Z_TOP) {
            if (topIndex < TAB_COUNT) {
                selectTab(topIndex);
                return;
            }
            it = lightButtons.get(topIndex - TAB_COUNT);
            r = topRects[topIndex];
        } else if (zone == Z_CARDS) {
            Recs.Pick p = cardIndex < cards.size() ? cards.get(cardIndex) : null;
            if (p == null || p.pkg == null) return;
            if (p.intent != null) {
                click.open();
                if (!WatchNext.open(activity, p)) showToast(Ui.tr("Não deu para abrir ", "Couldn't open ") + p.title + ".");
                return;
            }
            if (tab == CATALOG_TAB || showcase()) {
                openFromCatalog(p, cardRects[cardIndex]);
                return;
            }
            it = HomeApps.app(p.pkg, Recs.serviceName(p));
            r = cardRects[cardIndex];
        } else {
            if (shelf.isEmpty()) return;
            it = shelf.get(shelfIndex);
            r = tileRects[shelfIndex];
        }
        click.open();
        if (it.kind == HomeApps.ALL_APPS) {
            openSheet();
            return;
        }
        if (it.kind == HomeApps.SLEEP) {
            int minutes = SleepTimer.next(activity);
            showToast(minutes == 0 ? Ui.tr("Timer desligado.", "Sleep timer off.", "Temporizador apagado.")
                    : Ui.tr("A TV desliga em ", "The TV turns off in ", "La TV se apaga en ") + minutes + " min.");
            updateSleepLabel();
            return;
        }
        if (it.kind == HomeApps.INPUTS) {
            openDrawer();
            for (int i = 0; i < drawerItems.size(); i++) {
                if (drawerItems.get(i).kind == HomeApps.INPUT) {
                    drawerIndex = i;
                    break;
                }
            }
            retarget(false);
            return;
        }
        boolean opened = HomeApps.open(activity, it, this, Math.round(r.left), Math.round(r.top),
                Math.round(r.width()), Math.round(r.height()));
        if (!opened) showToast(Ui.tr("Não deu para abrir ", "Couldn't open ") + it.label + ".");
    }

    // MARK: - Moving apps on the shelf

    private boolean canReorder() {
        if (sheetOpen || zone != Z_SHELF || shelfIndex >= shelf.size() || shelf.size() < 2) return false;
        HomeApps.Item it = shelf.get(shelfIndex);
        return it.kind == HomeApps.APP && it.pkg != null;
    }

    private void startReorder() {
        handler.removeCallbacks(holdStart);
        holdArmed = false;
        if (reordering || !canReorder()) return;
        wake();
        reordering = reorderAnim = okHeld = true;
        reorderOrigin = new ArrayList<>(shelf);
        liftIndex = shelfIndex;
        reorderAt = SystemClock.uptimeMillis();
        moveLift.target = 1;
        click.open();
        showToast(Ui.tr("← → para mover · OK para soltar · Voltar para cancelar",
                "← → to move · OK to drop · Back to cancel"));
        retarget(false);    // TalkBack reads "Moving <app>, position i of n"
    }

    /** Keys while moving an app: nothing else on the home screen gets them (system keys still go to the system). */
    private boolean reorderKey(KeyEvent e, boolean down) {
        switch (e.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_LEFT:
            case KeyEvent.KEYCODE_DPAD_RIGHT:
                if (down) shiftReorder(e.getKeyCode() == KeyEvent.KEYCODE_DPAD_LEFT ? -1 : 1);
                return true;
            case KeyEvent.KEYCODE_DPAD_UP:
            case KeyEvent.KEYCODE_DPAD_DOWN:
                return true;
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                // The press that lifted the app is still held: its repeats and release do nothing; a new press drops it.
                if (down && e.getRepeatCount() == 0) {
                    okDown = okHeld = false;
                    dropReorder();
                } else if (!down) {
                    okDown = okHeld = false;
                }
                return true;
            case KeyEvent.KEYCODE_BACK:
                if (down) {
                    if (e.getRepeatCount() == 0) backDown = true;
                } else {
                    if (backDown && !e.isCanceled()) {
                        cancelReorder(false);
                        click.move();
                    }
                    backDown = false;
                }
                return true;
            default:
                return false;
        }
    }

    /** Swaps the lifted app with its neighbor; both slide from where they are to their new slots. */
    private void shiftReorder(int dx) {
        int i = shelfIndex, j = i + dx;
        if (j < 0 || j >= shelf.size()) return;
        HomeApps.Item it = shelf.get(i);
        shelf.set(i, shelf.get(j));
        shelf.set(j, it);
        Spring f = tileFocus[i];
        tileFocus[i] = tileFocus[j];
        tileFocus[j] = f;
        Spring s = tileSlide[i];
        tileSlide[i] = tileSlide[j];
        tileSlide[j] = s;
        float d = tileRects[i].left - tileRects[j].left;
        tileSlide[j].value += d;
        tileSlide[i].value -= d;
        shelfIndex = liftIndex = j;
        reorderAnim = true;
        wake();
        retarget(false);
        click.move();
    }

    /** OK: drops the app where it is and saves the shelf's new order. */
    private void dropReorder() {
        if (!reordering) return;
        reordering = false;
        moveLift.target = 0;
        boolean changed = false;
        for (int i = 0; i < shelf.size() && !changed; i++) changed = shelf.get(i) != reorderOrigin.get(i);
        reorderOrigin = null;
        click.open();
        retarget(false);
        if (!changed) return;
        final List<String> order = new ArrayList<>();
        for (HomeApps.Item it : shelf) order.add(it.pkg);
        final Context c = getContext().getApplicationContext();
        LOADER.execute(() -> {
            // The saved list may hold more than the shelf shows (apps not installed, past the limit): the shown ones
            // take their new order in the same slots, the others stay where they were.
            List<String> all = HomeApps.shelfPackages(c), out = new ArrayList<>(all);
            int next = 0;
            for (int s = 0; s < out.size() && next < order.size(); s++) {
                if (order.contains(out.get(s))) out.set(s, order.get(next++));
            }
            if (next < order.size()) {
                out = new ArrayList<>(order);
                for (String p : all) if (!order.contains(p)) out.add(p);
            }
            final List<String> save = out;
            handler.post(() -> Prefs.setShelf(save));
        });
    }

    /** Back, or leaving the home screen: the original order comes back (sliding, unless `snap`). */
    private void cancelReorder(boolean snap) {
        if (!reordering && !reorderAnim) return;
        if (reordering) {
            reordering = false;
            moveLift.target = 0;
            int n = shelf.size();
            if (reorderOrigin != null && reorderOrigin.size() == n) {
                HomeApps.Item lifted = shelf.get(shelfIndex);
                Spring[] focus = new Spring[n], slide = new Spring[n];
                for (int a = 0; a < n; a++) {
                    int b = shelf.indexOf(reorderOrigin.get(a));
                    focus[a] = tileFocus[b];
                    slide[a] = tileSlide[b];
                    slide[a].value += tileRects[b].left - tileRects[a].left;
                }
                shelf = reorderOrigin;
                tileFocus = focus;
                tileSlide = slide;
                shelfIndex = liftIndex = shelf.indexOf(lifted);
            }
            reorderOrigin = null;
            retarget(snap);
        }
        if (snap) {
            moveLift.snap(0);
            for (Spring s : tileSlide) s.snap(0);
            wiggle = 0;
            liftIndex = -1;
            reorderAnim = false;
            tilesDirty = true;
        } else {
            wake();
        }
    }

    /** One frame of the move: the slides, the lift and the wiggle. Returns true while any of them moves. */
    private boolean stepReorder(float dt, long up) {
        boolean moving = step(tileSlide, dt) | moveLift.step(dt);
        if (reordering || moveLift.value > 0.001f) {
            wiggle = WIGGLE_DEG * (float) Math.sin((up - reorderAt) * (2 * Math.PI / WIGGLE_MS));
            moving = true;
        } else {
            wiggle = 0;
        }
        reorderAnim = moving;
        return moving;
    }

    // MARK: - TalkBack: every reachable item as a virtual node (Spoken). Id = group * 1000 + index + 1.

    /** The drawer's weather (0) and quotes (1) cards: read by TalkBack, never focused by the remote. */
    private static final int GRID = 9, WIDGETS = 8;

    private static int a11yId(int group, int index) {
        return group * 1000 + index + 1;
    }

    @Override public AccessibilityNodeProvider getAccessibilityNodeProvider() {
        return spoken;
    }

    private void a11yItems(List<Integer> out) {
        if (sheetOpen) {
            for (int i = 0; i < others.size(); i++) out.add(a11yId(GRID, i));
            return;
        }
        for (int i = picksOn() ? 0 : TAB_COUNT; i < TAB_COUNT + lightButtons.size(); i++) out.add(a11yId(Z_TOP, i));
        for (int i = 0; i < Math.min(cardRects.length, cards.size()); i++) out.add(a11yId(Z_CARDS, i));
        for (int i = 0; i < shelf.size(); i++) out.add(a11yId(Z_SHELF, i));
        for (int i = 0; i < drawerItems.size(); i++) out.add(a11yId(Z_DRAWER, i));
        if (drawerOpen && Prefs.weather()) out.add(a11yId(WIDGETS, 0));
        if (drawerOpen && HomeData.quotesCard()) out.add(a11yId(WIDGETS, 1));
    }

    private String a11yText(int id) {
        int group = (id - 1) / 1000, i = (id - 1) % 1000;
        switch (group) {
            case GRID:
                if (i >= others.size()) return "";
                return label(others, i) + ", " + (i + 1) + Ui.tr(" de ", " of ") + others.size();
            case Z_TOP:
                if (i >= TAB_COUNT) return label(lightButtons, i - TAB_COUNT);
                return tabs()[i] + Ui.tr(", aba ", ", tab ") + (i + 1) + Ui.tr(" de ", " of ") + TAB_COUNT;
            case Z_CARDS: return i < cards.size() ? pickText(cards.get(i)) : "";
            case Z_SHELF:
                if (reordering && i == shelfIndex) {
                    return Ui.tr("Movendo ", "Moving ", "Moviendo ") + label(shelf, i)
                            + Ui.tr(", posição ", ", position ", ", posición ") + (i + 1) + Ui.tr(" de ", " of ") + shelf.size();
                }
                return label(shelf, i);
            case Z_DRAWER: return label(drawerItems, i);
            case WIDGETS: return i == 0 ? weatherText() : quotesText();
            default: return "";
        }
    }

    /**
     * Where the item is now. Items off screen (the closed drawer, the sheet's hidden rows, the top while the drawer is
     * open) get a thin strip at that edge, so TalkBack's arrows can still reach them; going there brings them in.
     */
    private boolean a11yBounds(int id, Rect out) {
        int group = (id - 1) / 1000, i = (id - 1) % 1000;
        float dy;
        switch (group) {
            case GRID: {
                if (!sheetOpen || i >= others.size()) return false;
                float gridWidth = COLUMNS * GRID_W + (COLUMNS - 1) * GRID_GAP;
                float x = ((1920 - gridWidth) / 2 + (i % COLUMNS) * (GRID_W + GRID_GAP)) * k;
                float y = (GRID_TOP + (i / COLUMNS - scroll.target) * GRID_ROW) * k;
                a11yRect.set(x, y, x + GRID_W * k, y + GRID_H * k);
                dy = 0;
                break;
            }
            case Z_TOP:
                if (i >= TAB_COUNT + lightButtons.size()) return false;
                a11yRect.set(topRects[i]);
                dy = (i < TAB_COUNT ? heroLayer : status).getTranslationY();
                break;
            case Z_CARDS:
                if (i >= Math.min(cardRects.length, cards.size())) return false;
                a11yRect.set(cardRects[i]);
                dy = heroLayer.getTranslationY();
                break;
            case Z_SHELF:
                if (i >= tileRects.length) return false;
                a11yRect.set(tileRects[i]);
                dy = tiles.getTranslationY();
                break;
            case Z_DRAWER:
                if (i >= drawerRects.length) return false;
                a11yRect.set(drawerRects[i]);
                dy = drawerOpen ? 0 : h;
                break;
            case WIDGETS:
                if (!drawerOpen) return false;
                a11yRect.set(i == 0 ? weatherCard : quotesCard);
                dy = 0;
                break;
            default:
                return false;
        }
        a11yRect.offset(0, dy);
        float edge = 24 * k;
        if (a11yRect.top >= h - edge) a11yRect.set(a11yRect.left, h - edge, a11yRect.right, h);
        else if (a11yRect.bottom <= edge) a11yRect.set(a11yRect.left, 0, a11yRect.right, edge);
        a11yRect.round(out);
        return out.width() > 0 && out.height() > 0;
    }

    private int a11yFocused() {
        if (sheetOpen) return others.isEmpty() ? -1 : a11yId(GRID, gridIndex);
        switch (zone) {
            case Z_TOP: return a11yId(Z_TOP, topIndex);
            case Z_CARDS: return cardIndex < cards.size() ? a11yId(Z_CARDS, cardIndex) : -1;
            case Z_SHELF: return shelf.isEmpty() ? -1 : a11yId(Z_SHELF, shelfIndex);
            case Z_DRAWER: return drawerItems.isEmpty() ? -1 : a11yId(Z_DRAWER, drawerIndex);
            case WIDGETS: return a11yId(WIDGETS, widgetIndex);
            default: return -1;
        }
    }

    /** The drawer's widgets that are on (0 weather, 1 quotes), with the drawer open. */
    private List<Integer> widgets() {
        List<Integer> out = new ArrayList<>();
        if (!drawerOpen) return out;
        if (Prefs.weather()) out.add(0);
        if (HomeData.quotesCard()) out.add(1);
        return out;
    }

    /** A spoken-feedback service (TalkBack) is on. */
    private boolean talkBack() {
        AccessibilityManager am = (AccessibilityManager) getContext().getSystemService(Context.ACCESSIBILITY_SERVICE);
        return am != null && am.isEnabled()
                && !am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_SPOKEN).isEmpty();
    }

    /** TalkBack moved to an item: the remote's focus goes there (opening or closing the drawer on the way). */
    private void a11yFocus(int id) {
        wake();
        int group = (id - 1) / 1000, i = (id - 1) % 1000;
        if (group == GRID) {
            if (!sheetOpen || i >= others.size()) return;
            gridIndex = i;
            retargetGrid(false);
            gridDirty = true;
            click.move();
            return;
        }
        if (sheetOpen || reordering) return;
        switch (group) {
            case Z_DRAWER:
                if (i >= drawerItems.size()) return;
                if (!drawerOpen) openDrawer();
                drawerIndex = i;
                break;
            case WIDGETS:
                if (!widgets().contains(i)) return;
                widgetIndex = i;
                break;
            case Z_TOP:
                if (i >= TAB_COUNT + lightButtons.size() || (i < TAB_COUNT && !picksOn())) return;
                if (drawerOpen) closeDrawer();
                topIndex = i;
                if (i < TAB_COUNT) selectTab(i);
                break;
            case Z_CARDS:
                if (i >= cards.size()) return;
                if (drawerOpen) closeDrawer();
                cardIndex = i;
                break;
            case Z_SHELF:
                if (i >= shelf.size()) return;
                if (drawerOpen) closeDrawer();
                shelfIndex = i;
                break;
            default:
                return;
        }
        zone = group;
        retarget(false);
        click.move();
    }

    private static String label(List<HomeApps.Item> list, int i) {
        return i >= 0 && i < list.size() && list.get(i).label != null ? list.get(i).label : "";
    }

    /** A recommendation: title, details and the app that plays it. */
    private static String pickText(Recs.Pick p) {
        if (p == null) return "";
        StringBuilder s = new StringBuilder(p.title == null ? "" : p.title);
        if (p.meta != null && !p.meta.isEmpty()) s.append(", ").append(p.meta);
        String app = Recs.serviceName(p);
        if (app != null && !app.isEmpty()) s.append(". App: ").append(app);
        return s.toString();
    }

    /** The weather card as TalkBack reads it: what it shows, in the same words and units. */
    private String weatherText() {
        StringBuilder s = new StringBuilder(Prefs.city().isEmpty() ? Ui.tr("Clima", "Weather") : Prefs.city());
        HomeData.Weather wx = data.weather;
        if (Double.isNaN(wx.temp)) return s.append(". ").append(Ui.tr("Sem dados do clima agora.", "No weather data right now.")).toString();
        s.append(". ").append(HomeData.degrees(wx.temp)).append(", ").append(HomeData.condition(wx.code, wx.isDay))
                .append(Ui.tr(". Máxima ", ". High ")).append(HomeData.degrees(wx.max))
                .append(Ui.tr(", mínima ", ", low ")).append(HomeData.degrees(wx.min)).append('.');
        int hours = Math.min(weatherHours(), wx.hours.size());
        if (hours > 0) s.append(Ui.tr(" Próximas horas: ", " Next hours: "));
        for (int i = 0; i < hours; i++) {
            s.append(HomeData.hour(wx.hours.get(i))).append(' ').append(HomeData.degrees(wx.hourTemp.get(i))).append(i < hours - 1 ? ", " : ".");
        }
        if (wx.sunrise != 0) s.append(Ui.tr(" Nascer do sol ", " Sunrise ")).append(Ui.time(wx.sunrise)).append('.');
        if (wx.sunset != 0) s.append(Ui.tr(" Pôr do sol ", " Sunset ")).append(Ui.time(wx.sunset)).append('.');
        return s.toString();
    }

    /** The quotes card as TalkBack reads it: each quote with its price and change. */
    private String quotesText() {
        StringBuilder s = new StringBuilder(quotesTitle());
        String stale = HomeData.staleness(data.pricesAt, 5 * 60_000L);
        s.append(", ").append(stale.isEmpty() ? changeLabel() : Ui.tr("de ", "", "de ") + stale).append('.');
        int[] coins = HomeData.active();
        for (int j = 0; j < coins.length && j < HomeData.CARD_ROWS; j++) {
            int i = coins[j];
            s.append(' ').append(HomeData.coinName(i)).append(' ').append(HomeData.quote(i, data.prices[i]));
            if (!Double.isNaN(data.changes[i])) s.append(", ").append(HomeData.change(i, data.changes[i]));
            s.append('.');
        }
        return s.toString();
    }

    // MARK: - Light design: title, tabs, cards and icon-only buttons

    private void layoutLight() {
        if (lTab == null) return;
        // Tab hit regions and spacing stay unchanged; the active capsule is drawn within each region.
        float pad = 16 * k, tabTop = L_TABBAR_TOP * k, tabBottom = tabRuleBottom();
        float x = MARGIN * k;
        for (int i = 0; i < TAB_COUNT; i++) {
            float tw = lTabOn.measureText(caps(tabs()[i]));
            topRects[i].set(Math.round(x - pad), Math.round(tabTop), Math.round(x + tw + pad), tabBottom);
            x += tw + L_TAB_GAP * k;
        }
        float b = L_BUTTON * k, right = CONTENT_R * k, cy = L_BUTTON_CY * k;
        for (int i = 2; i >= 0; i--) {
            topRects[TAB_COUNT + i].set(Math.round(right - b), Math.round(cy - b / 2), Math.round(right), Math.round(cy + b / 2));
            right -= b + L_BUTTON_GAP * k;
        }
        if (showcase()) layoutShowcase(); else layoutCards();
        heroDirty = statusDirty = true;
    }

    /** The three cards: a wide one and two tall ones. */
    private void layoutCards() {
        float top = L_CARDS_TOP * k, ch = L_CARD_H * k, g = L_CARD_GAP * k, left = MARGIN * k, tall = L_TALL_W * k;
        float wide = (CONTENT_R - MARGIN) * k - 2 * tall - 2 * g;
        cardRects[0].set(Math.round(left), Math.round(top), Math.round(left + wide), Math.round(top + ch));
        cardRects[1].set(cardRects[0].right + Math.round(g), cardRects[0].top, cardRects[0].right + Math.round(g + tall), cardRects[0].bottom);
        cardRects[2].set(cardRects[1].right + Math.round(g), cardRects[0].top, cardRects[1].right + Math.round(g + tall), cardRects[0].bottom);
    }

    /** The showcase: row 1's posters, then row 2's, each under its label and hairline, the last one ending on the margin. */
    private void layoutShowcase() {
        int n = cards.size();
        for (int i = 0; i < n && i < cardRects.length; i++) {
            boolean second = i >= showRow1;
            int col = second ? i - showRow1 : i;
            float top = ((second ? SHOW_LABEL_2 : SHOW_LABEL_1) + 30) * k, x = (MARGIN + col * (SHOW_W + SHOW_GAP)) * k;
            cardRects[i].set(Math.round(x), Math.round(top), Math.round(x) + Math.round(SHOW_W * k), Math.round(top) + Math.round(SHOW_H * k));
        }
    }

    /** Explore shows the genre showcase (with a TMDB key); the other tabs, three cards. */
    private boolean showcase() {
        return tab == 0 && picksOn();
    }

    /** Explore: the next or the previous pair of genres (focus on the first poster going forward, the last going back). */
    private boolean turnGenrePage(int dx, boolean second) {
        int to = genrePage + dx;
        if (to < 0 || to >= Genres.pages()) return false;
        genrePage = to;
        updateCards();
        if (cards.isEmpty()) return true;
        int from = second ? showRow1 : 0, to2 = second ? cards.size() : showRow1;
        if (from >= to2) {     // that row is empty on this page: the other one
            from = second ? 0 : showRow1;
            to2 = second ? showRow1 : cards.size();
        }
        cardIndex = dx > 0 ? from : Math.max(from, to2 - 1);
        return true;
    }

    /** The three cards of the current tab (Explore, Movies, Shows, Continue, Catalog). */
    private void updateCards() {
        List<Recs.Pick> paged = pagedSource();
        List<Recs.Pick> list;
        if (showcase()) {
            // Two genres: up to 11 posters each, row 1 first.
            genrePage = Math.max(0, Math.min(genrePage, Genres.pages() - 1));
            List<Recs.Pick> a = genres.items(2 * genrePage), b = genres.items(2 * genrePage + 1);
            list = new ArrayList<>(a.subList(0, Math.min(SHOW_COLS, a.size())));
            showRow1 = list.size();
            list.addAll(b.subList(0, Math.min(SHOW_COLS, b.size())));
            genres.refresh(genrePage);
        } else if (paged != null) {
            page = Math.max(0, Math.min(page, (paged.size() - 1) / 3));
            list = paged.subList(page * 3, Math.min(page * 3 + 3, paged.size()));
            if (tab == CATALOG_TAB) catalog.prefetch(list);
        } else {
            list = recs.now(TAB_KIND[tab]);
        }
        if (!samePicks(list, cards)) {
            cards = list;
            cardIndex = Math.min(cardIndex, Math.max(0, list.size() - 1));
            if (labelPaint != null) {
                if (showcase()) layoutShowcase(); else layoutCards();
            }
            if (zone == Z_CARDS && list.isEmpty()) {
                zone = Z_SHELF;
                retarget(false);
            }
        }
        for (Recs.Pick p : recs.wanted()) recs.backdrop(p);   // all the tabs preloaded: switching tabs is instant
        // Continue's and the catalog's first page too, and in those tabs the next page.
        for (List<Recs.Pick> more : java.util.Arrays.asList(watchNext.items(), catalog.items())) {
            int from = more == paged ? page * 3 : 0, to = more == paged ? page * 3 + 6 : 3;
            for (int i = from; i < more.size() && i < to; i++) recs.backdrop(more.get(i));
        }
        heroDirty = true;
        spoken.focusMoved();    // new picks under the focused card
    }

    private void selectTab(int index) {
        if (index == tab) return;
        tab = index;
        page = 0;
        cards = new ArrayList<>();   // Explore's showcase and the cards share the rects: updateCards lays them out again
        updateCards();
        heroDirty = true;
    }

    /** Flat graphite behind both the home and the drawer, without bitmap fields or blur. */
    private void drawLightSky(Canvas c) {
        fill.setShader(null);
        fill.setColor(Theme.color(Theme.BACKGROUND, night >= 0.5f));
        c.drawRect(screen, fill);
    }

    private void drawLightTop(Canvas c) {
        applyTone();
        if (!picksOn()) {
            drawClock(c);
            if (HomeData.coinCount() > 0) drawQuotes(c);
            return;
        }
        // The time, large and tight, where a title would be; the greeting after it.
        c.drawText(clockText, MARGIN * k - Ui.leftBearing(lTitle, clockText), L_TITLE_BASE * k, lTitle);
        c.drawText(greetingText, MARGIN * k + lTitle.measureText(clockText) + 48 * k, L_TITLE_BASE * k, lGreeting);
        // Your team, between the greeting and the corner buttons: the last result (or the score now) and the next game.
        Team.draw(c, topRects[TAB_COUNT].left - 40 * k, 104 * k, 142 * k, k, teamBig, teamSmall, teamTag, teamImage, rule());
        // The active category remains orange. A separate outer ring marks remote focus.
        hairline(c, MARGIN * k, CONTENT_R * k, L_TAB_RULE * k);
        for (int i = 0; i < TAB_COUNT; i++) {
            RectF r = topRects[i];
            String label = caps(tabs()[i]);
            float lx = r.left + 16 * k;
            tmp.set(r.left, r.top, r.right, r.top + Theme.TAB_HEIGHT * k);
            if (i == tab) {
                fill.setShader(null);
                fill.setColor(Theme.color(Theme.ACCENT, night >= 0.5f));
                c.drawRoundRect(tmp, Theme.TAB_RADIUS * k, Theme.TAB_RADIUS * k, fill);
            }
            float focus = Ui.clamp(topFocus[i].value);
            if (focus > 0.01f) {
                tmp.inset(-Theme.TAB_FOCUS_GAP * k, -Theme.TAB_FOCUS_GAP * k);
                float radius = (Theme.TAB_RADIUS + Theme.TAB_FOCUS_GAP) * k;
                rim.setShader(null);
                rim.setColor(Ui.alpha(Theme.color(Theme.FOCUS, night >= 0.5f), focus));
                rim.setStrokeWidth(Theme.TAB_FOCUS_STROKE * k);
                c.drawRoundRect(tmp, radius, radius, rim);
                rim.setStrokeWidth(Theme.Legacy.HomeEffect.RIM_STROKE * k);
            }
            Paint p = i == tab ? lTabOn : lTab;
            if (i == tab) lTabOn.setColor(Theme.color(Theme.ON_ACCENT, night >= 0.5f));
            c.drawText(label, lx, L_TAB_BASE * k, p);
        }
        if (showcase()) {
            drawShowcase(c);
            if (HomeData.coinCount() > 0) drawQuotes(c);
            return;
        }
        int top = -1;
        for (int i = 0; i < 3; i++) if (cardFocus[i].value > 0.02f && (top < 0 || cardFocus[i].value > cardFocus[top].value)) top = i;
        for (int i = 0; i < 3; i++) if (i != top) drawLightCard(c, i);
        if (top >= 0) drawLightCard(c, top);
        if (pagedSource() != null) drawPageArrows(c);
        if (tab == CATALOG_TAB && cards.isEmpty()) {
            c.drawText(catalog.loading() ? Ui.tr("Carregando o catálogo…", "Loading the catalog…", "Cargando el catálogo…")
                    : Ui.tr("Não deu para carregar o catálogo agora.", "Couldn't load the catalog right now.", "No se pudo cargar el catálogo ahora."),
                    MARGIN * k, (L_CARDS_TOP + 60) * k, lCardMeta);
        }
        if (tab == NEXT_TAB && cards.isEmpty()) {
            c.drawText(Ui.tr("Nada para continuar agora. O que você começar a ver na Netflix, no Prime Video, no Disney+ e no Max aparece aqui.",
                    "Nothing to continue right now. What you start on Netflix, Prime Video, Disney+ and Max shows up here.",
                    "Nada para continuar ahora. Lo que empieces en Netflix, Prime Video, Disney+ y Max aparece aquí."),
                    MARGIN * k, (L_CARDS_TOP + 60) * k, lCardMeta);
        }
        if (HomeData.coinCount() > 0) drawQuotes(c);
    }

    private boolean picksOn() {
        return Recs.hasKey(getContext());
    }

    private void drawClock(Canvas c) {
        float x = MARGIN * k;
        c.drawText(dateText, x - Ui.leftBearing(lDate, dateText), L_DATE_BASE * k, lDate);
        c.drawText(clockText, x - Ui.leftBearing(lClock, clockText), L_CLOCK_BASE * k, lClock);
        HomeData.Weather wx = data.weather;
        if (!Prefs.weather() || Double.isNaN(wx.temp)) return;
        if (data.weatherAt != weatherLineAt || Prefs.version != weatherLinePrefs || !Ui.language.equals(weatherLineLanguage)) {
            weatherLineAt = data.weatherAt;
            weatherLinePrefs = Prefs.version;
            weatherLineLanguage = Ui.language;
            weatherLine = HomeData.degrees(wx.temp) + "  " + HomeData.condition(wx.code, wx.isDay) + "   "
                    + (Ui.ru ? "Макс. " + HomeData.degrees(wx.max) + "  Мин. " + HomeData.degrees(wx.min)
                    : Ui.en ? "H:" + HomeData.degrees(wx.max) + "  L:" + HomeData.degrees(wx.min)
                    : Ui.de ? "H:" + HomeData.degrees(wx.max) + "  T:" + HomeData.degrees(wx.min)
                    : "Máx. " + HomeData.degrees(wx.max) + "  Mín. " + HomeData.degrees(wx.min));
        }
        float size = 40 * k, base = L_WEATHER_BASE * k;
        String symbol = Symbols.weather(wx.code, wx.isDay);
        float sw = Symbols.width(getContext(), symbol, size);
        Symbols.draw(getContext(), c, symbol, x + sw / 2, base - Ui.halfCap(lWeather), size, lWeather.getColor());
        c.drawText(weatherLine, x + sw + 16 * k, base, lWeather);
    }

    /**
     * Recommendation card: the title's photo (already at the focused card size, so it stays sharp), rounded corners and
     * a soft shadow; when focused, it grows 5%, rises and lights up its border. Below it, the title (and on the large
     * card, genre · runtime · service).
     */
    private void drawLightCard(Canvas c, int i) {
        RectF base = cardRects[i];
        Recs.Pick p = i < cards.size() ? cards.get(i) : null;
        if (p == null && pagedSource() != null) return;   // a last page with fewer than three: no gray placeholders
        float f = Ui.clamp(cardFocus[i].value);
        float bw = base.width();
        cell.set(base);
        Backdrop b = p == null ? null : recs.backdrop(p);
        Bitmap img = b == null ? null : (i == 0 ? b.wide : b.tall);
        if (img != null) {
            roundImage(c, img, cell, Theme.POSTER_RADIUS * k);
        } else {
            fill.setShader(null);
            fill.setColor(Theme.color(Theme.PLACEHOLDER, night >= 0.5f));
            c.drawRoundRect(cell, Theme.POSTER_RADIUS * k, Theme.POSTER_RADIUS * k, fill);
        }
        if (f > 0.01f) {
            float out = Theme.Legacy.HomeEffect.FOCUS_GAP * k + Theme.Legacy.HomeEffect.FOCUS_HALF_STROKE * k;
            tmp.set(cell.left - out, cell.top - out, cell.right + out, cell.bottom + out);
            rim.setShader(null);
            rim.setColor(Ui.alpha(Theme.color(Theme.FOCUS, night >= 0.5f), f));
            rim.setStrokeWidth(Theme.Legacy.HomeEffect.FOCUS_STROKE * k);
            c.drawRoundRect(tmp, Theme.POSTER_RADIUS * k + out, Theme.POSTER_RADIUS * k + out, rim);
        }
        if (p == null) return;
        if (p.progress >= 0) drawProgress(c, cell, p.progress, 1);
        if (tab == CATALOG_TAB) drawBadge(c, cell, p, 1);
        fitPaint.set(lCardTitle);
        c.drawText(TextUtils.ellipsize(caps(p.title), fitPaint, bw, TextUtils.TruncateAt.END).toString(), base.left, base.bottom + 57 * k, lCardTitle);
        // Continue and Catalog: every card says the episode or kind and the app (the narrow ones too).
        if ((i == 0 || pagedSource() != null) && p.meta != null && !p.meta.isEmpty()) {
            fitPaint.set(lCardMeta);
            c.drawText(TextUtils.ellipsize(p.meta, fitPaint, bw, TextUtils.TruncateAt.END).toString(), base.left, base.bottom + 87 * k, lCardMeta);
        }
    }

    /**
     * Explore's showcase: per row the genre's label in caps on a hairline (the focused title at its right), and the
     * posters under it; the focused one gets the 3 px orange outline. Arrows outside the margins when there are more
     * pairs of genres before or after.
     */
    private void drawShowcase(Canvas c) {
        int focused = zone == Z_CARDS && cardIndex < cards.size() ? cardIndex : -1;
        float left = MARGIN * k, right = CONTENT_R * k;
        for (int row = 0; row < 2; row++) {
            int genre = 2 * genrePage + row;
            if (genre >= 2 * Genres.pages()) break;
            float base = (row == 0 ? SHOW_LABEL_1 : SHOW_LABEL_2) * k;
            c.drawText(caps(Genres.label(genre)), left, base, mLabel);
            hairline(c, left, right, base + 14 * k);
            int from = row == 0 ? 0 : showRow1, to = row == 0 ? showRow1 : cards.size();
            if (focused >= from && focused < to) {
                // The focused title at the right of its row's label: "MATCHBOX THE MOVIE  2025 · APPLE TV".
                Recs.Pick p = cards.get(focused);
                float x = right - mMutedRight.measureText(p.meta);
                c.drawText(p.meta, right, base, mMutedRight);
                String title = caps(p.title);
                fitPaint.set(mLabelRight);
                c.drawText(TextUtils.ellipsize(title, fitPaint, x - left - 520 * k, TextUtils.TruncateAt.END).toString(), x - 18 * k, base, mLabelRight);
            }
            if (from == to) {
                c.drawText(genres.loading(genrePage) ? Ui.tr("Carregando…", "Loading…", "Cargando…")
                        : Ui.tr("Nada deste gênero nos seus apps agora.", "Nothing in this genre on your apps right now.",
                        "Nada de este género en tus apps ahora."), left, base + 120 * k, lCardMeta);
            }
            for (int i = from; i < to; i++) {
                RectF r = cardRects[i];
                Bitmap poster = genres.poster(cards.get(i), Math.round(r.width()), Math.round(r.height()));
                if (poster != null) {
                    roundImage(c, poster, r, Theme.POSTER_RADIUS * k);
                } else {
                    fill.setShader(null);
                    fill.setColor(Theme.color(Theme.PLACEHOLDER, night >= 0.5f));
                    c.drawRoundRect(r, Theme.POSTER_RADIUS * k, Theme.POSTER_RADIUS * k, fill);
                }
                float f = Ui.clamp(cardFocus[i].value);
                if (f > 0.01f) {
                    float out = Theme.Legacy.HomeEffect.FOCUS_GAP * k + Theme.Legacy.HomeEffect.FOCUS_HALF_STROKE * k;
                    tmp.set(r.left - out, r.top - out, r.right + out, r.bottom + out);
                    rim.setShader(null);
                    rim.setColor(Ui.alpha(Theme.color(Theme.FOCUS, night >= 0.5f), f));
                    rim.setStrokeWidth(Theme.Legacy.HomeEffect.FOCUS_STROKE * k);
                    c.drawRoundRect(tmp, Theme.POSTER_RADIUS * k + out, Theme.POSTER_RADIUS * k + out, rim);
                }
            }
        }
        // More pairs of genres: arrows in the side margins, at the middle of the two rows.
        float cy = ((SHOW_LABEL_1 + 30 + SHOW_LABEL_2 + 30 + SHOW_H) / 2) * k, size = 34 * k;
        if (genrePage < Genres.pages() - 1) Symbols.drawSharp(getContext(), c, "chevron_right", (CONTENT_R + MARGIN / 2) * k, cy, size, M_MUTED);
        if (genrePage > 0) {
            float cx = MARGIN / 2 * k;
            c.save();
            c.scale(-1, 1, cx, cy);
            Symbols.drawSharp(getContext(), c, "chevron_right", cx, cy, size, M_MUTED);
            c.restore();
        }
    }

    /** Continue and Catalog: small arrows beside the cards when there are more titles before or after these three. */
    private void drawPageArrows(Canvas c) {
        int pages = (pagedSource().size() + 2) / 3;
        if (pages <= 1) return;
        float cy = (L_CARDS_TOP + L_CARD_H / 2) * k, size = 34 * k;
        int color = M_MUTED;
        if (page < pages - 1) Symbols.draw(getContext(), c, "chevron_right", (CONTENT_R + MARGIN / 2) * k, cy, size, color);
        if (page > 0) {
            float cx = MARGIN / 2 * k;
            c.save();
            c.scale(-1, 1, cx, cy);
            Symbols.draw(getContext(), c, "chevron_right", cx, cy, size, color);
            c.restore();
        }
    }

    /** Catalog: the service's name on a pill in its color, at the card's top left. */
    private void drawBadge(Canvas c, RectF card, Recs.Pick p, float s) {
        String name = Recs.serviceName(p);
        if (name == null || name.isEmpty()) return;
        float pad = 16 * k * s, h = 36 * k * s, w = badgePaint.measureText(name) * s + 28 * k * s;
        tmp.set(card.left + pad, card.top + pad, card.left + pad + w, card.top + pad + h);
        fill.setShader(null);
        fill.setColor(Catalog.color(p));
        c.drawRect(tmp, fill);
        c.save();
        c.scale(s, s, tmp.centerX(), tmp.centerY());
        c.drawText(name, tmp.centerX() - badgePaint.measureText(name) / 2, tmp.centerY() + Ui.halfCap(badgePaint), badgePaint);
        c.restore();
    }

    /**
     * Catalog: Netflix and Apple TV titles open on their own page (when Wikidata has the service's id); the rest open
     * the app, with a note of what to search for.
     */
    private void openFromCatalog(Recs.Pick p, RectF r) {
        click.open();
        catalog.open(p, intent -> {
            if (intent != null) {
                try {
                    activity.startActivity(intent);
                    return;
                } catch (RuntimeException e) {
                    AppLog.w("catalog: couldn't open " + p.title + ": " + e);
                }
            }
            HomeApps.Item it = HomeApps.app(p.pkg, Recs.serviceName(p));
            if (!HomeApps.open(activity, it, this, Math.round(r.left), Math.round(r.top), Math.round(r.width()), Math.round(r.height()))) {
                showToast(Ui.tr("Não deu para abrir ", "Couldn't open ") + it.label + ".");
                return;
            }
            // The system's toast: the app is already in front, over the home screen's own notices.
            android.widget.Toast.makeText(activity, Ui.tr("Procure por ", "Search for ", "Busca ") + "\u201c" + p.title + "\u201d"
                    + Ui.tr(" no app.", " in the app.", " en la app."), android.widget.Toast.LENGTH_LONG).show();
        });
    }

    /** How much was watched: a white bar on a dark track at the bottom of the card's photo, like the Apple TV app. */
    private void drawProgress(Canvas c, RectF card, float progress, float s) {
        float pad = 24 * k * s, bh = 7 * k * s, y = card.bottom - pad - bh;
        tmp.set(card.left + pad, y, card.right - pad, y + bh);
        fill.setShader(null);
        fill.setColor(Theme.Legacy.Palette.BLACK_A80);
        c.drawRect(tmp, fill);
        tmp.right = tmp.left + Math.max(bh, tmp.width() * progress);
        fill.setColor(Theme.Legacy.Palette.WHITE_AFF);
        c.drawRect(tmp, fill);
    }

    /**
     * Live TV, HDMI Inputs and Settings: round surfaces within the existing hit regions; the focused one uses orange with a dark
     * symbol and its name appears below it.
     */
    private void drawLightButtons(Canvas c) {
        applyTone();
        for (int i = 0; i < 3; i++) {
            RectF r = topRects[TAB_COUNT + i];
            float f = Ui.clamp(topFocus[TAB_COUNT + i].value);
            float radius = Math.min(r.width(), r.height()) / 2f;
            fill.setShader(null);
            fill.setColor(blend(Theme.color(Theme.SURFACE, night >= 0.5f), Theme.color(Theme.FOCUS, night >= 0.5f), f));
            c.drawRoundRect(r, radius, radius, fill);
            Symbols.drawSharp(getContext(), c, lightButtons.get(i).symbol, r.centerX(), r.centerY(), 27 * k,
                    blend(M_INK, Theme.color(Theme.ON_ACCENT_SMALL, night >= 0.5f), f));
            if (f > 0.01f) {
                String label = caps(lightButtons.get(i).label);
                float half = lTip.measureText(label) / 2;
                float x = Math.min(r.centerX(), CONTENT_R * k - half);
                lTip.setColor(Ui.alpha(M_INK, f));
                c.drawText(label, x, r.bottom + 40 * k, lTip);
            }
        }
    }

    private boolean moveLight(int dx, int dy) {
        switch (zone) {
            case Z_SHELF:
                if (dy >= 0 || shelf.isEmpty()) return false;
                if (cards.isEmpty()) {
                    zone = Z_TOP;
                    topIndex = picksOn() ? tab : TAB_COUNT;
                } else if (showcase()) {
                    // Up from the apps: the showcase's lower row (the upper one if that's empty).
                    zone = Z_CARDS;
                    int from = showRow1 < cards.size() ? showRow1 : 0, to = showRow1 < cards.size() ? cards.size() : showRow1;
                    float x = tileRects[shelfIndex].centerX();
                    cardIndex = from;
                    for (int i = from; i < to; i++) {
                        if (Math.abs(cardRects[i].centerX() - x) < Math.abs(cardRects[cardIndex].centerX() - x)) cardIndex = i;
                    }
                } else {
                    zone = Z_CARDS;
                    cardIndex = nearest(cardRects, cards.size(), tileRects[shelfIndex].centerX());
                }
                break;
            case Z_CARDS:
                if (showcase()) {
                    if (!moveShowcase(dx, dy)) return true;
                    break;
                }
                if (dx != 0) {
                    int next = Math.max(0, Math.min(cards.size() - 1, cardIndex + dx));
                    if (next == cardIndex && pagedSource() != null && turnPage(dx)) break;
                    if (next == cardIndex) return true;
                    cardIndex = next;
                } else if (dy < 0) {
                    zone = Z_TOP;
                    topIndex = tab;     // as on Apple TV: moving up to the tabs focuses the selected tab
                } else {
                    zone = Z_SHELF;
                    if (!shelf.isEmpty()) shelfIndex = nearest(tileRects, tileRects.length, cardRects[cardIndex].centerX());
                }
                break;
            case Z_TOP:
                if (dx != 0) {
                    int next = Math.max(picksOn() ? 0 : TAB_COUNT, Math.min(TAB_COUNT + 2, topIndex + dx));
                    if (next == topIndex) return true;
                    topIndex = next;
                    if (topIndex < TAB_COUNT) selectTab(topIndex);   // like Apple TV tabs: focusing a tab selects it
                } else if (dy > 0) {
                    if (cards.isEmpty()) {
                        zone = Z_SHELF;
                    } else {
                        zone = Z_CARDS;
                        cardIndex = nearest(cardRects, cards.size(), topRects[topIndex].centerX());
                    }
                } else {
                    return true;
                }
                break;
            default:
                return false;
        }
        retarget(false);
        click.move();
        return true;
    }

    /** The showcase's focus: false when it stays put (the end of the last pair, say). */
    private boolean moveShowcase(int dx, int dy) {
        boolean second = cardIndex >= showRow1;
        int from = second ? showRow1 : 0, to = second ? cards.size() : showRow1, col = cardIndex - from;
        if (dx != 0) {
            int next = cardIndex + dx;
            if (next < from || next >= to) return turnGenrePage(dx, second);
            cardIndex = next;
        } else if (dy < 0) {
            if (second && showRow1 > 0) {
                cardIndex = Math.min(col, showRow1 - 1);
            } else {
                zone = Z_TOP;
                topIndex = tab;
            }
        } else {
            if (!second && showRow1 < cards.size()) {
                cardIndex = showRow1 + Math.min(col, cards.size() - showRow1 - 1);
            } else {
                zone = Z_SHELF;
                if (!shelf.isEmpty()) shelfIndex = nearest(tileRects, tileRects.length, cardRects[cardIndex].centerX());
            }
        }
        return true;
    }

    /** Continue and Catalog: the whole list the cards page through; null in the other tabs. */
    private List<Recs.Pick> pagedSource() {
        return tab == NEXT_TAB ? watchNext.items() : tab == CATALOG_TAB ? catalog.items() : null;
    }

    /** Continue and Catalog: past the last card, the next three (focus on the first); past the first, the three before (on the last). */
    private boolean turnPage(int dx) {
        int pages = (pagedSource().size() + 2) / 3, to = page + dx;
        if (to < 0 || to >= pages) return false;
        page = to;
        updateCards();
        cardIndex = dx > 0 ? 0 : cards.size() - 1;
        return true;
    }

    private static int nearest(RectF[] rects, int count, float x) {
        int best = 0;
        float bestDistance = Float.MAX_VALUE;
        for (int i = 0; i < Math.min(count, rects.length); i++) {
            float d = Math.abs(rects[i].centerX() - x);
            if (d < bestDistance) {
                bestDistance = d;
                best = i;
            }
        }
        return best;
    }

    // MARK: - Helpers

    private static float smoothstep(float e0, float e1, float x) {
        float t = Ui.clamp((x - e0) / (e1 - e0));
        return t * t * (3 - 2 * t);
    }

    private static int blend(int a, int b, float f) {
        int aa = a >>> 24, ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int ba = b >>> 24, br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return (Math.round(aa + (ba - aa) * f) << 24) | (Math.round(ar + (br - ar) * f) << 16)
                | (Math.round(ag + (bg - ag) * f) << 8) | Math.round(ab + (bb - ab) * f);
    }
}
