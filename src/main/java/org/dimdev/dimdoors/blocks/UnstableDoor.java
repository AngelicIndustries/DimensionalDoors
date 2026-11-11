package org.dimdev.dimdoors.blocks;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;

public class UnstableDoor extends BaseDimDoor {

    public UnstableDoor(Material material, DDProperties properties) {
        super(material, properties);
    }

    @Override
    public void placeLink(World world, int x, int y, int z) {
        if (!world.isRemote && world.getBlock(x, y - 1, z) == this) {
            NewDimData dimension = PocketManager.getDimensionData(world);
            dimension.createLink(x, y, z, LinkType.RANDOM, world.getBlockMetadata(x, y - 1, z));
        }
    }

    @Override
    public Item getDoorItem() {
        return DimDoors.itemUnstableDoor;
    }

    /** An Iron Door, from the lower half only, as the other doors drop: the upper half dropped a second one. */
    @Override
    public Item getItemDropped(int metadata, Random random, int fortune) {
        return isUpperDoorBlock(metadata) ? null : Items.iron_door;
    }
}
