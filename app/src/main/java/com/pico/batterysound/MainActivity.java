package com.pico.batterysound;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.provider.OpenableColumns;
import android.view.*;
import android.widget.*;

public final class MainActivity extends Activity {
    private static final int INK = Color.rgb(33, 48, 39), GREEN = Color.rgb(68, 112, 80), MUTED = Color.rgb(105, 119, 109);
    private final SoundPlayer player = new SoundPlayer();
    private LinearLayout content;
    private TextView battery, batteryState, backgroundState;
    private Button monitor;
    private String pickingKey;
    private boolean registered;
    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1), scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            battery.setText(level >= 0 && scale > 0 ? Math.round(level * 100f / scale) + "%" : "—");
            boolean plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
            batteryState.setText(plugged ? "已连接电源" : "正在使用电池");
        }
    };
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if (state != null) pickingKey = state.getString("pickingKey");
        render();
    }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("pickingKey", pickingKey); }
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    private GradientDrawable background(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color); drawable.setCornerRadius(dp(radius)); return drawable;
    }
    private TextView label(String text, int size, int color, boolean bold) {
        TextView view = new TextView(this); view.setText(text); view.setTextSize(size); view.setTextColor(color);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(4), 0, dp(4)); return view;
    }
    private Button button(String title, Runnable action) {
        Button view = new Button(this); view.setText(title); view.setAllCaps(false); view.setTextColor(GREEN); view.setTextSize(14);
        view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.rgb(230, 238, 227)));
        view.setElevation(0);
        view.setOnClickListener(v -> action.run()); return view;
    }
    private LinearLayout card() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20), dp(16), dp(20), dp(16));
        box.setBackground(background(Color.WHITE, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2); params.setMargins(0, 0, 0, dp(14));
        content.addView(box, params); return box;
    }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(243, 246, 240));
        content = new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(22), dp(16), dp(22), dp(24));
        scroll.addView(content); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((view, insets) -> {
            if (Build.VERSION.SDK_INT >= 30) {
                android.graphics.Insets bars = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            } else view.setPadding(insets.getSystemWindowInsetLeft(), insets.getSystemWindowInsetTop(), insets.getSystemWindowInsetRight(), insets.getSystemWindowInsetBottom());
            return insets;
        });
        scroll.requestApplyInsets();
        content.addView(label("PICO / BATTERY SOUND", 12, GREEN, true));
        content.addView(label("电量有声，心中有数。", 27, INK, true));
        TextView subtitle = label("三个时刻，一声提醒。", 14, MUTED, false); subtitle.setPadding(0, dp(4), 0, dp(22)); content.addView(subtitle);
        LinearLayout status = card();
        status.addView(label("当前电量", 13, MUTED, false));
        battery = label("—", 48, GREEN, true); status.addView(battery);
        batteryState = label("读取电量中", 14, MUTED, false); status.addView(batteryState);
        monitor = button("开启电量提醒", this::toggleMonitoring);
        monitor.setBackgroundTintList(android.content.res.ColorStateList.valueOf(GREEN)); monitor.setTextColor(Color.WHITE);
        status.addView(monitor); updateMonitor();
        LinearLayout volumeBox = card();
        // Snapshot both sleep defaults before the user changes either awake slider.
        Prefs.volume(this, "charging", true); Prefs.volume(this, "low", true);
        volumeBox.addView(label("亮屏音量", 19, INK, true));
        addVolumeControl(volumeBox, "charging", "充电提示音", false);
        addVolumeControl(volumeBox, "low", "低电量提示音（含超低电量）", false);
        volumeBox.addView(label("0% 静音，100% 为原始音量，最高 150% 增强；实际响度仍受系统通知音量和勿扰模式影响。", 12, MUTED, false));
        LinearLayout sleepVolumeBox = card();
        sleepVolumeBox.addView(label("息屏 / 休眠音量", 19, INK, true));
        addVolumeControl(sleepVolumeBox, "charging", "充电提示音", true);
        addVolumeControl(sleepVolumeBox, "low", "低电量提示音（含超低电量）", true);
        sleepVolumeBox.addView(label("播放提醒时自动按内置屏幕和休眠状态选择音量。这里的试听使用息屏音量，无需关闭屏幕。", 12, MUTED, false));
        addAlert("charging", "充电提示", "插上充电器时，播放一次", 0);
        addAlert("low", "低电量提醒", "电量降至 " + Prefs.low(this) + "% 时提醒", Prefs.low(this));
        addAlert("critical", "超低电量提醒", "电量降至 " + Prefs.critical(this) + "% 时提醒", Prefs.critical(this));
        content.addView(label("开启后通过常驻通知在后台监听。提示音跟随通知音量与勿扰设置，最长播放 10 秒。", 12, MUTED, false));
        content.addView(label("接电时电量下降也会提醒。每档响一次，回升至阈值以上 2 个百分点后可再次提醒。插线只提示充电，拔线重新检查电量；收到系统状态更新后立即处理。", 12, MUTED, false));
        LinearLayout background = card();
        background.addView(label("自启与后台运行", 19, INK, true));
        Switch autoStart = new Switch(this); autoStart.setText("开机恢复提醒"); autoStart.setTextSize(16);
        autoStart.setChecked(Prefs.get(this).getBoolean("auto_start", true));
        autoStart.setOnCheckedChangeListener((v, checked) -> Prefs.get(this).edit().putBoolean("auto_start", checked).apply());
        background.addView(autoStart);
        background.addView(label("仅恢复上次已开启的提醒，开机首次解锁后生效。手动关闭提醒后不会自动开启。", 12, MUTED, false));
        backgroundState = label("", 13, GREEN, false); background.addView(backgroundState); updateBackgroundState();
        background.addView(button("允许不受电池优化限制", this::requestBackgroundAllowance));
        background.addView(button("应用设置 / 自启动权限", this::openStartupSettings));
        background.addView(label("vivo / iQOO：请在系统设置中允许本应用自启动，并将后台耗电管理设为允许后台高耗电；如需，可在最近任务中锁定应用。不同系统版本的入口名称可能不同。", 12, MUTED, false));
        background.addView(label("划走界面不会主动关闭监听。系统回收后会请求恢复，但系统强制停止、厂商清理或省电限制仍可能中断提醒；强制停止后需重新打开应用。", 12, MUTED, false));
        Intent current = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (current != null) batteryReceiver.onReceive(this, current);
    }
    private void addVolumeControl(LinearLayout box, String key, String title, boolean asleep) {
        TextView value = label(title + " · " + Prefs.volume(this, key, asleep) + "%", 14, GREEN, false);
        box.addView(value);
        SeekBar slider = new SeekBar(this);
        slider.setMax(150); slider.setProgress(Prefs.volume(this, key, asleep));
        slider.setContentDescription(title + "音量");
        box.addView(slider, new LinearLayout.LayoutParams(-1, dp(48)));
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
                if (!fromUser) return;
                Prefs.get(MainActivity.this).edit().putInt(Prefs.volumeKey(key, asleep), progress).apply();
                value.setText(title + " · " + progress + "%" + (progress == 0 ? "（静音）" : ""));
                player.updateVolume(MainActivity.this);
            }
            @Override public void onStartTrackingTouch(SeekBar bar) { }
            @Override public void onStopTrackingTouch(SeekBar bar) { }
        });
        box.addView(button("试听" + (key.equals("charging") ? "充电提示音" : "低电量提示音"), () -> player.play(this, key, asleep)));
    }
    private void addAlert(String key, String title, String description, int threshold) {
        LinearLayout box = card();
        Switch toggle = new Switch(this); toggle.setText(title); toggle.setTextSize(19); toggle.setTextColor(INK); toggle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        toggle.setChecked(Prefs.enabled(this, key)); toggle.setOnCheckedChangeListener((v, checked) -> Prefs.get(this).edit().putBoolean(key + "_enabled", checked).apply());
        box.addView(toggle); box.addView(label(description, 14, MUTED, false));
        box.addView(label("提示音 · " + Prefs.get(this).getString(key + "_name", "内置提示音"), 12, MUTED, false));
        LinearLayout row = new LinearLayout(this);
        row.addView(button("试听", () -> player.play(this, key)), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(button("更换声音", () -> selectSound(key)), new LinearLayout.LayoutParams(0, -2, 1));
        if (threshold > 0) row.addView(button("设定电量", () -> changeThreshold(key)), new LinearLayout.LayoutParams(0, -2, 1));
        box.addView(row);
    }
    private void selectSound(String key) {
        new AlertDialog.Builder(this).setTitle("选择提示音").setItems(new String[]{"使用内置提示音", "从手机选择音频"}, (dialog, which) -> {
            if (which == 0) {
                Prefs.get(this).edit().remove(key + "_uri").remove(key + "_name").apply(); render();
            } else {
                pickingKey = key;
                Intent pick = new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("audio/*").addCategory(Intent.CATEGORY_OPENABLE)
                        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                try { startActivityForResult(pick, 10); }
                catch (ActivityNotFoundException ignored) { Toast.makeText(this, "设备没有可用的文件选择器", Toast.LENGTH_SHORT).show(); }
            }
        }).show();
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request != 10 || result != RESULT_OK || data == null || data.getData() == null || pickingKey == null) return;
        Uri uri = data.getData();
        try { getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION); }
        catch (SecurityException error) { Toast.makeText(this, "无法保留此文件的访问权限，请选择本地音频", Toast.LENGTH_LONG).show(); return; }
        String name = "自定义音频";
        try (android.database.Cursor cursor = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) name = cursor.getString(0);
        } catch (Exception ignored) { }
        Prefs.get(this).edit().putString(pickingKey + "_uri", uri.toString()).putString(pickingKey + "_name", name).apply(); render();
    }
    private void changeThreshold(String key) {
        NumberPicker picker = new NumberPicker(this); picker.setMinValue(1); picker.setMaxValue(99);
        picker.setValue(key.equals("low") ? Prefs.low(this) : Prefs.critical(this));
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("提醒电量（%）").setView(picker).setNegativeButton("取消", null).setPositiveButton("保存", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(button -> {
            picker.clearFocus(); int value = picker.getValue();
            if ((key.equals("low") && value <= Prefs.critical(this)) || (key.equals("critical") && value >= Prefs.low(this))) {
                Toast.makeText(this, "超低电量必须小于低电量阈值", Toast.LENGTH_SHORT).show(); return;
            }
            Prefs.get(this).edit().putInt(key, value).apply(); dialog.dismiss(); render();
        })); dialog.show();
    }
    private void toggleMonitoring() {
        if (Prefs.wanted(this)) {
            Prefs.get(this).edit().putBoolean("monitor_enabled", false).putBoolean("monitoring", false).remove("policy_state").commit();
            stopService(new Intent(this, BatteryService.class)); updateMonitor();
        } else if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 20);
        } else startMonitoring();
    }
    private void startMonitoring() {
        boolean previouslyEnabled = Prefs.wanted(this);
        Prefs.get(this).edit().putBoolean("monitor_enabled", true).commit();
        try {
            startForegroundService(new Intent(this, BatteryService.class));
            Prefs.get(this).edit().remove("restore_error").apply(); updateMonitor();
        } catch (RuntimeException error) {
            Prefs.get(this).edit().putBoolean("monitor_enabled", previouslyEnabled).putBoolean("restore_error", true).commit();
            updateMonitor(); Toast.makeText(this, "无法启动，请检查后台限制后重新开启", Toast.LENGTH_LONG).show();
        }
    }
    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == 20) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) startMonitoring();
            else Toast.makeText(this, "请允许通知权限，以便显示监听状态和停止按钮", Toast.LENGTH_LONG).show();
        }
    }
    private void updateMonitor() {
        monitor.setText(Prefs.wanted(this) ? "已启用 · 点击关闭" : "开启电量提醒");
        updateBackgroundState();
    }
    private void updateBackgroundState() {
        if (backgroundState == null) return;
        boolean exempt = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());
        backgroundState.setText((exempt ? "电池优化：已豁免" : "电池优化：仍受系统管理")
                + (Prefs.get(this).getBoolean("restore_error", false) ? "\n上次恢复受阻，请检查系统后台权限" : ""));
    }
    private void openSettings(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException error) { Toast.makeText(this, "请手动前往系统设置的应用管理页面", Toast.LENGTH_LONG).show(); }
    }
    private void openStartupSettings() {
        if (Build.MANUFACTURER.equalsIgnoreCase("vivo") || Build.MANUFACTURER.equalsIgnoreCase("iqoo")) {
            try {
                startActivity(new Intent("com.iqoo.secure.BGSTARTUPMANAGER").setPackage("com.vivo.permissionmanager"));
                return;
            } catch (ActivityNotFoundException | SecurityException ignored) { }
        }
        openSettings(new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + getPackageName())));
    }
    private void requestBackgroundAllowance() {
        if (getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName())) {
            Toast.makeText(this, "已允许不受电池优化限制", Toast.LENGTH_SHORT).show(); return;
        }
        new AlertDialog.Builder(this).setTitle("允许后台电量提醒")
                .setMessage("为减少熄屏后的电量提醒中断，可允许本应用不受电池优化限制。仍需允许手机厂商的自启动和后台运行权限。")
                .setNegativeButton("暂不设置", null).setPositiveButton("前往允许", (d, w) -> openSettings(new Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:" + getPackageName())))).show();
    }
    @Override protected void onResume() {
        super.onResume();
        boolean running = false;
        ActivityManager manager = getSystemService(ActivityManager.class);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (BatteryService.class.getName().equals(service.service.getClassName())) { running = true; break; }
        }
        if (Prefs.wanted(this) && !running) startMonitoring();
        updateMonitor();
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(batteryReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(batteryReceiver, filter);
        registered = true;
    }
    @Override protected void onPause() { super.onPause(); if (registered) { unregisterReceiver(batteryReceiver); registered = false; } player.stop(); }
}
