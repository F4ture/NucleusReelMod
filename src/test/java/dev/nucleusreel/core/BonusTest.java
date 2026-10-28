package dev.nucleusreel.core;

import java.util.List;
import java.util.Random;

public final class BonusTest {
    public static void main(String[] args) {
        Random random=new Random(20261028);
        Reward alloy=new Reward("Divan's Alloy",2), ordinary=new Reward("Fine Jade Gemstone",32);
        int wins=0,empty=0,lateMisses=0;
        var rare=SlotOutcome.DEFAULT_TRIPLE_REWARDS;
        for(int i=0;i<100000;i++) {
            var win=SlotOutcome.create(alloy,RewardCatalog.PREVIEW_REWARDS,0.25,rare,0.4,0.5,random);
            var plan=BonusPlan.choose(alloy,ordinary,win,rare,true,0.5,0.5,BonusPlan.Preview.AUTO,random);
            if(plan.active()) {
                wins++;
                if(!plan.won()||plan.reels().failedReel()!=2||plan.reels().triple()
                    ||!SlotOutcome.same(plan.reels().symbol(0),alloy)||!SlotOutcome.same(plan.reels().symbol(1),alloy)
                    ||SlotOutcome.same(plan.reels().symbol(2),alloy))throw new AssertionError("Alloy bonus must stage a fixed third miss");
                if(!plan.reels().symbol(2).equals(ordinary))throw new AssertionError("Fake miss did not preserve actual ordinary item/count");
            } else if(!plan.reels().triple())throw new AssertionError("Alloy without bonus must retain the normal win");
            var base=SlotOutcome.create(ordinary,RewardCatalog.PREVIEW_REWARDS,0.25,rare,0.4,0.5,random);
            if(base.failedReel()==2)lateMisses++;
            var lose=BonusPlan.choose(ordinary,base,rare,true,0.5,0.5,BonusPlan.Preview.AUTO,random);
            if(lose.active()) {
                empty++;
                if(lose.won()||base.failedReel()!=2||!lose.reels().symbol(2).equals(ordinary))
                    throw new AssertionError("Empty bonus eligibility/reward changed");
            }
        }
        if(Math.abs(wins-50000)>700||Math.abs(empty/(double)lateMisses-0.5)>0.012)
            throw new AssertionError("Incorrect bonus probabilities");
        for(Reward reward:List.of(new Reward("Quick Claw",1),new Reward("Jaderald",1))) {
            var original=SlotOutcome.create(reward,RewardCatalog.PREVIEW_REWARDS,0.25,random);
            if(BonusPlan.choose(reward,original,rare,true,1,1,BonusPlan.Preview.AUTO,random).active())
                throw new AssertionError("Other real rare rewards must not become empty bonuses");
        }
        var normal=SlotOutcome.create(ordinary,RewardCatalog.PREVIEW_REWARDS,0.25,rare,0,1,random);
        if(BonusPlan.choose(ordinary,normal,rare,false,1,1,BonusPlan.Preview.AUTO,random).active())
            throw new AssertionError("Disabled bonus still started");
        if(BonusPlan.choose(ordinary,normal,rare,true,1,1,BonusPlan.Preview.OFF,random).active())
            throw new AssertionError("Deterministic normal test became a bonus");
        if(!BonusPlan.choose(ordinary,normal,rare,false,0,0,BonusPlan.Preview.EMPTY,random).active())
            throw new AssertionError("Forced bonus test not independent of settings");
        try { BonusPlan.choose(ordinary,normal,rare,true,1,1,BonusPlan.Preview.WIN,random);throw new AssertionError("Fake Alloy awarded"); }
        catch(IllegalArgumentException expected) { }
        if(!RewardPriority.forAnimation(List.of(new Reward("Quick Claw",1),alloy),List.of("Quick Claw","Divan"),List.of("Quick Claw")).equals(alloy))
            throw new AssertionError("Actual bundle Alloy must reach the bonus decision");
        for(boolean won:new boolean[]{false,true}) for(int door=0;door<3;door++) {
            var time=new BonusTimeline(won);
            if(time.choose(door))throw new AssertionError("Choice accepted during fade-in");
            for(int tick=0;tick<15000;tick++)time.tick();
            if(time.stage()!=BonusTimeline.Stage.CHOICE||time.revealed()||time.door()!=-1)
                throw new AssertionError("Waiting must not select a path or reveal the reward");
            if(time.choose(-1)||time.choose(3)||!time.choose(door)||time.choose((door+1)%3))
                throw new AssertionError("Invalid/repeated path choice");
            for(int t=0;t<BonusTimeline.WALK;t++)time.tick();
            if(time.stage()!=BonusTimeline.Stage.ROOM||time.revealed()||time.door()!=door||time.won()!=won)
                throw new AssertionError("Path changed the destination outcome");
            for(int t=0;t<BonusTimeline.REVEAL-1;t++)time.tick();
            if(time.revealed())throw new AssertionError("Chat/reward revealed early");
            time.tick();if(!time.revealed())throw new AssertionError("Pedestal reveal did not release the reward");
            if(time.light(0)!=1||time.light(BonusTimeline.LIGHTS_OUT)!=(won?1:0))throw new AssertionError("Torches did not fade correctly");
            for(int t=BonusTimeline.REVEAL;t<(won?BonusTimeline.WIN_END:BonusTimeline.EMPTY_END);t++)time.tick();
            if(time.stage()!=BonusTimeline.Stage.DONE||!time.revealed())throw new AssertionError("Bonus did not finish");
            if(BonusTimeline.hit(BonusTimeline.doorX(door),170)!=door)throw new AssertionError("Door hit target");
        }
        if(BonusTimeline.hit(20,180)!=-1||BonusTimeline.hit(300,350)!=-1)throw new AssertionError("Choice outside a tunnel");
        Reward fragment=new Reward("Divan Fragment",2), sapphire=new Reward("Flawless Sapphire Gemstone",1);
        var bundle=List.of(alloy,new Reward("Quick Claw",1),ordinary,fragment,sapphire);
        Reward picked=RewardPriority.ordinary(bundle,RewardPriority.DEFAULT,rare);
        if(!sapphire.equals(picked))throw new AssertionError("Highest-priority actual common drop was not selected");
        var testWin=SlotOutcome.create(alloy,List.of(),0,random);
        var actualMiss=BonusPlan.choose(alloy,picked,testWin,rare,true,1,0,BonusPlan.Preview.AUTO,random);
        if(!actualMiss.reels().symbol(2).equals(sapphire))throw new AssertionError("Displayed miss is not from this bundle");
        if(!RewardPriority.ordinary(bundle,List.of("Divan Fragment","Flawless"),rare).equals(fragment))
            throw new AssertionError("Ordinary reward priority ignored");
        if(RewardPriority.ordinary(List.of(alloy,new Reward("Jaderald",1)),RewardPriority.DEFAULT,rare)!=null)
            throw new AssertionError("A rare item became a common miss");
        if(BonusPlan.choose(alloy,null,testWin,rare,true,1,1,BonusPlan.Preview.AUTO,random).active())
            throw new AssertionError("Missing ordinary loot must not be fabricated");
        if(MiningTimeline.END<=40||BonusTimeline.WALK<=70||BonusTimeline.ENTER<=20)
            throw new AssertionError("Transition and walk must be slower than the previous release");
        float darkness=0;
        for(int t=0;t<=MiningTimeline.END;t++) {
            float next=MiningTimeline.darkness(t);
            if(next<darkness||next<0||next>1||!Float.isFinite(next))throw new AssertionError("Invalid transition fade");
            if(t<=MiningTimeline.HITS.getLast()&&next!=0)throw new AssertionError("Darkness hid the mining impacts");
            darkness=next;
        }
        if(darkness!=1||MiningTimeline.cracks(MiningTimeline.OPEN)!=3)throw new AssertionError("Mining handoff is incomplete");
        if(BonusTimeline.flightAge(BonusTimeline.FLIGHT_START)!=FinaleTimeline.ALLOY_BREAK
            ||BonusTimeline.flightAge(BonusTimeline.WIN_END)!=240
            ||BonusTimeline.WIN_END<=BonusTimeline.FLIGHT_START+FinaleTimeline.ALLOY_CATCH-FinaleTimeline.ALLOY_BREAK)
            throw new AssertionError("Alloy continuation cut off sun, fall or catch");
        java.util.Set<Integer> colors=new java.util.HashSet<>();
        for(String gem:List.of("Ruby","Amber","Jade","Sapphire","Amethyst","Topaz","Jasper"))
            colors.add(RewardColor.of(new Reward("Fine "+gem+" Gemstone",1)));
        if(colors.size()!=7||RewardColor.of(new Reward("Fine Ruby Gemstone",1))==RewardColor.of(ordinary))
            throw new AssertionError("Gem highlights must follow the actual gem hue");
        System.out.println("100,000 Alloy runs: "+wins+" bonus wins. 100,000 ordinary runs: "+lateMisses+" third misses, "+empty+" empty bonuses. Six routes, indefinite choice, input lock, torch fade and reveal deadlines verified.");
    }
}
