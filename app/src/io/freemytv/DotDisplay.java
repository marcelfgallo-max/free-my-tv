package io.freemytv;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/** Rebuilds geometry only when the displayed value or size changes. Color is supplied at draw time. */
final class DotDisplay {
    private final Path dots = new Path();
    private final boolean compact;
    private final Path punctuation;
    DotDisplay() { this(false); }
    DotDisplay(boolean compact) { this(compact, false); }
    DotDisplay(boolean compact, boolean accentColon) {
        this.compact = compact;
        punctuation = accentColon ? new Path() : null;
    }
    private String value = "";
    private float height = -1, width;

    float draw(Canvas canvas, String text, float x, float baseline, float desiredHeight,
               float maxWidth, Paint ink) {
        return draw(canvas, text, x, baseline, desiredHeight, maxWidth, ink, ink);
    }

    float draw(Canvas canvas, String text, float x, float baseline, float desiredHeight,
               float maxWidth, Paint ink, Paint colonInk) {
        if (!DotMatrix.supports(text)) {
            canvas.drawText(text, x, baseline, ink);
            return ink.measureText(text);
        }
        float h = DotMatrix.fittedHeight(text, desiredHeight, maxWidth, compact);
        if (!value.equals(text) || height != h) {
            value = text;
            height = h;
            dots.rewind();
            if (punctuation != null) punctuation.rewind();
            float step = h / DotMatrix.HEIGHT, offset = 0, radius = DotMatrix.RADIUS * step;
            for (int i = 0; i < text.length(); i++) {
                char ch = text.charAt(i);
                int cols = DotMatrix.columns(ch, compact);
                for (int y = 0; y < 7; y++) {
                    int bits = DotMatrix.row(ch, y, compact);
                    for (int col = 0; col < cols; col++) {
                        if ((bits & (1 << (cols - col - 1))) != 0)
                            (ch == ':' && punctuation != null ? punctuation : dots).addCircle(offset + col * step + radius, y * step + radius - h,
                                    radius, Path.Direction.CW);
                    }
                }
                offset += (cols - 1 + 2 * DotMatrix.RADIUS + DotMatrix.GAP) * step;
            }
            width = DotMatrix.widthUnits(text, compact) * step;
        }
        canvas.save();
        canvas.translate(x, baseline);
        canvas.drawPath(dots, ink);
        if (punctuation != null) canvas.drawPath(punctuation, colonInk);
        canvas.restore();
        return width;
    }
}
