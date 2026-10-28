package dev.nucleusreel.core;

import java.util.List;

/** Slow, synchronized pickaxe transition after the final reel has stopped. */
public final class MiningTimeline {
    public static final List<Integer> HITS = List.of(38, 66, 94);
    public static final int OPEN = 94, FADE = 104, END = 140;
    private MiningTimeline() {}
    public static int cracks(float age) { return (int)HITS.stream().filter(t -> age >= t).count(); }
    public static float darkness(float age) { return FinaleTimeline.phase(age, FADE, END); }
    public static float hole(float age) { return FinaleTimeline.phase(age, OPEN, END - 6); }
    public static float zoom(float age) { return FinaleTimeline.phase(age, 24, END); }
    /** Lift and then strike once per impact; no sudden icon replacement. */
    public static float swing(float age) {
        for(int hit:HITS) if(age >= hit - 14 && age <= hit + 10) {
            return age <= hit ? FinaleTimeline.phase(age,hit-14,hit) : 1-FinaleTimeline.phase(age,hit,hit+10);
        }
        return 0;
    }
}
