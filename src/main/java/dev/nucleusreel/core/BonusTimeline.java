package dev.nucleusreel.core;

/** Input-independent timeline: the door changes the route, never the reward. No automatic selection. */
public final class BonusTimeline {
    public enum Stage { CHOICE, WALK, ROOM, DONE }
    public static final int ENTER = 48, WALK = 126, REVEAL = 80;
    public static final int LIGHTS_OUT = 112, FLIGHT_START = 130;
    public static final int WIN_END = FLIGHT_START + 240 - FinaleTimeline.ALLOY_BREAK, EMPTY_END = 210;
    private final boolean won;
    private Stage stage = Stage.CHOICE;
    private int age, door = -1;
    public BonusTimeline(boolean won) { this.won = won; }
    public Stage stage() { return stage; }
    public int age() { return age; }
    public int door() { return door; }
    public boolean won() { return won; }
    public boolean canChoose() { return stage == Stage.CHOICE && age >= ENTER; }
    public boolean choose(int index) {
        if (index < 0 || index > 2 || !canChoose()) return false;
        door = index; age = 0; stage = Stage.WALK; return true;
    }
    public void tick() {
        if (stage == Stage.DONE) return;
        // Waiting never finishes or selects a path, and the bounded clock cannot overflow.
        age = Math.min(age + 1, 12000);
        if (stage == Stage.WALK && age >= WALK) { stage = Stage.ROOM; age = 0; }
        else if (stage == Stage.ROOM && age >= (won ? WIN_END : EMPTY_END)) { stage = Stage.DONE; age = 0; }
    }
    public boolean revealed() { return stage == Stage.DONE || stage == Stage.ROOM && age >= REVEAL; }
    public float light(float time) { return won ? 1 : 1 - FinaleTimeline.phase(time, REVEAL, LIGHTS_OUT); }
    public static float flightAge(float roomAge) { return FinaleTimeline.ALLOY_BREAK + roomAge - FLIGHT_START; }
    public static boolean flying(float roomAge) { return roomAge >= FLIGHT_START; }
    public static float doorX(int index) { return 136 + index * 164; }
    public static int hit(double x, double y) {
        for (int i = 0; i < 3; i++) if (x >= doorX(i) - 50 && x <= doorX(i) + 50 && y >= 106 && y <= 270) return i;
        return -1;
    }
}
