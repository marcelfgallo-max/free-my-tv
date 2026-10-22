package io.freemytv;

/** Original seven-row numeric dot patterns, not an embedded commercial font. */
final class DotMatrix {
    private DotMatrix() {}
    static final float RADIUS = 0.43f, GAP = 1.1f, HEIGHT = 6 + 2 * RADIUS;
    private static final int[][] DIGITS = {
        {14,17,17,17,17,17,14}, {4,12,4,4,4,4,14},
        {14,17,1,2,4,8,31}, {30,1,1,14,1,1,30},
        {2,6,10,18,31,2,2}, {31,16,16,30,1,1,30},
        {14,16,16,30,17,17,14}, {31,1,2,4,8,8,8},
        {14,17,17,14,17,17,14}, {14,17,17,15,1,1,14}
    };
    static int columns(char ch) {
        if (ch >= '0' && ch <= '9') return 5;
        if (ch == ':') return 1;
        if (ch == '°' || ch == '-' || ch == '−') return 3;
        return 0;
    }
    static int columns(char ch, boolean compact) {
        return compact && ch == '1' ? 3 : columns(ch);
    }
    static int row(char ch, int y, boolean compact) {
        return compact && ch == '1' ? row(ch, y) >> 1 : row(ch, y);
    }
    static int row(char ch, int y) {
        if (y < 0 || y > 6) return 0;
        if (ch >= '0' && ch <= '9') return DIGITS[ch - '0'][y];
        if (ch == ':') return y == 1 || y == 5 ? 1 : 0;
        if (ch == '°') return y == 0 || y == 2 ? 2 : y == 1 ? 5 : 0;
        if (ch == '-' || ch == '−') return y == 3 ? 7 : 0;
        return 0;
    }
    static boolean supports(String text) {
        if (text == null || text.isEmpty()) return false;
        for (int i = 0; i < text.length(); i++) if (columns(text.charAt(i)) == 0) return false;
        return true;
    }
    static float widthUnits(String text) { return widthUnits(text, false); }
    static float widthUnits(String text, boolean compact) {
        if (!supports(text)) return 0;
        float w = 0;
        for (int i = 0; i < text.length(); i++) {
            if (i > 0) w += GAP;
            w += columns(text.charAt(i), compact) - 1 + 2 * RADIUS;
        }
        return w;
    }
    static float fittedHeight(String text, float desired, float maxWidth) {
        return fittedHeight(text, desired, maxWidth, false);
    }
    static float fittedHeight(String text, float desired, float maxWidth, boolean compact) {
        float units = widthUnits(text, compact);
        return units == 0 ? 0 : Math.min(desired, Math.max(0, maxWidth) * HEIGHT / units);
    }
}
