package dev.nucleusreel.mixin;

import dev.nucleusreel.NucleusReelClient;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ChatListener.class, priority = 1500)
public abstract class ChatListenerMixin {
    @Inject(method = "handleSystemMessage", at = @At("HEAD"), cancellable = true)
    private void nucleusreel$receive(Component message, boolean remote, CallbackInfo ci) {
        NucleusReelClient client = NucleusReelClient.instance();
        if (client != null && client.onSystemMessage(message, remote)) ci.cancel();
    }
}
