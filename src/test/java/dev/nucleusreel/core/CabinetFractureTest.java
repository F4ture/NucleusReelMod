package dev.nucleusreel.core;

public final class CabinetFractureTest {
 private static double area(java.util.List<CabinetFracture.Point> points) {
  double a=0;for(int i=0;i<points.size();i++){var p=points.get(i);var q=points.get((i+1)%points.size());a+=p.x()*q.y()-q.x()*p.y();}return Math.abs(a)/2;
 }
 public static void main(String[]args){
  if(CabinetFracture.SHARDS.size()!=25)throw new AssertionError("Expected many irregular fragments");
  double total=0;
  for(var shard:CabinetFracture.SHARDS){
   total+=area(shard.polygon());
   if(!CabinetFracture.contains(shard,shard.centre()))throw new AssertionError("Decoration anchor outside its fragment");
   float previous=0;
   for(float t=0;t<=82;t+=0.25f){
    var motion=CabinetFracture.motion(shard,t);
    if(!Float.isFinite(motion.x()+motion.y()+motion.rotation())||motion.y()<previous)throw new AssertionError("Fragment flew upwards");
    if(Math.abs(motion.x())>33)throw new AssertionError("Fragment exploded sideways");
    if(t<FinaleTimeline.ALLOY_BREAK&&(motion.x()!=0||motion.y()!=0||motion.rotation()!=0))throw new AssertionError("Premature break");
    previous=motion.y();
   }
   if(CabinetFracture.motion(shard,82).y()<900)throw new AssertionError("Fragments must fall offscreen");
  }
  if(Math.abs(total-area(CabinetFracture.BOUNDS))>1)throw new AssertionError("Missing or overlapping cabinet area");
  for(float y=-187.5f;y<192;y+=5)for(float x=-257.5f;x<287;x+=5)
   if(CabinetFracture.owner(new CabinetFracture.Point(x,y))<0)throw new AssertionError("Hole in fracture mesh");
  for(var edge:CabinetFracture.EDGES){
   int shared=0;
   var mid=new CabinetFracture.Point((edge.a().x()+edge.b().x())/2,(edge.a().y()+edge.b().y())/2);
   for(var shard:CabinetFracture.SHARDS)if(CabinetFracture.contains(shard,mid))shared++;
   if(shared<2)throw new AssertionError("A visible crack does not coincide with a shared fragment boundary");
  }
  if(!CabinetFracture.clip(CabinetFracture.BOUNDS,java.util.List.of()).isEmpty())throw new AssertionError("Empty scissor must hide geometry");
  System.out.println("25-fragment mesh verified: full cabinet coverage, exact shared crack boundaries, downward gravity, no outward launch.");
 }
}
