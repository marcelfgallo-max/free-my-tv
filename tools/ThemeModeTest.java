package io.freemytv;

/** Host checks for stored-mode fallback and foreground/background contracts. */
public final class ThemeModeTest {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static double luminance(int c) {
        double sum = 0;
        double[] weights = {0.2126, 0.7152, 0.0722};
        for (int i = 0; i < 3; i++) {
            double v = ((c >> (16 - 8 * i)) & 255) / 255.0;
            sum += weights[i] * (v <= 0.04045 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4));
        }
        return sum;
    }
    private static double contrast(int a, int b) {
        double x = luminance(a), y = luminance(b);
        return (Math.max(x, y) + 0.05) / (Math.min(x, y) + 0.05);
    }
    public static void main(String[] args) {
        check(Theme.MODE_NIGHT.equals(Theme.normalizeMode(null)), "new install remains Night");
        check(Theme.MODE_NIGHT.equals(Theme.normalizeMode("obsolete")), "unknown persisted mode fallback");
        check(Theme.MODE_DAY.equals(Theme.normalizeMode("day")), "Day restore");
        check(Theme.MODE_NIGHT.equals(Theme.normalizeMode("night")), "Night restore");
        check(Theme.color(Theme.TEXT_PRIMARY, true) == Theme.Legacy.HomeColor.M_INK, "Night primary preserved");
        check(Theme.color(Theme.TEXT_SECONDARY, true) == Theme.Legacy.HomeColor.M_MUTED, "Night secondary preserved");
        check(Theme.color(Theme.DIVIDER, true) == Theme.Legacy.HomeColor.M_HAIRLINE, "Night rule preserved");
        for (boolean night : new boolean[] {false, true}) {
            check(contrast(Theme.color(Theme.TEXT_PRIMARY, night), Theme.color(Theme.BACKGROUND, night)) > 7, "primary text contrast");
            check(contrast(Theme.color(Theme.LOGO, night), Theme.color(Theme.SURFACE, night)) > 7, "logo contrast");
            check(Theme.color(Theme.FOCUS, night) == 0xFFFF5A20, "orange focus preserved");
        }
        check(contrast(Theme.color(Theme.TEXT_SECONDARY, false), Theme.color(Theme.BACKGROUND, false)) > 4.5, "Day secondary contrast");
        boolean rejected = false;
        try { Theme.color(999, false); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected, "unknown role rejected");
        System.out.println("ThemeMode: 15 checks passed");
    }
}
