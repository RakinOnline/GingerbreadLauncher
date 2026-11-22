package com.gingerbread.launcher;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Bundle;
import android.view.View;

/** Gingerbread easter egg: gingerbread man. Long-press him to turn him into the zombie. */
public class EggActivity extends Activity {
    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(new EggView(this));
    }

    static class EggView extends View {
        private int stage = 0;           // 0 = gingerbread man, 1 = zombie
        private float hop = 0f;
        private final long t0 = System.nanoTime();
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Path path = new Path();

        EggView(Context c) {
            super(c);
            setOnClickListener(v -> { if (stage == 1) { hop = 1f; invalidate(); } });
            setOnLongClickListener(v -> { stage = 1 - stage; hop = 0f; invalidate(); return true; });
        }

        @Override protected void onDraw(Canvas c) {
            int w = getWidth(), h = getHeight();
            boolean z = stage == 1;
            c.drawColor(z ? 0xFF14210F : 0xFFF2E6C9);
            float t = (System.nanoTime() - t0) / 1e9f;
            float s = Math.min(w, h) * 0.55f;
            float cx = w / 2f, cy = h * 0.42f;

            float jump = 0f;
            if (z) {
                jump = -Math.abs((float) Math.sin(t * 2)) * s * 0.03f;
                if (hop > 0f) {
                    jump -= (float) Math.sin((1f - hop) * Math.PI) * s * 0.25f;
                    hop = Math.max(0f, hop - 0.03f);
                }
            }
            c.save();
            if (z) c.rotate((float) Math.sin(t * 2.2f) * 6f, cx, cy + s * 0.78f);
            c.translate(0, jump);
            drawMan(c, cx, cy, s, z, t);
            c.restore();

            p.setStyle(Paint.Style.FILL);
            p.setTextAlign(Paint.Align.CENTER);
            p.setColor(z ? 0xFFB7D86A : 0xFF5A3A18);
            p.setTextSize(Math.min(w, h) * 0.07f);
            c.drawText(z ? "Zombie Gingerbread" : "Android 2.3 Gingerbread", cx, h * 0.80f, p);
            p.setTextSize(Math.min(w, h) * 0.04f);
            p.setColor(z ? 0xFF8FA66A : 0xFF8A6A40);
            c.drawText(z ? "tap to make him hop  |  long-press to cure him"
                         : "long-press for a surprise", cx, h * 0.86f, p);
            p.setTextAlign(Paint.Align.LEFT);

            if (z) postInvalidateOnAnimation();
        }

        private void drawMan(Canvas c, float cx, float cy, float s, boolean z, float t) {
            int body = z ? 0xFF8BA146 : 0xFFC27A3A;
            int icing = z ? 0xFFE8EBC9 : 0xFFFFFFFF;

            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setColor(body);
            p.setStrokeWidth(s * 0.2f);
            float armY = z ? cy - s * 0.5f + (float) Math.sin(t * 4) * s * 0.03f
                           : cy + s * 0.05f + (float) Math.sin(t * 3) * s * 0.05f;
            c.drawLine(cx - s * 0.2f, cy - s * 0.15f, cx - s * 0.62f, armY, p);
            c.drawLine(cx + s * 0.2f, cy - s * 0.15f, cx + s * 0.62f, armY, p);
            c.drawLine(cx - s * 0.12f, cy + s * 0.25f, cx - s * 0.26f, cy + s * 0.78f, p);
            c.drawLine(cx + s * 0.12f, cy + s * 0.25f, cx + s * 0.26f, cy + s * 0.78f, p);

            p.setStyle(Paint.Style.FILL);
            r.set(cx - s * 0.3f, cy - s * 0.33f, cx + s * 0.3f, cy + s * 0.38f);
            c.drawRoundRect(r, s * 0.12f, s * 0.12f, p);
            c.drawCircle(cx, cy - s * 0.58f, s * 0.3f, p);

            p.setColor(icing);
            for (int i = 0; i < 3; i++) c.drawCircle(cx, cy - s * 0.12f + i * s * 0.17f, s * 0.04f, p);

            if (!z) {
                p.setColor(0xFFFFFFFF);
                c.drawCircle(cx - s * 0.11f, cy - s * 0.64f, s * 0.035f, p);
                c.drawCircle(cx + s * 0.11f, cy - s * 0.64f, s * 0.035f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.025f);
                r.set(cx - s * 0.12f, cy - s * 0.62f, cx + s * 0.12f, cy - s * 0.45f);
                c.drawArc(r, 20, 140, false, p);
            } else {
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(s * 0.03f);
                p.setColor(0xFFB3261E);
                float d = s * 0.045f, ey = cy - s * 0.64f;
                for (int sign = -1; sign <= 1; sign += 2) {
                    float ex = cx + sign * s * 0.11f;
                    c.drawLine(ex - d, ey - d, ex + d, ey + d, p);
                    c.drawLine(ex - d, ey + d, ex + d, ey - d, p);
                }
                p.setColor(icing);
                path.reset();
                float my = cy - s * 0.47f;
                path.moveTo(cx - s * 0.14f, my);
                for (int i = 1; i <= 6; i++) {
                    path.lineTo(cx - s * 0.14f + i * s * 0.28f / 6f, my + (i % 2 == 0 ? 0 : s * 0.03f));
                }
                c.drawPath(path, p);
                // stitches on his chest
                p.setStrokeWidth(s * 0.02f);
                for (int i = 0; i < 3; i++) {
                    float y = cy + s * 0.08f + i * s * 0.07f;
                    c.drawLine(cx + s * 0.12f, y, cx + s * 0.24f, y + s * 0.03f, p);
                }
            }
            p.setStyle(Paint.Style.FILL);
        }
    }
}
