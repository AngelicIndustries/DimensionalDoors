package org.dimdev.dimdoors.helpers;

/**
 * Finds where a player can stand on the surface of a block column, from the top down, so it works at any world height.
 * Leaves and logs are looked through; water, lava and floating land (a thin layer over a tall gap, like a sky island)
 * high above the world's ground level give no spot, and the caller tries another column instead of digging down into
 * caves.
 */
public final class SurfaceFinder {

    /** What a block is to a player looking for a place to stand. */
    public enum Kind {
        /** Can be stood in: air, flowers, grass, snow. */
        PASSABLE,
        /** Looked through from above, but not stood in: leaves, logs. */
        FOLIAGE,
        LIQUID,
        SOLID
    }

    public interface Column {

        Kind kind(int y);
    }

    /** Land at most this thick over at least this much air counts as floating. */
    static final int FLOATING_MAX_THICKNESS = 24, FLOATING_MIN_GAP = 12;

    private SurfaceFinder() {}

    /**
     * The y the player's feet go at, or -1 when the column has no surface to stand on. Land only counts as floating
     * above floatingAbove, so ground over a cave near the world's ground level still counts.
     */
    public static int standingY(Column column, int top, int floatingAbove) {
        int y = top;
        while (y > 0 && column.kind(y) != Kind.SOLID) {
            if (column.kind(y) == Kind.LIQUID) return -1;
            y--;
        }
        if (y <= 0) return -1;
        if (column.kind(y + 1) != Kind.PASSABLE || column.kind(y + 2) != Kind.PASSABLE) return -1;
        return y > floatingAbove && isFloating(column, y) ? -1 : y + 1;
    }

    private static boolean isFloating(Column column, int ground) {
        int y = ground;
        while (y > 0 && column.kind(y) != Kind.PASSABLE) {
            if (ground - y >= FLOATING_MAX_THICKNESS) return false;
            y--;
        }
        int gapTop = y;
        while (y > 0 && column.kind(y) == Kind.PASSABLE) {
            if (gapTop - y + 1 >= FLOATING_MIN_GAP) return true;
            y--;
        }
        return false;
    }
}
