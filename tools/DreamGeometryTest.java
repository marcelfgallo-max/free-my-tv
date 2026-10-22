package io.freemytv;

/** Host checks for photo crop coverage and numeric layout, with no Android runtime. */
public final class DreamGeometryTest {
    private static int checks;
    private static void require(boolean ok, String message) {
        checks++;
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        float[] out = new float[3];
        for (float scale : new float[] {.5f, 1, 2}) {
            float left = Theme.DREAM_PHOTO_INSET * scale, top = left;
            float width = (1920 - 2 * Theme.DREAM_PHOTO_INSET) * scale;
            float height = (1080 - 2 * Theme.DREAM_PHOTO_INSET) * scale;
            for (int[] size : new int[][] {{2112,1188}, {3840,2160}, {1000,1000}, {1200,1800}}) {
                for (int step = 0; step <= 100; step++) {
                    float progress = step / 100f;
                    for (float px : new float[] {-.8f, .8f}) for (float py : new float[] {-.6f, .6f}) {
                        for (boolean zoomIn : new boolean[] {false,true}) {
                            float f = zoomIn ? progress : 1 - progress;
                            DreamGeometry.place(left, top, width, height, size[0], size[1], 1 + .1f * f,
                                    px * f, py * f, out);
                            require(out[0] <= left + .01f && out[1] <= top + .01f, "photo exposes top/left edge");
                            require(out[0] + size[0]*out[2] >= left+width-.01f
                                    && out[1] + size[1]*out[2] >= top+height-.01f, "photo exposes bottom/right edge");
                            require(Float.isFinite(out[2]) && out[2] > 0, "invalid photo scale");
                        }
                    }
                }
            }
            for (int minute = 0; minute < 1440; minute++) {
                for (boolean twelve : new boolean[] {false,true}) {
                    int hour = minute / 60;
                    String text = String.format(java.util.Locale.US, twelve ? "%d:%02d" : "%02d:%02d",
                            twelve ? (hour % 12 == 0 ? 12 : hour % 12) : hour, minute % 60);
                    float max = (Theme.DREAM_CLOCK_CARD_WIDTH - 2 * Theme.DREAM_PANEL_PADDING)*scale;
                    float h = DotMatrix.fittedHeight(text,Theme.DREAM_CLOCK_HEIGHT*scale,max);
                    require(DotMatrix.widthUnits(text)*h/DotMatrix.HEIGHT <= max+.01f, "corner clock exceeds panel");
                    require(Theme.DREAM_CLOCK_BASELINE*scale-h >= (Theme.DREAM_CLOCK_RULE_Y+24)*scale, "clock overlaps date separator");
                    h = DotMatrix.fittedHeight(text,Theme.DREAM_FALLBACK_CLOCK_HEIGHT*scale,1120*scale);
                    require(DotMatrix.widthUnits(text)*h/DotMatrix.HEIGHT<=1120*scale+.01f,"fallback clock too wide");
                }
            }
        }
        require(Theme.DREAM_SHOW_MS > Theme.DREAM_FADE_MS, "invalid slideshow timing");
        require(Theme.DREAM_PHOTO_BRIGHTNESS_NIGHT > 0 && Theme.DREAM_PHOTO_BRIGHTNESS_NIGHT <= 1, "night brightness");
        System.out.println("Dream geometry: " + checks + " assertions passed (photo coverage, both clock formats, 3 display scales).");
    }
}
