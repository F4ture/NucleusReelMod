package dev.nucleusreel.core;

public final class SlotSpinTest {
    public static void main(String[] args) {
        for (int duration : new int[]{20, 40, 120, 300}) {
            for (int mode = 0; mode < 4; mode++) {
                SlotSpin spin = new SlotSpin(duration, mode > 0, mode == 3 ? 1 : mode == 2 ? 2 : -1);
                int[] order = mode == 0 ? new int[]{0, 2, 1} : new int[]{0, 1, 2};
                if (!(spin.stopTick(order[0]) < spin.stopTick(order[1]) && spin.stopTick(order[1]) < spin.stopTick(order[2])))
                    throw new AssertionError("Incorrect stop order");
                if (spin.stopTick(order[2]) != spin.durationTicks())
                    throw new AssertionError("Last reel must match the music/chat deadline");
                if (spin.durationTicks() != duration + (mode > 0 ? mode == 3 ? 40 : 80 : 0))
                    throw new AssertionError("Suspense must extend the actual duration");
                for (int reel = 0; reel < 3; reel++) {
                    if (spin.position(reel, -1) != SlotSpin.START) throw new AssertionError("Start position");
                    float previous = spin.position(reel, 0);
                    for (float time = 0.125f; time <= spin.durationTicks() + 20; time += 0.125f) {
                        float position = spin.position(reel, time);
                        if (position < previous || position > spin.landingIndex(reel) || !Float.isFinite(position))
                            throw new AssertionError("Reel reversed or overshot: mode=" + mode + " reel=" + reel + " t=" + time);
                        if (spin.stopped(reel, time) && position != spin.landingIndex(reel))
                            throw new AssertionError("Stopped reel must keep the result exactly on the line");
                        if (position - previous > 2) throw new AssertionError("Discontinuous motion");
                        float focus = spin.focusAmount(time);
                        if (!Float.isFinite(focus) || focus < 0 || focus > 1) throw new AssertionError("Invalid camera zoom");
                        previous = position;
                    }
                    if (spin.stopped(reel, spin.stopTick(reel) - 0.125f)) throw new AssertionError("Premature stop");
                }
                if (mode > 0) {
                    if (spin.focusReel(spin.stopTick(0) - 0.1f) != -1
                        || spin.focusReel(spin.stopTick(0)) != 1
                        || spin.focusReel(spin.stopTick(1)) != (mode == 3 ? -1 : 2)
                        || spin.focusReel(spin.durationTicks()) != -1)
                        throw new AssertionError("Camera must follow second then third reel only after previous locks");
                    if (spin.focusAmount(spin.durationTicks() + 12) != 0) throw new AssertionError("Camera did not reset");
                    for (int reel = 1; reel < 3; reel++) {
                        if (mode == 3 && reel == 2) {
                            if (spin.focusCentre(spin.stopTick(2)) != 0 || spin.focusReel(spin.stopTick(1) + 1) != -1)
                                throw new AssertionError("Second failure must not zoom into the third reel");
                            continue;
                        }
                        if (spin.stopTick(reel) - spin.stopTick(reel - 1) < 40) throw new AssertionError("Suspense too short");
                        if (Math.abs(spin.position(reel, spin.stopTick(reel - 1)) - (spin.landingIndex(reel) - spin.approachDistance(reel))) > 0.001)
                            throw new AssertionError("Slow motion must start as target enters view");
                    }
                    if (mode >= 2) {
                        int failed = mode == 3 ? 1 : 2;
                        float previousSpeed = Float.POSITIVE_INFINITY;
                        boolean crossedRare = false;
                        for (float t = spin.stopTick(failed - 1); t < spin.stopTick(failed) - 1; t += 0.5f) {
                            float position = spin.position(failed, t);
                            float speed = spin.position(failed, t + 0.5f) - position;
                            if (speed <= 0 || speed > previousSpeed + 0.00002f)
                                throw new AssertionError("A near miss must keep decelerating, without stopping and restarting");
                            if (position >= spin.landingIndex(failed) - 1) crossedRare = true;
                            previousSpeed = speed;
                        }
                        if (!crossedRare) throw new AssertionError("The fixed rare symbol must pass the line before the ordinary result lands");
                    }
                }
            }
        }
        System.out.println("Normal, rare-win, second and third failure timing verified at 1, 2, 6 and 15 base seconds: no reversal, no reacceleration, fixed rare symbol passes the line, no third zoom after second failure, music/chat deadline.");
    }
}
