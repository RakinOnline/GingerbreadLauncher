package com.gingerbread.launcher;

import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.Shader;

/** Draws a dark wallpaper with glowing coloured lines, in the style of the Nexus live wallpaper. */
final class WallpaperArt {
    private WallpaperArt() {}

    static Bitmap create(int w, int h) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF07070C, 0xFF121019, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);

        // purple-blue haze in the lower half
        p.setShader(new RadialGradient(w * 0.5f, h * 0.62f, w * 0.95f, 0x99667799, 0x00000000, Shader.TileMode.CLAMP));
        c.save();
        c.scale(1f, 0.33f, w * 0.5f, h * 0.62f);
        c.drawCircle(w * 0.5f, h * 0.62f, w * 0.95f, p);
        c.restore();
        p.setShader(null);

        float t = w * 0.012f;
        line(c, p, w * 0.26f, h * 0.03f, w * 0.26f, h * 0.52f, 0xFFE53935, t);
        line(c, p, w * 0.26f, h * 0.47f, w * 0.26f, h * 0.95f, 0xFFFFC107, t);
        line(c, p, w * 0.74f, h * 0.06f, w * 0.74f, h * 0.93f, 0xFF3D5AFE, t);
        line(c, p, 0, h * 0.43f, w * 0.45f, h * 0.43f, 0xFFE53935, t * 0.8f);
        line(c, p, w * 0.82f, h * 0.44f, w, h * 0.44f, 0xFFFFC107, t * 0.8f);
        line(c, p, 0, h * 0.545f, w * 0.65f, h * 0.545f, 0xFFF4F27A, t * 1.5f);
        line(c, p, w * 0.45f, h * 0.549f, w, h * 0.549f, 0xFF69F0AE, t * 1.5f);
        line(c, p, w * 0.28f, h * 0.585f, w * 0.95f, h * 0.585f, 0xFF69F0AE, t * 0.5f);
        return b;
    }

    private static void line(Canvas c, Paint p, float x1, float y1, float x2, float y2, int col, float width) {
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.BUTT);
        p.setColor(col);
        p.setAlpha(90);
        p.setStrokeWidth(width * 3);
        p.setMaskFilter(new BlurMaskFilter(width * 2, BlurMaskFilter.Blur.NORMAL));
        c.drawLine(x1, y1, x2, y2, p);
        p.setMaskFilter(null);
        p.setColor(col);
        p.setStrokeWidth(width);
        c.drawLine(x1, y1, x2, y2, p);
    }
}
