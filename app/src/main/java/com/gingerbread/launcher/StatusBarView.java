package com.gingerbread.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.os.BatteryManager;
import android.view.View;
import java.util.Date;

/** Gingerbread-style status bar: dark gradient, green signal + battery, clock on the right. */
public class StatusBarView extends View {
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private int battery = 100;

    private final BroadcastReceiver rx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_BATTERY_CHANGED.equals(i.getAction())) {
                int l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                int s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                battery = s > 0 ? l * 100 / s : l;
            }
            invalidate();
        }
    };

    public StatusBarView(Context c) { super(c); }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter f = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        Ui.register(getContext(), rx, f);
    }

    @Override protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try { getContext().unregisterReceiver(rx); } catch (IllegalArgumentException ignored) {}
    }

    @Override protected void onDraw(Canvas c) {
        Context ctx = getContext();
        float w = getWidth(), h = getHeight(), m = Ui.dp(ctx, 6);

        p.setStyle(Paint.Style.FILL);
        p.setShader(new LinearGradient(0, 0, 0, h, 0xFF3C3C3C, 0xFF000000, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, h, p);
        p.setShader(null);

        // clock
        String t = android.text.format.DateFormat.getTimeFormat(ctx).format(new Date());
        p.setColor(Color.WHITE);
        p.setTextSize(h * 0.56f);
        float tw = p.measureText(t);
        float base = h / 2f - (p.ascent() + p.descent()) / 2f;
        c.drawText(t, w - m - tw, base, p);
        float x = w - m - tw - m * 1.5f;

        // battery (green fill)
        float bw = Ui.dp(ctx, 22), bh = h * 0.42f;
        x -= bw;
        r.set(x, h / 2f - bh / 2f, x + bw, h / 2f + bh / 2f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(ctx, 1));
        p.setColor(0xFFBBBBBB);
        c.drawRoundRect(r, 2, 2, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(battery <= 15 ? 0xFFFF4444 : Ui.GREEN);
        float in = Ui.dp(ctx, 2);
        c.drawRect(x + in, r.top + in, x + in + (bw - 2 * in) * battery / 100f, r.bottom - in, p);
        p.setColor(0xFFBBBBBB);
        c.drawRect(x + bw, h / 2f - bh * 0.2f, x + bw + Ui.dp(ctx, 2), h / 2f + bh * 0.2f, p);
        x -= m * 1.5f;

        // signal bars (decorative)
        float barW = Ui.dp(ctx, 3), gap = Ui.dp(ctx, 1.5f);
        float total = 5 * barW + 4 * gap;
        x -= total;
        float bottom = h / 2f + h * 0.25f;
        for (int i = 0; i < 5; i++) {
            float bhi = h * 0.2f + i * h * 0.1f;
            float left = x + i * (barW + gap);
            p.setColor(i < 4 ? Ui.GREEN : 0xFF666666);
            c.drawRect(left, bottom - bhi, left + barW, bottom, p);
        }
    }
}
