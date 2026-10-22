package io.freemytv;

import java.util.Locale;

/** Host geometry checks for all minutes and signed temperatures, independent of Android rendering. */
public final class DotMatrixTest {
    private static int checks;
    private static void check(boolean value, String message) {
        checks++;
        if (!value) throw new AssertionError(message);
    }
    private static void fits(String text, float height, float room) {
        check(DotMatrix.supports(text), "unsupported value: " + text);
        float actual = DotMatrix.fittedHeight(text, height, room);
        float width = DotMatrix.widthUnits(text) * actual / DotMatrix.HEIGHT;
        check(actual > 0 && actual <= height && width <= room + 0.001f, "overflow: " + text);
        for (int i = 0; i < text.length(); i++) {
            int columns = DotMatrix.columns(text.charAt(i));
            for (int row = 0; row < 7; row++)
                check((DotMatrix.row(text.charAt(i), row) >> columns) == 0, "glyph outside advance");
        }
    }
    public static void main(String[] args) {
        float firstWidth = DotMatrix.widthUnits("00:00");
        for (int hour = 0; hour < 24; hour++) for (int minute = 0; minute < 60; minute++) {
            String clock = String.format(Locale.US, "%02d:%02d", hour, minute);
            fits(clock, Theme.CLOCK_DOT_HEIGHT, 400);
            check(firstWidth == DotMatrix.widthUnits(clock), "24-hour clock changes greeting position");
            fits(String.format(Locale.US, "%d:%02d", hour % 12 == 0 ? 12 : hour % 12, minute), Theme.CLOCK_DOT_HEIGHT, 400);
        }
        for (int temp = -99; temp <= 199; temp++) fits(temp + "°", Theme.TEMPERATURE_DOT_HEIGHT, 280);
        for (int temp = -99; temp <= 199; temp++) {
            String text = temp + "°";
            float height = DotMatrix.fittedHeight(text, Theme.TEMPERATURE_DOT_HEIGHT, 280, true);
            check(DotMatrix.widthUnits(text, true) * height / DotMatrix.HEIGHT <= 280.001f, "compact temperature overflow");
        }
        check(DotMatrix.widthUnits("18°", true) < DotMatrix.widthUnits("18°"), "temperature one must be narrower");
        fits("−12°", Theme.TEMPERATURE_DOT_HEIGHT, 280);
        check(!DotMatrix.supports("—"), "missing data must use text fallback");
        check(!DotMatrix.supports("") && !DotMatrix.supports(null), "empty values must use fallback");
        check(DotMatrix.fittedHeight("18°", 142, 0) == 0, "zero-width bound");
        check(Theme.DRAWER_ROW_TOP + Theme.DRAWER_ROW_HEIGHT < Theme.WIDGET_TOP, "cards overlap controls");
        check(Theme.WIDGET_TOP + Theme.WIDGET_HEIGHT <= 1020, "widgets outside safe area");
        check(Theme.WIDGET_HEIGHT - 2 * Theme.WIDGET_PADDING > 309, "four quotes rows clipped");
        System.out.println("DotMatrix: " + checks + " geometry checks passed (all 1440 minutes, 12/24h, signed temperatures and drawer bounds)");
    }
}
