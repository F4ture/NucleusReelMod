package dev.nucleusreel.core;

import java.util.*;

/** A single convex fracture mesh supplies both the visible cracks and the falling pieces. */
public final class CabinetFracture {
    public record Point(float x, float y) {}
    public record Edge(Point a, Point b) {}
    public record Shard(int id, Point centre, List<Point> polygon) {
        public Shard { polygon = List.copyOf(polygon); }
    }
    public record Motion(float x, float y, float rotation) {}
    public static final List<Point> BOUNDS = rectangle(-258, -188, 287, 192);
    public static final List<Shard> SHARDS = create();
    public static final List<Edge> EDGES = edges();
    private CabinetFracture() {}

    private static List<Shard> create() {
        Random random = new Random(7102026);
        List<Point> seeds = new ArrayList<>();
        int[][] anchors={{-231,-158},{-137,-152},{-37,-160},{76,-146},{205,-151},{264,-60},
            {-192,-64},{-78,-83},{34,-70},{159,-60},{-240,27},{-118,0},{0,0},{118,0},{218,41},
            {-195,100},{-91,70},{44,83},{150,109},{249,153},{-235,165},{-128,161},{-13,151},{84,166},{188,178}};
        for(var anchor:anchors)seeds.add(new Point(anchor[0]+random.nextInt(13)-6,anchor[1]+random.nextInt(11)-5));
        List<Shard> shards = new ArrayList<>();
        for(int i=0;i<seeds.size();i++) {
            Point seed=seeds.get(i); List<Point> polygon=BOUNDS;
            for(Point other:seeds) if(other!=seed) {
                float nx=other.x-seed.x, ny=other.y-seed.y;
                float d=(other.x*other.x+other.y*other.y-seed.x*seed.x-seed.y*seed.y)/2;
                polygon=halfPlane(polygon,nx,ny,d);
            }
            shards.add(new Shard(i,seed,polygon));
        }
        return List.copyOf(shards);
    }
    private static List<Edge> edges() {
        Map<String,Edge> unique=new LinkedHashMap<>();
        for(var shard:SHARDS) for(int i=0;i<shard.polygon.size();i++) {
            Point a=shard.polygon.get(i),b=shard.polygon.get((i+1)%shard.polygon.size());
            // Outside edges are the existing cabinet silhouette, not new cracks.
            if((Math.abs(a.x-b.x)<0.01 && (Math.abs(a.x+258)<0.01||Math.abs(a.x-287)<0.01))
                ||(Math.abs(a.y-b.y)<0.01&&(Math.abs(a.y+188)<0.01||Math.abs(a.y-192)<0.01)))continue;
            String ka=Math.round(a.x*10)+":"+Math.round(a.y*10), kb=Math.round(b.x*10)+":"+Math.round(b.y*10);
            unique.putIfAbsent(ka.compareTo(kb)<0?ka+"/"+kb:kb+"/"+ka,new Edge(a,b));
        }
        return List.copyOf(unique.values());
    }
    public static List<Point> rectangle(float l,float t,float r,float b) {
        return List.of(new Point(l,t),new Point(r,t),new Point(r,b),new Point(l,b));
    }
    public static List<Point> clip(List<Point> subject,List<Point> boundary) {
        if(boundary.size()<3)return List.of();
        List<Point> result=subject;
        for(int i=0;i<boundary.size()&&!result.isEmpty();i++) {
            Point a=boundary.get(i), b=boundary.get((i+1)%boundary.size());
            float nx=b.y-a.y,ny=a.x-b.x;
            result=halfPlane(result,nx,ny,nx*a.x+ny*a.y);
        }
        return result;
    }
    private static List<Point> halfPlane(List<Point> polygon,float nx,float ny,float d) {
        if(polygon.isEmpty())return List.of();
        List<Point> out=new ArrayList<>();
        Point a=polygon.getLast(); float da=nx*a.x+ny*a.y-d;
        for(Point b:polygon) {
            float db=nx*b.x+ny*b.y-d;
            boolean inA=da<=0.001f,inB=db<=0.001f;
            if(inA!=inB) {
                float t=da/(da-db);
                out.add(new Point(a.x+(b.x-a.x)*t,a.y+(b.y-a.y)*t));
            }
            if(inB)out.add(b);
            a=b;da=db;
        }
        return out;
    }
    public static boolean contains(Shard shard,Point p) {
        var polygon=shard.polygon;
        for(int i=0;i<polygon.size();i++) {
            Point a=polygon.get(i),b=polygon.get((i+1)%polygon.size());
            if((b.x-a.x)*(p.y-a.y)-(b.y-a.y)*(p.x-a.x)<-0.01f)return false;
        }
        return true;
    }
    public static int owner(Point p) {
        for(var shard:SHARDS)if(contains(shard,p))return shard.id;
        return -1;
    }
    public static Motion motion(Shard shard,float age) {
        float t=Math.max(0,age-FinaleTimeline.ALLOY_BREAK-(shard.id*7%5)*0.7f);
        // No upward launch or wide lateral explosion: gravity pulls every piece down.
        float y=0.34f*t*t+(shard.id%4)*0.07f*t;
        float x=(shard.id*17%11-5)*0.11f*t;
        float rotation=(shard.id%2==0?1:-1)*0.0045f*t;
        return new Motion(x,y,rotation);
    }
}
