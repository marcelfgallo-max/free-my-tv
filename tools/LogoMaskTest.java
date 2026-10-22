package io.freemytv;

/** Host-side regression checks for alpha and the YouTube negative-space triangle. */
public final class LogoMaskTest {
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        int[] generic = {0x00FF0000, 0x80FF0000, 0xFF123456, 0xFFFFFFFF};
        LogoMask.white(generic, 4, 1, false);
        check(generic[0] >>> 24 == 0, "transparent background");
        check(generic[1] == 0x80FFFFFF, "antialiased alpha");
        check(generic[2] == 0xFFFFFFFF, "colored ink becomes white");
        check(generic[3] == 0xFFFFFFFF, "white ink retained");
        int w = 8, h = 5;
        int[] youtube = new int[w * h];
        for (int y = 1; y <= 3; y++) for (int x = 1; x <= 4; x++) youtube[y * w + x] = 0xFFFF0000;
        youtube[2 * w + 2] = 0xFFFFFFFF;
        youtube[2 * w + 3] = 0xFFFF8080;
        youtube[2 * w + 6] = 0xFFFFFFFF;
        LogoMask.white(youtube, w, h, true);
        check(youtube[2 * w + 2] >>> 24 == 0, "play triangle cutout");
        check(youtube[2 * w + 3] >>> 24 == 127, "triangle antialias edge");
        check(youtube[w + 1] == 0xFFFFFFFF, "play shape retained");
        check(youtube[2 * w + 6] == 0xFFFFFFFF, "wordmark outside play retained");
        int[] noRed = {0xFFEEEEEE, 0x00000000, 0x80112233};
        LogoMask.white(noRed, 3, 1, true);
        check(noRed[0] == 0xFFFFFFFF && noRed[1] >>> 24 == 0 && noRed[2] == 0x80FFFFFF, "missing red shape is nondestructive");
        System.out.println("LogoMask: 9 checks passed");
    }
}
