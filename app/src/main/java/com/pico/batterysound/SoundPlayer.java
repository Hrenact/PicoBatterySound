package com.pico.batterysound;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

final class SoundPlayer {
    private MediaPlayer player;
    private android.media.audiofx.LoudnessEnhancer enhancer;
    private boolean boostUnavailable;
    private String playingKey = "charging";
    private Boolean previewAsleep;
    private long requestedNs;
    private final Handler handler = new Handler(Looper.getMainLooper());
    void play(Context context, String key) {
        play(context, key, null);
    }
    void play(Context context, String key, Boolean asleepOverride) {
        requestedNs = System.nanoTime();
        stop();
        previewAsleep = asleepOverride;
        String selected = Prefs.get(context).getString(key + "_uri", "");
        if (!start(context, key, selected)) {
            if (!selected.isEmpty()) {
                Toast.makeText(context, "所选音频无法读取，使用内置提示音", Toast.LENGTH_SHORT).show();
                start(context, key, "");
            }
        }
    }
    private boolean start(Context context, String key, String selected) {
        try {
            playingKey = key;
            player = new MediaPlayer();
            boostUnavailable = false;
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            int raw = key.equals("charging") ? R.raw.charging_started : R.raw.low_battery;
            Uri uri = selected.isEmpty() ? Uri.parse("android.resource://" + context.getPackageName() + "/" + raw) : Uri.parse(selected);
            player.setDataSource(context, uri);
            player.setOnCompletionListener(mp -> stop());
            player.setOnErrorListener((mp, what, extra) -> {
                stop();
                if (!selected.isEmpty()) {
                    Toast.makeText(context, "所选音频无法播放，使用内置提示音", Toast.LENGTH_SHORT).show();
                    start(context, key, "");
                }
                return true;
            });
            player.setOnPreparedListener(mp -> {
                android.util.Log.i("PicoAudioTiming", "key=" + key + " prepared_ms=" + elapsedMs());
                updateVolume(context);
                mp.start();
                android.util.Log.i("PicoAudioTiming", "key=" + key + " start_return_ms=" + elapsedMs());
                observePlayback(mp, key, requestedNs);
                handler.postDelayed(this::stop, 10000);
            });
            player.prepareAsync();
            return true;
        } catch (Exception error) { stop(); return false; }
    }
    private double elapsedMs() { return (System.nanoTime() - requestedNs) / 1_000_000.0; }
    private void observePlayback(MediaPlayer expected, String key, long request) {
        if (player != expected) return;
        try {
            android.media.MediaTimestamp timestamp = expected.getTimestamp();
            if (timestamp != null && timestamp.getAnchorMediaTimeUs() > 0 && timestamp.getMediaClockRate() > 0) {
                long anchor = timestamp.getAnchorSytemNanoTime();
                double estimatedStart = anchor - timestamp.getAnchorMediaTimeUs() * 1000.0 / timestamp.getMediaClockRate();
                android.util.Log.i("PicoAudioTiming", "key=" + key
                        + " first_progress_ms=" + ((System.nanoTime() - request) / 1_000_000.0)
                        + " estimated_timeline_start_ms=" + ((estimatedStart - request) / 1_000_000.0)
                        + " media_us=" + timestamp.getAnchorMediaTimeUs());
                return;
            }
        } catch (IllegalStateException ignored) { return; }
        if (System.nanoTime() - request < 2_000_000_000L) {
            handler.postDelayed(() -> observePlayback(expected, key, request), 10);
        } else android.util.Log.i("PicoAudioTiming", "key=" + key + " timestamp_unavailable");
    }
    void updateVolume(Context context) {
        if (player != null) {
            boolean asleep = previewAsleep != null ? previewAsleep : Prefs.asleep(context);
            float gain = Prefs.volume(context, playingKey, asleep) / 100f;
            android.util.Log.i("PicoAudioGain", "key=" + playingKey + " profile=" + (asleep ? "sleep" : "awake") + " volume=" + Math.round(gain * 100));
            player.setVolume(Math.min(1f, gain), Math.min(1f, gain));
            try {
                if (gain > 1f && !boostUnavailable) {
                    if (enhancer == null) enhancer = new android.media.audiofx.LoudnessEnhancer(player.getAudioSessionId());
                    enhancer.setTargetGain((int) Math.round(2000 * Math.log10(gain)));
                    if (enhancer.setEnabled(true) != android.media.audiofx.AudioEffect.SUCCESS) throw new IllegalStateException("Cannot enable audio boost");
                    android.util.Log.i("PicoAudioGain", "key=" + playingKey + " gain_mB=" + enhancer.getTargetGain() + " enabled=" + enhancer.getEnabled());
                } else if (enhancer != null) {
                    enhancer.release(); enhancer = null;
                }
            } catch (RuntimeException error) {
                if (enhancer != null) { enhancer.release(); enhancer = null; }
                boostUnavailable = true;
                android.util.Log.w("PicoAudioGain", "Boost unavailable", error);
                Toast.makeText(context, "设备暂不支持音量增强，按 100% 播放", Toast.LENGTH_SHORT).show();
            }
        }
    }
    void stop() {
        handler.removeCallbacksAndMessages(null);
        if (enhancer != null) { enhancer.release(); enhancer = null; }
        if (player != null) { player.release(); player = null; }
    }
}
