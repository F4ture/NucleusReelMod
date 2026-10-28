package dev.nucleusreel.music;

import dev.nucleusreel.NucleusReelClient;
import dev.nucleusreel.core.RewardCelebration;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Util;
import net.minecraft.util.valueproviders.ConstantFloat;
import java.io.IOException;
import java.util.concurrent.*;

public final class CoinSound extends AbstractTickableSoundInstance implements FabricSoundInstance {
    private final Identifier asset;
    private PcmWavStream opened;
    private boolean cancelled;

    public CoinSound(RewardCelebration celebration, float volume) {
        super(SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath("nucleusreel", celebration.sound())),
            SoundSource.PLAYERS, SoundInstance.createUnseededRandom());
        asset = Identifier.fromNamespaceAndPath("nucleusreel", "sounds/" + celebration.sound() + ".wav");
        this.volume = volume;
        pitch = 1;
        relative = true;
        looping = false;
        attenuation = SoundInstance.Attenuation.NONE;
    }

    @Override public void tick() { }
    public void setLevel(float level) { volume = level; }
    @Override public WeighedSoundEvents resolve(SoundManager manager) {
        sound = new Sound(identifier, ConstantFloat.of(1), ConstantFloat.of(1), 1, Sound.Type.FILE, true, false, 16);
        WeighedSoundEvents event = new WeighedSoundEvents(identifier, null);
        event.addSound(sound);
        return event;
    }

    @Override public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, Identifier id, boolean loop) {
        // Capture the resource manager on the client thread before decoding in the background.
        var resources = Minecraft.getInstance().getResourceManager();
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                if (cancelled) throw new CancellationException();
                try {
                    opened = new PcmWavStream(resources.open(asset));
                    return (AudioStream) opened;
                } catch (IOException exception) {
                    NucleusReelClient.LOGGER.warn("Cannot play bundled coin sound {}", asset, exception);
                    throw new CompletionException(exception);
                }
            }
        }, Util.nonCriticalIoPool());
    }

    public synchronized void cancel() {
        cancelled = true;
        stop();
        if (opened != null) {
            try { opened.close(); }
            catch (IOException exception) { NucleusReelClient.LOGGER.debug("Closing coin sound", exception); }
            opened = null;
        }
    }
}
