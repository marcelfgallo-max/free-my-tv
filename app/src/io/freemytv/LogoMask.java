package io.freemytv;

/** Alpha-only shelf logo conversion. Called by the loader, never by drawing code. */
final class LogoMask {
    private LogoMask() {}

    static void white(int[] pixels, int width, int height, boolean youtube) {
        int left = width, right = -1, top = height, bottom = -1;
        if (youtube) {
            // Locate the existing red play shape, so white lettering outside it stays intact.
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int p = pixels[y * width + x];
                    int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                    if ((p >>> 24) > 128 && r > 100 && r > g * 1.5f && r > b * 1.5f) {
                        left = Math.min(left, x); right = Math.max(right, x);
                        top = Math.min(top, y); bottom = Math.max(bottom, y);
                    }
                }
            }
        }
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int i = y * width + x, p = pixels[i], alpha = p >>> 24;
                if (x >= left && x <= right && y >= top && y <= bottom) {
                    // Preserve the play triangle as negative space, including its antialiased edge.
                    int r = (p >> 16) & 255, g = (p >> 8) & 255, b = p & 255;
                    float white = Math.min(g, b) / (float) Math.max(1, r);
                    alpha = Math.round(alpha * (1f - Math.min(1f, white)));
                }
                pixels[i] = (alpha << 24) | 0x00FFFFFF;
            }
        }
    }
}
