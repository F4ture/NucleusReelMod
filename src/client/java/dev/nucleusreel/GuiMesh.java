package dev.nucleusreel;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.nucleusreel.core.CabinetFracture;
import dev.nucleusreel.core.CabinetFracture.Point;
import dev.nucleusreel.mixin.GuiGraphicsAccessor;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import org.joml.Matrix3x2f;
import java.util.*;

/** Many ordered coloured polygons submitted as ONE GUI element, not thousands of tiny rectangles. */
public final class GuiMesh {
    private final float[] xy;
    private final int[] colors;
    private final ScreenRectangle localBounds;
    private GuiMesh(float[] xy,int[] colors,ScreenRectangle bounds){this.xy=xy;this.colors=colors;localBounds=bounds;}
    public int vertexCount(){return colors.length;}
    public void draw(GuiGraphicsExtractor g){
        if(colors.length==0)return;
        Matrix3x2f pose=new Matrix3x2f(g.pose());
        ScreenRectangle bounds=localBounds.transformMaxBounds(pose);
        if(bounds.right()<0||bounds.left()>g.guiWidth()||bounds.bottom()<0||bounds.top()>g.guiHeight())return;
        ((GuiGraphicsAccessor)g).nucleusreel$getRenderState().addGuiElement(new State(this,pose,bounds));
    }
    private record State(GuiMesh mesh,Matrix3x2f pose,ScreenRectangle bounds) implements GuiElementRenderState {
        public RenderPipeline pipeline(){return RenderPipelines.GUI;}
        public TextureSetup textureSetup(){return TextureSetup.noTexture();}
        public ScreenRectangle scissorArea(){return null;}
        public void buildVertices(VertexConsumer consumer){
            for(int i=0;i<mesh.colors.length;i++)
                consumer.addVertexWith2DPose(pose,mesh.xy[i*2],mesh.xy[i*2+1]).setColor(mesh.colors[i]);
        }
    }
    public static final class Builder {
        private float[] xy=new float[256];private int[] colors=new int[128];private int size;
        private float minX=Float.POSITIVE_INFINITY,minY=minX,maxX=Float.NEGATIVE_INFINITY,maxY=maxX;
        public boolean isEmpty(){return size==0;}
        private void vertex(Point p,int color){
            if(size==colors.length){colors=Arrays.copyOf(colors,size*2);xy=Arrays.copyOf(xy,size*4);}
            xy[size*2]=p.x();xy[size*2+1]=p.y();colors[size++]=color;
            minX=Math.min(minX,p.x());minY=Math.min(minY,p.y());maxX=Math.max(maxX,p.x());maxY=Math.max(maxY,p.y());
        }
        public void polygon(List<Point> polygon,int color){
            if(polygon.size()<3||(color>>>24)==0)return;
            // GUI pipeline expects quads. A triangle is a quad with the last vertex repeated.
            // Reverse winding to match Minecraft's own ColoredRectangleRenderState.
            if(polygon.size()==4){
                vertex(polygon.get(0),color);vertex(polygon.get(3),color);vertex(polygon.get(2),color);vertex(polygon.get(1),color);
            }else for(int i=1;i<polygon.size()-1;i++){
                vertex(polygon.get(0),color);vertex(polygon.get(i+1),color);vertex(polygon.get(i),color);vertex(polygon.get(i),color);
            }
        }
        public void line(Point a,Point b,float width,int color,List<Point> clip){
            float dx=b.x()-a.x(),dy=b.y()-a.y(),length=(float)Math.hypot(dx,dy);
            if(length<0.001)return;
            float nx=-dy/length*width/2,ny=dx/length*width/2;
            var polygon=List.of(new Point(a.x()-nx,a.y()-ny),new Point(b.x()-nx,b.y()-ny),
                new Point(b.x()+nx,b.y()+ny),new Point(a.x()+nx,a.y()+ny));
            polygon(clip==null?polygon:CabinetFracture.clip(polygon,clip),color);
        }
        public GuiMesh build(){
            ScreenRectangle bounds=size==0?ScreenRectangle.empty():new ScreenRectangle((int)Math.floor(minX),(int)Math.floor(minY),
                (int)Math.ceil(maxX)-(int)Math.floor(minX),(int)Math.ceil(maxY)-(int)Math.floor(minY));
            return new GuiMesh(Arrays.copyOf(xy,size*2),Arrays.copyOf(colors,size),bounds);
        }
    }
}
