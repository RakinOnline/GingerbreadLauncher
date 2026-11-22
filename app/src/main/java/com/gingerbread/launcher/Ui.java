package com.gingerbread.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Build;

final class Ui {
    static final int GREEN = 0xFF99CC00;

    private Ui() {}

    static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    static void register(Context c, BroadcastReceiver r, IntentFilter f) {
        if (Build.VERSION.SDK_INT >= 33) {
            c.registerReceiver(r, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            c.registerReceiver(r, f);
        }
    }
}
