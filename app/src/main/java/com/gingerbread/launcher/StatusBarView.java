package com.gingerbread.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.BatteryManager;
import android.view.View;
import java.util.Date;

/** Android 4-style status bar with Gingerbread green icons. */
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
        p.setColor(Color.BLACK);
        c.drawRect(0, 0, w, h, p);

        // clock
        String t = android.text.format.DateFormat.getTimeFormat(ctx).format(new Date());
        p.setColor(Color.WHITE);
        p.setTextSize(h * 0.56f);
        float tw = p.measureText(t);
        float base = h / 2f - (p.ascent() + p.descent()) / 2f;
        c.drawText(t, w - m - tw, base, p);
        float x = w - m - tw - m * 1.6f;

        // battery (vertical, green)
        float bw = Ui.dp(ctx, 9), bh = Ui.dp(ctx, 15);
        x -= bw;
        float top = h / 2f - bh / 2f + Ui.dp(ctx, 1);
        p.setStyle(Paint.Style.FILL);
        p.setColor(0xFFBBBBBB);
        c.drawRect(x + bw * 0.3f, top - Ui.dp(ctx, 1.5f), x + bw * 0.7f, top, p);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Ui.dp(ctx, 1));
        r.set(x, top, x + bw, top + bh);
        c.drawRect(r, p);
        p.setStyle(Paint.Style.FILL);
        p.setColor(battery <= 15 ? 0xFFFF4444 : Ui.GREEN);
        float in = Ui.dp(ctx, 1.5f);
        float fillH = (bh - 2 * in) * battery / 100f;
        c.drawRect(x + in, top + bh - in - fillH, x + bw - in, top + bh - in, p);
        x -= m * 1.6f;

        // signal bars (decorative, green)
        float barW = Ui.dp(ctx, 2.5f), gap = Ui.dp(ctx, 1.2f);
        float total = 5 * barW + 4 * gap;
        x -= total;
        float bottom = h / 2f + h * 0.26f;
        p.setColor(Ui.GREEN);
        for (int i = 0; i < 5; i++) {
            float bhi = h * 0.18f + i * h * 0.1f;
            float left = x + i * (barW + gap);
            c.drawRect(left, bottom - bhi, left + barW, bottom, p);
        }
        x -= m * 1.8f;

        // wifi (decorative, green)
        float s = h * 0.27f;
        p.setColor(Ui.GREEN);
        p.setStrokeWidth(Ui.dp(ctx, 1.6f));
        Glyphs.wifi(c, p, x - s, h / 2f, s);
    }
}
