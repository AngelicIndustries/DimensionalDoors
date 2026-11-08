package org.dimdev.dimdoors.dungeon;

import java.util.Arrays;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCommandBlock;
import net.minecraft.block.BlockDispenser;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockNote;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockRailPowered;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneLight;
import net.minecraft.block.BlockRedstoneTorch;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockTNT;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * With Realistic Torches installed, a dungeon's torches become its lit torches, as torches the world generates do, and
 * redstone torches that power nothing become its burnt-out unlit torches: rooms such as the lava trap hang unlit
 * redstone torches between their torches as dead ones. A redstone torch whose power reaches a redstone component,
 * beside it or beside the block above it, stays.
 */
final class TorchSwaps {

    /** The block a torch with each metadata hangs on, as an offset; 5 and anything else stand on the floor. */
    private static final int[][] HUNG_ON = { { 0, -1, 0 }, { -1, 0, 0 }, { 1, 0, 0 }, { 0, 0, -1 }, { 0, 0, 1 },
        { 0, -1, 0 } };
    private static final int[][] SIDES = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 },
        { 0, 0, -1 } };

    private static boolean looked;
    private static Block lit, unlit;

    private TorchSwaps() {}

    /** The block to place for the schematic's block at x, y, z. */
    static Block replacement(DungeonSchematic schematic, int x, int y, int z, Block block, int metadata) {
        if (block != Blocks.torch && block != Blocks.redstone_torch && block != Blocks.unlit_redstone_torch) {
            return block;
        }
        if (!looked) {
            looked = true;
            lit = GameRegistry.findBlock("RealisticTorches", "TorchLit");
            unlit = GameRegistry.findBlock("RealisticTorches", "TorchUnlit");
            if (lit == null || unlit == null) lit = unlit = null;
        }
        if (lit == null) return block;
        if (block == Blocks.torch) return lit;
        return powersComponent(schematic, x, y, z, metadata) ? block : unlit;
    }

    /**
     * Sets up a placed lit torch as placing it in the world would, which a dungeon pasted straight into its chunks
     * skips: its tile entity, and its burn-out timer.
     */
    static void light(World world, int x, int y, int z, Block block, int metadata) {
        if (lit == null || block != lit) return;
        world.setTileEntity(x, y, z, block.createTileEntity(world, metadata));
        block.onBlockAdded(world, x, y, z);
    }

    /**
     * Whether a redstone torch's power reaches a redstone component: one beside it, other than the block it hangs on,
     * or beside the block above it, which it powers through.
     */
    private static boolean powersComponent(DungeonSchematic schematic, int x, int y, int z, int metadata) {
        int[] hungOn = HUNG_ON[Math.min(metadata & 7, 5)];
        for (int[] side : SIDES) {
            if (!Arrays.equals(side, hungOn) && isComponent(schematic.blockAt(x + side[0], y + side[1], z + side[2]))) {
                return true;
            }
        }
        if (schematic.blockAt(x, y + 1, z)
            .isNormalCube()) {
            for (int[] side : SIDES) {
                if (side[1] != -1 && isComponent(schematic.blockAt(x + side[0], y + 1 + side[1], z + side[2]))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isComponent(Block block) {
        return block instanceof BlockRedstoneWire || block instanceof BlockRedstoneDiode
            || block instanceof BlockRedstoneTorch
            || block instanceof BlockPistonBase
            || block instanceof BlockDispenser
            || block instanceof BlockTNT
            || block instanceof BlockRedstoneLight
            || block instanceof BlockNote
            || block instanceof BlockDoor
            || block instanceof BlockTrapDoor
            || block instanceof BlockFenceGate
            || block instanceof BlockRailPowered
            || block instanceof BlockHopper
            || block instanceof BlockCommandBlock;
    }
}
