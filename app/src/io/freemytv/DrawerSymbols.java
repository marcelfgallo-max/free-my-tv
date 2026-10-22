package io.freemytv;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import java.util.HashMap;
import java.util.Map;

/** Original 24-unit utility pictograms for the drawer. Paths are built once, shared by both themes. */
final class DrawerSymbols {
    private DrawerSymbols() {}
    private static final class Icon {
        final Path line = new Path(), solid = new Path();
    }
    private static final Map<String, Icon> ICONS = create();
    private static final Paint STROKE = new Paint(Paint.ANTI_ALIAS_FLAG), FILL = new Paint(Paint.ANTI_ALIAS_FLAG);
    static {
        STROKE.setStyle(Paint.Style.STROKE);
        STROKE.setStrokeWidth(Theme.DRAWER_ICON_STROKE);
        STROKE.setStrokeCap(Paint.Cap.ROUND);
        STROKE.setStrokeJoin(Paint.Join.ROUND);
    }
    static boolean has(String name) { return ICONS.containsKey(name); }
    static boolean draw(Canvas canvas, String name, float cx, float cy, float size, int color) {
        Icon icon = ICONS.get(name);
        if (icon == null) return false;
        STROKE.setColor(color);
        FILL.setColor(color);
        canvas.save();
        canvas.translate(cx - size / 2, cy - size / 2);
        canvas.scale(size / 24, size / 24);
        canvas.drawPath(icon.line, STROKE);
        canvas.drawPath(icon.solid, FILL);
        canvas.restore();
        return true;
    }
    private static void line(Path p, float x, float y, float x2, float y2) {
        p.moveTo(x, y); p.lineTo(x2, y2);
    }
    private static void circle(Path p, float x, float y, float radius) {
        p.addCircle(x, y, radius, Path.Direction.CW);
    }
    private static void box(Path p, float left, float top, float right, float bottom, float radius) {
        p.addRoundRect(left, top, right, bottom, radius, radius, Path.Direction.CW);
    }
    private static void triangle(Path p, float x, float y, float half, float height) {
        p.moveTo(x, y); p.lineTo(x + half, y + height); p.lineTo(x - half, y + height); p.close();
    }
    private static void plus(Path p, float x, float y, float arm) {
        line(p, x - arm, y, x + arm, y); line(p, x, y - arm, x, y + arm);
    }
    private static void cloud(Path p, float dy) {
        p.moveTo(6, 17 + dy);
        p.cubicTo(1, 17 + dy, 1, 10 + dy, 6, 10 + dy);
        p.cubicTo(6, 3 + dy, 16, 3 + dy, 17, 10 + dy);
        p.cubicTo(23, 9 + dy, 24, 17 + dy, 18, 17 + dy);
        p.close();
    }
    private static void moon(Path p) {
        p.moveTo(16, 2);
        p.cubicTo(3, 1, 0, 16, 9, 21);
        p.cubicTo(15, 24, 21, 20, 22, 15);
        p.cubicTo(13, 17, 9, 9, 16, 2);
        p.close();
    }
    private static Map<String, Icon> create() {
        Map<String, Icon> map = new HashMap<>();
        Icon icon = new Icon();
        Path p = icon.line;
        p.moveTo(7, 5); p.lineTo(17, 5);
        p.cubicTo(20, 5, 20, 8, 21, 12); p.lineTo(22, 18);
        p.cubicTo(22.5f, 21, 20, 21, 18, 19); p.lineTo(15, 16); p.lineTo(9, 16); p.lineTo(6, 19);
        p.cubicTo(4, 21, 1.5f, 21, 2, 18); p.lineTo(3, 12); p.cubicTo(4, 8, 4, 5, 7, 5); p.close();
        plus(p, 7.3f, 10, 2);
        circle(icon.solid, 17, 8.3f, 0.7f); circle(icon.solid, 19, 10.3f, 0.7f);
        circle(icon.solid, 15, 10.3f, 0.7f); circle(icon.solid, 17, 12.3f, 0.7f);
        map.put("controller", icon); map.put("gamepad", icon);
        icon = new Icon(); box(icon.line, 2, 7, 22, 17, 1.2f);
        line(icon.line, 6, 12, 9, 12); circle(icon.solid, 15, 12, 0.9f); circle(icon.solid, 18, 12, 0.9f);
        map.put("settop", icon);
        icon = new Icon();
        // An open gap in the screen frame separates the airplay triangle from the monitor.
        icon.line.moveTo(7, 17); icon.line.lineTo(2, 17); icon.line.lineTo(2, 3);
        icon.line.lineTo(22, 3); icon.line.lineTo(22, 17); icon.line.lineTo(17, 17);
        triangle(icon.solid, 12, 14, 4.5f, 7); map.put("airplay", icon);
        icon = new Icon(); box(icon.line, 2, 6, 22, 18, 1.2f);
        plus(icon.line, 7, 12, 2); plus(icon.line, 17, 12, 2); map.put("handheld", icon);
        icon = new Icon(); box(icon.line, 2, 3, 22, 17, 0.8f);
        triangle(icon.solid, 12, 17, 4, 4); line(icon.line, 8, 21, 16, 21);
        map.put("tv", icon); map.put("live_tv", icon);
        icon = new Icon();
        for (int y = 0; y < 3; y++) for (int x = 0; x < 3; x++) circle(icon.solid, 5 + 7 * x, 5 + 7 * y, 1.65f);
        map.put("apps", icon);
        icon = new Icon();
        for (int i = 0; i < 32; i++) {
            double angle = (i + 0.5) * Math.PI / 16;
            float radius = (i % 4 < 2) ? 10 : 8;
            float x = 12 + (float) Math.cos(angle) * radius, y = 12 + (float) Math.sin(angle) * radius;
            if (i == 0) icon.line.moveTo(x, y); else icon.line.lineTo(x, y);
        }
        icon.line.close(); circle(icon.line, 12, 12, 3.4f); map.put("settings", icon);
        icon = new Icon(); box(icon.line, 2, 2, 22, 18, 1);
        line(icon.line, 6, 7, 13, 7); line(icon.line, 16, 7, 18, 7); line(icon.line, 15, 5, 15, 9);
        line(icon.line, 6, 13, 8, 13); line(icon.line, 11, 13, 18, 13); line(icon.line, 9, 11, 9, 15);
        triangle(icon.solid, 12, 18, 4, 4); map.put("tv_settings", icon);
        icon = new Icon(); moon(icon.line); map.put("bedtime", icon); map.put("clear_night", icon);
        icon = new Icon(); cloud(icon.line, 1); map.put("cloud", icon);
        icon = new Icon(); cloud(icon.line, -2);
        for (int i = 0; i < 3; i++) line(icon.line, 7 + i * 5, 18, 5 + i * 5, 22);
        map.put("rainy", icon);
        icon = new Icon(); circle(icon.line, 12, 12, 4);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            line(icon.line, 12 + 7 * (float) Math.cos(a), 12 + 7 * (float) Math.sin(a),
                    12 + 10 * (float) Math.cos(a), 12 + 10 * (float) Math.sin(a));
        }
        map.put("sunny", icon);
        icon = new Icon(); cloud(icon.line, -2);
        line(icon.line, 4, 19, 20, 19); line(icon.line, 7, 22, 17, 22); map.put("foggy", icon);
        icon = new Icon(); cloud(icon.line, -2);
        icon.line.moveTo(13, 16); icon.line.lineTo(10, 20); icon.line.lineTo(14, 20); icon.line.lineTo(11, 23);
        map.put("thunderstorm", icon);
        return map;
    }
}
