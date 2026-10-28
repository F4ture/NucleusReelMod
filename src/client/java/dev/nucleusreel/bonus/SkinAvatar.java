package dev.nucleusreel.bonus;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;

/** Small back-facing skin puppet. Uses the player's loaded skin, including slim arms and outer layers.
 * No player entity, camera, inventory or world state is mutated. */
public final class SkinAvatar {
    private SkinAvatar() {}
    public static void draw(GuiGraphicsExtractor g, float x, float feet, float size, float walk, boolean moving) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var skin = player.getSkin();
        Identifier texture = skin.body().texturePath();
        int arm = skin.model() == PlayerModelType.SLIM ? 3 : 4;
        float swing = moving ? (float)Math.sin(walk * 0.55) * 0.22f : 0;
        g.pose().pushMatrix();
        g.pose().translate(x, feet - (moving ? Math.abs((float)Math.sin(walk * 0.55)) * size : 0));
        g.pose().scale(size);
        limb(g, texture, -4, -12, 4, 12, 12, 20, 12, 36, swing);
        limb(g, texture, 0, -12, 4, 12, 28, 52, 12, 52, -swing);
        // Back of the torso, then jacket. Head side/top add a little depth without an entity renderer.
        face(g, texture, -4, -24, 8, 12, 32, 20);
        face(g, texture, -4, -24, 8, 12, 32, 36);
        limb(g, texture, -4 - arm, -24, arm, 12, 48 + arm, 20, 48 + arm, 36, -swing);
        limb(g, texture, 4, -24, arm, 12, 40 + arm, 52, 56 + arm, 52, swing);
        g.fill(-5, -32, 5, -23, 0xFF10121C);
        face(g, texture, -4, -32, 8, 8, 24, 8);
        face(g, texture, -4, -32, 8, 8, 56, 8);
        g.pose().popMatrix();
    }
    private static void limb(GuiGraphicsExtractor g, Identifier skin, int x, int y, int w, int h,
                             int u, int v, int outerU, int outerV, float angle) {
        g.pose().pushMatrix();g.pose().translate(x + w / 2f, y);g.pose().rotate(angle);
        face(g, skin, -w / 2, 0, w, h, u, v);
        face(g, skin, -w / 2, 0, w, h, outerU, outerV);
        g.pose().popMatrix();
    }
    private static void face(GuiGraphicsExtractor g, Identifier skin, int x, int y, int w, int h, int u, int v) {
        g.blit(RenderPipelines.GUI_TEXTURED, skin, x, y, (float)u, (float)v, w, h, w, h, 64, 64);
    }
}
