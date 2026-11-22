package com.gingerbread.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;

/**
 * Gingerbread SlidingTab lock slider. Drag the green lock handle right to unlock;
 * drag the speaker handle left to toggle sound / vibrate.
 */
public class SlidingTabView extends View {
    public interface Listener { void onUnlock(); void onSoundToggle(); }

    private Listener listener;
    private boolean soundOn = true;
    private int active = -1;      // 0 = lock handle, 1 = sound handle
    private float frac = 0f;      // 0..1 drag progress of the active handle
    private float pad, hw, hh, travel;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public SlidingTabView(Context c) { super(c); }

    public void setListener(Listener l) { listener = l; }

    public void setSoundOn(boolean on) { soundOn = on; invalidate(); }

    @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
        pad = Ui.dp(getContext(), 10);
        hw = Ui.dp(getContext(), 76);
        hh = Math.min(h - 2 * pad, Ui.dp(getContext(), 64));
        travel = Math.max(1f, w - 2 * pad - hw);
    }

    private float top() { return (getHeight() - hh) / 2f; }
    private float leftX() { return pad + (active == 0 ? frac * travel : 0f); }
    private float rightX() { return pad + travel - (active == 1 ? frac * travel : 0f); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: {
                float x = e.getX(), y = e.getY(), t = top();
                if (y >= t && y <= t + hh) {
                    if (x >= leftX() && x <= leftX() + hw) active = 0;
                    else if (x >= rightX() && x <= rightX() + hw) active = 1;
                }
                if (active >= 0) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                    performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    invalidate();
                    return true;
                }
                return false;
            }
            case MotionEvent.ACTION_MOVE: {
                if (active < 0) return false;
                float hx = Math.max(pad, Math.min(pad + travel, e.getX() - hw / 2f));
                frac = active == 0 ? (hx - pad) / travel : (pad + travel - hx) / travel;
                invalidate();
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                if (active < 0) return false;
                boolean done = frac > 0.85f && e.getAction() == MotionEvent.ACTION_UP;
                int a = active;
                active = -1;
                frac = 0f;
                invalidate();
                if (done && listener != null) {
                    if (a == 0) listener.onUnlock(); else listener.onSoundToggle();
                }
                return true;
            }
        }
        return false;
    }

    @Override protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), t = top();
        Context ctx = getContext();

        // track
        float trackH = hh * 0.62f;
        r.set(pad, h / 2f - trackH / 2f, w - pad, h / 2f + trackH / 2f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF1E1E1E);
        c.drawRoundRect(r, trackH / 2f, trackH / 2f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(ctx, 1));
        p.setColor(0xFF444444);
        c.drawRoundRect(r, trackH / 2f, trackH / 2f, p);

        // hint
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFF888888);
        p.setTextSize(Ui.dp(ctx, 14));
        p.setTextAlign(Paint.Align.CENTER);
        c.drawText(active == 1 ? (soundOn ? "Vibrate" : "Sound on") : "Slide to unlock",
                w / 2f, h / 2f - (p.ascent() + p.descent()) / 2f, p);
        p.setTextAlign(Paint.Align.LEFT);

        // lock handle (green)
        float lx = leftX();
        r.set(lx, t, lx + hw, t + hh);
        p.setColor(active == 0 ? 0xFFB5E000 : Ui.GREEN);
        c.drawRoundRect(r, hh / 2f, hh / 2f, p);
        drawLock(c, lx + hw / 2f, t + hh / 2f, hh * 0.22f);

        // sound handle (gray)
        float rx = rightX();
        r.set(rx, t, rx + hw, t + hh);
        p.setColor(active == 1 ? 0xFF777777 : 0xFF555555);
        c.drawRoundRect(r, hh / 2f, hh / 2f, p);
        drawSpeaker(c, rx + hw / 2f, t + hh / 2f, hh * 0.2f);
    }

    private void drawLock(Canvas c, float cx, float cy, float s) {
        p.setColor(0xFF1B2600);
        p.setStyle(Paint.Style.FILL);
        r.set(cx - s, cy - s * 0.2f, cx + s, cy + s);
        c.drawRoundRect(r, s * 0.2f, s * 0.2f, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.3f);
        r.set(cx - s * 0.6f, cy - s * 1.1f, cx + s * 0.6f, cy + s * 0.3f);
        c.drawArc(r, 180, 180, false, p);
        c.drawLine(cx - s * 0.6f, cy - s * 0.4f, cx - s * 0.6f, cy - s * 0.2f, p);
        c.drawLine(cx + s * 0.6f, cy - s * 0.4f, cx + s * 0.6f, cy - s * 0.2f, p);
        p.setStyle(Paint.Style.FILL);
    }

    private void drawSpeaker(Canvas c, float cx, float cy, float s) {
        p.setColor(0xFFEEEEEE);
        p.setStyle(Paint.Style.FILL);
        r.set(cx - s, cy - s * 0.35f, cx - s * 0.4f, cy + s * 0.35f);
        c.drawRect(r, p);
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(cx - s * 0.4f, cy - s * 0.35f);
        path.lineTo(cx + s * 0.2f, cy - s * 0.9f);
        path.lineTo(cx + s * 0.2f, cy + s * 0.9f);
        path.lineTo(cx - s * 0.4f, cy + s * 0.35f);
        path.close();
        c.drawPath(path, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(s * 0.18f);
        p.setStrokeCap(Paint.Cap.ROUND);
        if (soundOn) {
            r.set(cx - s * 0.1f, cy - s * 0.5f, cx + s * 0.6f, cy + s * 0.5f);
            c.drawArc(r, -50, 100, false, p);
            r.set(cx - s * 0.3f, cy - s * 0.9f, cx + s * 1.0f, cy + s * 0.9f);
            c.drawArc(r, -50, 100, false, p);
        } else {
            p.setColor(0xFFFF6655);
            c.drawLine(cx - s, cy - s, cx + s * 0.9f, cy + s, p);
        }
        p.setStyle(Paint.Style.FILL);
        p.setStrokeCap(Paint.Cap.BUTT);
    }
}
