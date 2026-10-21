package com.pico.batterysound;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Resume only the monitoring previously requested by the user. */
public final class RestoreReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        boolean boot = Intent.ACTION_BOOT_COMPLETED.equals(action);
        if (!boot && !Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) return;
        if (!Prefs.wanted(context)) return;
        if (boot && !Prefs.get(context).getBoolean("auto_start", true)) return;
        try {
            context.startForegroundService(new Intent(context, BatteryService.class));
            Prefs.get(context).edit().remove("restore_error").apply();
            Log.i("PicoBattery", "Restore requested: " + action);
        } catch (RuntimeException error) {
            Prefs.get(context).edit().putBoolean("restore_error", true).apply();
            Log.w("PicoBattery", "Background restore denied by system", error);
        }
    }
}
