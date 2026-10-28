package dev.nucleusreel.core;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses server loot blocks. Never treats a bare item announcement as a run. */
public final class LootParser {
    public enum Type { NONE, START, REWARD, END }
    public record Result(Type type, Reward reward) {}
    private static final Pattern FORMAT = Pattern.compile("(?i)§[0-9A-FK-ORX]");
    private static final Pattern BORDER = Pattern.compile("▬{20,}");
    private static final Pattern SUFFIX = Pattern.compile("^(.+?)\\s+[x×]([0-9][0-9,]*)$");
    private static final Pattern PREFIX = Pattern.compile("^(?:[x×]([0-9][0-9,]*)|([0-9][0-9,]*)[x×]?)\\s+(.+)$");
    private boolean open;

    public static String plain(String text) {
        return FORMAT.matcher(text).replaceAll("").replace('\u00a0', ' ').replace('\u2019', '\'');
    }

    public boolean isOpen() { return open; }
    public void reset() { open = false; }

    public Result accept(String raw) {
        String line = plain(raw);
        if (line.strip().equals("CRYSTAL NUCLEUS LOOT BUNDLE")) {
            open = true;
            return new Result(Type.START, null);
        }
        if (!open) return new Result(Type.NONE, null);
        if (BORDER.matcher(line.strip()).matches()) {
            open = false;
            return new Result(Type.END, null);
        }
        if (!line.startsWith("    ")) return new Result(Type.NONE, null);
        String name = line.strip();
        int count = 1;
        Matcher suffix = SUFFIX.matcher(name);
        Matcher prefix = PREFIX.matcher(name);
        try {
            if (suffix.matches()) {
                name = suffix.group(1);
                count = Integer.parseInt(suffix.group(2).replace(",", ""));
            } else if (prefix.matches()) {
                name = prefix.group(3);
                count = Integer.parseInt((prefix.group(1) != null ? prefix.group(1) : prefix.group(2)).replace(",", ""));
            }
        } catch (NumberFormatException ignored) {
            return new Result(Type.NONE, null);
        }
        name = name.replaceFirst("^[^\\p{L}\\p{N}]+", "").strip();
        String lower = name.toLowerCase(Locale.ROOT);
        // Restrict deferral to identifiable rewards, not arbitrary indented chat.
        boolean item = lower.endsWith(" gemstone") || lower.endsWith(" crystal")
            || lower.equals("divan's alloy") || lower.equals("divan fragment")
            || lower.equals("quick claw") || lower.equals("claw fossil")
            || lower.equals("helix fossil") || lower.equals("precious pearl")
            || lower.equals("jaderald") || lower.equals("prehistoric egg")
            || lower.equals("gemstone mixture") || lower.startsWith("enchanted book")
            || lower.equals("lapidary i") || lower.equals("fortune iv")
            || lower.endsWith(" goblin egg") || lower.equals("goblin egg")
            || lower.equals("oil barrel") || lower.equals("pickonimbus 2000")
            || lower.equals("bob-omb") || lower.equals("recall potion");
        if (!item || count < 1) return new Result(Type.NONE, null);
        return new Result(Type.REWARD, new Reward(name, count));
    }
}
