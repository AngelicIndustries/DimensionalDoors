package org.dimdev.dimdoors.helpers;

import net.minecraft.world.World;

import org.dimdev.dimdoors.Point3D;

/**
 * Where a safe exit lands. The exit's own column is searched up and down as before; when that spot would need a
 * platform (its 3x3 floor isn't all solid, for one over crops or flowers, which can't be cleared away), the columns
 * around it are tried in growing rings, and the nearest spot that needs none wins. Without one, the spot that needs
 * the fewest platform blocks.
 */
public final class ExitSpot {

    private ExitSpot() {}

    public static Point3D find(World world, int x, int startY, int z, int radius) {
        Point3D best = null;
        int bestCost = Integer.MAX_VALUE;
        for (int ring = 0; ring <= radius; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != ring) continue;
                    Point3D spot = column(world, x + dx, startY, z + dz);
                    if (spot == null) continue;
                    int cost = platformBlocks(world, spot);
                    if (cost == 0) return spot;
                    if (cost < bestCost) {
                        best = spot;
                        bestCost = cost;
                    }
                }
            }
        }
        return best;
    }

    /** The nearest safe spot up or down the column, as safe exits have always chosen it. */
    private static Point3D column(World world, int x, int startY, int z) {
        Point3D up = yCoordHelper.findSafeCubeUp(world, x, startY, z);
        Point3D down = yCoordHelper.findSafeCubeDown(world, x, startY, z);
        if (up == null) return down;
        if (down == null) return up;
        return up.getY() - startY <= startY - down.getY() ? up : down;
    }

    /** How many blocks of the 3x3 floor would become Fabric of Reality. */
    private static int platformBlocks(World world, Point3D spot) {
        int count = 0;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (!world.isBlockNormalCubeDefault(spot.getX() + dx, spot.getY(), spot.getZ() + dz, false)) count++;
            }
        }
        return count;
    }
}
