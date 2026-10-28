package dev.nucleusreel.bonus;

import dev.nucleusreel.GuiMesh;
import dev.nucleusreel.RewardIcons;
import dev.nucleusreel.core.CabinetFracture;
import dev.nucleusreel.core.CabinetFracture.Point;
import dev.nucleusreel.core.FinaleTimeline;
import dev.nucleusreel.core.MiningTimeline;
import dev.nucleusreel.core.Reward;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
import java.util.List;

/** Three cached fracture stages, bounded chips and a native pack-aware pickaxe. */
public final class MiningTransition {
    private final ItemStack pickaxe = RewardIcons.forReward(new Reward("Pickonimbus 2000", 1));
    private final GuiMesh[] cracks = new GuiMesh[3];
    private final GuiMesh hole;
    public MiningTransition() {
        float[][] lines={{0,0,-18,-8,-25,-28},{0,0,12,-13,33,-21,47,-28},{0,0,24,5,37,18,53,20},
            {0,0,-15,13,-32,17,-42,28},{0,0,5,12,2,28},{-18,-8,-38,-6,-53,-15}};
        for(int stage=0;stage<3;stage++) {
            var b=new GuiMesh.Builder();
            for(int i=0;i<2+stage*2;i++)for(int j=0;j<lines[i].length-2;j+=2) {
                Point from=new Point(lines[i][j],lines[i][j+1]),to=new Point(lines[i][j+2],lines[i][j+3]);
                b.line(from,to,4,0xAAE5E4D6,CabinetFracture.rectangle(-53,-28,53,28));
                b.line(from,to,2,0xFF070C12,CabinetFracture.rectangle(-53,-28,53,28));
            }
            cracks[stage]=b.build();
        }
        var b=new GuiMesh.Builder();
        // Pixel-stepped aperture, expanded by the pose instead of rebuilding polygons every frame.
        CaveGeometry.rect(b,-1,-0.65f,1,0.65f,0xFF010306);
        CaveGeometry.rect(b,-0.8f,-0.85f,0.8f,0.85f,0xFF010306);
        CaveGeometry.rect(b,-0.5f,-1,0.5f,1,0xFF010306);
        hole=b.build();
    }
    public void render(GuiGraphicsExtractor g,float age,int resultColor,boolean effects) {
        if(age<24)return;
        g.nextStratum();g.pose().pushMatrix();g.pose().translate(118,0);
        int stage=MiningTimeline.cracks(age);
        if(stage>0)cracks[stage-1].draw(g);
        if(age>=MiningTimeline.OPEN) {
            float open=MiningTimeline.hole(age);
            g.pose().pushMatrix();g.pose().scale(18+650*open,10+500*open);hole.draw(g);g.pose().popMatrix();
        }
        if(effects)for(int hit:MiningTimeline.HITS) {
            float t=age-hit;
            if(t<0||t>26)continue;
            for(int i=0;i<12;i++) {
                float vx=((i*17%19)-9)*0.48f;
                int x=Math.round(vx*t),y=Math.round(-t*(1.5f+i%4*0.3f)+t*t*0.16f);
                int color=i%3==0?resultColor:0xFF8D8E82;
                g.fill(x-2,y-2,x+3,y+2,alpha(color,(int)(255*(1-t/26))));
            }
        }
        if(age<112) {
            float enter=FinaleTimeline.phase(age,24,32),swing=MiningTimeline.swing(age);
            // Bring the head of the diagonal pickaxe onto the crack origin, not its handle.
            g.pose().pushMatrix();g.pose().translate(62-91*swing+40*(1-enter),-65+68*swing);
            g.pose().rotate(-0.85f+1.55f*swing);g.pose().scale(4.1f);
            g.fakeItem(pickaxe,-8,-8);g.pose().popMatrix();
        }
        g.pose().popMatrix();
    }
    private static int alpha(int color,int a){return (Math.clamp(a,0,255)<<24)|(color&0xFFFFFF);}
}
