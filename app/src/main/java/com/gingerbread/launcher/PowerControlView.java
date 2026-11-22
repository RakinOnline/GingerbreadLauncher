package com.gingerbread.launcher;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.view.MotionEvent;
import android.view.View;

/** Gingerbread "Power Control" bar: Wi-Fi, Bluetooth, GPS, Sync, Brightness with green indicator bars. */
public class PowerControlView extends View {
    public interface Listener { void onToggle(int index); }

    private Listener listener;
    private boolean[] on = new boolean[5];
    private int pressed = -1;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();

    public PowerControlView(Context c) { super(c); }

    public void setListener(Listener l) { listener = l; }

    public void setStates(boolean[] s) { on = s.clone(); invalidate(); }

    @Override public boolean onTouchEvent(MotionEvent e) {
        int idx = (int) (e.getX() / (getWidth() / 5f));
        idx = Math.max(0, Math.min(4, idx));
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN: pressed = idx; invalidate(); return true;
            case MotionEvent.ACTION_MOVE:
                if (idx != pressed && pressed != -1) { pressed = -1; invalidate(); }
                return true;
            case MotionEvent.ACTION_UP:
                int fired = pressed;
                pressed = -1;
                invalidate();
                if (fired == idx && fired >= 0 && listener != null) listener.onToggle(fired);
                return true;
            case MotionEvent.ACTION_CANCEL: pressed = -1; invalidate(); return true;
        }
        return super.onTouchEvent(e);
    }

    @Override protected void onDraw(Canvas c) {
        Context ctx = getContext();
        float w = getWidth(), h = getHeight(), cw = w / 5f;

        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF4A4A4A, 0xFF1F1F1F, Shader.TileMode.CLAMP));
        r.set(0, 0, w, h);
        c.drawRoundRect(r, Ui.dp(ctx, 6), Ui.dp(ctx, 6), p);
        p.setShader(null);

        for (int i = 0; i < 5; i++) {
            float cx = (i + 0.5f) * cw, cy = h * 0.43f, s = h * 0.2f;

            if (i == pressed) {
                p.setColor(0x33FFFFFF);
                p.setStyle(Paint.Style.FILL);
                r.set(i * cw + Ui.dp(ctx, 2), Ui.dp(ctx, 2), (i + 1) * cw - Ui.dp(ctx, 2), h - Ui.dp(ctx, 2));
                c.drawRoundRect(r, Ui.dp(ctx, 4), Ui.dp(ctx, 4), p);
            }

            int col = on[i] ? 0xFFF2F2F2 : 0xFF7A7A7A;
            p.setColor(col);
            p.setStrokeWidth(Ui.dp(ctx, 2));
            switch (i) {
                case 0: Glyphs.wifi(c, p, cx, cy, s); break;
                case 1: Glyphs.bluetooth(c, p, cx, cy, s); break;
                case 2: Glyphs.gps(c, p, cx, cy, s); break;
                case 3: Glyphs.sync(c, p, cx, cy, s); break;
                default: Glyphs.sun(c, p, cx, cy, s); break;
            }

            p.setStyle(Paint.Style.FILL);
            p.setColor(on[i] ? Ui.GREEN : 0xFF555555);
            c.drawRect(i * cw + Ui.dp(ctx, 4), h - Ui.dp(ctx, 9), (i + 1) * cw - Ui.dp(ctx, 4), h - Ui.dp(ctx, 5), p);

            if (i > 0) {
                p.setColor(0xFF121212);
                c.drawRect(i * cw - 1, Ui.dp(ctx, 4), i * cw, h - Ui.dp(ctx, 4), p);
            }
        }
    }
}
