package org.dimdev.dimdoors.dungeon;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DDLoot;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.schematic.WorldOperation;

/** Fills a newly built dungeon's empty loot containers (the Loot Containers option) with its loot. */
public class FillContainersOperation extends WorldOperation {

    private final Random random;
    private final DDProperties properties;
    private final DungeonData dungeon;
    private final Set<String> containers;

    public FillContainersOperation(Random random, DDProperties properties, DungeonData dungeon) {
        super("FillContainersOperation");
        this.random = random;
        this.properties = properties;
        this.dungeon = dungeon;
        this.containers = new HashSet<String>(Arrays.asList(properties.LootContainers));
    }

    @Override
    protected boolean applyToBlock(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        if (block.hasTileEntity(world.getBlockMetadata(x, y, z))
            && containers.contains(Block.blockRegistry.getNameForObject(block))) {
            TileEntity tileEntity = world.getTileEntity(x, y, z);
            if (tileEntity instanceof IInventory && isInventoryEmpty((IInventory) tileEntity)) {
                DDLoot.fillChest((IInventory) tileEntity, random, properties, dungeon);
            }
        }
        return true;
    }

    private static boolean isInventoryEmpty(IInventory inventory) {
        int size = inventory.getSizeInventory();
        for (int index = 0; index < size; index++) {
            if (inventory.getStackInSlot(index) != null) {
                return false;
            }
        }
        return true;
    }
}
