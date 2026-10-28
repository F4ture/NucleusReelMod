package dev.nucleusreel;

import dev.nucleusreel.core.RewardCelebration;
import dev.nucleusreel.core.SlotSpin;
import dev.nucleusreel.core.FinaleTimeline;
import dev.nucleusreel.core.CabinetFracture;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Local GUI props only: never added to the world, inventory, or a network packet. */
public final class RewardFinale {
    private final RewardCelebration celebration;
    private final ItemStack reward;
    private final ItemStack emerald = new ItemStack(Items.EMERALD);
    private Villager villager;
    private CabinetFragments fragments;
    private boolean villagerUnavailable;

    public RewardFinale(RewardCelebration celebration, ItemStack reward) {
        this.celebration = celebration;
        this.reward = reward;
    }

    public void drawCabinetEffect(GuiGraphicsExtractor g, float age) {
        if (celebration != RewardCelebration.ALLOY) return;
        GuiMesh.Builder cracks = new GuiMesh.Builder();
        var clip = CabinetFracture.rectangle(-246,-178,246,180);
        for (var edge : CabinetFracture.EDGES) {
            var a=edge.a();var b=edge.b();
            float distance=(float)Math.hypot((a.x()+b.x())/2,(a.y()+b.y())/2);
            float grow=Math.clamp(age/24*1.8f-distance/340*0.8f,0,1);
            if(grow<=0)continue;
            int x=Math.round(a.x()+(b.x()-a.x())*grow),y=Math.round(a.y()+(b.y()-a.y())*grow);
            cracks.line(new CabinetFracture.Point(a.x()+1,a.y()+1),new CabinetFracture.Point(x+1,y+1),4,0xFFB89B68,clip);
            cracks.line(a,new CabinetFracture.Point(x,y),2,0xFF0A100A,clip);
        }
        cracks.build().draw(g);
    }

    public void brokenCabinet(GuiGraphicsExtractor g, float age, java.util.function.Consumer<GuiGraphicsExtractor> machine) {
        if (age >= 82) return;
        if (fragments == null) fragments = new CabinetFragments(machine);
        fragments.render(g,age);
    }


    public void render(GuiGraphicsExtractor g, float age, float scale, float cx, float cy) {
        if (celebration == RewardCelebration.NORMAL) return;
        if (celebration == RewardCelebration.ALLOY) {
            drawAlloy(g, age, scale, cx, cy);
            return;
        }
        if (age < 8) return;
        float pop = FinaleTimeline.phase(age, 8, 24) * FinaleTimeline.fade(celebration, age);
        if (pop < 0.01f) return;
        if (celebration == RewardCelebration.JADERALD) {
            emeraldRain(g, age, scale, cx, cy);
            drawVillager(g, age, scale, pop, cx, cy);
        } else drawScratch(g, age, scale, cx, cy);
    }

    private void drawAlloy(GuiGraphicsExtractor g, float age, float scale, float cx, float cy) {
        drawAlloy(g, age, scale, cx, cy, false);
    }
    /** Reuse the same sun / fall / skin-hand finale, starting continuously at the pedestal. */
    public void renderAlloyContinuation(GuiGraphicsExtractor g, float age, float scale, float cx, float cy) {
        drawAlloy(g, age, scale, cx, cy, true);
    }
    private void drawAlloy(GuiGraphicsExtractor g, float age, float scale, float cx, float cy, boolean continuation) {
        float fade = FinaleTimeline.fade(celebration, age);
        if (fade <= 0) return;
        float width = g.guiWidth() / scale, height = g.guiHeight() / scale;
        cx += (g.guiWidth() / 2f - cx) * FinaleTimeline.phase(age, 24, 80);
        if (continuation) cy += (g.guiHeight() / 2f - cy) * FinaleTimeline.phase(age, 24, 90);
        var flight = FinaleTimeline.flight(age);
        int side = Minecraft.getInstance().player != null
            && Minecraft.getInstance().player.getMainArm() == HumanoidArm.LEFT ? -1 : 1;
        float x = side * flight.x() * width, y = flight.y() * height;
        g.nextStratum();
        g.pose().pushMatrix();
        g.pose().translate(cx, cy);
        g.pose().scale(scale);
        // Warm light floods the viewport as the reward becomes a sun.
        g.fill((int) (-width / 2 - 20), (int) (-height / 2), (int) (width / 2 + 20), (int) (height / 2),
            alpha(0xFFFFCA58, (int) (flight.sunlight() * 32 * fade)));
        if (age < 24) {
            float charge = FinaleTimeline.phase(age, 0, 24);
            halo(g, 0, 0, 16 + charge * 64, 0xFFFFD76F, charge * 0.8f);
            for (int i = 0; i < 16; i++) {
                double angle = i * Math.PI / 8 + age * 0.02;
                float radius = 150 - charge * 100;
                int px = (int) (Math.cos(angle) * radius), py = (int) (Math.sin(angle) * radius);
                g.fill(px - 2, py - 2, px + 3, py + 3, 0xFFFFE7A5);
            }
        }
        float blastAge = age - FinaleTimeline.ALLOY_BREAK;
        if (blastAge >= 0 && blastAge < 70) {
            for (int ring = 0; ring < 3; ring++) {
                float radius = Math.max(0, blastAge - ring * 4) * (7 + ring);
                ring(g, 0, 0, radius, 0xFFFFD781, (1 - blastAge / 70) * 0.8f);
            }
            for (int i = 0; !continuation && i < 58; i++) {
                double angle = i * 2.399963;
                float speed = 2 + (i * 17 % 29) * 0.13f;
                float px = (float) Math.cos(angle) * (125 + blastAge * 0.28f);
                float py = (float) Math.sin(angle) * 85 + blastAge * (0.5f + i % 3 * 0.25f) + blastAge * blastAge * 0.18f;
                g.pose().pushMatrix(); g.pose().translate(px, py); g.pose().rotate(i + blastAge * 0.06f);
                int size = 3 + i % 5;
                g.fill(-size, -size / 2, size, size / 2 + 2, alpha(i % 3 == 0 ? 0xFFD7B36F : 0xFF78836B,
                    (int) (255 * (1 - blastAge / 70))));
                g.pose().popMatrix();
            }
        }
        if (age >= 24) {
            if (age < 100) for (int i = 1; i <= 15; i++) {
                float sample = Math.max(24, age - i * 1.4f);
                var tail = FinaleTimeline.flight(sample);
                float tx = tail.x() * width + (float) Math.sin(i * 1.9 + age * 0.15) * i;
                float ty = tail.y() * height + i * 3;
                int size = Math.max(1, 8 - i / 2);
                g.fill((int) tx - size, (int) ty - size, (int) tx + size, (int) ty + size,
                    alpha(0xFFFFDA7C, 170 - i * 9));
            }
            float sun = flight.sunlight();
            float radius = 48 + sun * 80 + 4 * (float) Math.sin(age * 0.08);
            halo(g, x, y, radius, 0xFFFFD565, (0.4f + sun * 0.6f) * fade);
            if (sun > 0) {
                for (int ray = 0; ray < 24; ray++) {
                    g.pose().pushMatrix(); g.pose().translate(x, y);
                    g.pose().rotate(ray * (float) Math.PI / 12 + age * 0.0035f);
                    for (int segment = 0; segment < 4; segment++) {
                        int begin = 56 + segment * 40;
                        int thickness = (ray % 3 == 0 ? 6 : 3) + segment * 2;
                        g.fill(begin, -thickness, begin + 42, thickness,
                            alpha(0xFFFFEBB5, (int) (sun * (60 - segment * 12) * fade)));
                    }
                    g.pose().popMatrix();
                }
                ring(g, x, y, 92 + (float) Math.sin(age * 0.06) * 6, 0xFFFFEDAC, sun * fade);
            }
            for (int i = 0; i < 46; i++) {
                float px = (float) Math.sin(i * 62.31) * width * 0.48f;
                float py = (float) Math.cos(i * 31.17) * height * 0.45f;
                float twinkle = (float) Math.pow((1 + Math.sin(age * 0.09 + i)) * 0.5, 3) * sun * fade;
                star(g, px, py, 2 + i % 3, alpha(0xFFFFF1BC, (int) (twinkle * 220)));
            }
            if (flight.hand() > 0) drawHand(g, side, width * 0.30f, height * 0.28f + 12, flight.hand(), fade);
            float reveal = FinaleTimeline.phase(age, 24, 36);
            float size = continuation ? 3.6f + (9f * flight.size() - 3.6f) * FinaleTimeline.phase(age, 24, 50)
                : 9f * flight.size() * reveal;
            float rotation = continuation ? flight.rotation() * FinaleTimeline.phase(age,24,42) : flight.rotation();
            item(g, reward, x, y, size * fade, rotation);
            if (age >= FinaleTimeline.ALLOY_CATCH) {
                float t = age - FinaleTimeline.ALLOY_CATCH;
                ring(g, x, y, 15 + t * 2, 0xFFFFE5A0, Math.max(0, 1 - t / 26));
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI / 5;
                    star(g, x + (float) Math.cos(a) * (18 + t), y + (float) Math.sin(a) * (18 + t),
                        2, alpha(0xFFFFDC83, (int) (Math.max(0, 1 - t / 30) * 255)));
                }
            }
            if (age > 86) title(g, "DIVAN'S ALLOY", 0, height * 0.38f, 1.5f,
                alpha(0xFFFFDB79, (int) (255 * FinaleTimeline.phase(age, 86, 102) * fade)));
        }
        g.pose().popMatrix();
    }

    private void drawHand(GuiGraphicsExtractor g, int side, float x, float y, float enter, float fade) {
        g.pose().pushMatrix();
        g.pose().translate(side * x, y + 120 * (1 - enter));
        g.pose().rotate((float) Math.PI - side * 0.28f);
        g.fill(-25, -160, 25, 2, alpha(0xFF423026, (int) (240 * fade)));
        var player = Minecraft.getInstance().player;
        if (player != null) {
            var skin = player.getSkin();
            boolean slim = skin.model() == PlayerModelType.SLIM;
            int u = side > 0 ? 44 : 36, v = side > 0 ? 20 : 52;
            int sleeveU = side > 0 ? 44 : 52, sleeveV = side > 0 ? 36 : 52;
            g.blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), -22, -156,
                (float) u, (float) v, 44, 156, slim ? 3 : 4, 12, 64, 64);
            g.blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), -22, -156,
                (float) sleeveU, (float) sleeveV, 44, 156, slim ? 3 : 4, 12, 64, 64);
        } else {
            g.fill(-22, -156, 22, -46, 0xFF358C99);
            g.fill(-22, -46, 22, 0, 0xFFD3A17B);
            g.fill(-17, -42, 17, -3, 0xFFE8BC95);
        }
        g.pose().popMatrix();
    }

    private void drawScratch(GuiGraphicsExtractor g, float age, float scale, float cx, float cy) {
        float height=g.guiHeight()/scale,fade=FinaleTimeline.fade(celebration,age);
        if(fade<=0)return;
        g.nextStratum();g.pose().pushMatrix();g.pose().translate(cx,cy);g.pose().scale(scale);
        // Each cut follows the actual tip of a claw on the descending paw.
        float[] toesX={-38,-15,15,38},tipsY={-72,-90,-90,-72};
        float scars=(1-FinaleTimeline.phase(age,72,112))*fade;
        if(age>18&&scars>0) {
            g.enableScissor((int)(-g.guiWidth()/scale),(int)(-height/2),(int)(g.guiWidth()/scale),(int)(height/2));
            for(int claw=0;claw<4;claw++) {
              for(int layer=0;layer<3;layer++) {
                for(float t=18;t<Math.min(age,44);t+=0.75f) {
                    float next=Math.min(Math.min(age,44),t+0.75f);
                    float x1=FinaleTimeline.pawX(t)-toesX[claw]*2.1f;
                    float y1=FinaleTimeline.pawY(t,height)-tipsY[claw]*2.1f;
                    float x2=FinaleTimeline.pawX(next)-toesX[claw]*2.1f;
                    float y2=FinaleTimeline.pawY(next,height)-tipsY[claw]*2.1f;
                    float taper=0.45f+0.55f*(float)Math.sin(Math.clamp((y1+height/2)/height,0,1)*Math.PI);
                    int width=Math.max(1,Math.round((layer==0?13:layer==1?6:2)*taper));
                    int color=layer==0?0xFF20131A:layer==1?0xFFE68FA7:0xFFFFEBD4;
                    int strength=layer==0?175:layer==1?210:245;
                    int offset=layer==2?-1:0;
                    line(g,(int)x1+offset,(int)y1,(int)x2+offset,(int)y2,width,alpha(color,(int)(strength*scars)));
                }
              }
            }
            g.disableScissor();
        }
        if(age>=6&&age<60) {
            float px=FinaleTimeline.pawX(age),py=FinaleTimeline.pawY(age,height);
            // Wrist at the top, fingertips point down in the direction of the scratch.
            g.pose().pushMatrix();g.pose().translate(px,py);g.pose().rotate((float)Math.PI);g.pose().scale(2.1f);
            paw(g);
            for(int i=0;i<4;i++) {
                int x=(int)toesX[i],tip=(int)tipsY[i];
                for(int row=0;row<22;row+=2) {
                    int half=1+row/4,curve=(22-row)/9;
                    g.fill(x+curve-half,tip+row,x+curve+half+1,tip+row+2,0xFF66564C);
                    if(row>2)g.fill(x+curve-half+2,tip+row,x+curve+half,tip+row+2,0xFFFFF3DB);
                }
            }
            g.pose().popMatrix();
            if(age>18&&age<44)for(int i=0;i<16;i++) {
                int claw=i%4;
                float x=px-toesX[claw]*2.1f+(float)Math.sin(i*1.8+age)*14;
                float y=py-tipsY[claw]*2.1f-8-i%4*6;
                star(g,x,y,2,0xFFFFE1BB);
            }
        }
        float reveal=FinaleTimeline.clawReveal(age);
        if(reveal>0) {
            float pop=1+(float)Math.sin(Math.min((age-60)/24,1)*Math.PI)*0.22f;
            halo(g,0,0,95,0xFFFFCFA1,reveal*fade*0.8f);
            float burst=age-60;
            if(burst<32)ring(g,0,0,25+burst*3,0xFFFFDC9C,(1-burst/32)*fade);
            item(g,reward,0,(1-reveal)*35+(float)Math.sin(age*0.08)*3,6.3f*reveal*pop*fade,(1-reveal)*-0.35f);
            title(g,"QUICK CLAW",0,131,1.65f,alpha(0xFFFFDA89,(int)(255*reveal*fade)));
            for(int i=0;i<18;i++) {
                double angle=i*Math.PI/9+age*0.012;
                float radius=68+(float)Math.sin(age*0.07+i)*9;
                star(g,(float)Math.cos(angle)*radius,(float)Math.sin(angle)*radius,2,
                    alpha(i%2==0?0xFFFFBDD0:0xFFFFDE9C,(int)(190*reveal*fade)));
            }
        }
        g.pose().popMatrix();
    }


    private void emeraldRain(GuiGraphicsExtractor g,float age,float scale,float cx,float cy) {
        float width=g.guiWidth()/scale, height=g.guiHeight()/scale;
        float fade=FinaleTimeline.fade(celebration,age);
        g.nextStratum(); g.pose().pushMatrix(); g.pose().translate(cx,cy); g.pose().scale(scale);
        for(int i=0;i<42;i++) {
            float t=age-8-i*0.55f;
            if(t<0) continue;
            float speed=4.4f+(i*13%17)*0.23f, cycle=(height+70)/speed;
            float phase=t%cycle;
            float x=(((i*0.618034f)%1)-0.5f)*(width+30)+(float)Math.sin(t*0.035+i)*10;
            float y=-height/2-30+phase*speed;
            item(g,emerald,x,y,(0.65f+(i%4)*0.18f)*fade,(i%2==0?1:-1)*t*0.035f);
        }
        g.pose().popMatrix();
    }

    private static void title(GuiGraphicsExtractor g,String text,float x,float y,float size,int color) {
        g.pose().pushMatrix(); g.pose().translate(x,y); g.pose().scale(size);
        g.centeredText(Minecraft.getInstance().font,text,0,-4,color); g.pose().popMatrix();
    }
    private static void star(GuiGraphicsExtractor g,float x,float y,int radius,int color) {
        g.fill((int)x-1,(int)y-radius,(int)x+1,(int)y+radius+1,color);
        g.fill((int)x-radius,(int)y-1,(int)x+radius+1,(int)y+1,color);
    }
    private static void halo(GuiGraphicsExtractor g,float x,float y,float radius,int color,float brightness) {
        if(brightness<=0)return;
        for(int layer=8;layer>=1;layer--) {
            int r=(int)(radius*layer/8);
            for(int row=-r;row<r;row+=6) {
                int half=(int)Math.sqrt(Math.max(0,r*r-(row+3)*(row+3)));
                g.fill((int)x-half,(int)y+row,(int)x+half,(int)y+Math.min(row+6,r),
                    alpha(color,(int)((7+(8-layer)*2)*brightness)));
            }
        }
    }
    private static void ring(GuiGraphicsExtractor g,float x,float y,float radius,int color,float opacity) {
        if(opacity<=0)return;
        for(int i=0;i<80;i++) {
            double angle=i*Math.PI/40;
            int px=Math.round(x+(float)Math.cos(angle)*radius),py=Math.round(y+(float)Math.sin(angle)*radius);
            g.fill(px-1,py-1,px+2,py+2,alpha(color,(int)(200*opacity)));
        }
    }


    private void drawVillager(GuiGraphicsExtractor g, float age, float scale, float pop, float cx, float cy) {
        float size = scale * pop;
        // Avoid a zero-size offscreen entity target during the first fractional animation frames.
        if (size < 0.05f) return;
        float bob = (float) Math.sin(age * 0.15) * 2 * scale;
        if (villager == null && !villagerUnavailable && Minecraft.getInstance().level != null) {
            try {
                villager = new Villager(EntityType.VILLAGER, Minecraft.getInstance().level);
                villager.setNoAi(true);
                villager.setSilent(true);
                villager.setNoGravity(true);
                villager.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.EMERALD, 64));
            } catch (RuntimeException exception) {
                villagerUnavailable = true;
                NucleusReelClient.LOGGER.warn("Could not create the decorative GUI villager", exception);
            }
        }
        g.nextStratum();
        if (villager != null && !villagerUnavailable) {
            try {
                // Absolute GUI coordinates: inventory entity rendering does not need the cabinet's camera transform.
                InventoryScreen.extractEntityInInventoryFollowsMouse(g,
                    Math.round(cx - 74 * size), Math.round(cy - 103 * size + bob),
                    Math.round(cx + 74 * size), Math.round(cy + 97 * size + bob),
                    Math.max(1, Math.round(78 * size)), 0,
                    cx + (float) Math.sin(age * 0.08) * 12 * size, cy - 8 * size, villager);
            } catch (RuntimeException exception) {
                villagerUnavailable = true;
                NucleusReelClient.LOGGER.warn("Could not render the decorative GUI villager", exception);
            }
        }
        g.nextStratum();
        g.pose().pushMatrix();
        g.pose().translate(cx, cy + bob);
        g.pose().scale(size);
        if (villagerUnavailable || villager == null) fallbackVillager(g);
        // A visibly overflowing armful, using the active resource pack's emerald model.
        for (int row = 0; row < 3; row++) for (int column = 0; column < 5 - row; column++) {
            float x = (column - (4 - row) / 2f) * 15;
            float y = 27 - row * 12;
            item(g, emerald, x, y, 1.35f, (column % 2 == 0 ? -1 : 1) * 0.18f);
        }
        for (int i = 0; i < 5; i++) {
            float t = (age * 0.018f + i * 0.2f) % 1;
            float x = (i % 2 == 0 ? -1 : 1) * (46 + i * 3);
            item(g, emerald, x, 65 - 145 * t, 0.65f, t * 2 + i);
        }
        g.pose().popMatrix();
    }

    private static void paw(GuiGraphicsExtractor g) {
        // Chunky pixel silhouette with four toes and pink pads; no external image or font required.
        g.fill(-26, 12, 27, 83, 0xFF503D34);
        g.fill(-22, 12, 23, 80, 0xFFE9CDAA);
        g.fill(-16, 16, 16, 80, 0xFFFFEBD0);
        oval(g, 0, 2, 43, 39, 0xFF503D34);
        oval(g, 0, 0, 39, 35, 0xFFE9CDAA);
        oval(g, -2, -3, 33, 28, 0xFFFFEBD0);
        int[] xs = {-38, -15, 15, 38}, ys = {-29, -47, -47, -29};
        for (int i = 0; i < 4; i++) {
            oval(g, xs[i], ys[i], 16, 21, 0xFF503D34);
            oval(g, xs[i], ys[i] - 1, 12, 17, 0xFFFFEBD0);
            oval(g, xs[i], ys[i] + 2, 7, 11, 0xFFC67587);
            oval(g, xs[i] - 1, ys[i], 5, 8, 0xFFFFA9B9);
        }
        oval(g, -12, -4, 15, 15, 0xFFC67587);
        oval(g, 12, -4, 15, 15, 0xFFC67587);
        oval(g, 0, 7, 23, 18, 0xFFC67587);
        oval(g, -10, -5, 11, 11, 0xFFFFA9B9);
        oval(g, 10, -5, 11, 11, 0xFFFFA9B9);
        oval(g, 0, 6, 19, 14, 0xFFFFA9B9);
        g.fill(-16, 43, -10, 62, 0xFFCFAB85);
        g.fill(10, 53, 16, 75, 0xFFCFAB85);
    }

    private static void fallbackVillager(GuiGraphicsExtractor g) {
        g.fill(-28, -10, 28, 78, 0xFF684B32);
        g.fill(-23, -67, 23, -13, 0xFFC2946D);
        g.fill(-18, -45, -6, -39, 0xFFFFFFFF); g.fill(6, -45, 18, -39, 0xFFFFFFFF);
        g.fill(-13, -45, -8, -39, 0xFF27774E); g.fill(8, -45, 13, -39, 0xFF27774E);
        g.fill(-19, -51, 19, -47, 0xFF4A3428);
        g.fill(-6, -40, 7, -16, 0xFF9A6E4C);
        g.fill(-34, 15, 34, 27, 0xFF876141);
    }

    private static void oval(GuiGraphicsExtractor g, int x, int y, int rx, int ry, int color) {
        for (int row = -ry; row < ry; row += 3) {
            int half = (int) (rx * Math.sqrt(Math.max(0, 1 - (row + 1.5) * (row + 1.5) / (ry * ry))));
            g.fill(x - half, y + row, x + half, y + Math.min(ry, row + 3), color);
        }
    }
    private static void line(GuiGraphicsExtractor g, int x1, int y1, int x2, int y2, int width, int color) {
        int steps = Math.max(Math.abs(x2 - x1), Math.abs(y2 - y1));
        for (int i = 0; i <= steps; i += 2) {
            float t = steps == 0 ? 0 : i / (float) steps;
            int x = Math.round(x1 + (x2 - x1) * t), y = Math.round(y1 + (y2 - y1) * t);
            g.fill(x - width / 2, y - width / 2, x + (width + 1) / 2, y + (width + 1) / 2, color);
        }
    }
    private static int alpha(int color, int a) { return Math.clamp(a, 0, 255) << 24 | color & 0xFFFFFF; }
    private static void item(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float size, float rotation) {
        if (size <= 0.01f) return;
        g.pose().pushMatrix(); g.pose().translate(x, y); g.pose().rotate(rotation); g.pose().scale(size);
        g.fakeItem(stack, -8, -8); g.pose().popMatrix();
    }
}
