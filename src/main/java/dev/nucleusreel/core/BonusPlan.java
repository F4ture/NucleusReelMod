package dev.nucleusreel.core;

import java.util.List;
import java.util.random.RandomGenerator;

/** Cosmetic detour only. The server reward remains separate from the deliberately failed reel. */
public record BonusPlan(Kind kind, SlotOutcome reels) {
    public enum Kind { NONE, ALLOY, EMPTY }
    public enum Preview { AUTO, OFF, WIN, EMPTY }
    public boolean active() { return kind != Kind.NONE; }
    public boolean won() { return kind == Kind.ALLOY; }

    public static BonusPlan choose(Reward actual, SlotOutcome normal, List<String> rareRewards, boolean enabled,
                                   double winChance, double emptyChance, Preview preview, RandomGenerator random) {
        return choose(actual, RewardCelebration.forReward(actual) == RewardCelebration.NORMAL ? actual : null,
            normal, rareRewards, enabled, winChance, emptyChance, preview, random);
    }
    public static BonusPlan choose(Reward actual, Reward actualOrdinary, SlotOutcome normal, List<String> rareRewards, boolean enabled,
                                   double winChance, double emptyChance, Preview preview, RandomGenerator random) {
        for (double p : new double[]{winChance, emptyChance})
            if (!Double.isFinite(p) || p < 0 || p > 1) throw new IllegalArgumentException("Invalid bonus probability");
        boolean alloy = RewardCelebration.forReward(actual) == RewardCelebration.ALLOY;
        boolean ordinary = RewardCelebration.forReward(actual) == RewardCelebration.NORMAL
            && !SlotOutcome.guaranteedTriple(actual, rareRewards);
        Kind kind = Kind.NONE;
        if (preview == Preview.WIN) {
            if (!alloy) throw new IllegalArgumentException("Winning bonus requires a real Alloy result");
            kind = Kind.ALLOY;
        } else if (preview == Preview.EMPTY) {
            if (RewardCelebration.forReward(actual) != RewardCelebration.NORMAL)
                throw new IllegalArgumentException("Empty bonus requires an ordinary result");
            kind = Kind.EMPTY;
        } else if (preview == Preview.AUTO && enabled) {
            if (alloy && actualOrdinary != null && random.nextDouble() < winChance) kind = Kind.ALLOY;
            else if (ordinary && normal.failedReel() == 2 && random.nextDouble() < emptyChance) kind = Kind.EMPTY;
        }
        // A server bundle with no ordinary entry must never invent one for a fake failure.
        if (kind == Kind.ALLOY && actualOrdinary == null) kind = Kind.NONE;
        if (kind == Kind.NONE) {
            // Real Alloy always gets a truthful win, even with a customized rare list.
            if (alloy) {
                Reward icon = new Reward(actual.name(), 1);
                normal = new SlotOutcome(List.of(icon, actual, icon), 1, -1);
            }
            return new BonusPlan(kind, normal);
        }
        Reward teaser = new Reward("Divan's Alloy", 1);
        Reward last = kind == Kind.ALLOY ? actualOrdinary : actual;
        return new BonusPlan(kind, new SlotOutcome(List.of(teaser, teaser, last), 2, 2));
    }
}
