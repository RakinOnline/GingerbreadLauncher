package com.gingerbread.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/** Bottom key bar: Back, Home, Menu (drawn with the recents icon). */
public class TouchKeysView extends View {
    public interface KeyListener { void onKey(int index); }

    private KeyListener listener;
    private int pressed = -1;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint stroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF r = new RectF();

    public TouchKeysView(Context c) {
        super(c);
        fill.setStyle(Paint.Style.FILL);
        stroke.setStyle(Paint.Style.STROKE);
        stroke.setStrokeCap(Paint.Cap.ROUND);
        stroke.setStrokeJoin(Paint.Join.ROUND);
        stroke.setStrokeWidth(Ui.dp(c, 2.2f));
    }

    public void setListener(KeyListener l) { listener = l; }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int idx = (int) (e.getX() / (getWidth() / 3f));
        idx = Math.max(0, Math.min(2, idx));
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
        c.drawColor(0xFF000000);
        float s = h * 0.3f;
        for (int i = 0; i < 3; i++) {
            float cx = (i + 0.5f) * w / 3f, cy = h / 2f;
            boolean on = i == pressed;
            if (on) {
                fill.setColor(0x33FFFFFF);
                c.drawCircle(cx, cy, h * 0.5f, fill);
            }
            int col = on ? 0xFFFFFFFF : 0xFFB4B4B4;
            stroke.setColor(col);
            fill.setColor(col);
            if (i == 0) drawBack(c, cx, cy, s);
            else if (i == 1) drawHome(c, cx, cy, s);
            else drawRecents(c, cx, cy, s);
        }
    }

    private void drawBack(Canvas c, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx + s * 0.55f, cy + s * 0.5f);
        path.lineTo(cx + s * 0.55f, cy - s * 0.05f);
        path.quadTo(cx + s * 0.55f, cy - s * 0.35f, cx + s * 0.2f, cy - s * 0.35f);
        path.lineTo(cx - s * 0.3f, cy - s * 0.35f);
        c.drawPath(path, stroke);
        path.reset();
        path.moveTo(cx - s * 0.65f, cy - s * 0.35f);
        path.lineTo(cx - s * 0.15f, cy - s * 0.75f);
        path.lineTo(cx - s * 0.15f, cy + s * 0.05f);
        path.close();
        c.drawPath(path, fill);
    }

    private void drawHome(Canvas c, float cx, float cy, float s) {
        path.reset();
        path.moveTo(cx - s * 0.7f, cy);
        path.lineTo(cx, cy - s * 0.65f);
        path.lineTo(cx + s * 0.7f, cy);
        c.drawPath(path, stroke);
        path.reset();
        path.moveTo(cx - s * 0.5f, cy - s * 0.15f);
        path.lineTo(cx - s * 0.5f, cy + s * 0.6f);
        path.lineTo(cx + s * 0.5f, cy + s * 0.6f);
        path.lineTo(cx + s * 0.5f, cy - s * 0.15f);
        c.drawPath(path, stroke);
    }

    private void drawRecents(Canvas c, float cx, float cy, float s) {
        r.set(cx - s * 0.45f, cy - s * 0.6f, cx + s * 0.65f, cy + s * 0.2f);
        c.drawRoundRect(r, s * 0.1f, s * 0.1f, stroke);
        r.set(cx - s * 0.65f, cy - s * 0.2f, cx + s * 0.45f, cy + s * 0.6f);
        c.drawRoundRect(r, s * 0.1f, s * 0.1f, stroke);
    }
}
