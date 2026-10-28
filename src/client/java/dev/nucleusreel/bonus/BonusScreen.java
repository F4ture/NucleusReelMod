package dev.nucleusreel.bonus;

import dev.nucleusreel.NucleusReelClient;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Captures only local menu input. Does not move the real player or issue server commands. */
public final class BonusScreen extends Screen {
    private final BonusSequence bonus;
    private final NucleusReelClient owner;
    private int lastMouseX = Integer.MIN_VALUE, lastMouseY = Integer.MIN_VALUE;
    public BonusScreen(BonusSequence bonus, NucleusReelClient owner) {
        super(Component.literal("Crystal Nucleus — Choose a Tunnel"));this.bonus=bonus;this.owner=owner;
    }
    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g,int x,int y,float partial) { }
    @Override public void extractRenderState(GuiGraphicsExtractor g,int mouseX,int mouseY,float partial) {
        if(mouseX!=lastMouseX||mouseY!=lastMouseY) {
            bonus.highlight(BonusSequence.hit(mouseX,mouseY,width,height));lastMouseX=mouseX;lastMouseY=mouseY;
        }
        bonus.render(g,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if(event.button()==0){bonus.choose(BonusSequence.hit(event.x(),event.y(),width,height));return true;}
        return super.mouseClicked(event,doubleClick);
    }
    @Override public boolean keyPressed(KeyEvent event) {
        int key=event.key();
        if(key>=GLFW.GLFW_KEY_1&&key<=GLFW.GLFW_KEY_3){bonus.choose(key-GLFW.GLFW_KEY_1);return true;}
        if(key>=GLFW.GLFW_KEY_KP_1&&key<=GLFW.GLFW_KEY_KP_3){bonus.choose(key-GLFW.GLFW_KEY_KP_1);return true;}
        if(key==GLFW.GLFW_KEY_LEFT||key==GLFW.GLFW_KEY_RIGHT){bonus.highlight(Math.floorMod(bonus.highlighted()+(key==GLFW.GLFW_KEY_LEFT?-1:1),3));return true;}
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){bonus.choose(bonus.highlighted());return true;}
        return super.keyPressed(event);
    }
    @Override public void onClose() { owner.reset(); }
    @Override public void removed() { owner.bonusScreenRemoved(this); }
}
