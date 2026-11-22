package com.gingerbread.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.WallpaperManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LauncherActivity extends Activity
        implements TouchKeysView.KeyListener, SlidingTabView.Listener, PowerControlView.Listener {

    private static class AppInfo {
        String id;
        String label;
        Drawable icon;
        Intent intent;
    }

    private final List<AppInfo> allApps = new ArrayList<>();
    private final List<AppInfo> homeApps = new ArrayList<>();
    private IconAdapter homeAdapter, drawerAdapter;
    private FrameLayout middle;
    private View drawer, menuPanel, lockView;
    private SlidingTabView slider;
    private PowerControlView power;
    private SharedPreferences prefs;
    private boolean locked = true;
    private boolean appsDirty = true;

    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { lock(); }
    };
    private final BroadcastReceiver pkgChanged = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { appsDirty = true; }
    };

    // ------------------------------------------------------------------ lifecycle

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("gb", MODE_PRIVATE);

        if (Build.VERSION.SDK_INT >= 28) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        column.addView(new StatusBarView(this),
                new LinearLayout.LayoutParams(-1, Ui.dp(this, 25)));
        middle = new FrameLayout(this);
        column.addView(middle, new LinearLayout.LayoutParams(-1, 0, 1f));
        TouchKeysView keys = new TouchKeysView(this);
        keys.setListener(this);
        column.addView(keys, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));
        setContentView(column);

        buildHome();
        buildDrawer();
        buildMenu();
        buildLock();

        Ui.register(this, screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        IntentFilter pf = new IntentFilter();
        pf.addAction(Intent.ACTION_PACKAGE_ADDED);
        pf.addAction(Intent.ACTION_PACKAGE_REMOVED);
        pf.addAction(Intent.ACTION_PACKAGE_REPLACED);
        pf.addDataScheme("package");
        Ui.register(this, pkgChanged, pf);
    }

    @Override protected void onResume() {
        super.onResume();
        hideSystemUi();
        refreshPower();
        if (appsDirty) {
            loadApps();
            rebuildHome();
            appsDirty = false;
        }
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (!locked) closeOverlays();
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemUi();
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        try { unregisterReceiver(screenOff); } catch (IllegalArgumentException ignored) {}
        try { unregisterReceiver(pkgChanged); } catch (IllegalArgumentException ignored) {}
    }

    @SuppressWarnings("deprecation")
    private void hideSystemUi() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    // ------------------------------------------------------------------ keys

    @Override public void onKey(int index) {
        if (locked) return;
        switch (index) {
            case 0: closeOverlays(); break;     // Back
            case 1: closeOverlays(); break;     // Home
            case 2: toggleMenu(); break;        // Menu
        }
    }

    @Override public void onBackPressed() {
        if (!locked) closeOverlays();
    }

    @Override public boolean onKeyUp(int keyCode, KeyEvent e) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (!locked) toggleMenu();
            return true;
        }
        return super.onKeyUp(keyCode, e);
    }

    // ------------------------------------------------------------------ lock screen

    @Override public void onUnlock() {
        locked = false;
        lockView.setVisibility(View.GONE);
    }

    @Override public void onSoundToggle() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        boolean normal = am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
        try {
            am.setRingerMode(normal ? AudioManager.RINGER_MODE_VIBRATE : AudioManager.RINGER_MODE_NORMAL);
        } catch (SecurityException ignored) {}
        slider.setSoundOn(isSoundOn());
    }

    private boolean isSoundOn() {
        AudioManager am = (AudioManager) getSystemService(AUDIO_SERVICE);
        return am.getRingerMode() == AudioManager.RINGER_MODE_NORMAL;
    }

    private void lock() {
        locked = true;
        closeOverlays();
        lockView.setVisibility(View.VISIBLE);
        slider.setSoundOn(isSoundOn());
    }

    private void buildLock() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setBackgroundColor(0xB3000000);
        l.setClickable(true);
        int p = Ui.dp(this, 20);
        l.setPadding(p, p * 2, p, p);

        TextClock clock = new TextClock(this);
        clock.setFormat12Hour("h:mm");
        clock.setFormat24Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(72);
        clock.setShadowLayer(4, 0, 2, Color.BLACK);
        l.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat12Hour("EEEE, MMMM d");
        date.setFormat24Hour("EEEE, MMMM d");
        date.setTextColor(0xFFDDDDDD);
        date.setTextSize(18);
        l.addView(date);

        l.addView(new View(this), new LinearLayout.LayoutParams(-1, 0, 1f));

        slider = new SlidingTabView(this);
        slider.setListener(this);
        l.addView(slider, new LinearLayout.LayoutParams(-1, Ui.dp(this, 96)));

        middle.addView(l, new FrameLayout.LayoutParams(-1, -1));
        lockView = l;
    }

    // ------------------------------------------------------------------ home

    private void buildHome() {
        LinearLayout home = new LinearLayout(this);
        home.setOrientation(LinearLayout.VERTICAL);
        int p = Ui.dp(this, 8);

        // Power Control bar
        power = new PowerControlView(this);
        power.setListener(this);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(-1, Ui.dp(this, 56));
        plp.setMargins(p, p * 2, p, p);
        home.addView(power, plp);

        // favourites grid
        FrameLayout gridHolder = new FrameLayout(this);
        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setVerticalSpacing(Ui.dp(this, 10));
        grid.setPadding(p, p, p, 0);
        grid.setSelector(new ColorDrawable(Color.TRANSPARENT));
        homeAdapter = new IconAdapter(homeApps);
        grid.setAdapter(homeAdapter);
        grid.setOnItemClickListener((parent, v, pos, id) -> launch(homeApps.get(pos)));
        grid.setOnItemLongClickListener((parent, v, pos, id) -> {
            AppInfo a = homeApps.remove(pos);
            saveFav();
            homeAdapter.notifyDataSetChanged();
            toast("Removed " + a.label);
            return true;
        });
        TextView hint = new TextView(this);
        hint.setText("Long-press an app in the drawer\nto put it on your home screen.");
        hint.setTextColor(0x66FFFFFF);
        hint.setTextSize(12);
        hint.setGravity(Gravity.CENTER);
        gridHolder.addView(grid, new FrameLayout.LayoutParams(-1, -1));
        gridHolder.addView(hint, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        grid.setEmptyView(hint);
        home.addView(gridHolder, new LinearLayout.LayoutParams(-1, 0, 1f));

        // dock: phone, app drawer, browser
        LinearLayout dockRow = new LinearLayout(this);
        dockRow.setGravity(Gravity.CENTER);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.HORIZONTAL);
        box.setGravity(Gravity.CENTER);
        int bp = Ui.dp(this, 6);
        box.setPadding(bp, bp, bp, bp);
        GradientDrawable bgd = new GradientDrawable();
        bgd.setColor(0xDD1C1C1C);
        bgd.setCornerRadius(Ui.dp(this, 8));
        bgd.setStroke(Ui.dp(this, 1), 0xFF000000);
        box.setBackground(bgd);
        for (int i = 0; i < 3; i++) {
            final int type = i;
            DockIcon ic = new DockIcon(this, type);
            ic.setOnClickListener(v -> dockTap(type));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(Ui.dp(this, 58), Ui.dp(this, 58));
            lp.setMargins(Ui.dp(this, 4), 0, Ui.dp(this, 4), 0);
            box.addView(ic, lp);
        }
        dockRow.addView(box, new LinearLayout.LayoutParams(-2, -2));
        home.addView(dockRow, new LinearLayout.LayoutParams(-1, Ui.dp(this, 84)));

        middle.addView(home, new FrameLayout.LayoutParams(-1, -1));
    }

    private void dockTap(int type) {
        if (type == 0) {
            startSafe(new Intent(Intent.ACTION_DIAL));
        } else if (type == 1) {
            toggleDrawer();
        } else {
            try {
                startActivity(Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER));
            } catch (Exception e) {
                startSafe(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")));
            }
        }
    }

    private void saveFav() {
        StringBuilder sb = new StringBuilder();
        for (AppInfo a : homeApps) {
            if (sb.length() > 0) sb.append('|');
            sb.append(a.id);
        }
        prefs.edit().putString("fav", sb.toString()).apply();
    }

    private void rebuildHome() {
        homeApps.clear();
        String s = prefs.getString("fav", "");
        if (!s.isEmpty()) {
            for (String id : s.split("\\|")) {
                for (AppInfo a : allApps) {
                    if (a.id.equals(id)) { homeApps.add(a); break; }
                }
            }
        }
        homeAdapter.notifyDataSetChanged();
        drawerAdapter.notifyDataSetChanged();
    }

    // ------------------------------------------------------------------ power control

    private void refreshPower() {
        boolean[] st = new boolean[5];
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            st[0] = wm != null && wm.isWifiEnabled();
        } catch (Exception ignored) {}
        try {
            BluetoothManager bm = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            BluetoothAdapter ad = bm == null ? null : bm.getAdapter();
            st[1] = ad != null && ad.isEnabled();
        } catch (Exception ignored) {}
        try {
            LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
            st[2] = Build.VERSION.SDK_INT >= 28 ? lm.isLocationEnabled()
                    : lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ignored) {}
        try { st[3] = ContentResolver.getMasterSyncAutomatically(); } catch (Exception ignored) {}
        try {
            st[4] = Settings.System.getInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE)
                    == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
        } catch (Exception ignored) {}
        power.setStates(st);
    }

    /** Android 13 doesn't let apps flip these switches directly, so open the matching settings screen. */
    @Override public void onToggle(int i) {
        Intent in;
        switch (i) {
            case 0:
                in = new Intent(Build.VERSION.SDK_INT >= 29
                        ? Settings.Panel.ACTION_INTERNET_CONNECTIVITY : Settings.ACTION_WIFI_SETTINGS);
                break;
            case 1: in = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS); break;
            case 2: in = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS); break;
            case 3: in = new Intent(Settings.ACTION_SYNC_SETTINGS); break;
            default: in = new Intent(Settings.ACTION_DISPLAY_SETTINGS); break;
        }
        startSafe(in);
    }

    // ------------------------------------------------------------------ drawer (APPS | WIDGETS)

    private void buildDrawer() {
        LinearLayout d = new LinearLayout(this);
        d.setOrientation(LinearLayout.VERTICAL);
        d.setBackgroundColor(0xFF000000);
        d.setClickable(true);

        DrawerTabs tabs = new DrawerTabs(this);
        d.addView(tabs, new LinearLayout.LayoutParams(-1, Ui.dp(this, 48)));

        FrameLayout body = new FrameLayout(this);
        final GridView g = new GridView(this);
        g.setNumColumns(4);
        g.setVerticalSpacing(Ui.dp(this, 16));
        int p = Ui.dp(this, 8);
        g.setPadding(p, Ui.dp(this, 16), p, p);
        g.setSelector(new ColorDrawable(Color.TRANSPARENT));
        drawerAdapter = new IconAdapter(allApps);
        g.setAdapter(drawerAdapter);
        g.setOnItemClickListener((parent, v, pos, id) -> {
            AppInfo a = allApps.get(pos);
            closeOverlays();
            launch(a);
        });
        g.setOnItemLongClickListener((parent, v, pos, id) -> {
            AppInfo a = allApps.get(pos);
            for (AppInfo h : homeApps) {
                if (h.id.equals(a.id)) { toast("Already on Home"); return true; }
            }
            homeApps.add(a);
            saveFav();
            homeAdapter.notifyDataSetChanged();
            toast("Added " + a.label + " to Home");
            return true;
        });
        final TextView widgetsMsg = new TextView(this);
        widgetsMsg.setText("Widgets aren't supported\nin this launcher yet.");
        widgetsMsg.setTextColor(0xFF888888);
        widgetsMsg.setGravity(Gravity.CENTER);
        widgetsMsg.setVisibility(View.GONE);
        body.addView(g, new FrameLayout.LayoutParams(-1, -1));
        body.addView(widgetsMsg, new FrameLayout.LayoutParams(-1, -1, Gravity.CENTER));
        d.addView(body, new LinearLayout.LayoutParams(-1, 0, 1f));

        tabs.setListener(i -> {
            g.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
            widgetsMsg.setVisibility(i == 0 ? View.GONE : View.VISIBLE);
        });

        d.setVisibility(View.GONE);
        middle.addView(d, new FrameLayout.LayoutParams(-1, -1));
        drawer = d;
    }

    private void toggleDrawer() {
        if (locked) return;
        if (drawer.getVisibility() == View.VISIBLE) {
            drawer.setVisibility(View.GONE);
        } else {
            menuPanel.setVisibility(View.GONE);
            drawer.setVisibility(View.VISIBLE);
        }
    }

    private void closeOverlays() {
        drawer.setVisibility(View.GONE);
        menuPanel.setVisibility(View.GONE);
    }

    // ------------------------------------------------------------------ menu panel

    private void buildMenu() {
        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setBackgroundColor(0xF2181818);
        panel.setClickable(true);

        View line = new View(this);
        line.setBackgroundColor(0xFF666666);
        panel.addView(line, new LinearLayout.LayoutParams(-1, Ui.dp(this, 1)));

        String[] names = {"Search", "Wallpaper", "Lock", "Manage apps", "Settings", "About"};
        int[] icons = {
                android.R.drawable.ic_menu_search,
                android.R.drawable.ic_menu_gallery,
                android.R.drawable.ic_lock_lock,
                android.R.drawable.ic_menu_manage,
                android.R.drawable.ic_menu_preferences,
                android.R.drawable.ic_menu_info_details};
        for (int row = 0; row < 2; row++) {
            LinearLayout rl = new LinearLayout(this);
            rl.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                rl.addView(menuTile(names[idx], icons[idx], idx),
                        new LinearLayout.LayoutParams(0, Ui.dp(this, 80), 1f));
            }
            panel.addView(rl, new LinearLayout.LayoutParams(-1, -2));
        }
        panel.setVisibility(View.GONE);
        middle.addView(panel, new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM));
        menuPanel = panel;
    }

    private View menuTile(String name, int iconRes, final int idx) {
        LinearLayout t = new LinearLayout(this);
        t.setOrientation(LinearLayout.VERTICAL);
        t.setGravity(Gravity.CENTER);

        ImageView iv = new ImageView(this);
        Drawable d = getResources().getDrawable(iconRes, getTheme()).mutate();
        d.setColorFilter(0xFFDDDDDD, PorterDuff.Mode.SRC_IN);
        iv.setImageDrawable(d);
        t.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 32), Ui.dp(this, 32)));

        TextView tv = new TextView(this);
        tv.setText(name);
        tv.setTextColor(0xFFDDDDDD);
        tv.setTextSize(12);
        tv.setGravity(Gravity.CENTER);
        t.addView(tv, new LinearLayout.LayoutParams(-2, -2));

        StateListDrawable sl = new StateListDrawable();
        sl.addState(new int[]{android.R.attr.state_pressed}, new ColorDrawable(0x4499CC00));
        sl.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
        t.setBackground(sl);
        t.setOnClickListener(v -> onMenuItem(idx));
        return t;
    }

    private void toggleMenu() {
        if (menuPanel.getVisibility() == View.VISIBLE) {
            menuPanel.setVisibility(View.GONE);
        } else {
            drawer.setVisibility(View.GONE);
            menuPanel.setVisibility(View.VISIBLE);
        }
    }

    private void onMenuItem(int idx) {
        closeOverlays();
        switch (idx) {
            case 0: doSearch(); break;
            case 1: showWallpaperChoices(); break;
            case 2: lock(); break;
            case 3: startSafe(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)); break;
            case 4: startSafe(new Intent(Settings.ACTION_SETTINGS)); break;
            case 5: showAbout(); break;
        }
    }

    private void showWallpaperChoices() {
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("Wallpaper")
                .setItems(new String[]{"Gingerbread Nexus style", "Choose another..."}, (dlg, which) -> {
                    if (which == 0) {
                        applyNexusWallpaper();
                    } else {
                        startSafe(Intent.createChooser(new Intent(Intent.ACTION_SET_WALLPAPER), "Select wallpaper"));
                    }
                })
                .show();
    }

    private void applyNexusWallpaper() {
        final DisplayMetrics dm = getResources().getDisplayMetrics();
        new Thread(() -> {
            boolean ok = true;
            try {
                WallpaperManager.getInstance(getApplicationContext())
                        .setBitmap(WallpaperArt.create(dm.widthPixels, dm.heightPixels));
            } catch (Exception e) {
                ok = false;
            }
            final boolean fin = ok;
            runOnUiThread(() -> toast(fin ? "Wallpaper applied" : "Couldn't set wallpaper"));
        }).start();
    }

    private void showAbout() {
        TextView v = new TextView(this);
        v.setText("Android version\n2.3.6");
        v.setTextSize(20);
        v.setPadding(Ui.dp(this, 24), Ui.dp(this, 16), Ui.dp(this, 24), Ui.dp(this, 16));
        v.setOnClickListener(x -> startActivity(new Intent(this, EggActivity.class)));
        new AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("About phone")
                .setView(v)
                .setPositiveButton("OK", null)
                .show();
    }

    // ------------------------------------------------------------------ helpers

    private void loadApps() {
        PackageManager pm = getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        allApps.clear();
        for (ResolveInfo ri : list) {
            ActivityInfo ai = ri.activityInfo;
            if (ai.packageName.equals(getPackageName())) continue;
            AppInfo a = new AppInfo();
            a.label = String.valueOf(ri.loadLabel(pm));
            a.icon = ri.loadIcon(pm);
            ComponentName cn = new ComponentName(ai.packageName, ai.name);
            a.id = cn.flattenToString();
            a.intent = new Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_LAUNCHER)
                    .setComponent(cn)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            allApps.add(a);
        }
        Collections.sort(allApps, (x, y) -> x.label.compareToIgnoreCase(y.label));
    }

    private void launch(AppInfo a) {
        try {
            startActivity(a.intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            toast("Can't open " + a.label);
        }
    }

    private void startSafe(Intent i) {
        try {
            startActivity(i);
        } catch (Exception e) {
            toast("Not available on this phone");
        }
    }

    private void doSearch() {
        try {
            startActivity(new Intent(Intent.ACTION_WEB_SEARCH));
        } catch (Exception e) {
            startSafe(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")));
        }
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ views

    private class IconAdapter extends BaseAdapter {
        private final List<AppInfo> data;

        IconAdapter(List<AppInfo> data) { this.data = data; }

        @Override public int getCount() { return data.size(); }
        @Override public Object getItem(int i) { return data.get(i); }
        @Override public long getItemId(int i) { return i; }

        @Override public View getView(int i, View cv, ViewGroup parent) {
            LinearLayout row;
            if (cv == null) {
                Context c = LauncherActivity.this;
                row = new LinearLayout(c);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                row.addView(new ImageView(c),
                        new LinearLayout.LayoutParams(Ui.dp(c, 52), Ui.dp(c, 52)));
                TextView tv = new TextView(c);
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(12);
                tv.setSingleLine();
                tv.setEllipsize(TextUtils.TruncateAt.END);
                tv.setGravity(Gravity.CENTER);
                tv.setShadowLayer(3, 0, 1, Color.BLACK);
                tv.setPadding(0, Ui.dp(c, 4), 0, 0);
                row.addView(tv, new LinearLayout.LayoutParams(-1, -2));
            } else {
                row = (LinearLayout) cv;
            }
            AppInfo a = data.get(i);
            ((ImageView) row.getChildAt(0)).setImageDrawable(a.icon);
            ((TextView) row.getChildAt(1)).setText(a.label);
            return row;
        }
    }

    /** "APPS | WIDGETS" header with the blue underline. */
    private static class DrawerTabs extends View {
        interface Listener { void onTab(int index); }

        private Listener listener;
        private int sel = 0;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);

        DrawerTabs(Context c) { super(c); }

        void setListener(Listener l) { listener = l; }

        @Override public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) return true;
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float x = e.getX();
                int hit = -1;
                if (x >= Ui.dp(getContext(), 8) && x < Ui.dp(getContext(), 96)) hit = 0;
                else if (x >= Ui.dp(getContext(), 96) && x < Ui.dp(getContext(), 204)) hit = 1;
                if (hit >= 0 && hit != sel) {
                    sel = hit;
                    invalidate();
                    if (listener != null) listener.onTab(sel);
                }
                return true;
            }
            return super.onTouchEvent(e);
        }

        @Override protected void onDraw(Canvas c) {
            Context ctx = getContext();
            float w = getWidth(), h = getHeight();
            float[] x0 = {Ui.dp(ctx, 8), Ui.dp(ctx, 96)};
            float[] x1 = {Ui.dp(ctx, 96), Ui.dp(ctx, 204)};
            String[] names = {"APPS", "WIDGETS"};

            p.setStyle(Paint.Style.FILL);
            p.setColor(0xFF333333);
            c.drawRect(0, h - 1, w, h, p);

            p.setTextSize(Ui.dp(ctx, 14));
            p.setFakeBoldText(true);
            p.setTextAlign(Paint.Align.CENTER);
            for (int i = 0; i < 2; i++) {
                p.setColor(i == sel ? Color.WHITE : 0xFF888888);
                c.drawText(names[i], (x0[i] + x1[i]) / 2f, h / 2f - (p.ascent() + p.descent()) / 2f, p);
                if (i == sel) {
                    p.setColor(0xFF33B5E5);
                    c.drawRect(x0[i], h - Ui.dp(ctx, 3), x1[i], h, p);
                }
            }
            p.setTextAlign(Paint.Align.LEFT);
            p.setFakeBoldText(false);
        }
    }

    /** Dock icons: phone, app drawer, browser. */
    private static class DockIcon extends View {
        private final int type;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final RectF r = new RectF();
        private final Drawable phone;

        DockIcon(Context c, int type) {
            super(c);
            this.type = type;
            setClickable(true);
            phone = c.getResources().getDrawable(android.R.drawable.ic_menu_call, c.getTheme()).mutate();
            phone.setColorFilter(Ui.GREEN, PorterDuff.Mode.SRC_IN);
        }

        @Override protected void drawableStateChanged() {
            super.drawableStateChanged();
            invalidate();
        }

        @Override protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f;
            Context ctx = getContext();
            if (isPressed()) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0x44FFFFFF);
                r.set(0, 0, w, h);
                c.drawRoundRect(r, Ui.dp(ctx, 6), Ui.dp(ctx, 6), p);
            }
            if (type == 0) {
                int s = (int) (w * 0.62f);
                phone.setBounds((int) cx - s / 2, (int) cy - s / 2, (int) cx + s / 2, (int) cy + s / 2);
                phone.draw(c);
            } else if (type == 1) {
                p.setStyle(Paint.Style.FILL);
                p.setColor(0xFFDDDDDD);
                float step = w * 0.17f;
                for (int i = -1; i <= 1; i++) {
                    for (int j = -1; j <= 1; j++) {
                        c.drawCircle(cx + i * step, cy + j * step, w * 0.05f, p);
                    }
                }
            } else {
                float rad = w * 0.3f;
                p.setStyle(Paint.Style.FILL);
                p.setShader(new RadialGradient(cx - rad * 0.3f, cy - rad * 0.3f, rad * 1.5f,
                        0xFF9AD6F7, 0xFF1D5FA8, Shader.TileMode.CLAMP));
                c.drawCircle(cx, cy, rad, p);
                p.setShader(null);
                p.setColor(0xFF7CB342);
                c.drawCircle(cx - rad * 0.3f, cy - rad * 0.25f, rad * 0.3f, p);
                c.drawCircle(cx + rad * 0.35f, cy + rad * 0.3f, rad * 0.25f, p);
                p.setStyle(Paint.Style.STROKE);
                p.setStrokeWidth(Ui.dp(ctx, 1));
                p.setColor(0x99FFFFFF);
                c.drawOval(cx - rad * 0.45f, cy - rad, cx + rad * 0.45f, cy + rad, p);
                c.drawLine(cx - rad, cy, cx + rad, cy, p);
            }
        }
    }
}
