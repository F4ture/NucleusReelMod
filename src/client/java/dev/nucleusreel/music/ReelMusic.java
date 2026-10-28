package dev.nucleusreel.music;

import dev.nucleusreel.NucleusReelClient;
import dev.nucleusreel.ReelConfig;
import net.fabricmc.fabric.api.client.sound.v1.FabricSoundInstance;
import net.fabricmc.loader.api.FabricLoader;
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
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

public final class ReelMusic {
    private TrackSound current;
    public static Path folder() { return FabricLoader.getInstance().getConfigDir().resolve("nucleus-reel/music"); }

    public void start(ReelConfig config, Consumer<String> notice) {
        stop();
        if (!config.musicEnabled || config.musicTrack.isBlank() || config.musicVolume == 0) return;
        try {
            Files.createDirectories(folder());
            Path path = LocalTrackStream.resolve(folder(), config.musicTrack);
            current = new TrackSound(path, (float) config.musicVolume, notice);
            Minecraft.getInstance().getSoundManager().play(current);
        } catch (Exception exception) {
            notice.accept("Music: " + exception.getMessage());
        }
    }

    public void update(int elapsed, int duration, ReelConfig config) {
        if (current == null) return;
        if (!config.musicEnabled || elapsed >= duration) { stop(); return; }
        float fade = Math.min(Math.min(elapsed / 8f, (duration - elapsed) / 12f), 1f);
        current.setVolume((float) config.musicVolume * Math.max(0, fade));
    }

    public void stop() {
        if (current == null) return;
        current.cancel();
        Minecraft.getInstance().getSoundManager().stop(current);
        current = null;
    }

    private static final class TrackSound extends AbstractTickableSoundInstance implements FabricSoundInstance {
        private static final Identifier ID = Identifier.fromNamespaceAndPath("nucleusreel", "custom_music");
        private final Path path;
        private final Consumer<String> notice;
        private volatile boolean cancelled;
        private LocalTrackStream opened;
        TrackSound(Path path, float volume, Consumer<String> notice) {
            super(SoundEvent.createVariableRangeEvent(ID), SoundSource.MUSIC, SoundInstance.createUnseededRandom());
            this.path = path;
            this.notice = notice;
            this.volume = Math.max(0.001f, volume / 8);
            pitch = 1;
            looping = true;
            relative = true;
            attenuation = SoundInstance.Attenuation.NONE;
        }
        void setVolume(float volume) { this.volume = volume; }
        @Override public void tick() { if (cancelled) stop(); }
        @Override public WeighedSoundEvents resolve(SoundManager manager) {
            sound = new Sound(ID, ConstantFloat.of(1), ConstantFloat.of(1), 1, Sound.Type.FILE, true, false, 16);
            WeighedSoundEvents event = new WeighedSoundEvents(ID, null);
            event.addSound(sound);
            return event;
        }
        synchronized void cancel() {
            cancelled = true;
            stop();
            if (opened != null) {
                try { opened.close(); }
                catch (IOException exception) { NucleusReelClient.LOGGER.debug("Closing music", exception); }
                opened = null;
            }
        }
        @Override public CompletableFuture<AudioStream> getAudioStream(SoundBufferLibrary library, Identifier id, boolean looping) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    synchronized (this) {
                        if (cancelled) throw new CancellationException();
                        opened = new LocalTrackStream(path);
                        return (AudioStream) opened;
                    }
                } catch (IOException exception) {
                    NucleusReelClient.LOGGER.warn("Cannot play custom reel music", exception);
                    Minecraft.getInstance().execute(() -> {
                        if (!cancelled) notice.accept("Cannot play track. Use OGG Vorbis or PCM WAV; see latest.log.");
                        cancel();
                    });
                    throw new CompletionException(exception);
                }
            }, Util.nonCriticalIoPool());
        }
    }
}
