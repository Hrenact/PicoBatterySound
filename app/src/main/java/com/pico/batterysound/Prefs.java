package com.pico.batterysound;

import android.content.Context;
import android.content.SharedPreferences;

final class Prefs {
    static SharedPreferences get(Context context) { return context.getSharedPreferences("settings", Context.MODE_PRIVATE); }
    static int low(Context context) { return get(context).getInt("low", 20); }
    static int critical(Context context) { return get(context).getInt("critical", 5); }
    static String volumeKey(String sound) { return sound.equals("charging") ? "charging_volume" : "low_volume"; }
    static String volumeKey(String sound, boolean asleep) { return (asleep ? "sleep_" : "") + volumeKey(sound); }
    static boolean asleep(Context context) {
        android.os.PowerManager power = context.getSystemService(android.os.PowerManager.class);
        android.hardware.display.DisplayManager manager = context.getSystemService(android.hardware.display.DisplayManager.class);
        android.view.Display display = manager.getDisplay(android.view.Display.DEFAULT_DISPLAY);
        int state = display == null ? android.view.Display.STATE_UNKNOWN : display.getState();
        return !power.isInteractive() || state == android.view.Display.STATE_OFF
                || state == android.view.Display.STATE_DOZE || state == android.view.Display.STATE_DOZE_SUSPEND;
    }
    static int volume(Context context, String sound) {
        return volume(context, sound, false);
    }
    static int volume(Context context, String sound, boolean asleep) {
        SharedPreferences prefs = get(context);
        int normal = prefs.getInt(volumeKey(sound), prefs.getInt("volume", 100));
        if (asleep && !prefs.contains(volumeKey(sound, true))) {
            prefs.edit().putInt(volumeKey(sound, true), normal).apply();
        }
        return Math.max(0, Math.min(150, asleep ? prefs.getInt(volumeKey(sound, true), normal) : normal));
    }
    static boolean enabled(Context context, String key) { return get(context).getBoolean(key + "_enabled", true); }
    static boolean wanted(Context context) {
        SharedPreferences prefs = get(context);
        if (!prefs.contains("monitor_enabled")) {
            prefs.edit().putBoolean("monitor_enabled", prefs.getBoolean("monitoring", false)).commit();
        }
        return prefs.getBoolean("monitor_enabled", false);
    }
}
