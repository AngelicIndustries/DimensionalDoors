package org.dimdev.dimdoors.blocks;

import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemDoor;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.util.Point4D;

public interface IDimDoor {

    /**
     * A function to enter a dim door and traverse its link, called when a player collides with an open door
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @param entity
     */
    void enterDimDoor(World world, int x, int y, int z, Entity entity);

    /**
     * called when a door is placed to determine how it will place a link
     *
     * @param world
     * @param x
     * @param y
     * @param z
     */
    void placeLink(World world, int x, int y, int z);

    Item getDoorItem();

    TileEntity initDoorTE(World world, int x, int y, int z);

    /**
     * checks if any of this doors blocks are overlapping with a rift
     *
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */
    boolean isDoorOnRift(World world, int x, int y, int z);

    /**
     * Places the door at the far end of a safe exit through this door, lower half at {@code x, y, z}, after
     * Dimensional Doors has linked that spot back to {@code source}, this door's link. The default is a Warp Door;
     * another mod's door may place its own kind, for example so that both ends share a lifespan.
     */
    default void placeExitDoor(World world, int x, int y, int z, int orientation, Point4D source) {
        ItemDoor.placeDoorBlock(world, x, y, z, orientation, DimDoors.warpDoor);
    }

    /**
     * Places one block of the platform under a safe exit through this door, where the ground isn't solid. The default
     * is Fabric of Reality, without block updates.
     */
    default void placeExitPlatformBlock(World world, int x, int y, int z, Point4D source) {
        world.setBlock(x, y, z, DimDoors.blockDimWall, 0, 2);
    }
}
