package dev.nucleusreel.bonus;

import dev.nucleusreel.GuiMesh;
import dev.nucleusreel.core.BonusTimeline;
import dev.nucleusreel.core.CabinetFracture.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Fixed perspective scenery, built once. Rendering never generates a world or tessellates stone. */
public final class CaveGeometry {
    public final GuiMesh entrance, room, pedestal, glow;
    public CaveGeometry() {
        entrance = cave(true); room = cave(false);
        var b = new GuiMesh.Builder();
        polygon(b, 0xFF202333, 240,282, 300,262, 360,282, 300,307);
        polygon(b, 0xFF4C4F60, 240,282, 300,307, 300,320, 240,295);
        polygon(b, 0xFF323744, 300,307, 360,282, 360,295, 300,320);
        polygon(b, 0xFF6E6D76, 264,218, 300,204, 336,218, 300,234);
        polygon(b, 0xFF474858, 264,218, 300,234, 300,294, 264,278);
        polygon(b, 0xFF343746, 300,234, 336,218, 336,278, 300,294);
        polygon(b, 0xFFCCB474, 256,214, 300,197, 344,214, 300,233);
        polygon(b, 0xFF74694D, 256,214, 300,233, 300,241, 256,222);
        polygon(b, 0xFF574E40, 300,233, 344,214, 344,222, 300,241);
        polygon(b, 0xFF141C29, 268,214, 300,202, 332,214, 300,227);
        // Divan-style gold corner inlays.
        rect(b,277,244,280,274,0xFF9B8A60);rect(b,320,244,323,274,0xFF756746);
        polygon(b,0xFFB6A673,292,251,300,256,300,271,292,266);
        pedestal=b.build();
        b=new GuiMesh.Builder();
        for(int ring=7;ring>=1;ring--) {
            List<Point> points=new ArrayList<>();
            for(int i=0;i<16;i++){double a=i*Math.PI/8;points.add(new Point((float)Math.cos(a)*ring*8,(float)Math.sin(a)*ring*8));}
            b.polygon(points,0x07FFB64E);
        }
        glow=b.build();
    }
    private static GuiMesh cave(boolean tunnels) {
        var b=new GuiMesh.Builder();
        rect(b,0,0,600,400,0xFF0B1018);
        polygon(b,0xFF252B31,0,0,600,0,532,72,68,72);
        polygon(b,0xFF1D2429,0,0,68,72,68,270,0,400);
        polygon(b,0xFF161E24,600,0,532,72,532,270,600,400);
        rect(b,68,72,532,270,0xFF30363B);
        polygon(b,0xFF292C2D,0,400,68,270,532,270,600,400);
        Random random=new Random(47);
        // Rectangular voxel faces and pixel-sized seams, not smooth cave arches.
        for(int row=0;row<5;row++)for(int col=0;col<13;col++) {
            int x=60+col*38,y=64+row*42,v=38+random.nextInt(12);
            int color=0xFF000000|(v<<16)|((v+5)<<8)|(v+8);
            rect(b,x+1,y+1,x+37,y+40,color);
            rect(b,x+2,y+2,x+36,y+4,0xFF4A5153);
            rect(b,x+33,y+5,x+36,y+38,0xFF242D32);
            for(int i=0;i<3;i++) {
                int px=x+5+random.nextInt(25),py=y+9+random.nextInt(25);
                rect(b,px,py,px+4,py+3,0xFF30393D);
            }
        }
        for(int i=0;i<12;i++) {
            int x=i*52,h=20+(i*23%4)*12;
            rect(b,x,0,x+54,h,0xFF141C22);
            rect(b,x+4,h,x+46,h+9,0xFF222B30);
            if(i%3==0)rect(b,x+16,h+9,x+40,h+21,0xFF182126);
        }
        for(int i=0;i<9;i++) {
            float x=-90+i*98;
            b.line(new Point(300+(x-300)*0.24f,270),new Point(x,400),1.3f,0xFF171F23,null);
        }
        for(int y:new int[]{283,310,348,394})rect(b,0,y,600,y+1,0xFF1B2225);
        if(tunnels)for(int i=0;i<3;i++) {
            int x=(int)BonusTimeline.doorX(i);
            rect(b,x-51,106,x+51,270,0xFF050A0F);
            polygon(b,0xFF17252B,x-49,108,x-27,144,x-27,245,x-49,269);
            polygon(b,0xFF101C22,x+49,108,x+27,144,x+27,245,x+49,269);
            polygon(b,0xFF252A28,x-49,269,x-27,245,x+27,245,x+49,269);
            // Repeating support sets disappear into each shaft.
            for(int depth=0;depth<3;depth++) {
                int half=45-depth*9,top=118+depth*15,bottom=265-depth*9;
                rect(b,x-half,top,x-half+3,bottom,0xFF384034);
                rect(b,x+half-3,top,x+half,bottom,0xFF26362C);
                rect(b,x-half,top,x+half,top+4,0xFF46503B);
            }
            for(int side:new int[]{-1,1}) {
                int post=x+side*57;
                rect(b,post-6,101,post+6,274,0xFF5B4934);
                rect(b,post-5,103,post-2,272,0xFF8B7149);
                rect(b,post+3,104,post+5,271,0xFF312F26);
                for(int y:new int[]{126,230}) {rect(b,post-7,y,post+7,y+9,0xFF353B3B);rect(b,post-3,y+2,post,y+5,0xFF929486);}
            }
            rect(b,x-66,91,x+66,106,0xFF6C593D);
            rect(b,x-65,91,x+65,94,0xFFB09763);
            rect(b,x-65,102,x+65,107,0xFF2D2B25);
            // Actual mine-track proportions in perspective, with sleepers and paired rails.
            for(int n=0;n<6;n++) {
                float t=n/5f,y=249+t*72,half=11+t*17;
                rect(b,x-half,y,x+half,y+2+t*2,0xFF66533A);
            }
            b.line(new Point(x-8,247),new Point(x-24,324),2,0xFF848977,null);
            b.line(new Point(x+8,247),new Point(x+24,324),2,0xFF848977,null);
        } else {
            rect(b,222,89,378,257,0xFF151E24);
            rect(b,222,89,378,96,0xFF988253);
            rect(b,224,96,232,257,0xFF4A534C);rect(b,368,96,376,257,0xFF303E3A);
            rect(b,247,117,353,121,0xFF73674D);
            // Stepped, gold-inlaid Nucleus shrine.
            polygon(b,0xFF6A5C43,300,128,318,146,300,168,282,146);
            polygon(b,0xFFBC9A5A,300,134,309,146,300,158,291,146);
        }
        for(int side:new int[]{0,1}) {
            int x=side==0?0:552;
            rect(b,x,332,x+48,400,0xFF182125);
            rect(b,x+4,318,x+42,336,0xFF39413D);
            rect(b,x+9,306,x+34,320,0xFF4A5148);
            rect(b,x+4,336,x+12,392,0xFF2B3533);
        }
        return b.build();
    }
    public static void rect(GuiMesh.Builder b,float l,float t,float r,float bottom,int color){polygon(b,color,l,t,r,t,r,bottom,l,bottom);}
    public static void polygon(GuiMesh.Builder b,int color,float... xy) {
        List<Point> points=new ArrayList<>();float area=0;
        for(int i=0;i<xy.length;i+=2){points.add(new Point(xy[i],xy[i+1]));int n=(i+2)%xy.length;area+=xy[i]*xy[n+1]-xy[n]*xy[i+1];}
        if(area<0)Collections.reverse(points);
        b.polygon(points,color);
    }
}
