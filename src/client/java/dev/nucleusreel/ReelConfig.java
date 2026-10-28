package dev.nucleusreel;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.nucleusreel.core.RewardPriority;
import dev.nucleusreel.core.SlotOutcome;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReelConfig {
    // Public-edition rules are constants, not JSON settings. Legacy overrides are ignored.
    public static final double REEL_MATCH_CHANCE = 0.25;
    public static final double SECOND_REEL_MISS_CHANCE = 0.40;
    public static final double THIRD_REEL_MISS_CHANCE = 0.50;
    public static final boolean BONUS_ENABLED = true;
    public static final double BONUS_WIN_CHANCE = 0.50;
    public static final double BONUS_EMPTY_CHANCE = 0.50;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public boolean enabled = true;
    public boolean hideRewardChat = true;
    public boolean sound = true;
    public boolean effects = true;
    public boolean coinSounds = true;
    public double coinVolume = 0.7;
    public List<String> tripleRewards = new ArrayList<>(SlotOutcome.DEFAULT_TRIPLE_REWARDS);
    public boolean musicEnabled = false;
    public String musicTrack = "";
    public double musicVolume = 0.7;
    public double durationSeconds = 6;
    public double resultSeconds = 2.5;
    public double scale = 1;
    public List<String> rewardPriority = new ArrayList<>(RewardPriority.DEFAULT);

    public static Path path() { return FabricLoader.getInstance().getConfigDir().resolve("nucleus-reel.json"); }

    public static ReelConfig load() {
        if (!Files.exists(path())) return new ReelConfig();
        try {
            ReelConfig config = GSON.fromJson(Files.readString(path(), StandardCharsets.UTF_8), ReelConfig.class);
            if (config == null) throw new IllegalArgumentException("Empty configuration");
            config.sanitize();
            return config;
        } catch (Exception exception) {
            NucleusReelClient.LOGGER.warn("Could not read Nucleus Reel config; using defaults without overwriting the file", exception);
            return new ReelConfig();
        }
    }

    public void sanitize() {
        durationSeconds = clamp(durationSeconds, 1, 15, 6);
        resultSeconds = clamp(resultSeconds, 1, 10, 2.5);
        scale = clamp(scale, 0.5, 1.5, 1);
        musicVolume = clamp(musicVolume, 0, 1, 0.7);
        coinVolume = clamp(coinVolume, 0, 1, 0.7);
        if (tripleRewards == null) tripleRewards = new ArrayList<>(SlotOutcome.DEFAULT_TRIPLE_REWARDS);
        else {
            tripleRewards = new ArrayList<>(tripleRewards);
            tripleRewards.removeIf(s -> s == null || s.isBlank());
        }
        if (musicTrack == null) musicTrack = "";
        if (rewardPriority == null || rewardPriority.isEmpty()) rewardPriority = new ArrayList<>(RewardPriority.DEFAULT);
        else rewardPriority.removeIf(s -> s == null || s.isBlank());
        if (rewardPriority.isEmpty()) rewardPriority = new ArrayList<>(RewardPriority.DEFAULT);
    }

    private static double clamp(double n, double min, double max, double fallback) {
        return Double.isFinite(n) ? Math.clamp(n, min, max) : fallback;
    }

    public void save() throws IOException {
        sanitize();
        Files.createDirectories(path().getParent());
        Files.writeString(path(), GSON.toJson(this), StandardCharsets.UTF_8);
    }
}
