package com.gingerbread.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Four "touch keys under the glass": Back, Home, Menu, Search. */
public class TouchKeysView extends View {
    public interface KeyListener { void onKey(int index); }

    private KeyListener listener;
    private int pressed = -1;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bg = new Paint();
    private final Path path = new Path();
    private final RectF r = new RectF();

    public TouchKeysView(Context c) {
        super(c);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(Ui.dp(c, 2.5f));
    }

    public void setListener(KeyListener l) { listener = l; }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        bg.setShader(new LinearGradient(0, 0, 0, h, 0xFF1A1A1A, 0xFF000000, Shader.TileMode.CLAMP));
    }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int idx = (int) (e.getX() / (getWidth() / 4f));
        idx = Math.max(0, Math.min(3, idx));
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressed = idx;
                performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                invalidate();
                return true;
            case MotionEvent.ACTION_MOVE:
                if (idx != pressed && pressed != -1) { pressed = -1; invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
                int fired = pressed;
                pressed = -1;
                invalidate();
                if (fired == idx && fired >= 0 && listener != null) listener.onKey(fired);
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressed = -1;
                invalidate();
                return true;
        }
        return super.onTouchEvent(e);
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        c.drawRect(0, 0, w, h, bg);
        fill.setShader(null);
        fill.setColor(0xFF444444);
        c.drawRect(0, 0, w, 1, fill);

        float s = h * 0.28f;
        for (int i = 0; i < 4; i++) {
            float cx = (i + 0.5f) * w / 4f, cy = h / 2f;
            boolean on = i == pressed;
            int col = on ? 0xFFCFEFFF : 0xFF5E7C8C;
            if (on) {
                fill.setShader(new RadialGradient(cx, cy, h * 0.6f, 0x66A5DFFF, 0x00A5DFFF, Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, h * 0.6f, fill);
                fill.setShader(null);
            }
            stroke.setColor(col);
            fill.setColor(col);
            switch (i) {
                case 0: drawBack(c, cx, cy, s); break;
                case 1: drawHome(c, cx, cy, s); break;
                case 2: drawMenu(c, cx, cy, s); break;
                default: drawSearch(c, cx, cy, s); break;
            }
        }
    }

    private void drawBack(Canvas c, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx + s * 0.5f, cy + s * 0.45f);
        path.lineTo(cx + s * 0.5f, cy - s * 0.05f);
        path.quadTo(cx + s * 0.5f, cy - s * 0.3f, cx + s * 0.2f, cy - s * 0.3f);
        path.lineTo(cx - s * 0.3f, cy - s * 0.3f);
        c.drawPath(path, stroke);
        path.reset();
        path.moveTo(cx - s * 0.6f, cy - s * 0.3f);
        path.lineTo(cx - s * 0.15f, cy - s * 0.65f);
        path.lineTo(cx - s * 0.15f, cy + s * 0.05f);
        path.close();
        c.drawPath(path, fill);
    }

    private void drawHome(Canvas c, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx - s * 0.6f, cy);
        path.lineTo(cx, cy - s * 0.6f);
        path.lineTo(cx + s * 0.6f, cy);
        c.drawPath(path, stroke);
        path.reset();
        path.moveTo(cx - s * 0.4f, cy - s * 0.15f);
        path.lineTo(cx - s * 0.4f, cy + s * 0.55f);
        path.lineTo(cx + s * 0.4f, cy + s * 0.55f);
        path.lineTo(cx + s * 0.4f, cy - s * 0.15f);
        c.drawPath(path, stroke);
    }

    private void drawMenu(Canvas c, float cx, float cy, float s) {
        r.set(cx - s * 0.6f, cy - s * 0.5f, cx + s * 0.6f, cy + s * 0.5f);
        c.drawRoundRect(r, s * 0.15f, s * 0.15f, stroke);
        for (int k = -1; k <= 1; k++) {
            c.drawLine(cx - s * 0.35f, cy + k * s * 0.28f, cx + s * 0.35f, cy + k * s * 0.28f, stroke);
        }
    }

    private void drawSearch(Canvas c, float cx, float cy, float s) {
        c.drawCircle(cx - s * 0.12f, cy - s * 0.12f, s * 0.38f, stroke);
        c.drawLine(cx + s * 0.16f, cy + s * 0.16f, cx + s * 0.6f, cy + s * 0.6f, stroke);
    }
}
