package dev.nucleusreel.mixin;

import dev.nucleusreel.NucleusReelClient;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class HudMixin {
    @Inject(method = "extractRenderState", at = @At("RETURN"))
    private void nucleusreel$render(GuiGraphicsExtractor graphics, DeltaTracker delta, CallbackInfo ci) {
        NucleusReelClient client = NucleusReelClient.instance();
        if (client != null) client.render(graphics, delta);
    }
}
