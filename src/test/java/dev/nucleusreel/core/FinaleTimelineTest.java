package dev.nucleusreel.core;
import java.util.HashSet;
public final class FinaleTimelineTest {
 public static void main(String[] args) {
  if(FinaleTimeline.duration(RewardCelebration.ALLOY,50)!=240
   || FinaleTimeline.duration(RewardCelebration.QUICK_CLAW,50)!=160
   || FinaleTimeline.duration(RewardCelebration.JADERALD,50)!=100
   || FinaleTimeline.duration(RewardCelebration.NORMAL,37)!=37)throw new AssertionError("Wrong finale duration");
  for(var type:RewardCelebration.values()){
   int end=FinaleTimeline.duration(type,50),last=-1;
   var ticks=new HashSet<Integer>();
   for(var cue:FinaleTimeline.cues(type)){
    if(cue.tick()<last||cue.tick()<0||cue.tick()>=end||!ticks.add(cue.tick()))throw new AssertionError("Cue outside timeline / duplicated");
    if(!Float.isFinite(cue.volume())||cue.volume()<=0||cue.volume()>1)throw new AssertionError("Invalid sound level");
    last=cue.tick();
   }
   if(type!=RewardCelebration.NORMAL && FinaleTimeline.cues(type).size()<7)throw new AssertionError("Missing cinematic sound sequence");
   if(FinaleTimeline.fade(type,end)!=0)throw new AssertionError("Finale must close");
  }
  var previous=FinaleTimeline.flight(0);
  for(float t=0.125f;t<=240;t+=0.125f){
   var f=FinaleTimeline.flight(t);
   if(!Float.isFinite(f.x()+f.y()+f.size()+f.rotation()+f.sunlight()+f.hand()))throw new AssertionError("Invalid flight");
   if(f.x()<0||f.x()>0.301f||f.y() < -0.291f||f.y()>0.281f||f.size()<=0)throw new AssertionError("Flight outside screen envelope");
   if(Math.abs(f.x()-previous.x())>0.005||Math.abs(f.y()-previous.y())>0.005)throw new AssertionError("Flight discontinuity");
   if(t<155&&f.y()>previous.y()+0.00001)throw new AssertionError("Ascension reversed");
   if(t>155&&f.y()<previous.y()-0.00001)throw new AssertionError("Descent reversed");
   previous=f;
  }
  var sky=FinaleTimeline.flight(110);
  if(sky.sunlight()!=1||Math.abs(sky.y()+0.29)>0.0001)throw new AssertionError("Sun stage missing");
  var caught=FinaleTimeline.flight(202);
  if(caught.hand()!=1||Math.abs(caught.x()-0.3)>0.0001||Math.abs(caught.y()-0.28)>0.0001)throw new AssertionError("Alloy missed the hand");
  if(FinaleTimeline.scratchProgress(18)!=0||FinaleTimeline.scratchProgress(44)!=1)throw new AssertionError("Scratch must run top to bottom");
  if(FinaleTimeline.clawReveal(60)!=0||FinaleTimeline.clawReveal(82)!=1)throw new AssertionError("Reward appears only after the scratch");
  for(float height:new float[]{200,400,800}) {
   float previousY=FinaleTimeline.pawY(0,height);
   for(float t=0.125f;t<=60;t+=0.125f) {
    float y=FinaleTimeline.pawY(t,height);
    if(!Float.isFinite(y)||y<previousY)throw new AssertionError("Paw reversed or jumped");
    previousY=y;
   }
   if(FinaleTimeline.pawY(18,height)+90*2.1f>=-height/2)throw new AssertionError("Claws must enter above the viewport");
   if(FinaleTimeline.pawY(60,height)-83*2.1f<=height/2)throw new AssertionError("Paw must leave before the reward appears");
  }
  System.out.println("Finales verified: Alloy 12 s, Quick Claw 8 s, Jaderald 5 s; unchanged sky-to-hand flight; downward paw scratch before reward reveal.");
 }
}
