package io.freemytv;

/** Semantic visual tokens; Legacy preserves values from the pre-design-system rendering. */
final class Theme {
    private Theme() {}

    static final int BACKGROUND = 0;
    static final int SURFACE = 1;
    static final int ACCENT = 2;
    static final int FOCUS = 3;
    static final int ON_ACCENT = 4;
    static final int ON_ACCENT_SMALL = 5;
    static final int ACCENT_ORANGE = 0xFFFF5A20;
    static final int ACCENT_INK_LARGE = 0xFFFFFFFF;
    static final int ACCENT_INK_SMALL = 0xFF292B2E;
    static final float TAB_HEIGHT = 56f;
    static final float TAB_RADIUS = 28f;
    static final float TAB_FOCUS_GAP = 4f;
    static final float TAB_FOCUS_STROKE = 3f;
    static final int BACKGROUND_DAY = 0xFFE6E5E8;
    static final int BACKGROUND_NIGHT = 0xFF202124;
    static final int SURFACE_DAY = 0xFFFAF9FB;
    static final int SURFACE_NIGHT = 0xFF2B2C2F;
    static final float DOCK_RADIUS = 48f;
    static final float POSTER_RADIUS = 12f;
    static final float APP_FOCUS_RADIUS = 24f;
    static final int LOGO = 6;
    static final int LOGO_DAY = 0xFF292B2E;
    static final int LOGO_NIGHT = 0xFFF5F5F7;

    // Phase 6: numeric dot display and rounded drawer, in logical Canvas units.
    static final float CLOCK_DOT_HEIGHT = 92f, CLOCK_DOT_LARGE_HEIGHT = 148f;
    static final float TEMPERATURE_DOT_HEIGHT = 158f;
    static final float DOCK_SEPARATOR_HEIGHT = 64f, SEPARATOR_STROKE = 1.5f;
    static final int SOFT_DIVIDER = 15;
    static final float DRAWER_SHIFT = 700f, DRAWER_TITLE_BASE = 418f;
    static final float DRAWER_ROW_TOP = 450f, DRAWER_ROW_HEIGHT = 168f, DRAWER_CELL_GAP = 12f;
    static final float DRAWER_CARD_RADIUS = 32f, DRAWER_ICON_SIZE = 56f;
    static final float DRAWER_ICON_STROKE = 1.65f;
    static final float WIDGET_TOP = 650f, WIDGET_HEIGHT = 368f, WIDGET_PADDING = 24f;
    static final float DRAWER_LABEL_SIZE = 25f, DRAWER_META_SIZE = 21f, DRAWER_CELL_TEXT_SIZE = 20f;
    static final float DRAWER_TRACKING = 0.02f, MARKET_TEXT_SIZE = 26f, MARKET_CHANGE_SIZE = 24f;
    static final float WEATHER_HOUR_SIZE = 22f, WEATHER_HOUR_TEMP_SIZE = 32f;
    static final float WEATHER_DETAIL_SIZE = 20f, WEATHER_CONDITION_ICON_SIZE = 40f, WEATHER_ICON_SIZE = 48f;
    static final float DOTTED_RULE_STEP = 6f, DOTTED_RULE_RADIUS = 0.85f;

    // Home refinement: aligned outer edges and a consistent vertical rhythm.
    static final float HOME_TAB_RULE_Y = 256f;
    static final float HOME_GENRE_BASE_1 = 296f, HOME_GENRE_BASE_2 = 584f;
    static final float HOME_POSTER_OFFSET = 32f, HOME_GENRE_RULE_OFFSET = 16f;
    static final float HOME_TAB_PADDING = 16f, HOME_TAB_GAP = 32f;
    static final float HOME_DETAIL_GAP = 32f, HOME_META_GAP = 24f;

    // Screensaver: shared endpoints, quiet photo frame and cached information panels.
    static final long DREAM_SHOW_MS = 30_000, DREAM_FADE_MS = 3_000, DREAM_FIRST_MS = 1_600;
    static final float DREAM_PHOTO_INSET = 64f, DREAM_PHOTO_RADIUS = 48f, DREAM_PANEL_RADIUS = 32f;
    static final float DREAM_CLOCK_CARD_WIDTH = 680f, DREAM_CLOCK_HEIGHT = 132f;
    static final float DREAM_FALLBACK_CLOCK_HEIGHT = 220f, DREAM_TITLE_WIDTH = 816f, DREAM_TITLE_SIZE = 52f;
    static final float DREAM_PHOTO_SATURATION_DAY = .95f, DREAM_PHOTO_SATURATION_NIGHT = .85f;
    static final float DREAM_PHOTO_BRIGHTNESS_NIGHT = .86f;

    static final String MODE_DAY = "day", MODE_NIGHT = "night";
    static final int TEXT_PRIMARY = 7, TEXT_SECONDARY = 8, DIVIDER = 9, DISABLED = 10;
    static final int POSITIVE = 11, NEGATIVE = 12, PLACEHOLDER = 13, CEC_ACTIVE = 14;

    /** Unset or unrecognized settings keep the installed Night appearance. */
    static String normalizeMode(String value) {
        return MODE_DAY.equals(value) ? MODE_DAY : MODE_NIGHT;
    }

    /** Endpoint colors only. Switching is immediate, avoiding a per-frame theme crossfade. */
    static int color(int role, boolean night) {
        switch (role) {
            case BACKGROUND: return night ? BACKGROUND_NIGHT : BACKGROUND_DAY;
            case SURFACE: return night ? SURFACE_NIGHT : SURFACE_DAY;
            case ACCENT:
            case FOCUS: return ACCENT_ORANGE;
            case ON_ACCENT: return ACCENT_INK_LARGE;
            case ON_ACCENT_SMALL: return ACCENT_INK_SMALL;
            case LOGO: return night ? LOGO_NIGHT : LOGO_DAY;
            case SOFT_DIVIDER: return night ? 0xFF48494D : 0xFFD7D7DD;
            // Keep validated Night ink values while enabling legible Day endpoints.
            case TEXT_PRIMARY: return night ? Legacy.HomeColor.M_INK : 0xFF292B2E;
            case TEXT_SECONDARY: return night ? Legacy.HomeColor.M_MUTED : 0xFF62636A;
            case DIVIDER: return night ? Legacy.HomeColor.M_HAIRLINE : 0xFFD7D7DD;
            case DISABLED: return night ? Legacy.Palette.WHITE_A73 : 0xFF88898F;
            case POSITIVE: return night ? 0xFFB8B9BF : 0xFF62636A;
            case NEGATIVE: return night ? Legacy.HomeColor.M_DOWN : 0xFFC43B24;
            case PLACEHOLDER: return night ? Legacy.Palette.WHITE_A14 : 0x14292B2E;
            case CEC_ACTIVE: return night ? Legacy.HomeColor.M_UP : 0xFF21854C;
            default: throw new IllegalArgumentException("Unknown theme color role: " + role);
        }
    }

    /** Existing Canvas units and ARGB values. Do not substitute the new palette here. */
    static final class Legacy {
        private Legacy() {}

        static final class HomeColor {
            private HomeColor() {}
            static final int SMOKE = 0xFF111113;
            static final int INK = 0xFF1C1C1E;
            static final int WHITE = 0xFFF5F5F7;
            static final int UP_COLOR = 0xFF30D158;
            static final int DOWN_COLOR = 0xFFFF453A;
            static final int M_INK = 0xFFFFFFFF;
            static final int M_MUTED = 0xB8FFFFFF;
            static final int M_HAIRLINE = 0x8CFFFFFF;
            static final int M_UP = 0xFF5EDCFF;
            static final int M_DOWN = 0xFFFF5A3C;
            static final int L_INK = 0xFF1D1D1F;
            static final int L_DIM = 0xFF6E6E73;
            static final int L_FAINT = 0xFF8E8E93;
        }

        static final class AppColor {
            private AppColor() {}
            static final int NETFLIX_TOP = 0xFF0A0A0A;
            static final int NETFLIX_BOTTOM = 0xFF000000;
            static final int YOUTUBE_TOP = 0xFF1C1C1E;
            static final int YOUTUBE_BOTTOM = 0xFF09090A;
            static final int GLOBOPLAY_TOP = 0xFFFF8A1F;
            static final int GLOBOPLAY_BOTTOM = 0xFFFF2D5A;
            static final int PRIME_TOP = 0xFF2AA8FF;
            static final int PRIME_BOTTOM = 0xFF0A68D4;
            static final int DISNEY_TOP = 0xFF173584;
            static final int DISNEY_BOTTOM = 0xFF08143F;
            static final int MAX_TOP = 0xFF5C30E8;
            static final int MAX_BOTTOM = 0xFF221684;
            static final int APPLETV_TOP = 0xFF1C1C1E;
            static final int APPLETV_BOTTOM = 0xFF000000;
        }

        static final class Palette {
            private Palette() {}
            static final int WHITE_AFF = 0xFFFFFFFF;
            static final int WHITE_AC7 = 0xC7FFFFFF;
            static final int WHITE_AD1 = 0xD1FFFFFF;
            static final int BLACK_A59 = 0x59000000;
            static final int WHITE_AF2 = 0xF2FFFFFF;
            static final int BLACK_A99 = 0x99000000;
            static final int WHITE_AB8 = 0xB8FFFFFF;
            static final int WHITE_A99 = 0x99FFFFFF;
            static final int WHITE_AB3 = 0xB3FFFFFF;
            static final int WHITE_AEB = 0xEBFFFFFF;
            static final int WHITE_AAD = 0xADFFFFFF;
            static final int GLASS_DAY = 0xFFEBEBF0;
            static final int GLASS_NIGHT = 0xFF141416;
            static final int TEXT_DIM_NIGHT = 0xFF98989D;
            static final int APP_FALLBACK = 0xFF161618;
            static final int WHITE_A73 = 0x73FFFFFF;
            static final int BLACK_AFF = 0xFF000000;
            static final int CEC_FOCUSED = 0xFF0B12E6;
            static final int BLACK_AA8 = 0xA8000000;
            static final int WHITE_A1A = 0x1AFFFFFF;
            static final int WHITE_A00 = 0x00FFFFFF;
            static final int WHITE_A14 = 0x14FFFFFF;
            static final int BLACK_A80 = 0x80000000;
            static final int WHITE_A8C = 0x8CFFFFFF;
            static final int WHITE_AE6 = 0xE6FFFFFF;
            static final int BLACK_AB3 = 0xB3000000;
            static final int WHITE_A80 = 0x80FFFFFF;
            static final int WHITE_A59 = 0x59FFFFFF;
            static final int BLACK_A73 = 0x73000000;
        }

        static final class HomeText {
            private HomeText() {}
            static final int LABEL_PAINT_SIZE = 25;
            static final int TITLE_PAINT_SIZE = 76;
            static final int META_PAINT_SIZE = 29;
            static final int CLOCK_PAINT_SIZE = 176;
            static final int L_TITLE_SIZE = 124;
            static final int L_GREETING_SIZE = 40;
            static final int L_TAB_SIZE = 28;
            static final int L_TAB_ON_SIZE = 28;
            static final int L_CARD_TITLE_SIZE = 34;
            static final int L_CARD_META_SIZE = 23;
            static final int BADGE_PAINT_SIZE = 20;
            static final int L_QUOTE_SIZE = 24;
            static final int L_TIP_SIZE = 22;
            static final int L_DATE_SIZE = 34;
            static final int L_CLOCK_SIZE = 200;
            static final int L_WEATHER_SIZE = 34;
            static final int L_CLOCK_QUOTE_SIZE = 28;
            static final int BUTTON_PAINT_SIZE = 29;
            static final int CAPSULE_TEXT_SIZE = 29;
            static final int TIP_PAINT_SIZE = 25;
            static final int QUOTE_PAINT_SIZE = 21;
            static final int STALE_QUOTE_SIZE = 21;
            static final int ROW_TITLE_SIZE = 25;
            static final int DRAWER_LABEL_SIZE = 23;
            static final int CARD_TITLE_SIZE = 25;
            static final int CARD_TEXT_SIZE = 29;
            static final int CARD_DIM_SIZE = 23;
            static final int CARD_DIM_RIGHT_SIZE = 21;
            static final int BIG_TEMP_SIZE = 100;
            static final int HOUR_PAINT_SIZE = 22;
            static final int HOUR_TEMP_SIZE = 26;
            static final int PRICE_PAINT_SIZE = 26;
            static final int CHANGE_PAINT_SIZE = 21;
            static final int SHEET_TITLE_SIZE = 40;
            static final int GRID_LABEL_SIZE = 24;
            static final int TOAST_PAINT_SIZE = 26;
            static final int M_LABEL_SIZE = 26;
            static final int M_CELL_SIZE = 21;
            static final int M_NAME_SIZE = 31;
            static final int M_PRICE_SIZE = 31;
            static final int M_CHANGE_SIZE = 25;
            static final int M_MUTED_SIZE = 22;
            static final int M_HOUR_SIZE = 24;
            static final int M_HOUR_TEMP_SIZE = 38;
            static final int M_BIG_SIZE = 184;
            static final int M_BIG_DEGREE_SIZE = 84;
            static final int TEAM_BIG_SIZE = 26;
            static final int TEAM_SMALL_SIZE = 21;
            static final int TEAM_TAG_SIZE = 21;
            static final float LABEL_PAINT_TRACKING = 0.1f;
            static final float TITLE_PAINT_TRACKING = 0.005f;
            static final float META_PAINT_TRACKING = 0.014f;
            static final float CLOCK_PAINT_TRACKING = -0.01f;
            static final float L_TITLE_TRACKING = -0.05f;
            static final float L_GREETING_TRACKING = -0.02f;
            static final float L_TAB_TRACKING = -0.01f;
            static final float L_TAB_ON_TRACKING = -0.01f;
            static final float L_CARD_TITLE_TRACKING = -0.03f;
            static final float L_CLOCK_TRACKING = -0.01f;
            static final float M_LABEL_TRACKING = -0.01f;
            static final float M_CELL_TRACKING = -0.01f;
            static final float M_NAME_TRACKING = -0.02f;
            static final float M_PRICE_TRACKING = -0.02f;
            static final float M_CHANGE_TRACKING = -0.01f;
            static final float M_HOUR_TEMP_TRACKING = -0.035f;
            static final float M_BIG_TRACKING = -0.055f;
            static final float TEAM_BIG_TRACKING = -0.01f;
            static final int M_CELL_MIN_SIZE = 17;
        }

        static final class HomeLayout {
            private HomeLayout() {}
            static final float MARGIN = 96f;
            static final float CONTENT_R = 1824f;
            static final float HERO_TITLE_W = 900f;
            static final float SHELF_PAD = 28f;
            static final float TILE_GAP = 36f;
            static final float TILE_RADIUS = 22f;
            static final float TILE_MAX_W = 270f;
            static final float CAPSULE_TOP = 46f;
            static final float CAPSULE_H = 68f;
            static final float BUTTON = 56f;
            static final float SHIFT = 604f;
            static final float ROW_TITLE = 482f;
            static final float ROW_TOP = 510f;
            static final float ROW_H = 137f;
            static final float CARDS_TOP = 684f;
            static final float CARDS_H = 310f;
            static final float GRID_W = 300f;
            static final float GRID_H = 169f;
            static final float GRID_GAP = 40f;
            static final float GRID_ROW = 239f;
            static final float GRID_TOP = 196f;
            static final float GRID_RADIUS = 22f;
            static final float L_TITLE_BASE = 155f;
            static final float L_TABBAR_TOP = 176f;
            static final float L_TABBAR_H = 63f;
            static final float L_CARDS_TOP = 280f;
            static final float L_CARD_H = 420f;
            static final float L_TAB_BASE = 214f;
            static final float L_TAB_RULE = 239f;
            static final float L_TAB_GAP = 64f;
            static final float L_TALL_W = 328f;
            static final float L_CARD_GAP = 32f;
            static final float L_BUTTON = 72f;
            static final float L_BUTTON_GAP = 8f;
            static final float L_BUTTON_CY = 108f;
            static final float L_PLATTER_TOP = 848f;
            static final float L_TILE_H = 120f;
            static final float L_DATE_BASE = 330f;
            static final float L_CLOCK_BASE = 540f;
            static final float L_WEATHER_BASE = 626f;
            static final float L_QUOTES_BASE = 694f;
            static final float SHOW_W = 138f;
            static final float SHOW_H = 207f;
            static final float SHOW_GAP = 21f;
            static final float SHOW_LABEL_1 = 300f;
            static final float SHOW_LABEL_2 = 588f;
            static final int COLUMNS = 5;
            static final int VISIBLE_ROWS = 3;
            static final int SHOW_COLS = 11;
            static final int DRAWER_GROUP_GAP = 48;
            static final int WIDGET_LABEL_BASE = 28;
            static final int WIDGET_RULE_Y = 53;
            static final int TEMPERATURE_BASE = 231;
            static final int WEATHER_FOOTER_BASE = 288;
            static final int WEATHER_HOURS_X = 304;
            static final int WEATHER_HOUR_BASE = 104;
            static final int WEATHER_ICON_Y = 158;
            static final int WEATHER_HOUR_TEMP_BASE = 228;
            static final int CEC_INSET_X = 16;
            static final int CEC_INSET_Y = 16;
            static final int CEC_DOT_RADIUS = 6;
        }

        static final class HomeMotion {
            private HomeMotion() {}
            static final float FOCUS_GROW = 0.2f;
            static final long TOAST_MS = 6000L;
            static final long FADE_MS = 800L;
            static final long HOLD_MS = 500L;
            static final long WIGGLE_MS = 300L;
            static final float LIFT_GROW = 0.08f;
            static final float WIGGLE_DEG = 1.5f;
            static final float MOVE_LIFT_RESPONSE_S = 0.3f;
            static final float MOVE_LIFT_DAMPING = 0.72f;
            static final float HERO_FOCUS_RESPONSE_S = 0.28f;
            static final float HERO_FOCUS_DAMPING = 0.85f;
            static final float DRAWER_RESPONSE_S = 0.46f;
            static final float DRAWER_DAMPING = 0.92f;
            static final float SHEET_RESPONSE_S = 0.38f;
            static final float SHEET_DAMPING = 0.9f;
            static final float SCROLL_RESPONSE_S = 0.34f;
            static final float SCROLL_DAMPING = 1f;
            static final float TOP_FOCUS_RESPONSE_S = 0.26f;
            static final float TOP_FOCUS_DAMPING = 0.85f;
            static final float CARD_FOCUS_RESPONSE_S = 0.32f;
            static final float CARD_FOCUS_DAMPING = 0.78f;
            static final float STATUS_FOCUS_RESPONSE_S = 0.26f;
            static final float STATUS_FOCUS_DAMPING = 0.85f;
            static final float TILE_FOCUS_RESPONSE_S = 0.34f;
            static final float TILE_FOCUS_DAMPING = 0.74f;
            static final float TILE_SLIDE_RESPONSE_S = 0.32f;
            static final float TILE_SLIDE_DAMPING = 0.86f;
            static final float DRAWER_FOCUS_RESPONSE_S = 0.28f;
            static final float DRAWER_FOCUS_DAMPING = 0.85f;
            static final float GRID_FOCUS_RESPONSE_S = 0.3f;
            static final float GRID_FOCUS_DAMPING = 0.82f;
            static final int TOAST_ENTER_REDRAW_MS = 350;
            static final int TOAST_EXIT_REDRAW_MS = 450;
            static final float TOAST_ENTER_S = 0.3f;
            static final float TOAST_EXIT_S = 0.4f;
        }

        static final class HomeEffect {
            private HomeEffect() {}
            static final float SHADOW_BLUR = 26f;
            static final float DEEP_BLUR = 30f;
            static final int SOFT_BLUR = 12;
            static final int HALO_BLUR = 22;
            static final int BORDER_STROKE = 2;
            static final float RIM_STROKE = 2.5f;
            static final int FOCUS_STROKE = 3;
            static final float GRID_FOCUS_STROKE = 1.5f;
            static final float SPARK_STROKE = 2.5f;
            static final int HERO_TEXT_SHADOW_BLUR = 22;
            static final int HERO_TEXT_SHADOW_DY = 2;
            static final int TOOLTIP_SHADOW_BLUR = 16;
            static final int TOOLTIP_SHADOW_DY = 1;
            static final int QUOTE_SHADOW_BLUR = 14;
            static final int QUOTE_SHADOW_DY = 1;
            static final int FOCUS_GAP = 8;
            static final float FOCUS_HALF_STROKE = 1.5f;
            static final float GRID_SHADOW_ALPHA = 0.55f;
            static final float GRID_VEIL_ALPHA = 0.08f;
            static final float GRID_RIM_ALPHA = 0.4f;
        }

        static final class Canvas {
            private Canvas() {}
            static final float WIDTH = 1920f;
        }

        static final class Frame {
            private Frame() {}
            static final float FPS = 60f;
            static final float MAX_STEP_S = 0.05f;
            static final int IDLE_MS = 1000;
        }

        static final class Font {
            private Font() {}
            static final int DISPLAY_OPTICAL_SIZE = 32;
            static final int THIN_WEIGHT = 200;
            static final int LIGHT_WEIGHT = 300;
            static final int MEDIUM_WEIGHT = 500;
            static final int SEMIBOLD_WEIGHT = 600;
            static final int TEXT_OPTICAL_SIZE = 14;
            static final int REGULAR_WEIGHT = 400;
        }

        static final class SettingsColor {
            private SettingsColor() {}
            static final int AMBER = 0xFFFF7A12;
            static final int INK = 0xFF000000;
            static final int HAIRLINE = 0x8CFFFFFF;
            static final int MUTED = 0xB8FFFFFF;
        }

        static final class SettingsText {
            private SettingsText() {}
            static final int TITLE_SIZE = 92;
            static final int SUBTITLE_SIZE = 26;
            static final int VERSION_SIZE = 22;
            static final int ROW_LABEL_SIZE = 28;
            static final int ROW_VALUE_SIZE = 26;
            static final int ROW_SUB_SIZE = 23;
            static final int HEADER_SIZE = 22;
            static final int CELL_LABEL_SIZE = 25;
            static final int CELL_VALUE_SIZE = 25;
            static final int WARN_SIZE = 23;
            static final int NOTICE_SIZE = 23;
            static final int HEADING_SIZE = 34;
            static final int HINT_SIZE = 23;
            static final float TITLE_TRACKING = -0.05f;
            static final float ROW_LABEL_TRACKING = -0.01f;
            static final float HEADER_TRACKING = -0.01f;
            static final float HEADING_TRACKING = -0.02f;
        }

        static final class SettingsLayout {
            private SettingsLayout() {}
            static final float LIST_ROW_H = 68f;
            static final float LIST_ROW_GAP = 0f;
            static final float LIST_X = 1000f;
            static final float LIST_R = 1824f;
            static final float LIST_TOP = 186f;
            static final float ROW_H = 74f;
            static final float ROW_GAP = 0f;
            static final float CHOICE_H = 104f;
            static final float MARGIN = 96f;
            static final int PICK_VISIBLE = 9;
            static final int TITLE_BASE = 470;
            static final int TITLE_RULE_RIGHT = 900;
            static final int TITLE_RULE_BASE = 500;
            static final int SUBTITLE_BASE = 548;
            static final int VERSION_BASE = 588;
            static final int ROW_PADDING_X = 30;
        }

        static final class SettingsMotion {
            private SettingsMotion() {}
            static final float FOCUS_RESPONSE_S = 0.28f;
            static final float FOCUS_DAMPING = 0.85f;
            static final int FALLBACK_FRAME_MS = 100;
        }

        static final class SettingsEffect {
            private SettingsEffect() {}
            static final float SPINNER_STROKE = 3.5f;
            static final int SHADOW_BLUR = 22;
        }
    }
}
