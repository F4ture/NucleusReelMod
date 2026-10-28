package dev.nucleusreel;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.nucleusreel.core.LootParser;
import dev.nucleusreel.core.Reward;
import dev.nucleusreel.core.RewardPriority;
import dev.nucleusreel.core.BonusPlan;
import dev.nucleusreel.bonus.BonusSequence;
import dev.nucleusreel.bonus.BonusScreen;
import dev.nucleusreel.music.LocalTrackStream;
import dev.nucleusreel.music.ReelMusic;
import net.minecraft.util.Util;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommands.literal;

public final class NucleusReelClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("nucleusreel");
    private static NucleusReelClient instance;
    private final Minecraft minecraft = Minecraft.getInstance();
    private final LootParser parser = new LootParser();
    private final List<Reward> collecting = new ArrayList<>();
    private final List<Component> deferred = new ArrayList<>();
    private record Bundle(List<Reward> rewards, List<Component> messages) {}
    private final Deque<Bundle> queue = new ArrayDeque<>();
    private ReelConfig config;
    private final ReelMusic music = new ReelMusic();
    private ReelAnimation animation;
    private BonusSequence bonus;
    private BonusScreen bonusScreen;
    private Bundle current;
    private boolean messagesReleased;
    private Object lastLevel;
    private long collectingSince;

    public static NucleusReelClient instance() { return instance; }

    @Override public void onInitializeClient() {
        instance = this;
        config = ReelConfig.load();
        if (!Files.exists(ReelConfig.path())) {
            try { config.save(); }
            catch (IOException exception) { LOGGER.warn("Could not create default Nucleus Reel settings", exception); }
        }
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, context) -> registerCommands(dispatcher));
        LOGGER.info("Nucleus Reel initialized for Minecraft 26.1.2");
    }

    void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher) {
            var root = dispatcher.register(
            literal("nucleusreel")
                .executes(ctx -> help())
                .then(literal("help").executes(ctx -> help()))
                .then(literal("status").executes(ctx -> status()))
                .then(literal("on").executes(ctx -> flag("enabled", true)))
                .then(literal("off").executes(ctx -> flag("enabled", false)))
                .then(literal("bonus").executes(ctx -> status())
                    .then(literal("on").executes(ctx -> flag("bonus", true)))
                    .then(literal("off").executes(ctx -> flag("bonus", false))))
                .then(literal("chance").executes(ctx -> status())
                    .then(literal("second").then(argument("percent", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> chance("second", IntegerArgumentType.getInteger(ctx, "percent")))))
                    .then(literal("third").then(argument("percent", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> chance("third", IntegerArgumentType.getInteger(ctx, "percent")))))
                    .then(literal("bonus-win").then(argument("percent", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> chance("bonus-win", IntegerArgumentType.getInteger(ctx, "percent")))))
                    .then(literal("bonus-empty").then(argument("percent", IntegerArgumentType.integer(0, 100))
                        .executes(ctx -> chance("bonus-empty", IntegerArgumentType.getInteger(ctx, "percent"))))))
                .then(literal("skip").executes(ctx -> { reset(); return 1; }))
                .then(literal("toggle").executes(ctx -> {
                    reset(); config.enabled = !config.enabled; save();
                    say("Nucleus Reel: " + (config.enabled ? "ON" : "OFF")); return 1;
                }))
                .then(literal("duration").then(argument("seconds", DoubleArgumentType.doubleArg(1, 15)).executes(ctx -> {
                    config.durationSeconds = DoubleArgumentType.getDouble(ctx, "seconds"); save();
                    say("Animation: " + config.durationSeconds + " seconds"); return 1;
                })))
                .then(literal("scale").then(argument("size", DoubleArgumentType.doubleArg(0.5, 1.5)).executes(ctx -> {
                    config.scale = DoubleArgumentType.getDouble(ctx, "size"); save();
                    say("Scale: " + config.scale); return 1;
                })))
                .then(literal("secondchance").then(argument("chance", DoubleArgumentType.doubleArg(0, 1)).executes(ctx -> {
                    config.secondReelMissChance = DoubleArgumentType.getDouble(ctx, "chance"); save();
                    say("Second-reel miss: " + Math.round(config.secondReelMissChance * 100) + "% of ordinary rewards"); return 1;
                })))
                .then(literal("misschance").then(argument("chance", DoubleArgumentType.doubleArg(0, 1)).executes(ctx -> {
                    config.thirdReelMissChance = DoubleArgumentType.getDouble(ctx, "chance"); save();
                    say("Third-reel miss: " + Math.round(config.thirdReelMissChance * 100) + "% after the second reel passes"); return 1;
                })))
                .then(literal("sound")
                    .then(literal("on").executes(ctx -> flag("sound", true)))
                    .then(literal("off").executes(ctx -> flag("sound", false)))
                    .then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                    config.sound = BoolArgumentType.getBool(ctx, "enabled"); save();
                    say("Sound: " + config.sound); return 1;
                })))
                .then(literal("effects")
                    .then(literal("on").executes(ctx -> flag("effects", true)))
                    .then(literal("off").executes(ctx -> flag("effects", false)))
                    .then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                    config.effects = BoolArgumentType.getBool(ctx, "enabled"); save();
                    say("Visual effects: " + config.effects); return 1;
                })))
                .then(literal("coins")
                    .then(literal("on").executes(ctx -> flag("coins", true)))
                    .then(literal("off").executes(ctx -> flag("coins", false)))
                    .then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                    config.coinSounds = BoolArgumentType.getBool(ctx, "enabled"); save();
                    say("Coin sounds: " + config.coinSounds); return 1;
                })))
                .then(literal("coinvolume").then(argument("level", DoubleArgumentType.doubleArg(0, 1)).executes(ctx -> {
                    config.coinVolume = DoubleArgumentType.getDouble(ctx, "level"); save();
                    say("Coin volume: " + config.coinVolume); return 1;
                })))
                .then(literal("music")
                    .executes(ctx -> { say("Music: " + config.musicTrack + " / " + (config.musicEnabled ? "ON" : "OFF")); return 1; })
                    .then(literal("folder").executes(ctx -> openMusicFolder()))
                    .then(literal("list").executes(ctx -> listMusic()))
                    .then(literal("on").executes(ctx -> { config.musicEnabled = true; save(); say("Reel music ON"); return 1; }))
                    .then(literal("off").executes(ctx -> { config.musicEnabled = false; music.stop(); save(); say("Reel music OFF"); return 1; }))
                    .then(literal("volume").then(argument("level", DoubleArgumentType.doubleArg(0, 1)).executes(ctx -> {
                        config.musicVolume = DoubleArgumentType.getDouble(ctx, "level"); save(); say("Music volume: " + config.musicVolume); return 1;
                    })))
                    .then(literal("track").then(argument("filename", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            try (var files = Files.list(ReelMusic.folder())) {
                                files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                                    .filter(NucleusReelClient::isMusicFile).forEach(builder::suggest);
                            } catch (IOException ignored) { }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> selectMusic(StringArgumentType.getString(ctx, "filename"))))))
                .then(literal("hidechat")
                    .then(literal("on").executes(ctx -> flag("hidechat", true)))
                    .then(literal("off").executes(ctx -> flag("hidechat", false)))
                    .then(argument("enabled", BoolArgumentType.bool()).executes(ctx -> {
                    reset(); config.hideRewardChat = BoolArgumentType.getBool(ctx, "enabled"); save();
                    say("Defer reward messages: " + config.hideRewardChat); return 1;
                })))
                .then(literal("reload").executes(ctx -> {
                    reset(); config = ReelConfig.load(); say("Configuration reloaded"); return 1;
                }))
            );
            dispatcher.register(literal("nr").executes(ctx -> help()).redirect(root));
    }

    private int help() {
        say("/nr bonus on|off — enable or disable bonus scenes");
        say("/nr chance second|third|bonus-win|bonus-empty <0–100> — chances in percent");
        say("/nr on|off | skip | status | reload — enable, disable, skip, or manage settings");
        say("/nr duration <1–15> | scale <0.5–1.5> — animation duration and size");
        say("/nr sound|effects|coins|hidechat on|off | coinvolume <0–1>");
        say("/nr music folder|list|track <file.ogg>|volume <0–1>|on|off");
        say("The full /nucleusreel command and legacy commands still work. Press Tab for suggestions.");
        return 1;
    }

    private int status() {
        say("Mod: " + (config.enabled ? "ON" : "OFF") + "; bonus scenes: " + (config.bonusEnabled ? "ON" : "OFF"));
        say("Second-reel miss: " + Math.round(config.secondReelMissChance * 100) + "%; third-reel miss after the second reel matches: " + Math.round(config.thirdReelMissChance * 100) + "%.");
        say("Bonus with Alloy: " + Math.round(config.bonusWinChance * 100) + "%; empty bonus after a third-reel miss: " + Math.round(config.bonusEmptyChance * 100) + "%.");
        return 1;
    }
    private int chance(String option, int percent) {
        double value = percent / 100.0;
        switch (option) {
            case "second" -> config.secondReelMissChance = value;
            case "third" -> config.thirdReelMissChance = value;
            case "bonus-win" -> config.bonusWinChance = value;
            case "bonus-empty" -> config.bonusEmptyChance = value;
            default -> throw new IllegalArgumentException(option);
        }
        save(); return status();
    }
    private int flag(String option, boolean enabled) {
        switch (option) {
            case "enabled" -> { reset(); config.enabled = enabled; }
            case "bonus" -> config.bonusEnabled = enabled;
            case "sound" -> config.sound = enabled;
            case "effects" -> config.effects = enabled;
            case "coins" -> config.coinSounds = enabled;
            case "hidechat" -> { reset(); config.hideRewardChat = enabled; }
            default -> throw new IllegalArgumentException(option);
        }
        save(); say(option + ": " + (enabled ? "ON" : "OFF")); return 1;
    }

    private static boolean isMusicFile(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.endsWith(".ogg") || lower.endsWith(".wav");
    }

    private int openMusicFolder() {
        try { Files.createDirectories(ReelMusic.folder()); Util.getPlatform().openPath(ReelMusic.folder()); }
        catch (Exception exception) { say("Could not open music folder: " + exception.getMessage()); return 0; }
        say("Copy OGG Vorbis or WAV here, then use /nucleusreel music track <filename>");
        return 1;
    }

    private int listMusic() {
        try {
            Files.createDirectories(ReelMusic.folder());
            try (var files = Files.list(ReelMusic.folder())) {
                List<String> names = files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
                    .filter(NucleusReelClient::isMusicFile).sorted().limit(50).toList();
                say(names.isEmpty() ? "No tracks. Use /nucleusreel music folder" : String.join(", ", names));
            }
        } catch (IOException exception) { say("Cannot read music folder: " + exception.getMessage()); return 0; }
        return 1;
    }

    private int selectMusic(String name) {
        try { Files.createDirectories(ReelMusic.folder()); LocalTrackStream.resolve(ReelMusic.folder(), name); }
        catch (Exception exception) { say("Cannot select track: " + exception.getMessage()); return 0; }
        config.musicTrack = name;
        config.musicEnabled = true;
        music.stop();
        save();
        say("Track selected: " + name + ". Music will play during the next reel. Minecraft Music volume must be above 0.");
        return 1;
    }

    private void save() {
        try { config.save(); }
        catch (IOException exception) {
            LOGGER.warn("Could not save Nucleus Reel configuration", exception);
            say("Could not save settings; the change applies only to this session.");
        }
    }

    /** Only server system messages reach this method; player chat is not used. */
    public boolean onSystemMessage(Component message, boolean remote) {
        // In 26.1.2 this flag means server-originated; overlays have a separate method.
        if (!config.enabled || !remote) return false;
        if (!parser.isOpen() && !inNucleus()) return false;
        boolean recognized = false;
        boolean complete = false;
        try {
            for (String line : message.getString().split("\\R")) {
                LootParser.Result result = parser.accept(line);
                switch (result.type()) {
                    case START -> {
                        // A missing closing border must never lose the prior messages.
                        flush(deferred);
                        collecting.clear();
                        collectingSince = System.nanoTime();
                        recognized = true;
                    }
                    case REWARD -> {
                        collecting.add(result.reward());
                        recognized = true;
                    }
                    case END -> { complete = true; recognized = true; }
                    case NONE -> { }
                }
            }
            boolean hide = recognized && config.hideRewardChat;
            if (hide) deferred.add(message.copy());
            if (deferred.size() > 256 || collecting.size() > 128) {
                abortCollection();
                return hide;
            }
            if (complete) finishCollection();
            return hide;
        } catch (Exception exception) {
            LOGGER.warn("Nucleus reward detection failed; restoring messages", exception);
            abortCollection();
            return false;
        }
    }

    private boolean inNucleus() {
        if (minecraft.player == null || minecraft.getCurrentServer() == null) return false;
        String host = minecraft.getCurrentServer().ip.toLowerCase(Locale.ROOT).split(":", 2)[0];
        if (!host.equals("hypixel.net") && !host.endsWith(".hypixel.net")) return false;
        double x = minecraft.player.getX();
        double y = minecraft.player.getY();
        double z = minecraft.player.getZ();
        return x >= 450 && x <= 575 && z >= 450 && z <= 575 && y >= 55 && y <= 200;
    }

    private void finishCollection() {
        if (collecting.isEmpty() || queue.size() >= 8) {
            abortCollection();
            return;
        }
        Bundle bundle = new Bundle(List.copyOf(collecting), new ArrayList<>(deferred));
        collecting.clear();
        deferred.clear();
        parser.reset();
        queue.addLast(bundle);
        if (animation == null) start(queue.removeFirst());
    }

    private void abortCollection() {
        flush(deferred);
        collecting.clear();
        parser.reset();
    }

    private void start(Bundle bundle) {
        current = bundle;
        messagesReleased = false;
        animation = new ReelAnimation(RewardPriority.forAnimation(bundle.rewards, config.rewardPriority, config.tripleRewards),
            RewardPriority.ordinary(bundle.rewards, config.rewardPriority, config.tripleRewards),
            config, -1, BonusPlan.Preview.AUTO);
        music.start(config, this::say);
    }

    private void tick() {
        if (minecraft.level != lastLevel) {
            reset();
            lastLevel = minecraft.level;
        }
        if (parser.isOpen() && System.nanoTime() - collectingSince > 5_000_000_000L) {
            LOGGER.warn("Incomplete Nucleus loot block; restoring messages without guessing a result");
            abortCollection();
        }
        if (animation == null) return;
        if (bonus != null) {
            if (minecraft.screen != bonusScreen) { reset(); return; }
            boolean finished = bonus.tick();
            if (bonus.revealed()) releaseCurrent();
            if (finished) finishAnimation();
            return;
        }
        boolean finished = animation.tick();
        music.update(animation.elapsedTicks(), animation.durationTicks(), config);
        if (animation.releaseMessages() && !messagesReleased) {
            releaseCurrent();
        }
        if (animation.bonusReady() && minecraft.screen == null && minecraft.player != null) {
            music.stop(); animation.stopSounds();
            bonus = new BonusSequence(animation.bonus().won(), animation.winner(), config);
            bonusScreen = new BonusScreen(bonus, this);
            minecraft.setScreen(bonusScreen);
        } else if (finished) finishAnimation();
    }

    private void finishAnimation() {
        releaseCurrent(); closeBonus();
        if (animation != null) animation.stopSounds();
        animation = null; current = null;
        if (!queue.isEmpty()) start(queue.removeFirst());
    }
    private void closeBonus() {
        if (bonus != null) bonus.stopSounds();
        var owned = bonusScreen; bonusScreen = null; bonus = null;
        if (owned != null && minecraft.screen == owned) minecraft.setScreen(null);
    }
    public void bonusScreenRemoved(BonusScreen screen) {
        if (bonusScreen == screen) {
            // A different screen took ownership: release chat/sounds but do not close that screen.
            bonusScreen = null;
            reset();
        }
    }

    private void releaseCurrent() {
        if (current != null && !messagesReleased) {
            for (Component message : current.messages) minecraft.gui.getChat().addServerSystemMessage(message);
            messagesReleased = true;
        }
    }

    private void flush(List<Component> messages) {
        for (Component message : messages) minecraft.gui.getChat().addServerSystemMessage(message);
        messages.clear();
    }

    public void reset() {
        music.stop();
        closeBonus();
        if (animation != null) animation.stopSounds();
        releaseCurrent();
        for (Bundle bundle : queue) {
            for (Component message : bundle.messages) minecraft.gui.getChat().addServerSystemMessage(message);
        }
        queue.clear();
        abortCollection();
        current = null;
        animation = null;
    }

    public void render(GuiGraphicsExtractor graphics, DeltaTracker delta) {
        if (bonus == null && animation != null && minecraft.level != null) animation.render(graphics, delta);
    }

    private void say(String text) {
        minecraft.gui.getChat().addClientSystemMessage(Component.literal("[Nucleus Reel] " + text));
    }
}
