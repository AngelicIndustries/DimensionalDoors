package org.dimdev.dimdoors.dungeon;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.dimdoors.DDLoot;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Grave chests as OpenBlocks graves, when OpenBlocks is installed and the OpenBlocks Graves option is on: a single
 * plain chest standing on a solid block becomes a grave holding the same loot, facing the way the chest did, named
 * after someone who died in a pocket dimension and with a death message of ours. OpenBlocks is reached by
 * reflection, so it stays optional.
 */
public final class OpenBlocksGraves {

    private static final Logger LOG = LogManager.getLogger("DimDoors");

    /** How many death messages the lang file has, dimdoors.grave.death.0 and on. */
    private static final int DEATH_MESSAGES = 5;

    /** Grave metadata by chest metadata: a chest facing north (2) makes a grave whose name faces north, and so on. */
    private static final int[] GRAVE_FACING = { 0, 0, 2, 0, 3, 1 };

    private static boolean looked;
    private static Block grave;
    private static Method setUsername, setDeathMessage, setLoot;

    private OpenBlocksGraves() {}

    /** Puts a grave holding the loot in place of the chest at x, y, z; false when there's no grave to put there. */
    public static boolean place(World world, int x, int y, int z, List<ItemStack> loot, Random random) {
        if (world.getBlock(x, y, z) != Blocks.chest || !standsAlone(world, x, y, z)
            || !world.isSideSolid(x, y - 1, z, ForgeDirection.UP)
            || !available(world)) {
            return false;
        }
        int facing = world.getBlockMetadata(x, y, z);
        world.setBlock(x, y, z, grave, GRAVE_FACING[facing < GRAVE_FACING.length ? facing : 0], 2);
        TileEntity tile = world.getTileEntity(x, y, z);
        try {
            String name = DDLoot.graveName(random);
            IChatComponent message = new ChatComponentTranslation(
                "dimdoors.grave.death." + random.nextInt(DEATH_MESSAGES),
                name);
            IInventory contents = new InventoryBasic("grave", false, Math.max(1, loot.size()));
            for (int slot = 0; slot < loot.size(); slot++) {
                contents.setInventorySlotContents(slot, loot.get(slot));
            }
            setUsername.invoke(tile, name);
            setDeathMessage.invoke(tile, message);
            setLoot.invoke(tile, contents);
            ((DungeonGrave) tile).dimdoors$markDungeonGrave();
            return true;
        } catch (Exception e) {
            LOG.warn("Couldn't fill an OpenBlocks grave, so it's a chest again", e);
            world.setBlock(x, y, z, Blocks.chest, facing, 2);
            return false;
        }
    }

    /** Not half of a double chest. */
    private static boolean standsAlone(World world, int x, int y, int z) {
        return world.getBlock(x - 1, y, z) != Blocks.chest && world.getBlock(x + 1, y, z) != Blocks.chest
            && world.getBlock(x, y, z - 1) != Blocks.chest
            && world.getBlock(x, y, z + 1) != Blocks.chest;
    }

    /** OpenBlocks' grave, with the methods we fill it through, and our mixin on its tile entity. */
    private static boolean available(World world) {
        if (!looked) {
            looked = true;
            if (!Loader.isModLoaded("OpenBlocks")) {
                return false;
            }
            Block block = GameRegistry.findBlock("OpenBlocks", "grave");
            if (block == null) {
                return false;
            }
            TileEntity tile = block.createTileEntity(world, 0);
            if (!(tile instanceof DungeonGrave)) {
                LOG.warn("OpenBlocks' grave doesn't take our changes, so grave chests stay chests");
                return false;
            }
            try {
                Class<?> type = tile.getClass();
                setUsername = type.getMethod("setUsername", String.class);
                setDeathMessage = type.getMethod("setDeathMessage", IChatComponent.class);
                setLoot = type.getMethod("setLoot", IInventory.class);
                grave = block;
            } catch (NoSuchMethodException e) {
                LOG.warn("OpenBlocks' grave has changed, so grave chests stay chests", e);
            }
        }
        return grave != null;
    }
}
