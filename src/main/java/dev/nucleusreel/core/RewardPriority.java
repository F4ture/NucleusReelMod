package dev.nucleusreel.core;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class RewardPriority {
    private RewardPriority() {}
    public static final List<String> DEFAULT = List.of(
        "Divan's Alloy", "Quick Claw", "Flawless", "Claw Fossil", "Precious Pearl",
        "Jaderald", "Divan Fragment", "Gemstone Mixture", "Helix Fossil",
        "Prehistoric Egg", "Lapidary", "Fortune", "Enchanted Book", "Crystal", "Fine"
    );

    public static int score(Reward reward, List<String> priority) {
        String name = reward.name().toLowerCase(Locale.ROOT);
        for (int i = 0; i < priority.size(); i++) {
            if (name.contains(priority.get(i).toLowerCase(Locale.ROOT))) return priority.size() - i;
        }
        return 0;
    }

    public static Reward best(List<Reward> rewards, List<String> priority) {
        return rewards.stream().max(Comparator.comparingInt(r -> score(r, priority))).orElseThrow();
    }

    public static Reward forAnimation(List<Reward> rewards, List<String> priority, List<String> tripleRewards) {
        var alloy = rewards.stream().filter(r -> RewardCelebration.forReward(r) == RewardCelebration.ALLOY).findFirst();
        if (alloy.isPresent()) return alloy.get();
        return best(rewards, priority, tripleRewards);
    }

    /** Most valuable actual non-jackpot entry according to the user's existing priority list. */
    public static Reward ordinary(List<Reward> rewards, List<String> priority, List<String> tripleRewards) {
        return rewards.stream().filter(r -> RewardCelebration.forReward(r) == RewardCelebration.NORMAL)
            .filter(r -> !SlotOutcome.guaranteedTriple(r, tripleRewards))
            .max(Comparator.comparingInt(r -> score(r, priority))).orElse(null);
    }

    public static Reward best(List<Reward> rewards, List<String> priority, List<String> tripleRewards) {
        List<Reward> rare = rewards.stream().filter(r -> SlotOutcome.guaranteedTriple(r, tripleRewards)).toList();
        // Never stage a failure for a bundle that actually contains a guaranteed rare reward.
        return best(rare.isEmpty() ? rewards : rare, priority);
    }
}
