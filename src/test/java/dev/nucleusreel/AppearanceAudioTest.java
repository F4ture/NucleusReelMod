package dev.nucleusreel;

import dev.nucleusreel.core.RewardCatalog;
import dev.nucleusreel.music.LocalTrackStream;
import dev.nucleusreel.music.PcmWavStream;
import dev.nucleusreel.music.CoinSound;
import dev.nucleusreel.core.RewardCelebration;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.*;
import java.util.*;

public final class AppearanceAudioTest {
    private static int checks;
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
        checks++;
    }
    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_INITIALIZERS
            .build(net.minecraft.data.registries.VanillaRegistries.createLookup()).forEach(pending -> pending.apply());
        check(RewardCatalog.definitions().size() == 63, "Complete bundled catalog");
        for (var item : RewardCatalog.definitions()) {
            var stack = RewardIcons.create(item, id -> true);
            check(!stack.isEmpty(), "Known vanilla material for " + item.name());
            var tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
            check(tag.getString("id").orElseThrow().equals(item.id()), "SkyBlock ID for " + item.name());
            check(stack.getHoverName().getString().equals(item.name()), "Display name for " + item.name());
            if (item.itemModel() != null) {
                check(stack.get(DataComponents.ITEM_MODEL).equals(Identifier.parse(item.itemModel())), "Available model");
                var fallback = RewardIcons.create(item, id -> false);
                check(!Identifier.parse(item.itemModel()).equals(fallback.get(DataComponents.ITEM_MODEL)), "No missing pack model");
            }
            if (item.skinValue() != null) check(stack.has(DataComponents.PROFILE), "Head skin without inventory");
            if (item.enchantment() != null) {
                check(tag.getCompound("enchantments").orElseThrow().getInt(item.enchantment()).orElseThrow()
                    == item.enchantmentLevel(), "Book enchantment for resource pack");
            }
        }
        check(RewardCatalog.find("§6Divan’s Alloy").id().equals("DIVAN_ALLOY"), "Formatted reward name");
        check(RewardCatalog.find("Lapidary I").enchantment().equals("lapidary"), "Book alias");
        check(RewardCatalog.find("Fortune IV").enchantmentLevel() == 4, "Fortune level");
        for (var reward : RewardCatalog.PREVIEW_REWARDS) check(RewardCatalog.find(reward.name()) != null, "Preview catalog entry");

        Path folder = Files.createTempDirectory("nucleus-reel-audio-test-");
        try {
            Path file = folder.resolve("My track.wav");
            byte[] pcm = new byte[1600];
            for (int i = 0; i < pcm.length; i++) pcm[i] = (byte) (i % 127);
            AudioFormat format = new AudioFormat(8000, 16, 1, true, false);
            try (var input = new AudioInputStream(new ByteArrayInputStream(pcm), format, pcm.length / 2)) {
                AudioSystem.write(input, AudioFileFormat.Type.WAVE, file.toFile());
            }
            check(LocalTrackStream.resolve(folder, "My track.wav").equals(file.toRealPath()), "Filename with spaces");
            try (var stream = new LocalTrackStream(file)) {
                check(stream.getFormat().getSampleRate() == 8000 && stream.getFormat().getChannels() == 1, "WAV format");
                check(Arrays.equals(bytes(stream.read(4096)), pcm), "PCM audio intact");
                check(Arrays.equals(bytes(stream.read(4096)), pcm), "Short track loops");
                stream.close();
                check(!stream.read(4096).hasRemaining(), "Stopped stream releases file and stays closed");
            }
            for (String name : List.of("../My track.wav", "C:\\My track.wav", "My track.mp3", "missing.ogg", "")) {
                try { LocalTrackStream.resolve(folder, name); throw new AssertionError("Accepted invalid path " + name); }
                catch (IOException expected) { checks++; }
            }
            Path broken = folder.resolve("broken.wav");
            Files.write(broken, new byte[] {1, 2, 3});
            try (var ignored = new LocalTrackStream(broken)) { throw new AssertionError("Accepted malformed WAV"); }
            catch (IOException expected) { checks++; }
        } finally {
            try (var files = Files.list(folder)) { for (Path path : files.toList()) Files.delete(path); }
            Files.delete(folder);
        }
        ReelConfig config = new ReelConfig();
        for (RewardCelebration celebration : RewardCelebration.values()) {
            if (celebration == RewardCelebration.NORMAL) continue;
            String resource = "/assets/nucleusreel/sounds/" + celebration.sound() + ".wav";
            try (var raw = AppearanceAudioTest.class.getResourceAsStream(resource)) {
                check(raw != null, "Bundled coin sound exists: " + resource);
                try (var stream = new PcmWavStream(raw)) {
                    check(stream.getFormat().getChannels() == 1 && stream.getFormat().getSampleRate() == 44100, "Coin PCM format");
                    int length = 0, peak = 0, chunks = 0;
                    while (true) {
                        var buffer = stream.read(4096).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                        if (!buffer.hasRemaining()) break;
                        length += buffer.remaining();
                        while (buffer.remaining() >= 2) peak = Math.max(peak, Math.abs((int) buffer.getShort()));
                        if (++chunks > 100) throw new AssertionError("Coin effect must end, not loop");
                    }
                    check(length > 44100 && length < 180000, "Coin clip duration");
                    check(peak > 1000 && peak < 30000, "Coin audio audible with headroom");
                    check(!stream.read(4096).hasRemaining(), "Coin sound remains at EOF");
                }
            }
            CoinSound sound = new CoinSound(celebration, 0.7f);
            sound.resolve(null);
            check(!sound.isLooping() && sound.isRelative() && sound.getSound().shouldStream(), "Coin sound playback flags");
            sound.cancel();
            check(sound.isStopped(), "Coin effect can be stopped on skip/disconnect");
        }
        config.coinVolume = Double.POSITIVE_INFINITY;
        config.musicVolume = Double.NaN;
        config.musicTrack = null;
        config.sanitize();
        check(config.musicVolume == 0.7 && config.musicTrack.equals(""), "Invalid music settings recovered");
        check(config.coinVolume == 0.7, "Invalid coin volume recovered");
        ReelConfig migrated = new com.google.gson.Gson().fromJson("{\"musicEnabled\":true,\"musicTrack\":\"song.ogg\",\"reelMatchChance\":1,\"nearMissChance\":0.3}", ReelConfig.class);
        migrated.sanitize();
        check(migrated.tripleRewards.equals(dev.nucleusreel.core.SlotOutcome.DEFAULT_TRIPLE_REWARDS), "Old config gains the default rare reward list");
        check(migrated.musicEnabled && migrated.musicTrack.equals("song.ogg"), "Old music selection preserved");
        checkFixedPublicSettings();
        var meow = net.minecraft.sounds.SoundEvents.CAT_SOUNDS
            .get(net.minecraft.world.entity.animal.feline.CatSoundVariants.SoundSet.CLASSIC).adultSounds().ambientSound().value();
        check(meow != null, "Minecraft 26.1.2 classic cat meow exists");
        System.out.println("Passed " + checks + " catalog / item appearance / audio checks without a player or inventory.");
    }
    private static void checkFixedPublicSettings() {
        var gson = new com.google.gson.Gson();
        var lockedKeys = java.util.List.of("reelMatchChance", "secondReelMissChance", "thirdReelMissChance",
            "bonusWinChance", "bonusEmptyChance", "nearMissChance", "REEL_MATCH_CHANCE", "SECOND_REEL_MISS_CHANCE",
            "THIRD_REEL_MISS_CHANCE", "BONUS_WIN_CHANCE", "BONUS_EMPTY_CHANCE");
        for (double override : new double[] {0, 0.2, 0.8, 1}) {
            var old = new com.google.gson.JsonObject();
            for (String key : lockedKeys) old.addProperty(key, override);
            old.addProperty("bonusEnabled", false);
            old.addProperty("BONUS_ENABLED", false);
            old.addProperty("musicEnabled", true);
            old.addProperty("musicTrack", "My song.ogg");
            old.addProperty("musicVolume", 0.35);
            old.addProperty("scale", 1.2);
            old.addProperty("durationSeconds", 9);
            old.addProperty("effects", false);
            ReelConfig loaded = gson.fromJson(old, ReelConfig.class);
            loaded.sanitize();
            check(ReelConfig.REEL_MATCH_CHANCE == 0.25 && ReelConfig.SECOND_REEL_MISS_CHANCE == 0.4
                && ReelConfig.THIRD_REEL_MISS_CHANCE == 0.5, "Legacy chance overrides cannot change public reel rules");
            check(ReelConfig.BONUS_ENABLED && ReelConfig.BONUS_WIN_CHANCE == 0.5 && ReelConfig.BONUS_EMPTY_CHANCE == 0.5,
                "Legacy bonus-disable and chance overrides cannot change public bonus rules");
            var serialized = gson.toJsonTree(loaded).getAsJsonObject();
            for (String key : lockedKeys) check(!serialized.has(key), "Locked rule is not written as a setting: " + key);
            check(!serialized.has("bonusEnabled") && !serialized.has("BONUS_ENABLED"), "Bonus toggle is not written as a setting");
            ReelConfig reloaded = gson.fromJson(serialized, ReelConfig.class);
            reloaded.sanitize();
            check(reloaded.musicEnabled && reloaded.musicTrack.equals("My song.ogg") && reloaded.musicVolume == 0.35,
                "Music settings survive public migration and round trip");
            check(reloaded.scale == 1.2 && reloaded.durationSeconds == 9 && !reloaded.effects,
                "Presentation settings survive public migration and round trip");
        }
    }
    private static byte[] bytes(ByteBuffer buffer) { byte[] bytes = new byte[buffer.remaining()]; buffer.get(bytes); return bytes; }
}
