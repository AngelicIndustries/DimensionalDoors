package org.dimdev.dimdoors.api;

import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.util.Point4D;

/**
 * Dimensional Doors links for other mods, on the server: what starts at a block, and closing a link without leaving a
 * rift behind. A door's link starts at its upper block.
 *
 * <p>
 * To add a door of your own, extend {@code org.dimdev.dimdoors.blocks.BaseDimDoor}. Any such door is immune to rift
 * regeneration, and it can build its own kind of door and platform at the far end of a safe exit through
 * {@code IDimDoor.placeExitDoor} and {@code IDimDoor.placeExitPlatformBlock}.
 */
public final class DimDoorsLinks {

    private DimDoorsLinks() {}

    /** Whether the world is one of the pocket dimensions, which a Warp Door leads out of. */
    public static boolean isPocket(World world) {
        NewDimData data = PocketManager.getDimensionData(world);
        return data != null && data.isPocketDimension();
    }

    /** Whether the world is a personal pocket, which no Warp Door may tunnel out of. */
    public static boolean isPersonalPocket(World world) {
        NewDimData data = PocketManager.getDimensionData(world);
        return data != null && data.type() == DimensionType.PERSONAL;
    }

    /** Whether a link starts at the block. */
    public static boolean hasLink(World world, int x, int y, int z) {
        return PocketManager.getLink(x, y, z, world) != null;
    }

    /** Where the link starting at the block leads, or null without a link or before its far end is built. */
    public static Point4D destination(World world, int x, int y, int z) {
        DimLink link = PocketManager.getLink(x, y, z, world);
        return link != null && link.hasDestination() ? link.destination() : null;
    }

    /** Deletes the link starting at the block, if there is one, so that no rift grows there. */
    public static void close(World world, int x, int y, int z) {
        DimLink link = PocketManager.getLink(x, y, z, world);
        if (link != null) {
            PocketManager.getDimensionData(world)
                .deleteLink(link);
        }
    }

    /** Deletes the link starting at the block in a dimension, which needn't be loaded. */
    public static void close(int dimension, int x, int y, int z) {
        NewDimData data = PocketManager.getDimensionData(dimension);
        DimLink link = data == null ? null : data.getLink(x, y, z);
        if (link != null) {
            data.deleteLink(link);
        }
    }

    /** Whether the block is a rift. */
    public static boolean isRift(World world, int x, int y, int z) {
        return world.getBlock(x, y, z) == DimDoors.blockRift;
    }

    /** Puts a rift at the block, for a link that still starts there. */
    public static void restoreRift(World world, int x, int y, int z) {
        world.setBlock(x, y, z, DimDoors.blockRift);
    }
}
