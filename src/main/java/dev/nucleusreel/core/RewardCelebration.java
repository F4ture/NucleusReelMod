package dev.nucleusreel.core;

/** Only the actual highlighted result selects a coin sound, never filler symbols. */
public enum RewardCelebration {
    NORMAL(null, 0), JADERALD("coins_jaderald", 1), QUICK_CLAW("coins_claw", 2), ALLOY("coins_alloy", 3);
    private final String sound;
    private final int strength;
    RewardCelebration(String sound, int strength) { this.sound = sound; this.strength = strength; }
    public String sound() { return sound; }
    public int strength() { return strength; }
    public static RewardCelebration forReward(Reward reward) {
        return switch (RewardCatalog.normalize(reward.name())) {
            case "jaderald" -> JADERALD;
            case "quick claw" -> QUICK_CLAW;
            case "divan's alloy" -> ALLOY;
            default -> NORMAL;
        };
    }
}
