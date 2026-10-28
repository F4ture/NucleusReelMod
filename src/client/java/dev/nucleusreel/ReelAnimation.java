package dev.nucleusreel;

import dev.nucleusreel.core.Reward;
import dev.nucleusreel.core.RewardCatalog;
import dev.nucleusreel.core.SlotSpin;
import dev.nucleusreel.core.SlotOutcome;
import dev.nucleusreel.core.RewardCelebration;
import dev.nucleusreel.core.FinaleTimeline;
import dev.nucleusreel.core.BonusPlan;
import dev.nucleusreel.core.MiningTimeline;
import dev.nucleusreel.bonus.MiningTransition;
import dev.nucleusreel.music.CoinSound;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.entity.animal.feline.CatSoundVariants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class ReelAnimation {
    private record Card(Reward reward, ItemStack icon) {}
    private static final int STEP = 60;
    private static final int[] CENTRES = {-118, 0, 118};
    private final List<List<Card>> reels = new ArrayList<>();
    private final Reward winner;
    private final ReelConfig config;
    private final SlotSpin spin;
    private final SlotOutcome outcome;
    private final BonusPlan bonus;
    private final MiningTransition mining;
    private final RewardCelebration celebration;
    private final int duration;
    private final int hold;
    private final boolean jackpot;
    private final float[] sparks = new float[512];
    private final int[] lastClicks = {-1, -1, -1};
    private final boolean[] locked = new boolean[3];
    private final ItemStack pickaxe = RewardIcons.forReward(new Reward("Pickonimbus 2000", 1));
    private final ItemStack divanFragment = RewardIcons.forReward(new Reward("Divan Fragment", 1));
    private final ItemStack[] crystals = java.util.stream.Stream.of("Perfect Amber Gemstone", "Perfect Sapphire Gemstone", "Perfect Jade Gemstone", "Perfect Amethyst Gemstone", "Perfect Topaz Gemstone")
        .map(name -> RewardIcons.forReward(new Reward(name, 1))).toArray(ItemStack[]::new);
    private static final int[] CRYSTAL_COLORS = {0xFFFFAC4E, 0xFF65A9FF, 0xFF7BE5A0, 0xFFC286EF, 0xFFFFDD6B};
    private final ItemStack lantern = new ItemStack(Items.LANTERN);
    private final ItemStack minecart = new ItemStack(Items.MINECART);
    private final ItemStack emerald = new ItemStack(Items.EMERALD);
    private final ItemStack coin = new ItemStack(Items.GOLD_NUGGET);
    private CoinSound coinSound;
    private final List<SimpleSoundInstance> effectSounds = new ArrayList<>();
    private final RewardFinale finale;
    private int elapsed;
    private boolean revealed;

    public ReelAnimation(Reward winner, ReelConfig config) {
        this(winner, config, false);
    }

    public ReelAnimation(Reward winner, ReelConfig config, boolean forceNearMiss) {
        this(winner, config, forceNearMiss ? 2 : -1);
    }

    public ReelAnimation(Reward winner, ReelConfig config, int forceFailedReel) {
        this(winner, config, forceFailedReel, forceFailedReel >= 0 ? BonusPlan.Preview.OFF : BonusPlan.Preview.AUTO);
    }

    public ReelAnimation(Reward winner, ReelConfig config, int forceFailedReel, BonusPlan.Preview preview) {
        this(winner, RewardCelebration.forReward(winner) == RewardCelebration.NORMAL ? winner : null, config, forceFailedReel, preview);
    }
    public ReelAnimation(Reward winner, Reward actualOrdinary, ReelConfig config, int forceFailedReel, BonusPlan.Preview preview) {
        this.winner = winner;
        this.config = config;
        hold = (int) Math.round(config.resultSeconds * 20);
        Random random = new Random();
        SlotOutcome original = SlotOutcome.create(winner, RewardCatalog.PREVIEW_REWARDS, config.reelMatchChance,
            forceFailedReel >= 0 ? SlotOutcome.DEFAULT_TRIPLE_REWARDS : config.tripleRewards,
            forceFailedReel != -1 ? forceFailedReel == 1 ? 1 : 0 : config.secondReelMissChance,
            forceFailedReel != -1 ? forceFailedReel == 2 ? 1 : 0 : config.thirdReelMissChance, random);
        bonus = BonusPlan.choose(winner, actualOrdinary, original, config.tripleRewards, config.bonusEnabled,
            config.bonusWinChance, config.bonusEmptyChance, preview, random);
        mining = bonus.active() ? new MiningTransition() : null;
        outcome = bonus.reels();
        celebration = bonus.active() ? RewardCelebration.NORMAL : RewardCelebration.forReward(winner);
        jackpot = !bonus.active() && RewardIcons.isJackpot(winner);
        spin = new SlotSpin((int) Math.round(config.durationSeconds * 20), outcome.suspense(), outcome.failedReel());
        duration = spin.durationTicks();
        finale = new RewardFinale(celebration, RewardIcons.forReward(winner));
        for (int i = 0; i < sparks.length; i++) sparks[i] = random.nextFloat();
        Map<String, ItemStack> icons = new HashMap<>();
        for (int reel = 0; reel < SlotSpin.REELS; reel++) {
            List<Card> strip = new ArrayList<>();
            for (int i = 0; i < spin.landingIndex(reel) + 4; i++) {
                Reward reward = i == spin.landingIndex(reel) ? outcome.symbol(reel)
                    : outcome.nearMiss() && reel == outcome.failedReel() && i == spin.landingIndex(reel) - 1 ? outcome.symbol(0)
                    : RewardCatalog.PREVIEW_REWARDS.get(random.nextInt(RewardCatalog.PREVIEW_REWARDS.size()));
                strip.add(new Card(reward, icons.computeIfAbsent(reward.name(), ignored -> RewardIcons.forReward(reward))));
            }
            reels.add(strip);
        }
    }

    public boolean tick() {
        elapsed++;
        if (!config.sound) stopSounds();
        else if (!config.coinSounds || config.coinVolume == 0) stopCoinSound();
        else if (coinSound != null) coinSound.setLevel((float) config.coinVolume);
        boolean click = false;
        for (int reel = 0; reel < SlotSpin.REELS; reel++) {
            int selected = Math.round(spin.position(reel, elapsed));
            if (!locked[reel] && spin.stopped(reel, elapsed)) {
                locked[reel] = true;
                playClick(0.75f + spin.stopOrder(reel) * 0.22f, 0.55f);
                if (reel == outcome.failedReel()) playEffect(SoundEvents.NOTE_BLOCK_BASS.value(), 0.55f, 0.55f);
                else if (outcome.suspense() && reel < 2 && (outcome.failedReel() < 0 || reel < outcome.failedReel()))
                    playEffect(SoundEvents.AMETHYST_BLOCK_CHIME, reel == 0 ? 0.7f : 1.0f, 0.6f);
            } else if (!locked[reel] && selected != lastClicks[reel]) {
                click = true;
            }
            lastClicks[reel] = selected;
        }
        // One ratchet sound per tick even when several reels cross a symbol.
        if (click) playClick(1.15f - 0.35f * elapsed / duration, 0.16f);
        if (!revealed && elapsed >= duration) {
            revealed = true;
            if (config.sound && config.coinSounds && config.coinVolume > 0 && celebration != RewardCelebration.NORMAL) {
                coinSound = new CoinSound(celebration, (float) config.coinVolume);
                Minecraft.getInstance().getSoundManager().play(coinSound);
            } else if (!outcome.nearMiss()) playEffect(SoundEvents.EXPERIENCE_ORB_PICKUP, jackpot ? 1.2f : 1.7f, 0.55f);
        }
        for (var cue : FinaleTimeline.cues(celebration)) if (elapsed - duration == cue.tick())
            playEffect(finaleSound(cue.sound()), cue.pitch(), cue.volume());
        if (bonus.active()) for (int hit : MiningTimeline.HITS) {
            if (elapsed - duration == hit - 5) playEffect(SoundEvents.PLAYER_ATTACK_SWEEP, 0.65f, 0.4f);
            if (elapsed - duration == hit) {
                playEffect(hit == MiningTimeline.OPEN ? SoundEvents.STONE_BREAK : SoundEvents.STONE_HIT, 0.65f, 0.85f);
                if (hit == MiningTimeline.OPEN) playEffect(SoundEvents.AMETHYST_BLOCK_CHIME, 0.5f, 0.6f);
            }
        }
        return !bonus.active() && elapsed >= duration + resultTicks();
    }

    private int resultTicks() { return config.effects ? FinaleTimeline.duration(celebration, hold) : hold; }

    public static SoundEvent finaleSound(FinaleTimeline.Sound sound) {
        return switch (sound) {
            case CRACK -> SoundEvents.GLASS_BREAK;
            case BOOM -> SoundEvents.GENERIC_EXPLODE.value();
            case LAUNCH -> SoundEvents.FIREWORK_ROCKET_LAUNCH;
            case SUN -> SoundEvents.BEACON_ACTIVATE;
            case CHIME -> SoundEvents.NOTE_BLOCK_CHIME.value();
            case FALL -> SoundEvents.TRIDENT_RETURN;
            case CATCH -> SoundEvents.ITEM_PICKUP;
            case LEVEL_UP -> SoundEvents.PLAYER_LEVELUP;
            case MEOW -> SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds().ambientSound().value();
            case PURR -> SoundEvents.CAT_SOUNDS.get(CatSoundVariants.SoundSet.CLASSIC).adultSounds().purrSound().value();
            case VILLAGER -> SoundEvents.VILLAGER_YES;
            case GEM -> SoundEvents.AMETHYST_BLOCK_CHIME;
            case HARP -> SoundEvents.NOTE_BLOCK_HARP.value();
            case SWIPE -> SoundEvents.PLAYER_ATTACK_SWEEP;
            case SCRATCH -> SoundEvents.GRINDSTONE_USE;
        };
    }

    public boolean isRevealed() { return elapsed >= duration; }
    public boolean releaseMessages() { return !bonus.active() && isRevealed(); }
    public boolean bonusReady() { return bonus.active() && elapsed >= duration + MiningTimeline.END; }
    public BonusPlan bonus() { return bonus; }
    public Reward winner() { return winner; }
    public int elapsedTicks() { return elapsed; }
    public int durationTicks() { return duration; }

    public void stopSounds() {
        stopCoinSound();
        for (var sound : effectSounds) Minecraft.getInstance().getSoundManager().stop(sound);
        effectSounds.clear();
    }

    private void stopCoinSound() {
        if (coinSound == null) return;
        coinSound.cancel();
        Minecraft.getInstance().getSoundManager().stop(coinSound);
        coinSound = null;
    }

    private void playEffect(SoundEvent event, float pitch, float volume) {
        if (!config.sound) return;
        SimpleSoundInstance sound = SimpleSoundInstance.forUI(event, pitch, volume);
        effectSounds.add(sound);
        Minecraft.getInstance().getSoundManager().play(sound);
    }

    private void playClick(float pitch, float volume) {
        if (config.sound) Minecraft.getInstance().getSoundManager().play(
            SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), pitch, volume));
    }

    public void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        float scale = (float) Math.min(config.scale, Math.min(g.guiWidth() / 600.0, g.guiHeight() / 400.0));
        float time = elapsed + delta.getGameTimeDeltaPartialTick(true);
        float after = Math.max(0, time - duration);
        boolean result = isRevealed();
        int accent = result ? resultColor() : 0xFFE4B666;
        g.nextStratum();
        float fade = config.effects && result ? FinaleTimeline.fade(celebration, after) : 1;
        if (celebration == RewardCelebration.NORMAL) fade = 1;
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha(0xFF070B0B, (int) (160 * fade)));
        g.pose().pushMatrix();
        // Counterbalance the lever so the whole machine is centred.
        g.pose().translate(g.guiWidth() / 2f - 17 * scale, g.guiHeight() / 2f);
        g.pose().scale(scale);
        float focus = config.effects ? spin.focusAmount(time) : 0;
        g.pose().scale(1 + focus * 0.6f);
        g.pose().translate(-spin.focusCentre(time) * focus, 0);
        if (bonus.active() && result && config.effects) {
            float zoom = MiningTimeline.zoom(after);
            g.pose().scale(1 + zoom * 0.5f); g.pose().translate(-118 * zoom * 0.4f, 0);
            for (int hit : MiningTimeline.HITS) if (after >= hit && after < hit + 6)
                g.pose().translate((float)Math.sin((after-hit)*3)*2*(1-(after-hit)/6), (float)Math.cos((after-hit)*2)*1.5f);
        }
        if (config.effects && result && (jackpot || outcome.triple()) && after < 11) {
            float shake = (1 - after / 11) * (celebration.strength() + (outcome.triple() ? 1 : 0)) * 0.65f;
            g.pose().translate((float) Math.sin(after * 2.7) * shake, (float) Math.cos(after * 2.1) * shake);
        }
        if (config.effects && result && celebration == RewardCelebration.ALLOY && after < 36) {
            float tremor = after < 24 ? 0.4f + 3.6f * FinaleTimeline.phase(after, 0, 24)
                : 7 * (1 - FinaleTimeline.phase(after, 24, 36));
            g.pose().translate((float) Math.sin(after * 2.3) * tremor, (float) Math.cos(after * 2.9) * tremor);
        }
        if (config.effects && result && celebration == RewardCelebration.ALLOY && after >= FinaleTimeline.ALLOY_BREAK)
            finale.brokenCabinet(g, after, capture -> drawMachine(capture, time, after, result, accent, false));
        else drawMachine(g, time, after, result, accent, true);
        if (bonus.active() && result) mining.render(g, after, resultColor(), config.effects);
        g.pose().popMatrix();
        if (config.effects && result)
            finale.render(g, after, scale, g.guiWidth() / 2f - 17 * scale, g.guiHeight() / 2f);
        if (bonus.active() && after >= MiningTimeline.FADE) {
            g.nextStratum();
            g.fill(0, 0, g.guiWidth(), g.guiHeight(), alpha(0xFF000000, (int)(255 * MiningTimeline.darkness(after))));
        }
    }

    private void drawMachine(GuiGraphicsExtractor g, float time, float after, boolean result, int accent, boolean animate) {
        cabinet(g, accent, config.effects ? time : 0);
        for (int reel = 0; reel < SlotSpin.REELS; reel++) drawReel(g, reel, time, accent);
        g.nextStratum();

        // The payout line has an open centre, keeping all three item icons clear.
        int line = result && outcome.triple() ? accent : 0xFFBD9655;
        border(g, -174, -31, 174, 32, line);
        g.fill(-174, -30, 174, -28, alpha(line, result ? 70 : 26));
        g.fill(-174, 29, 174, 31, alpha(line, result ? 70 : 26));
        arrow(g, -193, 0, true, line);
        arrow(g, 193, 0, false, line);
        if (result && !outcome.triple()) {
            // The real result stays identifiable even when effects are disabled or the suspense failed.
            int rx = CENTRES[outcome.resultReel()];
            border(g, rx - 55, -32, rx + 55, 33, accent);
            g.fill(rx - 55, -32, rx - 45, -29, accent); g.fill(rx + 45, -32, rx + 55, -29, accent);
            g.fill(rx - 55, 30, rx - 45, 33, accent); g.fill(rx + 45, 30, rx + 55, 33, accent);
        }

        for (int reel = 0; reel < SlotSpin.REELS; reel++) {
            int x = CENTRES[reel];
            bevel(g, x - 9, -109, x + 9, -102, 0xFF171B19, 0xFF74664D, 0xFF070B0A);
            int symbolColor = RewardIcons.color(outcome.symbol(reel));
            g.fill(x - 5, -107, x + 5, -104, spin.stopped(reel, time) ? symbolColor : 0xFF514837);
            if (config.effects && spin.stopped(reel, time)) {
                float age = time - spin.stopTick(reel);
                stopSparks(g, x, age, symbolColor, reel);
                shockwave(g, x, age, symbolColor);
            }
        }
        if (config.effects && animate) {
            int focusedReel = spin.focusReel(time);
            if (focusedReel >= 0) {
                int fx = CENTRES[focusedReel];
                int glow = alpha(0xFFFFDB8B, (int) (170 + 70 * Math.sin(time * 0.18)));
                border(g, fx - 56, -93, fx + 56, 93, glow);
                for (int r = 0; r < 3; r++) if (r != focusedReel)
                    g.fill(CENTRES[r] - 53, -90, CENTRES[r] + 53, 90, 0x48070A08);
            }
            dust(g, time);
            marquee(g, time, accent);
            if (result) {
                burst(g, after, accent);
                shower(g, after, accent);
                resultHalo(g, after, accent);
            }
            if (outcome.nearMiss()) {
                float missAge = time - spin.stopTick(outcome.failedReel());
                if (missAge >= 0 && missAge < 18) {
                    int fade = (int) (210 * (1 - missAge / 18));
                    int x = CENTRES[outcome.failedReel()];
                    border(g, x - 55, -33, x + 55, 34, alpha(RewardIcons.color(outcome.symbol(outcome.failedReel())), fade));
                    for (int r = 0; r < outcome.failedReel(); r++)
                        g.fill(CENTRES[r] - 53, -27, CENTRES[r] + 53, 28, alpha(0xFF6E2020, fade / 4));
                }
            }
        }

        if (!outcome.triple() && spin.stopped(outcome.resultReel(), time)) {
            int x = CENTRES[outcome.resultReel()], color = RewardIcons.color(outcome.symbol(outcome.resultReel()));
            g.nextStratum();
            g.fill(x - 51, -27, x + 51, 28, alpha(color, 24));
            border(g, x - 55, -32, x + 55, 33, color);
            border(g, x - 54, -31, x + 54, 32, alpha(color, 160));
        }
        bevel(g, -174, 112, 174, 151, 0xFF171E1B, 0xFF807051, 0xFF080C0A);
        if (result) {
            var font = Minecraft.getInstance().font;
            String label = font.plainSubstrByWidth((bonus.active() ? outcome.symbol(outcome.resultReel()) : winner).displayName(), 320);
            if (config.effects && celebration == RewardCelebration.QUICK_CLAW && after >= 8) label = "";
            g.pose().pushMatrix();
            float pop = config.effects ? 1 + 0.06f * (float) Math.sin(Math.min(after / 12, 1) * Math.PI) : 1;
            g.pose().translate(0, 131);
            g.pose().scale(pop);
            g.centeredText(font, label, 0, -4, accent);
            g.pose().popMatrix();
        } else {
            // A tiny moving minecart replaces all status/help/demo text.
            g.fill(-137, 138, 137, 140, 0xFF7D725B);
            for (int x = -133; x <= 133; x += 19) g.fill(x, 139, x + 6, 143, 0xFF4E3C2A);
            float cartX = -120 + 240 * Math.clamp(time / duration, 0, 1);
            item(g, minecart, cartX, 130, 1.45f, 0);
        }
        item(g, lantern, -202, 130, 1.55f, config.effects ? 0.09f * (float)Math.sin(time * 0.055) : 0);
        item(g, lantern, 202, 130, 1.55f, config.effects ? -0.09f * (float)Math.sin(time * 0.055) : 0);
        if (config.effects && result && animate) finale.drawCabinetEffect(g, after);
    }

    private void cabinet(GuiGraphicsExtractor g, int accent, float time) {
        g.fill(-250, -170, 251, 188, 0x70000000);
        if (config.effects) for (int i = 8; i > 0; i--)
            g.fill(-246 - i, -178 - i, 246 + i, 180 + i, alpha(accent, 3));
        bevel(g, -246, -178, 246, 180, 0xFF252D3D, 0xFF737989, 0xFF101320);

        // Staggered blocks and mortar form the stone backplate.
        for (int row = 0; row < 10; row++) {
            int y = -154 + row * 32;
            g.fill(-222, y, 222, y + 1, 0xFF171E2B);
            for (int x = -222 + (row % 2) * 28; x < 222; x += 56) {
                g.fill(x, y, x + 1, Math.min(y + 32, 164), 0xFF171E2B);
                g.fill(x + 3, y + 3, Math.min(x + 31, 222), y + 4, 0xFF394357);
            }
        }
        divanTrim(g, -244, -176, -222, 178);
        divanTrim(g, 222, -176, 244, 178);
        divanTrim(g, -244, -176, 244, -159);
        divanTrim(g, -244, 160, 244, 178);
        for (int x : new int[]{-233, 233}) for (int y : new int[]{-168, -94, 88, 169}) rivet(g, x, y);
        for (int x : new int[]{-159, 159}) rivet(g, x, 169);

        // Header, ore inlays and tools all use the active Minecraft item renderer.
        bevel(g, -177, -153, 177, -116, 0xFF182232, 0xFFD5B87C, 0xFF0B101B);
        g.centeredText(Minecraft.getInstance().font, "CRYSTAL NUCLEUS", 0, -142, 0xFFFFE1A1);
        // Five sockets echo the five Nucleus crystals; each pulses with its own colour.
        for (int i = 0; i < 5; i++) {
            int cx = (i - 2) * 22;
            int glow = alpha(CRYSTAL_COLORS[i], (int)(145 + 100 * (0.5 + 0.5 * Math.sin(time * 0.08 - i))));
            bevel(g, cx - 4, -126, cx + 5, -120, 0xFF0B131F, 0xFF5B6474, 0xFF0B1018);
            g.fill(cx - 2, -124, cx + 3, -122, glow);
        }
        item(g, pickaxe, -204, -133, 1.8f, -0.35f);
        item(g, divanFragment, 204, -133, 1.8f, 0.15f);

        for (int i = 0; i < 5; i++) {
            int x = i < 3 ? -204 : 204;
            int y = i < 3 ? -65 + i * 65 : -42 + (i - 3) * 84;
            crystalSocket(g, i, x, y, time);
        }
        bevel(g, -185, -98, 185, 100, 0xFF101522, 0xFFB6A276, 0xFF090C15);
        for (int x : new int[]{-180, 180}) for (int y : new int[]{-93, 95}) rivet(g, x, y);

        // Mechanical lever: a short pull at the start, then it springs upright.
        bevel(g, 245, -28, 264, 35, 0xFF363E36, 0xFF938875, 0xFF121B16);
        float pull = time < 12 ? (float) Math.sin(Math.PI * time / 12) : 0;
        int knobY = Math.round(-82 + pull * 69);
        bevel(g, 265, knobY + 10, 272, 12, 0xFFB2AB92, 0xFFE1D7B5, 0xFF514F42);
        bevel(g, 257, knobY, 282, knobY + 21, 0xFF416B70, 0xFFA7EEDD, 0xFF1C3A43);
        g.fill(261, knobY + 3, 278, knobY + 6, 0xFF80D9CD);
        rivet(g, 265, 17);
    }

    private void drawReel(GuiGraphicsExtractor g, int reel, float time, int accent) {
        int centre = CENTRES[reel];
        int left = centre - 53, right = centre + 53;
        float pos = spin.position(reel, time);
        List<Card> strip = reels.get(reel);
        g.enableScissor(left, -90, right, 90);
        g.fill(left, -90, right, 90, 0xFF111915);
        for (int i = Math.max(0, (int) pos - 2); i <= Math.min(strip.size() - 1, (int) pos + 2); i++) {
            float y = (pos - i) * STEP;
            int top = Math.round(y) - 28;
            Card card = strip.get(i);
            boolean landed = spin.stopped(reel, time) && i == spin.landingIndex(reel);
            boolean highlighted = landed && (reel == outcome.resultReel() || outcome.triple());
            bevel(g, left + 3, top, right - 3, top + 56,
                highlighted ? 0xFF3C4030 : 0xFF27332C, 0xFF4C5848, 0xFF151F19);
            g.fill(centre - 13, top + 50, centre + 13, top + 52, alpha(RewardIcons.color(card.reward), 180));
            float size = 2.5f;
            if (landed && config.effects) {
                float age = time - spin.stopTick(reel);
                size += 0.22f * (float) Math.sin(Math.min(age / 10, 1) * Math.PI);
            }
            item(g, card.icon, centre, y - 1, size, 0);
        }
        // Layer above item models; fade the first and third rows into the housing.
        g.nextStratum();
        g.fill(left, -90, right, -30, 0x3C07100C);
        g.fill(left, 32, right, 90, 0x3C07100C);
        if (isRevealed() && !outcome.triple() && reel != outcome.resultReel())
            g.fill(left, -29, right, 30, 0x3507100C);
        for (int i = 0; i < 8; i++) {
            int shade = alpha(0xFF080E0B, 15 + i * 15);
            g.fill(left, -69 - i * 3, right, -66 - i * 3, shade);
            g.fill(left, 66 + i * 3, right, 69 + i * 3, shade);
        }
        if (config.effects && spin.stopped(reel, time)) {
            int color = RewardIcons.color(outcome.symbol(reel));
            int pulse = (int) (150 + 70 * Math.sin((time - spin.stopTick(reel)) * 0.22));
            border(g, left + 2, -28, right - 2, 29, alpha(color, pulse));
        } else if (config.effects) {
            // Short edge trails suggest speed without blurring resource-pack icons.
            float speed = Math.max(0, spin.position(reel, time) - spin.position(reel, time - 1));
            for (int trail = 0; trail < 5; trail++) {
                int y = (int) ((time * 8 + trail * 37 + reel * 21) % 180) - 90;
                int length = (int) Math.min(22, speed * 50);
                g.fill(left + 1, y, left + 2, Math.min(90, y + length), alpha(0xFFFFD891, 100));
                g.fill(right - 2, y, right - 1, Math.min(90, y + length), alpha(0xFFFFD891, 65));
            }
        }
        g.disableScissor();
    }

    private static void divanTrim(GuiGraphicsExtractor g, int l, int t, int r, int b) {
        bevel(g, l, t, r, b, 0xFF8B7954, 0xFFF0D69B, 0xFF3B3540);
        if (b - t > r - l) {
            g.fill(l + 5, t + 3, r - 5, b - 3, 0xFF263747);
            g.fill(l + 6, t + 10, l + 8, b - 12, 0xFF477E7D);
        } else {
            g.fill(l + 5, t + 5, r - 5, b - 5, 0xFF263747);
            g.fill(l + 15, t + 5, r - 12, t + 6, 0xFF477E7D);
        }
    }

    private void crystalSocket(GuiGraphicsExtractor g, int index, int x, int y, float time) {
        int color = CRYSTAL_COLORS[index];
        float wave = (float)Math.sin(time * 0.065 + index * 1.7);
        bevel(g, x - 15, y - 22, x + 15, y + 23, 0xFF111B2A, 0xFF877957, 0xFF090E17);
        // Small energy conduits and floating native item models; no copied resource-pack images.
        g.fill(x - 11, y + 17, x + 11, y + 19, alpha(color, (int)(135 + wave * 45)));
        int scannerY = y - 18 + (int)((time * 0.32 + index * 9) % 34);
        g.fill(x - 12, scannerY, x - 10, scannerY + 4, alpha(color, 220));
        g.fill(x + 10, scannerY, x + 12, scannerY + 4, alpha(color, 220));
        item(g, crystals[index], x, y - 2 + wave * 2.2f, 1.4f, wave * 0.08f);
        if (config.effects) {
            int sx = x + Math.round(11 * (float)Math.cos(time * 0.045 + index));
            int sy = y + Math.round(15 * (float)Math.sin(time * 0.045 + index));
            g.fill(sx - 2, sy, sx + 3, sy + 1, alpha(color, 190));
            g.fill(sx, sy - 2, sx + 1, sy + 3, alpha(color, 190));
        }
    }

    private static void rivet(GuiGraphicsExtractor g, int x, int y) {
        bevel(g, x - 3, y - 3, x + 4, y + 4, 0xFF807A65, 0xFFBBB295, 0xFF302F26);
        g.fill(x - 1, y, x + 2, y + 1, 0xFF414538);
    }

    private static void item(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale, float rotation) {
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().rotate(rotation);
        g.pose().scale(scale);
        g.fakeItem(stack, -8, -8);
        g.pose().popMatrix();
    }

    private void dust(GuiGraphicsExtractor g, float time) {
        for (int i = 0; i < 18; i++) {
            float phase = (time * (0.002f + sparks[i * 4 + 1] * 0.002f) + sparks[i * 4 + 2]) % 1;
            int x = (int) (-212 + sparks[i * 4] * 424);
            int y = (int) (151 - phase * 300);
            if (Math.abs(x) < 186 && y > -155 && y < 151) continue;
            g.fill(x, y, x + 1, y + 2, alpha(0xFFFFD48A, (int) (95 * Math.sin(phase * Math.PI))));
        }
    }

    private void stopSparks(GuiGraphicsExtractor g, int x, float age, int color, int reel) {
        if (age > 17) return;
        for (int i = 0; i < 13; i++) {
            int px = x + Math.round((sparks[(i + reel * 7) * 4] - 0.5f) * age * 8);
            int py = -32 - Math.round(age * (1 + sparks[(i + reel * 7) * 4 + 1] * 2)) + Math.round(age * age * 0.09f);
            g.fill(px, py, px + 2, py + 2, alpha(color, (int) (220 * (1 - age / 17))));
        }
    }

    private void burst(GuiGraphicsExtractor g, float age, int color) {
        float life = celebration.strength() > 0 || outcome.triple() ? 40 : 28;
        if (age >= life) return;
        int count = 30 + celebration.strength() * 14 + (outcome.triple() ? 16 : 0);
        for (int i = 0; i < count; i++) {
            float angle = sparks[i * 4] * (float) (2 * Math.PI);
            float speed = 2 + sparks[i * 4 + 1] * (jackpot ? 5 : 3);
            int x = Math.round((float) Math.cos(angle) * speed * age);
            int y = Math.round((float) Math.sin(angle) * speed * age + 0.06f * age * age);
            if (Math.abs(x) > 218 || y < -108 || y > 104) continue;
            int size = sparks[i * 4 + 2] > 0.7f ? 3 : 2;
            g.fill(x, y, x + size, y + size, alpha(i % 3 == 0 ? 0xFFFFECC3 : color, (int) (230 * (1 - age / life))));
        }
    }

    private int resultColor() {
        if (bonus.active() || outcome.nearMiss()) return RewardIcons.color(outcome.symbol(outcome.resultReel()));
        return celebration == RewardCelebration.JADERALD ? 0xFF8BE9AB : RewardIcons.color(winner);
    }

    private void shockwave(GuiGraphicsExtractor g, int x, float age, int color) {
        if (age > 16) return;
        int grow = Math.round(age * 2);
        border(g, x - 24 - grow, -22 - grow, x + 24 + grow, 22 + grow,
            alpha(color, (int) (150 * (1 - age / 16))));
    }

    private void marquee(GuiGraphicsExtractor g, float time, int color) {
        int chase = (int) (time / 3) % 12;
        for (int i = 0; i < 12; i++) {
            int x = -198 + i * 36;
            int brightness = isRevealed() ? (int) (140 + 70 * Math.sin(time * 0.12 + i * 0.5))
                : (i == chase || i == (chase + 11) % 12 ? 210 : 45);
            g.fill(x - 2, -169, x + 2, -166, alpha(color, brightness));
            g.fill(-x - 2, 168, -x + 2, 171, alpha(color, brightness));
        }
    }

    private void resultHalo(GuiGraphicsExtractor g, float age, int color) {
        if (age > 32) return;
        int brightness = (int) (160 * (1 - age / 32));
        for (int reel = 0; reel < 3; reel++) {
            if (reel != outcome.resultReel() && !outcome.triple()) continue;
            int x = CENTRES[reel];
            float radius = 23 + Math.min(age, 14);
            for (int point = 0; point < 12; point++) {
                double angle = point * Math.PI / 6 + age * 0.035;
                int px = x + Math.round((float) Math.cos(angle) * radius);
                int py = Math.round((float) Math.sin(angle) * radius);
                g.fill(px - 1, py - 1, px + 2, py + 2, alpha(color, brightness));
            }
        }
    }

    private void shower(GuiGraphicsExtractor g, float age, int color) {
        if (age > 46 || (celebration == RewardCelebration.NORMAL && !outcome.triple())) return;
        int count = 12 + celebration.strength() * 6 + (outcome.triple() ? 8 : 0);
        for (int i = 0; i < count; i++) {
            float t = age - sparks[i * 4] * 10;
            if (t < 0 || t > 33) continue;
            float x = (sparks[i * 4 + 1] - 0.5f) * (70 + t * 14);
            float y = -32 - t * (3.4f + sparks[i * 4 + 2] * 2.4f) + t * t * 0.23f;
            if (Math.abs(x) > 218 || y > 104 || y < -111 || (Math.abs(x) < 172 && Math.abs(y) < 30)) continue;
            float size = (0.5f + sparks[i * 4 + 3] * 0.28f) * Math.min(1, (33 - t) / 5);
            ItemStack particle = celebration == RewardCelebration.JADERALD && i % 2 == 0 ? emerald : coin;
            item(g, particle, x, y, size, (sparks[i * 4 + 2] - 0.5f) * t * 0.5f);
        }
    }

    private static void arrow(GuiGraphicsExtractor g, int x, int y, boolean right, int color) {
        for (int i = 0; i < 7; i++) {
            int px = x + (right ? i : -i);
            g.fill(px, y - 7 + i, px + 1, y + 8 - i, color);
        }
    }
    private static int alpha(int color, int a) { return (Math.clamp(a, 0, 255) << 24) | (color & 0xFFFFFF); }
    private static void bevel(GuiGraphicsExtractor g, int l, int t, int r, int b, int fill, int light, int dark) {
        g.fill(l, t, r, b, fill);
        g.fill(l, t, r, t + 2, light); g.fill(l, t, l + 2, b, light);
        g.fill(l, b - 2, r, b, dark); g.fill(r - 2, t, r, b, dark);
    }
    private static void border(GuiGraphicsExtractor g, int l, int t, int r, int b, int c) {
        g.fill(l, t, r, t + 1, c); g.fill(l, b - 1, r, b, c);
        g.fill(l, t, l + 1, b, c); g.fill(r - 1, t, r, b, c);
    }
}
