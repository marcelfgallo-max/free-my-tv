package io.freemytv;

/** Visual constants. Legacy preserves the pre-design-system rendering exactly. */
final class Theme {
    private Theme() {}

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
    }
}
