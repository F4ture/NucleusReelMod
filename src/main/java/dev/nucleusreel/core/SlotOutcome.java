package dev.nucleusreel.core;

import java.util.*;
import java.util.random.RandomGenerator;

/** Real rare rewards always win. Ordinary rewards may fail on reel two or three, or spin normally. */
public record SlotOutcome(List<Reward> symbols, int resultReel, int failedReel) {
    public static final int RESULT_REEL = 1;
    public static final List<String> DEFAULT_TRIPLE_REWARDS = List.of("Divan's Alloy", "Quick Claw", "Jaderald");
    public SlotOutcome {
        symbols = List.copyOf(symbols);
        if (symbols.size() != 3) throw new IllegalArgumentException("Exactly three reel symbols are required");
        if (resultReel < 0 || resultReel > 2 || failedReel < -1 || failedReel == 0 || failedReel > 2)
            throw new IllegalArgumentException("Invalid result/failure reel");
    }
    public static SlotOutcome create(Reward winner,List<Reward> pool,double matchChance,RandomGenerator random) {
        return create(winner,pool,matchChance,DEFAULT_TRIPLE_REWARDS,random);
    }
    public static SlotOutcome create(Reward winner,List<Reward> pool,double matchChance,List<String> tripleRewards,RandomGenerator random) {
        return create(winner,pool,matchChance,tripleRewards,0,0,random);
    }
    /** Compatibility helper for explicitly requesting a third-reel miss. */
    public static SlotOutcome create(Reward winner,List<Reward> pool,double matchChance,List<String> tripleRewards,
                                     double thirdChance,RandomGenerator random) {
        return create(winner,pool,matchChance,tripleRewards,0,thirdChance,random);
    }
    public static SlotOutcome create(Reward winner,List<Reward> pool,double matchChance,List<String> tripleRewards,
                                     double secondChance,double thirdChance,RandomGenerator random) {
        probability(matchChance);probability(secondChance);probability(thirdChance);
        Reward decoration=new Reward(winner.name(),1);
        if(guaranteedTriple(winner,tripleRewards))
            return new SlotOutcome(List.of(decoration,winner,decoration),RESULT_REEL,-1);
        Map<String,Reward> distinct=new LinkedHashMap<>();
        for(Reward reward:pool)if(!same(reward,winner))
            distinct.putIfAbsent(RewardCatalog.normalize(reward.name()),new Reward(reward.name(),1));
        List<Reward> alternatives=List.copyOf(distinct.values());
        if(alternatives.isEmpty())throw new IllegalArgumentException("At least one alternative symbol is required");
        List<Reward> rare=alternatives.stream().filter(r->guaranteedTriple(r,tripleRewards)).toList();
        List<Reward> ordinary=alternatives.stream().filter(r->!guaranteedTriple(r,tripleRewards)).toList();
        if(!rare.isEmpty()) {
            int failure=random.nextDouble()<secondChance?1:random.nextDouble()<thirdChance?2:-1;
            if(failure>=0){
                Reward teaser=rare.get(random.nextInt(rare.size()));
                if(failure==2)return new SlotOutcome(List.of(teaser,teaser,winner),2,2);
                Reward right=ordinary.isEmpty()?decoration:ordinary.get(random.nextInt(ordinary.size()));
                return new SlotOutcome(List.of(teaser,winner,right),1,1);
            }
        }
        // If both failure checks pass with ordinary server loot, show a normal spin, not a fake jackpot.
        Reward left=ordinary.isEmpty()||random.nextDouble()<matchChance?decoration:ordinary.get(random.nextInt(ordinary.size()));
        Reward right=random.nextDouble()<matchChance?decoration:alternatives.get(random.nextInt(alternatives.size()));
        if(same(left,winner)&&same(right,winner))right=alternatives.get(random.nextInt(alternatives.size()));
        return new SlotOutcome(List.of(left,winner,right),RESULT_REEL,-1);
    }
    private static void probability(double chance) {
        if(!Double.isFinite(chance)||chance<0||chance>1)throw new IllegalArgumentException("Invalid probability");
    }
    public static boolean guaranteedTriple(Reward reward,List<String> tripleRewards) {
        String name=RewardCatalog.normalize(reward.name());
        return tripleRewards.stream().filter(Objects::nonNull).map(RewardCatalog::normalize)
            .filter(key->!key.isBlank()).anyMatch(name::contains);
    }
    public Reward symbol(int reel){return symbols.get(reel);}
    public boolean nearMiss(){return failedReel>=0;}
    public boolean suspense(){return triple()||nearMiss();}
    public boolean triple(){return same(symbol(0),symbol(1))&&same(symbol(1),symbol(2));}
    public static boolean same(Reward left,Reward right){return RewardCatalog.normalize(left.name()).equals(RewardCatalog.normalize(right.name()));}
}
