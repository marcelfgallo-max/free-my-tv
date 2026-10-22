package io.freemytv;

/** Center-crop and bounded pan inside the rounded photo frame; no per-frame allocations. */
final class DreamGeometry {
    private DreamGeometry() {}

    static void place(float left, float top, float width, float height, int imageWidth, int imageHeight,
                      float zoom, float panX, float panY, float[] out) {
        float scale = Math.max(width / imageWidth, height / imageHeight) * zoom;
        float extraX = imageWidth * scale - width, extraY = imageHeight * scale - height;
        out[0] = left - extraX / 2 + extraX / 2 * panX;
        out[1] = top - extraY / 2 + extraY / 2 * panY;
        out[2] = scale;
    }
}
