package dev.nucleusreel.bonus;

import dev.nucleusreel.RewardIcons;
import dev.nucleusreel.core.Reward;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Small bounded set of animated mining props. Textures are resolved by Minecraft's active packs. */
public final class CaveLife {
    private static final Identifier STONE=Identifier.withDefaultNamespace("textures/block/stone.png");
    private static final Identifier MOSS=Identifier.withDefaultNamespace("textures/block/mossy_cobblestone.png");
    private final ItemStack jade=RewardIcons.forReward(new Reward("Perfect Jade Gemstone",1));
    private final ItemStack amber=RewardIcons.forReward(new Reward("Perfect Amber Gemstone",1));
    private final ItemStack amethyst=RewardIcons.forReward(new Reward("Perfect Amethyst Gemstone",1));
    private final ItemStack fragment=RewardIcons.forReward(new Reward("Divan Fragment",1));
    private final ItemStack lantern=new ItemStack(Items.LANTERN), cart=new ItemStack(Items.MINECART);

    public void render(GuiGraphicsExtractor g,float time,boolean room,boolean effects) {
        // Sparse texture panels leave room for the cached voxel masonry and open tunnels.
        for(int x:new int[]{18,210,374,550})for(int y:new int[]{129,165,201}) {
            if(room&&(x==210||x==374))continue;
            g.blit(RenderPipelines.GUI_TEXTURED,y==201?MOSS:STONE,x,y,0f,0f,26,30,16,16,16,16);
            g.fill(x,y,x+26,y+30,0xA009141C);
        }
        int[] colors={0xFF69DE9F,0xFFFFBD62,0xFFBD8FF5};
        for(int i=0;i<3;i++) {
            int x=room?70+i*230:136+i*164;
            float pulse=effects?(float)Math.sin(time*0.035+i)*0.5f+0.5f:0.5f;
            if(!room) {
                g.fill(x-18,114,x+18,116,alpha(colors[i],(int)(80+pulse*100)));
                item(g,i==0?jade:i==1?amber:amethyst,x,122,0.75f,0);
            }
        }
        item(g,jade,42,295,1.7f,-0.25f);item(g,amethyst,560,293,1.8f,0.3f);
        item(g,room?fragment:amber,room?465:70,room?282:310,1.05f,0.15f);
        if(!room) {
            float travel=effects?(float)Math.sin(time*0.014)*7:0;
            item(g,cart,136+travel*0.2f,253+travel,1.05f+travel*0.01f,0);
        }
        for(int side:new int[]{0,1}) {
            int x=side==0?66:534;
            float angle=effects?(float)Math.sin(time*0.035+side)*0.055f:0;
            g.fill(x,57,x+1,90,0xFF68665B);
            item(g,lantern,x,95,1.25f,angle);
        }
        if(!effects)return;
        for(int i=0;i<14;i++) {
            float phase=(time*(0.0015f+i%3*0.0003f)+i*0.071f)%1;
            int x=30+(i*97%540)+(int)(Math.sin(time*0.012+i)*4),y=330-(int)(phase*220);
            g.fill(x,y,x+1,y+1,alpha(0xFFD4D0A9,(int)(70*Math.sin(phase*Math.PI))));
        }
        for(int i=0;i<3;i++) {
            float phase=(time+i*51)%180;
            if(phase<36) {int x=77+i*214,y=62+(int)(phase*phase*0.15f);g.fill(x,y,x+1,y+4,0x80669EB2);}
        }
        // Tiny bat silhouette; no entity renderer or world object is needed.
        float bat=time%420;
        if(bat<120) {
            int x=(int)(35+bat*4.4f),y=64+(int)(Math.sin(bat*0.07)*8),wing=(int)(Math.sin(time*0.5)*3);
            g.fill(x-2,y-1,x+2,y+3,0xFF060A10);
            g.fill(x-8,y+wing,x-2,y+wing+2,0xFF060A10);g.fill(x+2,y+wing,x+8,y+wing+2,0xFF060A10);
        }
    }
    private static void item(GuiGraphicsExtractor g,ItemStack stack,float x,float y,float size,float rotation) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().rotate(rotation);g.pose().scale(size);g.fakeItem(stack,-8,-8);g.pose().popMatrix();
    }
    private static int alpha(int color,int a){return (Math.clamp(a,0,255)<<24)|(color&0xFFFFFF);}
}
