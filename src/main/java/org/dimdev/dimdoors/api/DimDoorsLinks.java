package org.dimdev.dimdoors.api;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.items.ItemRiftSignature;
import org.dimdev.dimdoors.tileentities.TileEntityRift;
import org.dimdev.dimdoors.util.Point4D;
import org.dimdev.dimdoors.world.LimboProvider;

/**
 * Dimensional Doors links and rifts for other mods, on the server: what starts at a block, closing a link without
 * leaving a rift behind, and what the Rift Remover, Rift Stabilizer and Rift Signature do. A door's link starts at its
 * upper block.
 *
 * <p>
 * To add a door of your own, extend {@code org.dimdev.dimdoors.blocks.BaseDimDoor}. Any such door is immune to rift
 * regeneration, and it can build its own kind of door and platform at the far end of a safe exit through
 * {@code IDimDoor.placeExitDoor} and {@code IDimDoor.placeExitPlatformBlock}, and hand a pocket built through it a
 * lasting door with {@code IDimDoor.getPocketDoor}.
 */
public final class DimDoorsLinks {

    private DimDoorsLinks() {}

    /** Whether the world is one of the pocket dimensions, which a Warp Door leads out of. */
    public static boolean isPocket(World world) {
        NewDimData data = PocketManager.getDimensionData(world);
        return data != null && data.isPocketDimension();
    }

    /** Whether the world is Limbo. */
    public static boolean isLimbo(World world) {
        return world.provider instanceof LimboProvider;
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

    /**
     * Starts closing the rift at the block, as the Rift Remover does: it shrinks away, rifts within 6 blocks close with
     * it, and each one's link is deleted once it is gone. Returns false without a rift with a link, or if it is
     * already closing.
     */
    public static boolean closeRift(World world, int x, int y, int z) {
        TileEntityRift rift = riftAt(world, x, y, z);
        if (rift == null || rift.shouldClose) {
            return false;
        }
        rift.shouldClose = true;
        rift.markDirty();
        world.markBlockForUpdate(x, y, z);
        return true;
    }

    /**
     * Stabilizes the rift at the block for good, as the Rift Stabilizer does: it stops growing, unravelling blocks,
     * spreading and spawning Endermen. Returns false without a rift with a link, or if it is already stable or closing.
     */
    public static boolean stabilizeRift(World world, int x, int y, int z) {
        TileEntityRift rift = riftAt(world, x, y, z);
        if (rift == null || rift.isStabilized() || rift.shouldClose) {
            return false;
        }
        rift.stabilize();
        return true;
    }

    /** Whether the block is a rift with a link that has been stabilized. */
    public static boolean isStabilized(World world, int x, int y, int z) {
        TileEntityRift rift = riftAt(world, x, y, z);
        return rift != null && rift.isStabilized();
    }

    /** Whether the block is a rift that is closing. */
    public static boolean isClosing(World world, int x, int y, int z) {
        TileEntityRift rift = riftAt(world, x, y, z);
        return rift != null && rift.shouldClose;
    }

    /**
     * The height the Rift Signature puts a rift at when used on the block: where the upper half of a door standing on
     * it would be, allowing for replaceable blocks such as snow and for a door already there.
     */
    public static int riftHeight(World world, int x, int y, int z) {
        return ItemRiftSignature.adjustYForSpecialBlocks(world, x, y + 2, z);
    }

    /**
     * Opens a pair of rifts linked both ways, as the Rift Signature does, loading both worlds if needed. The
     * orientations are those of a door standing at each end. Returns false, changing nothing, if either end already
     * has a link or a rift can't replace the block there.
     */
    public static boolean openRiftPair(Point4D first, int firstOrientation, Point4D second, int secondOrientation) {
        if (first.equals(second)) {
            return false;
        }
        World firstWorld = PocketManager.loadDimension(first.getDimension());
        World secondWorld = PocketManager.loadDimension(second.getDimension());
        if (!canOpenRift(firstWorld, first) || !canOpenRift(secondWorld, second)) {
            return false;
        }
        NewDimData firstData = PocketManager.createDimensionData(firstWorld);
        NewDimData secondData = PocketManager.createDimensionData(secondWorld);
        DimLink forward = firstData
            .createLink(first.getX(), first.getY(), first.getZ(), LinkType.NORMAL, firstOrientation);
        DimLink back = secondData
            .createLink(second.getX(), second.getY(), second.getZ(), LinkType.NORMAL, secondOrientation);
        secondData.setLinkDestination(forward, second.getX(), second.getY(), second.getZ());
        firstData.setLinkDestination(back, first.getX(), first.getY(), first.getZ());
        DimDoors.blockRift.tryPlacingRift(firstWorld, first.getX(), first.getY(), first.getZ());
        DimDoors.blockRift.tryPlacingRift(secondWorld, second.getX(), second.getY(), second.getZ());
        return true;
    }

    private static boolean canOpenRift(World world, Point4D point) {
        return world != null && PocketManager.getLink(point.getX(), point.getY(), point.getZ(), world) == null
            && !DimDoors.blockRift.isBlockImmune(world, point.getX(), point.getY(), point.getZ());
    }

    private static TileEntityRift riftAt(World world, int x, int y, int z) {
        if (!isRift(world, x, y, z) || !hasLink(world, x, y, z)) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileEntityRift ? (TileEntityRift) tile : null;
    }
}
