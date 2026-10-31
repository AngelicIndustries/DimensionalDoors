package org.dimdev.dimdoors.helpers;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class SurfaceFinderTest {

    private static final int TOP = 511;
    /** Biomic's tall ground level, 192, plus 32. */
    private static final int FLOATING_ABOVE = 224;

    /** A 512-high column from the bottom up: S solid, . passable, L leaves or logs, W water. */
    private static SurfaceFinder.Column column(String... parts) {
        StringBuilder blocks = new StringBuilder();
        for (String part : parts) blocks.append(part);
        String column = blocks.toString();
        return y -> {
            char c = y < column.length() ? column.charAt(y) : '.';
            switch (c) {
                case 'S':
                    return SurfaceFinder.Kind.SOLID;
                case 'L':
                    return SurfaceFinder.Kind.FOLIAGE;
                case 'W':
                    return SurfaceFinder.Kind.LIQUID;
                default:
                    return SurfaceFinder.Kind.PASSABLE;
            }
        };
    }

    private static String repeat(char c, int n) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < n; i++) out.append(c);
        return out.toString();
    }

    @Test
    public void landsOnTallWorldSurface() {
        // Biomic-like: ground up to y 250, far above vanilla's 63 and the old 245 start.
        assertEquals(251, SurfaceFinder.standingY(column(repeat('S', 251)), TOP, FLOATING_ABOVE));
    }

    @Test
    public void looksThroughLeavesToTheGround() {
        assertEquals(201, SurfaceFinder.standingY(column(repeat('S', 201), "...", "LLL"), TOP, FLOATING_ABOVE));
    }

    @Test
    public void refusesATreeTrunk() {
        assertEquals(-1, SurfaceFinder.standingY(column(repeat('S', 201), "LLLLLL"), TOP, FLOATING_ABOVE));
    }

    @Test
    public void refusesWaterAndLava() {
        assertEquals(-1, SurfaceFinder.standingY(column(repeat('S', 180), repeat('W', 11)), TOP, FLOATING_ABOVE));
    }

    @Test
    public void refusesASkyIsland() {
        // Ground at 200, then 100 blocks of air, then a 10 block island.
        assertEquals(
            -1,
            SurfaceFinder.standingY(column(repeat('S', 201), repeat('.', 100), repeat('S', 10)), TOP, FLOATING_ABOVE));
    }

    @Test
    public void standsOnThinGroundOverACaveNearGroundLevel() {
        // 14 blocks of ground at y 179 to 192 over a 60 block cave, as seen on Biomic.
        assertEquals(
            193,
            SurfaceFinder.standingY(column(repeat('S', 119), repeat('.', 60), repeat('S', 14)), TOP, FLOATING_ABOVE));
    }

    @Test
    public void standsOverCaves() {
        // Ground 31 blocks thick over a 30 block cave, and 6 thick over a 4 block one.
        assertEquals(
            261,
            SurfaceFinder.standingY(column(repeat('S', 200), repeat('.', 30), repeat('S', 31)), TOP, FLOATING_ABOVE));
        assertEquals(
            220,
            SurfaceFinder.standingY(column(repeat('S', 210), repeat('.', 4), repeat('S', 6)), TOP, FLOATING_ABOVE));
    }

    @Test
    public void refusesLowLeavesOverhead() {
        assertEquals(-1, SurfaceFinder.standingY(column(repeat('S', 100), ".", "L"), TOP, FLOATING_ABOVE));
    }
}
