package dev.nucleusreel.core;

import com.google.gson.Gson;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Appearance metadata; texture images stay in the user's active packs. */
public final class RewardCatalog {
    public record Definition(String id, String name, String material, String itemModel, String tier,
                             boolean glowing, String skinValue, String skinSignature,
                             String enchantment, int enchantmentLevel) {}
    private static final Map<String, Definition> BY_NAME = load();
    private RewardCatalog() {}

    private static Map<String, Definition> load() {
        try (var stream = RewardCatalog.class.getResourceAsStream("/assets/nucleusreel/rewards.json")) {
            if (stream == null) throw new IllegalStateException("Missing reward catalog");
            Definition[] definitions = new Gson().fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), Definition[].class);
            Map<String, Definition> result = new LinkedHashMap<>();
            for (Definition definition : definitions) {
                if (definition.id() == null || definition.material() == null) throw new IllegalStateException("Invalid reward catalog");
                result.put(normalize(definition.name()), definition);
                // The public item API currently spells this name "Saphhire".
                if (definition.id().equals("SAPPHIRE_CRYSTAL")) result.put("sapphire crystal", definition);
                if (definition.enchantment() != null) result.put(normalize(definition.name().replace("Enchanted Book (", "").replace(")", "")), definition);
            }
            return Collections.unmodifiableMap(result);
        } catch (Exception exception) { throw new IllegalStateException("Cannot load reward appearance data", exception); }
    }

    public static String normalize(String name) {
        return LootParser.plain(name).strip().replaceFirst("^[^\\p{L}\\p{N}]+", "").toLowerCase(Locale.ROOT);
    }
    public static Definition find(String name) { return BY_NAME.get(normalize(name)); }
    public static Collection<Definition> definitions() { return new LinkedHashSet<>(BY_NAME.values()); }
    public static final List<Reward> PREVIEW_REWARDS = List.of(
        new Reward("Fine Jade Gemstone", 1), new Reward("Divan Fragment", 1), new Reward("Jaderald", 1),
        new Reward("Helix Fossil", 1), new Reward("Quick Claw", 1), new Reward("Fine Ruby Gemstone", 1),
        new Reward("Gemstone Mixture", 1), new Reward("Divan's Alloy", 1), new Reward("Prehistoric Egg", 1),
        new Reward("Flawless Jasper Gemstone", 1), new Reward("Claw Fossil", 1), new Reward("Precious Pearl", 1)
    );
}
