package dev.nucleusreel.core;

import java.util.List;
import static dev.nucleusreel.core.LootParser.Type.*;

public final class LootParserTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        LootParser parser = new LootParser();
        check(parser.accept("    Divan's Alloy").type() == NONE, "Ignore items outside a bundle");
        check(parser.accept("[VIP] Player: CRYSTAL NUCLEUS LOOT BUNDLE").type() == NONE, "Ignore player chat");
        check(parser.accept("  §r§5§lCRYSTAL NUCLEUS LOOT BUNDLE").type() == START, "Recognize formatted server header");
        check(parser.accept("    §r§a☘ Fine Jade Gemstone §r§8x32").reward().equals(new Reward("Fine Jade Gemstone", 32)), "Formatted quantities and glyphs");
        check(parser.accept("    Divan's Alloy").reward().count() == 1, "Implicit amount");
        check(parser.accept("    Divan’s Alloy").reward().name().equals("Divan's Alloy"), "Normalize typographic apostrophe");
        check(parser.accept("    2x Divan Fragment").reward().count() == 2, "Prefix quantity");
        check(parser.accept("    x3 Jaderald").reward().count() == 3, "Alternate prefix quantity");
        check(parser.accept("    Fine Ruby Gemstone x1,024").reward().count() == 1024, "Thousands separator");
        check(parser.accept("    Gemstone Powder x1,000").type() == NONE, "Ignore powder");
        check(parser.accept("    REWARDS").type() == NONE, "Ignore subheading");
        check(parser.accept("Party > Player: Quick Claw").type() == NONE, "Do not suppress unrelated chat");
        check(parser.accept("    Divan's Alloy x9999999999999").type() == NONE, "Fail open on invalid amounts");
        check(parser.accept("    Divan's Alloy x0").type() == NONE, "Reject zero amounts");
        check(parser.accept("    Enchanted Book (Lapidary I)").type() == REWARD, "Read book types");
        check(parser.accept("§3§l" + "▬".repeat(64)).type() == END, "End formatted loot block");
        check(parser.accept("    Quick Claw").type() == NONE, "No capture after end");
        parser.accept("CRYSTAL NUCLEUS LOOT BUNDLE");
        parser.reset();
        check(!parser.isOpen(), "Reset parser on timeout / world change");
        List<Reward> loot = List.of(new Reward("Fine Jade Gemstone", 32), new Reward("Quick Claw", 1), new Reward("Divan's Alloy", 1));
        check(RewardPriority.best(loot, RewardPriority.DEFAULT).name().equals("Divan's Alloy"), "Alloy wins over decorative reel entries");
        check(RewardPriority.best(loot, List.of("Quick Claw", "Divan's Alloy")).name().equals("Quick Claw"), "User priority works");
        check(RewardPriority.best(List.of(new Reward("Fine Jade Gemstone", 32)), RewardPriority.DEFAULT)
            .name().equals("Fine Jade Gemstone"), "Ordinary run does not invent a rare reward");
        var mixed = List.of(new Reward("Flawless Jasper Gemstone", 1), new Reward("Jaderald", 2));
        check(RewardPriority.best(mixed, RewardPriority.DEFAULT, SlotOutcome.DEFAULT_TRIPLE_REWARDS).equals(mixed.get(1)),
            "A real Jaderald is celebrated even if a higher ordinary priority appears in the same bundle");
        check(RewardPriority.best(loot, List.of("Fine", "Quick Claw"), SlotOutcome.DEFAULT_TRIPLE_REWARDS).name().equals("Quick Claw"),
            "The chosen result must be rare when the bundle contains one");
        check(RewardPriority.best(mixed, RewardPriority.DEFAULT, List.of()).equals(mixed.get(0)), "Empty rare list respects ordinary priority");
        check(RewardPriority.best(loot, RewardPriority.DEFAULT, List.of("Fine")).equals(loot.get(0)), "Custom rare classification is respected");
        System.out.println("Passed " + checks + " parser / priority checks.");
    }
}
