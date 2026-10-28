package dev.nucleusreel.core;

/** Gem hue for gemstones; item rarity for other catalog entries. */
public final class RewardColor {
    private RewardColor() {}
    public static int of(Reward reward) {
        String name=RewardCatalog.normalize(reward.name());
        if(name.contains("gemstone") || name.contains("crystal")) {
            if(name.contains("ruby"))return 0xFFEF5964;
            if(name.contains("amber"))return 0xFFFFAE50;
            if(name.contains("jade"))return 0xFF69DE9F;
            if(name.contains("sapphire")||name.contains("saphhire"))return 0xFF6CB9FF;
            if(name.contains("amethyst"))return 0xFFB58AF4;
            if(name.contains("topaz"))return 0xFFFFDB65;
            if(name.contains("jasper"))return 0xFFF185C8;
        }
        var definition=RewardCatalog.find(reward.name());
        if(definition==null||definition.tier()==null)return 0xFFCCD8ED;
        return switch(definition.tier()) {
            case "UNCOMMON" -> 0xFF69DE9F;
            case "RARE" -> 0xFF6DAAFF;
            case "EPIC" -> 0xFFD28BEE;
            case "LEGENDARY" -> 0xFFFFBB55;
            case "MYTHIC" -> 0xFFFF88D5;
            case "SPECIAL","VERY_SPECIAL" -> 0xFFFF7777;
            default -> 0xFFCCD8ED;
        };
    }
}
