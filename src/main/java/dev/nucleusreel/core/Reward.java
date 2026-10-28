package dev.nucleusreel.core;

public record Reward(String name, int count) {
    public Reward {
        if (name == null || name.isBlank() || count < 1) throw new IllegalArgumentException("Invalid reward");
    }

    public String displayName() {
        return count == 1 ? name : name + " x" + count;
    }
}
