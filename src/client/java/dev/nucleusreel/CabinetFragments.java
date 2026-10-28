package dev.nucleusreel;

import dev.nucleusreel.core.CabinetFracture;
import dev.nucleusreel.core.CabinetFracture.Point;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fStack;

import java.util.*;
import java.util.function.Consumer;

/** Records the real cabinet once and clips its paint to the same polygons as its cracks.
 * Item models remain attached to their fragment and still use the active resource pack. */
public final class CabinetFragments {
    private interface Paint { void draw(GuiGraphicsExtractor g); }
    private record MeshPaint(GuiMesh mesh) implements Paint {
        public void draw(GuiGraphicsExtractor g){mesh.draw(g);}
    }
    private record Icon(ItemStack stack,Matrix3x2f pose,int x,int y,List<Point> clip) implements Paint {
        public void draw(GuiGraphicsExtractor g) {
            beginClip(g,clip); g.pose().pushMatrix();g.pose().mul(pose);
            g.fakeItem(stack,x,y);g.pose().popMatrix();endClip(g,clip);
        }
    }
    private record Text(Font font,String text,Matrix3x2f pose,int x,int y,int color,List<Point> clip) implements Paint {
        public void draw(GuiGraphicsExtractor g) {
            beginClip(g,clip);g.pose().pushMatrix();g.pose().mul(pose);
            g.centeredText(font,text,x,y,color);g.pose().popMatrix();endClip(g,clip);
        }
    }
    private static final Paint LAYER = GuiGraphicsExtractor::nextStratum;
    private final List<List<Paint>> pieces = new ArrayList<>();
    private final boolean[] itemInLayer = new boolean[CabinetFracture.SHARDS.size()];
    private int meshCount;
    private int vertexCount;
    private final GuiMesh.Builder[] pending = new GuiMesh.Builder[CabinetFracture.SHARDS.size()];
    public CabinetFragments(Consumer<GuiGraphicsExtractor> machine) {
        for(var ignored:CabinetFracture.SHARDS)pieces.add(new ArrayList<>());
        for(int i=0;i<pending.length;i++)pending[i]=new GuiMesh.Builder();
        machine.accept(new Recorder());
        for(var shard:CabinetFracture.SHARDS){
            flush(shard.id());
            pieces.get(shard.id()).add(LAYER);
            GuiMesh.Builder edge=new GuiMesh.Builder();
            var polygon=shard.polygon();
            for(int i=0;i<polygon.size();i++)edge.line(polygon.get(i),polygon.get((i+1)%polygon.size()),2.4f,
                0xFF11150F,CabinetFracture.rectangle(-246,-178,246,180));
            addMesh(shard.id(),edge);
        }
    }
    public int meshCount(){return meshCount;}
    public int vertexCount(){return vertexCount;}
    private void addMesh(int id,GuiMesh.Builder builder){
        if(builder.isEmpty())return;
        GuiMesh mesh=builder.build();pieces.get(id).add(new MeshPaint(mesh));meshCount++;vertexCount+=mesh.vertexCount();
    }
    private void flush(int id){addMesh(id,pending[id]);pending[id]=new GuiMesh.Builder();}
    public void render(GuiGraphicsExtractor g,float age) {
        if(age>=82)return;
        for(var shard:CabinetFracture.SHARDS) {
            var motion=CabinetFracture.motion(shard,age);
            g.nextStratum();g.pose().pushMatrix();
            g.pose().translate(shard.centre().x()+motion.x(),shard.centre().y()+motion.y());
            g.pose().rotate(motion.rotation());g.pose().translate(-shard.centre().x(),-shard.centre().y());
            for(var paint:pieces.get(shard.id()))paint.draw(g);
            g.pose().popMatrix();
        }
    }
    private static Point transform(Matrix3x2f matrix,float x,float y) {
        return new Point(matrix.m00()*x+matrix.m10()*y+matrix.m20(),matrix.m01()*x+matrix.m11()*y+matrix.m21());
    }
    private static List<Point> rect(Matrix3x2f matrix,int l,int t,int r,int b) {
        return List.of(transform(matrix,l,t),transform(matrix,r,t),transform(matrix,r,b),transform(matrix,l,b));
    }
    private static void beginClip(GuiGraphicsExtractor g,List<Point> clip) {
        if(clip==null)return;
        if(clip.isEmpty()){g.enableScissor(0,0,0,0);return;}
        float l=Float.POSITIVE_INFINITY,t=l,r=Float.NEGATIVE_INFINITY,b=r;
        for(var p:clip){l=Math.min(l,p.x());r=Math.max(r,p.x());t=Math.min(t,p.y());b=Math.max(b,p.y());}
        g.enableScissor((int)Math.floor(l),(int)Math.floor(t),(int)Math.ceil(r),(int)Math.ceil(b));
    }
    private static void endClip(GuiGraphicsExtractor g,List<Point> clip){if(clip!=null)g.disableScissor();}
    private void fill(List<Point> polygon,int color) {
        for(var shard:CabinetFracture.SHARDS) {
            var clipped=CabinetFracture.clip(polygon,shard.polygon());
            if(clipped.size()>=3)pending[shard.id()].polygon(clipped,color);
        }
    }
    private final class Recorder extends GuiGraphicsExtractor {
        private final Matrix3x2fStack matrix = new Matrix3x2fStack(32);
        private final List<List<Point>> clips=new ArrayList<>();
        Recorder(){super(Minecraft.getInstance(),new GuiRenderState(),600,400);}
        @Override public Matrix3x2fStack pose(){return matrix;}
        private List<Point> clip(){return clips.isEmpty()?null:clips.getLast();}
        @Override public void enableScissor(int l,int t,int r,int b){
            var next=rect(matrix,l,t,r,b);
            if(clip()!=null)next=CabinetFracture.clip(next,clip());
            clips.add(next);
        }
        @Override public void disableScissor(){clips.removeLast();}
        @Override public void fill(int l,int t,int r,int b,int color){
            if((color>>>24)==0||l>=r||t>=b)return;
            var polygon=rect(matrix,l,t,r,b);
            if(clip()!=null)polygon=CabinetFracture.clip(polygon,clip());
            if(polygon.size()>2)CabinetFragments.this.fill(polygon,color);
        }
        @Override public void fakeItem(ItemStack stack,int x,int y){
            int owner=CabinetFracture.owner(transform(matrix,x+8,y+8));
            if(owner<0)return;
            flush(owner);
            pieces.get(owner).add(new Icon(stack,new Matrix3x2f(matrix),x,y,clip()));
            itemInLayer[owner]=true;
        }
        @Override public void centeredText(Font font,String text,int x,int y,int color){
            if(text.isEmpty())return;
            int owner=CabinetFracture.owner(transform(matrix,x,y));
            if(owner>=0){flush(owner);pieces.get(owner).add(new Text(font,text,new Matrix3x2f(matrix),x,y,color,clip()));}
        }
        @Override public void nextStratum(){
            for(int i=0;i<pieces.size();i++)if(itemInLayer[i]){flush(i);pieces.get(i).add(LAYER);itemInLayer[i]=false;}
        }
    }
}
