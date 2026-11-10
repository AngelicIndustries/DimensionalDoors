package org.dimdev.dimdoors.tileentities;

import org.dimdev.dimdoors.DimDoors;

/**
 * How a rift closes, timed to its closing sound. The crack swells, slower and slower as it nears its full size, while
 * it turns faster and faster; at the sound's snap (a small rift's) or hit (a big one's) it snaps shut, and the sound
 * rings on. Each sound file has its own event, and the rift's crack shape picks one, so the server and every client
 * know which is playing.
 */
public enum RiftClosing {

    // Where each file's snap or hit falls, in seconds
    SMALL_0("rift.close.small.0", 1.63),
    SMALL_1("rift.close.small.1", 2.00),
    SMALL_2("rift.close.small.2", 2.05),
    BIG_0("rift.close.big.0", 1.33),
    BIG_1("rift.close.big.1", 0.85),
    BIG_2("rift.close.big.2", 0.87);

    /** How much bigger the crack gets before it snaps shut. */
    public static final double SWELL = 0.5;
    /** How quickly the swelling eases off: the higher, the sooner it nears its full size. */
    private static final double EASE = 4;
    /** Seconds the crack takes to snap shut. */
    public static final double SNAP = 0.08;
    /** The crack's fastest turn, at the snap or hit, in radians a second. */
    private static final double TWIST = 3;

    public final String sound;
    /** Seconds until the crack is at its biggest and snaps shut. */
    public final double swell;

    RiftClosing(String sound, double swell) {
        this.sound = DimDoors.modid + ":" + sound;
        this.swell = swell;
    }

    /** The close for a rift of this size and crack shape. */
    public static RiftClosing of(boolean big, int curveId) {
        int variant = (curveId * 0x9E3779B1 >>> 16) % 3;
        return values()[(big ? 3 : 0) + variant];
    }

    public boolean isBig() {
        return ordinal() >= 3;
    }

    /** Seconds until the crack is gone. */
    public double end() {
        return swell + SNAP;
    }

    /** Ticks until the rift block can go, a little after the crack is gone. */
    public int endTicks() {
        return (int) Math.ceil(end() * 20) + 2;
    }

    /** Ticks until the snap or hit, when the rift pulls the rifts near it shut too. */
    public int swellTicks() {
        return (int) Math.ceil(swell * 20);
    }

    /** The crack's size, as a share of the open rift's, this many seconds into the close. */
    public double scale(double seconds) {
        if (seconds <= 0) return 1;
        if (seconds < swell) {
            return 1 + SWELL * (1 - Math.exp(-EASE * seconds / swell)) / (1 - Math.exp(-EASE));
        }
        if (seconds >= end()) return 0;
        double snapped = (seconds - swell) / SNAP;
        return (1 + SWELL) * (1 - snapped * snapped);
    }

    /** How far the crack has turned in its own plane, in radians, this many seconds into the close. */
    public double twist(double seconds) {
        if (seconds <= 0) return 0;
        if (seconds < swell) {
            double u = seconds / swell;
            return TWIST * swell * u * u / 2;
        }
        return TWIST * swell / 2 + TWIST * (Math.min(seconds, end()) - swell);
    }
}
