package com.gingerbread.launcher;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;

/** Small canvas-drawn icons. (cx,cy) = centre, s = half-size. Caller sets colour + stroke width. */
final class Glyphs {
    private Glyphs() {}

    static void wifi(Canvas c, Paint p, float cx, float cy, float s) {
        RectF r = new RectF();
        float by = cy + s * 0.7f;
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        for (int i = 1; i <= 3; i++) {
            float rad = s * 0.55f * i;
            r.set(cx - rad, by - rad, cx + rad, by + rad);
            c.drawArc(r, 225, 90, false, p);
        }
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, by, s * 0.14f, p);
    }

    static void bluetooth(Canvas c, Paint p, float cx, float cy, float s) {
        Path path = new Path();
        path.moveTo(cx - s * 0.5f, cy - s * 0.5f);
        path.lineTo(cx + s * 0.5f, cy + s * 0.5f);
        path.lineTo(cx, cy + s);
        path.lineTo(cx, cy - s);
        path.lineTo(cx + s * 0.5f, cy - s * 0.5f);
        path.lineTo(cx - s * 0.5f, cy + s * 0.5f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        c.drawPath(path, p);
    }

    static void gps(Canvas c, Paint p, float cx, float cy, float s) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawCircle(cx, cy, s * 0.5f, p);
        c.drawLine(cx, cy - s * 0.75f, cx, cy - s, p);
        c.drawLine(cx, cy + s * 0.75f, cx, cy + s, p);
        c.drawLine(cx - s * 0.75f, cy, cx - s, cy, p);
        c.drawLine(cx + s * 0.75f, cy, cx + s, cy, p);
        p.setStyle(Paint.Style.FILL);
        c.drawCircle(cx, cy, s * 0.16f, p);
    }

    static void sync(Canvas c, Paint p, float cx, float cy, float s) {
        RectF r = new RectF(cx - s * 0.7f, cy - s * 0.7f, cx + s * 0.7f, cy + s * 0.7f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        c.drawArc(r, 200, 130, false, p);
        c.drawArc(r, 20, 130, false, p);
        arrow(c, p, cx, cy, s * 0.7f, 330, s * 0.45f);
        arrow(c, p, cx, cy, s * 0.7f, 150, s * 0.45f);
        p.setStyle(Paint.Style.FILL);
    }

    private static void arrow(Canvas c, Paint p, float cx, float cy, float rad, float deg, float size) {
        double a = Math.toRadians(deg);
        float ex = cx + (float) Math.cos(a) * rad, ey = cy + (float) Math.sin(a) * rad;
        float tx = -(float) Math.sin(a), ty = (float) Math.cos(a);      // clockwise tangent
        float nx = (float) Math.cos(a), ny = (float) Math.sin(a);       // radial
        Path path = new Path();
        path.moveTo(ex + tx * size, ey + ty * size);
        path.lineTo(ex + nx * size * 0.8f, ey + ny * size * 0.8f);
        path.lineTo(ex - nx * size * 0.8f, ey - ny * size * 0.8f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
    }

    static void sun(Canvas c, Paint p, float cx, float cy, float s) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        c.drawCircle(cx, cy, s * 0.4f, p);
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45);
            float cs = (float) Math.cos(a), sn = (float) Math.sin(a);
            c.drawLine(cx + cs * s * 0.62f, cy + sn * s * 0.62f, cx + cs * s * 0.95f, cy + sn * s * 0.95f, p);
        }
    }
}
