package dev.nucleusreel.core;

import java.util.List;

/** Timings in client ticks; the finale runs AFTER the reel has revealed the server reward. */
public final class FinaleTimeline {
    public static final int ALLOY_BREAK = 24, ALLOY_SKY = 90, ALLOY_FALL = 155, ALLOY_CATCH = 202;
    public enum Sound { CRACK, BOOM, LAUNCH, SUN, CHIME, FALL, CATCH, LEVEL_UP, MEOW, PURR, VILLAGER, GEM, HARP, SWIPE, SCRATCH }
    public record Cue(int tick, Sound sound, float pitch, float volume) {}
    public record Flight(float x, float y, float size, float rotation, float sunlight, float hand) {}
    private FinaleTimeline() {}
    public static int duration(RewardCelebration type, int normalTicks) {
        return switch (type) { case ALLOY -> 240; case QUICK_CLAW -> 160; case JADERALD -> 100; default -> normalTicks; };
    }
    public static float phase(float age, float begin, float end) { return SlotSpin.smooth((age - begin) / (end - begin)); }
    public static float scratchProgress(float age) { return phase(age, 18, 44); }
    public static float clawReveal(float age) { return phase(age, 60, 82); }
    public static float pawY(float age,float height) {
        return -height/2-350 + 160*phase(age,6,18)
            + (height+270)*scratchProgress(age)+220*phase(age,44,60);
    }
    public static float pawX(float age) { return -24+48*scratchProgress(age); }
    public static float fade(RewardCelebration type, float age) {
        return 1 - phase(age, duration(type, 50) - 12, duration(type, 50));
    }
    /** Coordinates relative to the screen centre, normalized by viewport width/height. */
    public static Flight flight(float age) {
        float lift = phase(age, ALLOY_BREAK, ALLOY_SKY);
        float fall = phase(age, ALLOY_FALL, ALLOY_CATCH);
        float x = 0.30f * fall;
        float y = -0.29f * lift + 0.57f * fall;
        float size = 0.65f + 0.35f * lift - 0.62f * fall;
        float rotation = (float) Math.sin(age * 0.07) * 0.08f * (1 - fall) - 0.25f * fall;
        return new Flight(x, y, size, rotation,
            phase(age, 68, 100) * (1 - phase(age, ALLOY_FALL, 190)), phase(age, 170, ALLOY_CATCH));
    }
    private static Cue cue(int tick, Sound sound, float pitch, float volume) { return new Cue(tick,sound,pitch,volume); }
    private static final List<Cue> ALLOY = List.of(
        cue(0,Sound.CRACK,0.7f,0.6f), cue(12,Sound.CRACK,0.55f,0.75f),
        cue(ALLOY_BREAK,Sound.BOOM,0.7f,0.8f), cue(30,Sound.LAUNCH,0.65f,0.8f),
        cue(54,Sound.LAUNCH,1.15f,0.55f), cue(ALLOY_SKY,Sound.SUN,0.8f,0.65f),
        cue(104,Sound.CHIME,0.8f,0.65f),cue(116,Sound.CHIME,1.0f,0.6f),cue(128,Sound.CHIME,1.2f,0.6f),
        cue(140,Sound.CHIME,1.6f,0.6f),cue(ALLOY_FALL,Sound.FALL,0.75f,0.7f),
        cue(ALLOY_CATCH,Sound.CATCH,0.9f,0.85f),cue(208,Sound.LEVEL_UP,0.85f,0.7f));
    private static final List<Cue> CLAW = List.of(
        cue(10,Sound.MEOW,0.9f,0.7f),cue(18,Sound.SWIPE,0.7f,0.8f),
        cue(24,Sound.SCRATCH,0.85f,0.45f),cue(32,Sound.SWIPE,1f,0.65f),cue(42,Sound.SCRATCH,1.15f,0.4f),
        cue(60,Sound.CHIME,1.25f,0.7f),cue(70,Sound.HARP,1.5f,0.6f),cue(82,Sound.LEVEL_UP,1.35f,0.6f),
        cue(126,Sound.PURR,1.1f,0.5f));
    private static final List<Cue> JADERALD = List.of(
        cue(9,Sound.VILLAGER,1.1f,0.7f),cue(18,Sound.GEM,0.85f,0.6f),
        cue(30,Sound.GEM,1f,0.55f),cue(42,Sound.CHIME,1.2f,0.55f),cue(54,Sound.GEM,1.4f,0.55f),
        cue(66,Sound.CHIME,1.6f,0.6f),cue(78,Sound.VILLAGER,1.25f,0.6f));
    public static List<Cue> cues(RewardCelebration type) {
        return switch (type) { case ALLOY -> ALLOY; case QUICK_CLAW -> CLAW; case JADERALD -> JADERALD; default -> List.of(); };
    }
}
