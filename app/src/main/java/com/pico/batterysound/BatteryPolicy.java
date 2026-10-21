package com.pico.batterysound;

/** Battery alerts apply with or without external power; persist flags across restarts. */
public final class BatteryPolicy {
    public enum Event { NONE, CHARGING, LOW, CRITICAL }
    private boolean initialized, pluggedBefore, lowPlayed, criticalPlayed;
    public BatteryPolicy() { }
    public BatteryPolicy(int saved) {
        initialized = (saved & 1) != 0;
        pluggedBefore = (saved & 2) != 0;
        lowPlayed = (saved & 4) != 0;
        criticalPlayed = (saved & 8) != 0;
    }
    public int snapshot() {
        return (initialized ? 1 : 0) | (pluggedBefore ? 2 : 0) | (lowPlayed ? 4 : 0) | (criticalPlayed ? 8 : 0);
    }

    public Event update(int percent, boolean plugged, int low, int critical) {
        if (percent < 0 || percent > 100) return Event.NONE;
        if (!initialized) {
            initialized = true;
            pluggedBefore = plugged;
        }
        boolean connected = plugged && !pluggedBefore;
        boolean disconnected = !plugged && pluggedBefore;
        pluggedBefore = plugged;
        if (disconnected) {
            lowPlayed = false;
            criticalPlayed = false;
        }
        // A small recovery margin avoids repeated alerts around a threshold.
        if (percent >= Math.min(100, low + 2)) lowPlayed = false;
        if (percent >= Math.min(100, critical + 2)) criticalPlayed = false;
        // Connecting at an already-low level should play only the connection sound.
        // Keep critical armed if still above its threshold: wired discharge must still warn.
        if (connected) {
            if (percent <= low) lowPlayed = true;
            if (percent <= critical) criticalPlayed = true;
            return Event.CHARGING;
        }
        if (percent <= critical && !criticalPlayed) {
            criticalPlayed = true;
            lowPlayed = true;
            return Event.CRITICAL;
        }
        if (percent <= low && !lowPlayed) {
            lowPlayed = true;
            return Event.LOW;
        }
        return Event.NONE;
    }
}
