package dev.nucleusreel.core;

/** Pure timing shared by scrolling, camera, sounds, music and deferred chat. */
public final class SlotSpin {
    public static final int REELS = 3;
    public static final int START = 2;
    private static final float APPROACH = 1.65f;
    private final int baseDuration;
    private final int gap;
    private final boolean suspense;
    private final int failedReel;

    public SlotSpin(int duration) { this(duration, false, false); }
    public SlotSpin(int duration, boolean suspense, boolean nearMiss) {
        this(duration, suspense, nearMiss ? 2 : -1);
    }
    public SlotSpin(int duration, boolean suspense, int failedReel) {
        if (duration < 20) throw new IllegalArgumentException("At least one second is required");
        if (failedReel < -1 || failedReel == 0 || failedReel > 2 || (failedReel >= 0 && !suspense))
            throw new IllegalArgumentException("Invalid failure reel");
        baseDuration = duration;
        this.suspense = suspense;
        this.failedReel = failedReel;
        gap = Math.min(12, Math.max(3, duration / 7));
    }

    public int durationTicks() { return baseDuration + (suspense ? failedReel == 1 ? 40 : 80 : 0); }
    public int landingIndex(int reel) { checkReel(reel); return 30 + reel * 4; }
    public int stopOrder(int reel) { checkReel(reel); return suspense ? reel : reel == 0 ? 0 : reel == 2 ? 1 : 2; }
    public int stopTick(int reel) {
        return baseDuration - (2 - stopOrder(reel)) * gap + (suspense ? Math.min(reel, failedReel == 1 ? 1 : 2) * 40 : 0);
    }
    public boolean stopped(int reel, float time) { return time >= stopTick(reel); }
    public int focusReel(float time) {
        if (!suspense || time < stopTick(0) || time >= durationTicks()) return -1;
        if (failedReel == 1 && time >= stopTick(1)) return -1;
        return time < stopTick(1) ? 1 : 2;
    }
    public float focusAmount(float time) {
        if (!suspense) return 0;
        int end = failedReel == 1 ? stopTick(1) : durationTicks();
        return smooth((time - stopTick(0)) / 10) * smooth((end + 12 - time) / 12);
    }
    public float focusCentre(float time) {
        return suspense && failedReel != 1 ? 118 * smooth((time - stopTick(1)) / 10) : 0;
    }
    public static float smooth(float t) { t = Math.clamp(t, 0, 1); return t * t * (3 - 2 * t); }
    public float approachDistance(int reel) { return failedReel == reel ? 3f : APPROACH; }

    public float position(int reel, float time) {
        int end = landingIndex(reel);
        if (time >= stopTick(reel)) return end;
        if (!suspense || reel == 0 || (failedReel == 1 && reel == 2))
            return START + (end - START) * cruise(time / stopTick(reel));
        int begin = stopTick(reel - 1);
        float span = stopTick(reel) - begin;
        float approach = approachDistance(reel);
        float exponent = failedReel == reel ? 2f : 2.5f;
        if (time < begin) {
            float t = Math.clamp(time / begin, 0, 1);
            float distance = end - approach - START;
            // Match the velocity at the start of slow motion, without a pause or jump.
            float initialSpeed = approach * exponent / span;
            float blend = Math.clamp(initialSpeed * begin / distance, 0, 1);
            return START + distance * ((1 - blend) * cruise(t) + blend * t);
        }
        float t = Math.clamp((time - begin) / span, 0, 1);
        // A single deceleration curve: fixed symbols pass the line, without a stop/restart or replacement.
        return end - approach * (float) Math.pow(1 - t, exponent);
    }

    private static float cruise(float value) {
        float t = Math.clamp(value, 0, 1);
        float cruise = 0.6f;
        float velocity = 1f / (cruise + (1 - cruise) / 3);
        if (t <= cruise) return velocity * t;
        float remaining = (1 - t) / (1 - cruise);
        return velocity * (cruise + (1 - cruise) / 3 * (1 - remaining * remaining * remaining));
    }
    private static void checkReel(int reel) {
        if (reel < 0 || reel >= REELS) throw new IllegalArgumentException("Invalid reel index");
    }
}
