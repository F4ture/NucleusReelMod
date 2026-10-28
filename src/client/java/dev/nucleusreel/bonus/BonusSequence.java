package dev.nucleusreel.bonus;

import dev.nucleusreel.ReelConfig;
import dev.nucleusreel.RewardIcons;
import dev.nucleusreel.RewardFinale;
import dev.nucleusreel.ReelAnimation;
import dev.nucleusreel.core.BonusTimeline;
import dev.nucleusreel.core.FinaleTimeline;
import dev.nucleusreel.core.Reward;
import dev.nucleusreel.core.RewardCelebration;
import dev.nucleusreel.music.CoinSound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

public final class BonusSequence {
    private final BonusTimeline timeline;
    private final ReelConfig config;
    private final Reward actual;
    private final CaveGeometry scenery = new CaveGeometry();
    private final CaveLife life = new CaveLife();
    private final RewardFinale finale;
    private final ItemStack reward, torch = new ItemStack(Items.TORCH);
    private final List<SimpleSoundInstance> sounds = new ArrayList<>();
    private CoinSound coins;
    private int highlighted = 1;
    private int ambienceTicks;
    public BonusSequence(boolean won, Reward actual, ReelConfig config) {
        this.timeline = new BonusTimeline(won); this.config = config; this.actual = actual;
        reward = won ? RewardIcons.forReward(actual) : ItemStack.EMPTY;
        finale = won ? new RewardFinale(RewardCelebration.ALLOY, reward) : null;
    }
    public BonusTimeline timeline() { return timeline; }
    public boolean revealed() { return timeline.revealed(); }
    public int highlighted() { return highlighted; }
    public void highlight(int door) { if (door >= 0 && door < 3 && timeline.canChoose()) highlighted = door; }
    public boolean choose(int door) {
        if (!timeline.choose(door)) return false;
        highlighted = door; sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 0.5f); return true;
    }
    public boolean tick() {
        timeline.tick();
        ambienceTicks = (ambienceTicks + 1) % 4800;
        int age = timeline.age();
        if (!config.sound) stopSounds();
        boolean quiet = timeline.stage() != BonusTimeline.Stage.ROOM || !timeline.won() || !BonusTimeline.flying(age);
        if (quiet) {
            if (ambienceTicks == 1) sound(SoundEvents.AMBIENT_CAVE.value(), 0.8f, 0.15f);
            if (ambienceTicks % 160 == 49) sound(SoundEvents.POINTED_DRIPSTONE_DRIP_WATER, 0.85f, 0.2f);
            if (ambienceTicks % 120 == 16 && (timeline.stage() != BonusTimeline.Stage.ROOM || timeline.light(age) > 0.1))
                sound(SoundEvents.FIRE_AMBIENT, 0.8f, 0.12f);
            if (timeline.stage() == BonusTimeline.Stage.CHOICE && ambienceTicks % 360 == 150)
                sound(SoundEvents.CHAIN_STEP, 0.6f, 0.18f);
        }
        if (timeline.stage() == BonusTimeline.Stage.WALK) {
            if (age % 18 == 6) sound(SoundEvents.STONE_STEP, 0.7f + (age % 36) * 0.006f, 0.3f);
            if (age % 18 == 11) sound(SoundEvents.STONE_STEP, 0.55f, 0.08f);
        }
        if (timeline.stage() == BonusTimeline.Stage.ROOM) {
            if (age == 1) sound(SoundEvents.FLINTANDSTEEL_USE, 0.7f, 0.35f);
            if (age == BonusTimeline.REVEAL) {
                if (timeline.won()) {
                    sound(SoundEvents.BEACON_ACTIVATE, 1.0f, 0.65f);
                    if (config.sound && config.coinSounds && config.coinVolume > 0) {
                        coins = new CoinSound(RewardCelebration.ALLOY, (float)config.coinVolume);
                        Minecraft.getInstance().getSoundManager().play(coins);
                    }
                } else sound(SoundEvents.FIRE_EXTINGUISH, 0.75f, 0.6f);
            }
            if (!timeline.won() && age == BonusTimeline.REVEAL + 12) sound(SoundEvents.FIRE_EXTINGUISH, 0.55f, 0.4f);
            if (timeline.won() && (age == 100 || age == 114))
                sound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.6f + age * 0.01f, 0.5f);
            if (timeline.won() && BonusTimeline.flying(age)) {
                int flightAge = (int)BonusTimeline.flightAge(age);
                for (var cue : FinaleTimeline.cues(RewardCelebration.ALLOY))
                    if (cue.tick() == flightAge && cue.sound() != FinaleTimeline.Sound.BOOM)
                        sound(ReelAnimation.finaleSound(cue.sound()), cue.pitch(), cue.volume());
            }
        }
        return timeline.stage() == BonusTimeline.Stage.DONE;
    }
    private void sound(SoundEvent event, float pitch, float volume) {
        if (!config.sound) return;
        var instance = SimpleSoundInstance.forUI(event, pitch, volume);
        // Choice may wait indefinitely: keep only a bounded set of tracked ambient sounds.
        if (sounds.size() >= 32) Minecraft.getInstance().getSoundManager().stop(sounds.removeFirst());
        sounds.add(instance); Minecraft.getInstance().getSoundManager().play(instance);
    }
    public void stopSounds() {
        var manager = Minecraft.getInstance().getSoundManager();
        for (var sound : sounds) manager.stop(sound);
        sounds.clear();
        if (coins != null) { coins.cancel(); manager.stop(coins); coins = null; }
    }
    public static float scale(int width, int height) { return Math.min(width / 600f, height / 400f); }
    public static int hit(double mouseX, double mouseY, int width, int height) {
        float scale = scale(width, height);
        return BonusTimeline.hit((mouseX - (width - 600 * scale) / 2) / scale,
            (mouseY - (height - 400 * scale) / 2) / scale);
    }
    public void render(GuiGraphicsExtractor g, float partial) {
        float age = timeline.age() + Math.clamp(partial, 0, 1);
        float scale = scale(g.guiWidth(), g.guiHeight());
        g.nextStratum(); g.fill(0, 0, g.guiWidth(), g.guiHeight(), 0xFF050810);
        g.pose().pushMatrix();
        g.pose().translate((g.guiWidth() - 600 * scale) / 2, (g.guiHeight() - 400 * scale) / 2);
        g.pose().scale(scale);
        if (timeline.stage() == BonusTimeline.Stage.CHOICE || timeline.stage() == BonusTimeline.Stage.WALK)
            entrance(g, age);
        else chamber(g, age);
        g.pose().popMatrix();
        if (timeline.stage() == BonusTimeline.Stage.ROOM && timeline.won() && BonusTimeline.flying(age)) {
            float x = g.guiWidth()/2f;
            float y = (g.guiHeight()-400*scale)/2 + 177*scale;
            finale.renderAlloyContinuation(g, BonusTimeline.flightAge(age), scale, x, y);
            float fade = FinaleTimeline.phase(age, BonusTimeline.WIN_END-20, BonusTimeline.WIN_END);
            g.nextStratum();g.fill(0,0,g.guiWidth(),g.guiHeight(),alpha(0xFF000000,(int)(fade*255)));
        }
    }
    private void entrance(GuiGraphicsExtractor g, float age) {
        boolean walking = timeline.stage() == BonusTimeline.Stage.WALK;
        float progress = walking ? FinaleTimeline.phase(age, 0, BonusTimeline.WALK - 18) : 0;
        g.pose().pushMatrix();
        if (walking && config.effects) {
            float zoom=0.16f*progress;
            g.pose().translate(300,230);g.pose().scale(1+zoom);
            g.pose().translate(-300-(BonusTimeline.doorX(timeline.door())-300)*zoom,-230);
        }
        scenery.entrance.draw(g);
        life.render(g,ambienceTicks,false,config.effects);
        torch(g, 52, 184, 0.65f, age, 0);
        torch(g, 548, 184, 0.65f, age, 2);
        g.nextStratum();
        title(g, "CRYSTAL DEPTHS", 39, 0xFFD6C7A6, 1.25f);
        for (int i = 0; i < 3; i++) {
            int x = (int)BonusTimeline.doorX(i);
            boolean selected = i == highlighted && (timeline.canChoose() || walking);
            int color = selected ? 0xFFFFD68C : 0xFF9297A6;
            g.centeredText(Minecraft.getInstance().font, Integer.toString(i + 1), x, 77, color);
            if (selected) {
                int pulse = config.effects ? 125 + (int)(45 * Math.sin(age * 0.1)) : 150;
                g.fill(x - 49, 265, x + 49, 268, alpha(0xFFFFCF75, pulse));
                g.fill(x - 49, 128, x - 47, 266, alpha(0xFFFFCF75, pulse));
                g.fill(x + 47, 128, x + 49, 266, alpha(0xFFFFCF75, pulse));
            }
        }
        float x = walking ? 300 + (BonusTimeline.doorX(timeline.door()) - 300) * FinaleTimeline.phase(age, 0, 62) : 300;
        float feet = 347 - progress * 96;
        g.fill((int)x - (int)(18 * (1 - progress)), (int)feet - 3, (int)x + (int)(18 * (1 - progress)), (int)feet + 2, 0x70000000);
        SkinAvatar.draw(g, x, feet, 2.25f * (1 - progress * 0.84f), age * 0.6f, walking);
        g.pose().popMatrix();
        if (!walking) {
            String route = switch(highlighted) { case 0 -> "Jade Vein"; case 1 -> "Divan's Shaft"; default -> "Amethyst Grotto"; };
            title(g, route, 356, 0xFFE0D8C8, 1);
            title(g, "Click or 1 · 2 · 3    /    Esc — skip", 382, 0xFF8A93A5, 0.8f);
        }
        float fade = walking ? FinaleTimeline.phase(age, BonusTimeline.WALK - 28, BonusTimeline.WALK) : 1 - FinaleTimeline.phase(age, 0, BonusTimeline.ENTER);
        g.nextStratum();g.fill(0, 0, 600, 400, alpha(0xFF000000, (int)(fade * 255)));
    }
    private void chamber(GuiGraphicsExtractor g, float age) {
        scenery.room.draw(g);
        life.render(g,ambienceTicks,true,config.effects);
        float light = timeline.light(age);
        // Broad warm pools plus a darkening room; only a few cached meshes and native item draws.
        torch(g, 151, 186, light, age, 0);
        torch(g, 449, 186, light, age, 1);
        scenery.pedestal.draw(g);
        g.nextStratum();
        float reveal = FinaleTimeline.phase(age, BonusTimeline.REVEAL, BonusTimeline.REVEAL + 24);
        if (timeline.won()) {
            if (reveal > 0 && !BonusTimeline.flying(age)) {
                g.pose().pushMatrix();g.pose().translate(300, 177);g.pose().scale(1.5f * reveal);
                scenery.glow.draw(g);g.pose().popMatrix();
                if (config.effects) for (int i = 0; i < 12; i++) {
                    double angle = i * Math.PI / 6 + age * 0.015;
                    int x = 300 + (int)(Math.cos(angle) * 58), y = 175 + (int)(Math.sin(angle) * 36);
                    g.fill(x - 1, y - 1, x + 2, y + 2, alpha(0xFFFFDA8A, (int)(reveal * 220)));
                }
                item(g, reward, 300, 177 - (config.effects ? (float)Math.sin(age * 0.08) * 3 * (1-FinaleTimeline.phase(age,110,BonusTimeline.FLIGHT_START)) : 0), 3.6f * reveal);
                title(g, actual.displayName(), 324, alpha(0xFFFFDC8B, (int)(255 * reveal)), 1.45f);
            }
        } else {
            g.fill(0, 0, 600, 400, alpha(0xFF030711, (int)((1 - light) * 155)));
            if (age >= BonusTimeline.LIGHTS_OUT) {
                title(g, "EMPTY CHAMBER", 324, 0xFFA4A9B7, 1.25f);
                title(g, actual.displayName(), 349, 0xFF757F92, 0.85f);
            }
        }
        // Keep the same actual skin visible in the foreground of the destination room.
        if (!timeline.won() || age < BonusTimeline.FLIGHT_START+20) SkinAvatar.draw(g, 390, 371, 1.85f, 0, false);
        if (timeline.revealed() && (!timeline.won() || !BonusTimeline.flying(age))) title(g, "Esc — close", 383, 0xFF7C8596, 0.8f);
        float fade = 1 - FinaleTimeline.phase(age, 0, 36);
        if(timeline.won()) fade=Math.max(fade,0.92f*FinaleTimeline.phase(age,BonusTimeline.FLIGHT_START,BonusTimeline.FLIGHT_START+65));
        else fade = Math.max(fade, FinaleTimeline.phase(age, BonusTimeline.EMPTY_END-24, BonusTimeline.EMPTY_END));
        g.nextStratum();g.fill(0, 0, 600, 400, alpha(0xFF000000, (int)(fade * 255)));
    }
    private void torch(GuiGraphicsExtractor g, int x, int y, float light, float time, int seed) {
        float flicker = config.effects ? 0.9f + 0.1f * (float)Math.sin(time * 0.23 + seed * 3.4) : 1;
        if (light > 0.01) {
            g.pose().pushMatrix();g.pose().translate(x, y - 12);g.pose().scale(1.5f * light * flicker);
            scenery.glow.draw(g);g.pose().popMatrix();
        }
        g.fill(x - 8, y + 14, x + 8, y + 18, 0xFF0B111C);
        if (light > 0.12) item(g, torch, x, y, 1.7f);
        else { g.fill(x - 2, y - 5, x + 3, y + 13, 0xFF47392E);g.fill(x - 3, y - 8, x + 4, y - 3, 0xFF22232A); }
        if (light > 0.01) {
            int alpha = (int)(220 * light);
            g.fill(x - 3, y - 25, x + 4, y - 11, alpha(0xFFFF983F, alpha));
            g.fill(x - 1, y - 22, x + 2, y - 12, alpha(0xFFFFECB0, alpha));
        }
        if (!timeline.won() && timeline.stage() == BonusTimeline.Stage.ROOM && time > BonusTimeline.REVEAL && time < BonusTimeline.LIGHTS_OUT+24 && config.effects) {
            for (int i=0;i<4;i++) {
                int sy = y - 20 - (int)((time - BonusTimeline.REVEAL + i * 6) * 0.75);
                g.fill(x - 2 + i % 2 * 3, sy, x + 2 + i % 2 * 3, sy + 4, alpha(0xFF737886, (int)((BonusTimeline.LIGHTS_OUT+24-time)*3)));
            }
        }
    }
    private static void item(GuiGraphicsExtractor g, ItemStack stack, float x, float y, float scale) {
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale);g.fakeItem(stack,-8,-8);g.pose().popMatrix();
    }
    private static void title(GuiGraphicsExtractor g, String text, float y, int color, float scale) {
        g.pose().pushMatrix();g.pose().translate(300,y);g.pose().scale(scale);
        g.centeredText(Minecraft.getInstance().font,text,0,0,color);g.pose().popMatrix();
    }
    private static int alpha(int color,int a) { return (Math.clamp(a,0,255)<<24)|(color&0xFFFFFF); }
}
