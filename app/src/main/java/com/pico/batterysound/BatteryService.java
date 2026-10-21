package com.pico.batterysound;

import android.app.*;
import android.content.*;
import android.os.*;

public final class BatteryService extends Service {
    private BatteryPolicy policy;
    private final SoundPlayer sound = new SoundPlayer();
    private static final String CHANNEL = "battery_monitor";
    private boolean registered;
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            if (level < 0 || scale <= 0) return;
            int percent = Math.round(level * 100f / scale);
            int powerSource = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            boolean plugged = powerSource != 0;
            android.util.Log.i("PicoBattery", "Battery sample: level=" + percent + ", powerSource=" + powerSource);
            evaluateBattery(percent, plugged);
        }
    };
    private void evaluateBattery(int percent, boolean plugged) {
            Context context = this;
            BatteryPolicy.Event event = policy.update(percent, plugged, Prefs.low(context), Prefs.critical(context));
            // Persist only transitions, before audio starts, to avoid duplicate alerts after process death.
            if (Prefs.get(context).getInt("policy_state", 0) != policy.snapshot()) {
                Prefs.get(context).edit().putInt("policy_state", policy.snapshot()).commit();
            }
            String key = switch (event) { case CHARGING -> "charging"; case LOW -> "low"; case CRITICAL -> "critical"; default -> null; };
            if (key != null && Prefs.enabled(context, key)) {
                android.util.Log.i("PicoBattery", "Alert: " + key + ", level=" + percent + ", plugged=" + plugged);
                sound.play(context, key);
            }
            if (Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                getSystemService(NotificationManager.class).notify(1, notification("电量 " + percent + "% · " + (plugged ? "已连接电源" : "使用电池")));
            }
    }
    @Override public void onCreate() {
        super.onCreate();
        NotificationChannel channel = new NotificationChannel(CHANNEL, "电量监听状态", NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        getSystemService(NotificationManager.class).createNotificationChannel(channel);
        startForeground(1, notification("正在监听电量变化"));
        policy = new BatteryPolicy(Prefs.get(this).getInt("policy_state", 0));
        Prefs.volume(this, "charging", true);
        Prefs.volume(this, "low", true);
        android.util.Log.i("PicoBattery", "Screen profile: " + (Prefs.asleep(this) ? "sleep" : "awake"));
    }
    private void listen() {
        if (registered) return;
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(receiver, filter);
        registered = true;
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if ((intent != null && "STOP".equals(intent.getAction())) || !Prefs.wanted(this)) {
            Prefs.get(this).edit().putBoolean("monitor_enabled", false).putBoolean("monitoring", false).remove("policy_state").commit();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        listen();
        android.util.Log.i("PicoBattery", "Monitoring active; system restart=" + (intent == null));
        return START_STICKY;
    }
    private Notification notification(String text) {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent stop = PendingIntent.getService(this, 1, new Intent(this, BatteryService.class).setAction("STOP"), PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        return new Notification.Builder(this, CHANNEL).setSmallIcon(R.drawable.ic_battery).setContentTitle("Pico 电量提示音已开启")
                .setContentText(text).setContentIntent(open).setOngoing(true).setOnlyAlertOnce(true)
                .addAction(new Notification.Action.Builder(null, "停止监听", stop).build()).build();
    }
    @Override public void onDestroy() {
        if (registered) unregisterReceiver(receiver);
        sound.stop();
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
