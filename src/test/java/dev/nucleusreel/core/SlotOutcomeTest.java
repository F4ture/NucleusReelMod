package dev.nucleusreel.core;

import java.util.*;

public final class SlotOutcomeTest {
    public static void main(String[] args) {
        Random random = new Random(20261027);
        Set<String> leftSymbols = new HashSet<>(), rightSymbols = new HashSet<>();
        List<Reward> winners = List.of(new Reward("Divan's Alloy", 2), new Reward("Quick Claw", 1),
            new Reward("Jaderald", 3), new Reward("Fine Jade Gemstone", 32), new Reward("Divan Fragment", 2));
        int rare = 0, ordinary = 0;
        for (int i = 0; i < 10000; i++) {
            Reward winner = winners.get(i % winners.size());
            double chance = switch (i % 3) { case 0 -> 0; case 1 -> 0.25; default -> 1; };
            SlotOutcome outcome = SlotOutcome.create(winner, RewardCatalog.PREVIEW_REWARDS, chance, random);
            if (!outcome.symbol(1).equals(winner)) throw new AssertionError("Centre reward or amount changed");
            boolean isRare = i % winners.size() < 3;
            if (outcome.triple() != isRare) throw new AssertionError("Rare must always match three; ordinary must not");
            if (isRare) rare++;
            else {
                ordinary++;
                leftSymbols.add(outcome.symbol(0).name());
                rightSymbols.add(outcome.symbol(2).name());
            }
            for (int reel : new int[]{0, 2}) {
                if (outcome.symbol(reel).count() != 1) throw new AssertionError("Decoration copied a loot amount");
                if (RewardCatalog.find(outcome.symbol(reel).name()) == null) throw new AssertionError("Unknown icon");
            }
        }
        if (leftSymbols.size() < 8 || rightSymbols.size() < 10) throw new AssertionError("Insufficient side variety");
        Reward alloy = new Reward("Divan's Alloy", 1);
        if (!SlotOutcome.create(alloy, List.of(), 0, random).triple()) throw new AssertionError("Rare needs no alternative pool");
        Reward pearl = new Reward("Precious Pearl", 2);
        if (!SlotOutcome.create(pearl, RewardCatalog.PREVIEW_REWARDS, 0, List.of("Precious Pearl"), random).triple())
            throw new AssertionError("Custom rare reward");
        Reward flawless = new Reward("Flawless Jasper Gemstone", 1);
        if (!SlotOutcome.create(flawless, RewardCatalog.PREVIEW_REWARDS, 0, List.of("Flawless"), random).triple())
            throw new AssertionError("Custom rare group");
        Reward unknown = new Reward("Some Future Reward", 4);
        if (!SlotOutcome.create(unknown, RewardCatalog.PREVIEW_REWARDS, 0.25, random).symbol(1).equals(unknown))
            throw new AssertionError("Actual result must remain in the centre");
        try { SlotOutcome.create(unknown, List.of(unknown), 0.25, random); throw new AssertionError("Empty alternatives"); }
        catch (IllegalArgumentException expected) { }
        if (RewardCelebration.forReward(new Reward("§6Divan’s Alloy", 1)) != RewardCelebration.ALLOY
            || RewardCelebration.forReward(new Reward("Quick Claw", 1)) != RewardCelebration.QUICK_CLAW
            || RewardCelebration.forReward(new Reward("Jaderald", 1)) != RewardCelebration.JADERALD
            || RewardCelebration.forReward(new Reward("Fine Jade Gemstone", 32)) != RewardCelebration.NORMAL)
            throw new AssertionError("Coin sound reward routing");
        Set<String> teasers = new HashSet<>();
        for (int i = 0; i < 10000; i++) {
            Reward actual = winners.get(i % winners.size());
            SlotOutcome outcome = SlotOutcome.create(actual, RewardCatalog.PREVIEW_REWARDS, 1,
                SlotOutcome.DEFAULT_TRIPLE_REWARDS, 1, random);
            if (!outcome.symbol(outcome.resultReel()).equals(actual)) throw new AssertionError("Lost the real reward or quantity");
            if (i % winners.size() < 3) {
                if (!outcome.triple() || outcome.nearMiss()) throw new AssertionError("A real rare reward can never fail");
            } else {
                if (!outcome.nearMiss() || !outcome.suspense() || outcome.resultReel() != 2 || outcome.triple())
                    throw new AssertionError("Forced near miss must finish with the real ordinary reward on the right");
                if (!SlotOutcome.same(outcome.symbol(0), outcome.symbol(1))
                    || !SlotOutcome.guaranteedTriple(outcome.symbol(0), SlotOutcome.DEFAULT_TRIPLE_REWARDS))
                    throw new AssertionError("Near miss needs two identical rare symbols");
                if (outcome.symbol(0).count() != 1 || outcome.symbol(1).count() != 1)
                    throw new AssertionError("Decorative rare items cannot inherit the real stack size");
                if (RewardCelebration.forReward(outcome.symbol(outcome.resultReel())) != RewardCelebration.NORMAL)
                    throw new AssertionError("A failed combination must not celebrate the teased rare reward");
                teasers.add(outcome.symbol(0).name());
            }
        }
        if (teasers.size() != 3) throw new AssertionError("All three rare teasers must be reachable");
        int misses = 0;
        Reward ordinaryReward = new Reward("Fine Jade Gemstone", 32);
        for (int i = 0; i < 10000; i++) {
            var outcome = SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 0.25,
                SlotOutcome.DEFAULT_TRIPLE_REWARDS, 0.3, random);
            if (outcome.nearMiss()) misses++;
            else if (outcome.resultReel() != 1) throw new AssertionError("Normal result remains in the centre");
        }
        if (misses < 2800 || misses > 3200) throw new AssertionError("Unexpected near-miss frequency: " + misses);
        if (SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 1, List.of(), 1, random).nearMiss())
            throw new AssertionError("Empty rare list must disable suspense failures");
        for (double invalid : new double[]{-1, 1.01, Double.NaN, Double.POSITIVE_INFINITY}) {
            try {
                SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 0.25,
                    SlotOutcome.DEFAULT_TRIPLE_REWARDS, invalid, random);
                throw new AssertionError("Invalid failure probability accepted");
            } catch (IllegalArgumentException expected) { }
        }
        int[] stages = new int[3];
        Random conditionalRandom = new Random(20261028);
        for (int i = 0; i < 100000; i++) {
            var outcome = SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 0.25,
                SlotOutcome.DEFAULT_TRIPLE_REWARDS, 0.4, 0.5, conditionalRandom);
            stages[outcome.failedReel() < 0 ? 0 : outcome.failedReel()]++;
            if (outcome.triple() || !outcome.symbol(outcome.resultReel()).equals(ordinaryReward))
                throw new AssertionError("Conditional failure invented a jackpot or lost the actual reward");
            if (outcome.failedReel() == 1 && (outcome.resultReel() != 1 || SlotOutcome.same(outcome.symbol(0), outcome.symbol(1))))
                throw new AssertionError("Second failure must break the pair and keep the reward in the centre");
            if (outcome.failedReel() == 2 && !SlotOutcome.same(outcome.symbol(0), outcome.symbol(1)))
                throw new AssertionError("Third failure must start with two matching rares");
            if (outcome.nearMiss() && !SlotOutcome.guaranteedTriple(outcome.symbol(0), SlotOutcome.DEFAULT_TRIPLE_REWARDS))
                throw new AssertionError("Suspense must tease a rare item");
        }
        if (Math.abs(stages[1] - 40000) > 600 || Math.abs(stages[2] - 30000) > 600 || Math.abs(stages[0] - 30000) > 600)
            throw new AssertionError("Incorrect 40/30/30 distribution: " + Arrays.toString(stages));
        double conditionalRate = stages[2] / (double)(stages[0] + stages[2]);
        if (Math.abs(conditionalRate - 0.5) > 0.01) throw new AssertionError("Third chance is not conditional");
        for (double second : new double[]{0, 1}) for (double third : new double[]{0, 1}) {
            int expected = second == 1 ? 1 : third == 1 ? 2 : -1;
            var outcome = SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 1,
                SlotOutcome.DEFAULT_TRIPLE_REWARDS, second, third, conditionalRandom);
            if (outcome.failedReel() != expected) throw new AssertionError("Forced failure stage");
            for (Reward reward : winners.subList(0, 3)) {
                var win = SlotOutcome.create(reward, RewardCatalog.PREVIEW_REWARDS, 0,
                    SlotOutcome.DEFAULT_TRIPLE_REWARDS, second, third, conditionalRandom);
                if (!win.triple() || win.nearMiss()) throw new AssertionError("Real rare drop was cancelled");
            }
        }
        if (SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 1, List.of(), 1, 1, random).nearMiss())
            throw new AssertionError("Empty rare list must disable both failures");
        for (double invalid : new double[]{-1, 1.01, Double.NaN, Double.POSITIVE_INFINITY}) {
            try {
                SlotOutcome.create(ordinaryReward, RewardCatalog.PREVIEW_REWARDS, 0.25,
                    SlotOutcome.DEFAULT_TRIPLE_REWARDS, invalid, 0.5, random);
                throw new AssertionError("Invalid second probability accepted");
            } catch (IllegalArgumentException expected) { }
        }
        System.out.println("100,000 conditional outcomes: second=" + stages[1] + ", third=" + stages[2]
            + ", normal=" + stages[0] + "; no false jackpot, actual reward/count preserved.");
        System.out.println("20,000 further outcomes verified: real rare rewards never fail; forced misses preserve ordinary rewards; "
            + misses + "/10,000 ordinary rewards staged a near miss at 30%.");
        System.out.println("10,000 outcomes verified: " + rare + " guaranteed rare triples; " + ordinary
            + " ordinary centre results without triples; quantities preserved. Custom rare rewards verified.");
    }
}
