package org.dimdev.dimdoors.helpers;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLog;
import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DDTeleporter;
import org.dimdev.dimdoors.util.Point4D;

/** Sends a player who walks on Limbo's Eternal Fabric back to the Overworld, where Limbo Escape Destination says. */
public final class LimboEscape {

    public static final String SURFACE = "surface", BED = "bed", SPAWN = "spawn";
    private static final int ATTEMPTS = 8;
    /** Sky islands float well above the ground level; caves under ordinary ground don't make it floating. */
    private static final int FLOATING_ABOVE_GROUND = 32;

    private LimboEscape() {}

    public static void escape(EntityPlayerMP player, World overworld, int x, int z, DDProperties properties,
        Random random) {
        ChunkCoordinates destination = destination(player, overworld, x, z, properties, random);
        player.fallDistance = 0;
        DDTeleporter
            .teleportEntity(player, new Point4D(destination.posX, destination.posY, destination.posZ, 0), false);
        if (properties.LimboEscapeUnravelsLanding) unravel(overworld, destination, random);
        player.setPositionAndUpdate(destination.posX + 0.5, destination.posY, destination.posZ + 0.5);
        player.fallDistance = 0;
    }

    private static ChunkCoordinates destination(EntityPlayerMP player, World overworld, int x, int z,
        DDProperties properties, Random random) {
        if (BED.equals(properties.LimboEscapeDestination)) {
            ChunkCoordinates bed = player.getBedLocation(0);
            if (bed != null) {
                overworld.getChunkProvider()
                    .loadChunk(bed.posX >> 4, bed.posZ >> 4);
                ChunkCoordinates safe = EntityPlayer.verifyRespawnCoordinates(overworld, bed, player.isSpawnForced(0));
                if (safe != null) return safe;
            }
        } else if (!SPAWN.equals(properties.LimboEscapeDestination)) {
            int range = properties.LimboReturnRange / 2;
            for (int attempt = 0; attempt < ATTEMPTS; attempt++) {
                int destinationX = x + MathHelper.getRandomIntegerInRange(random, -range, range);
                int destinationZ = z + MathHelper.getRandomIntegerInRange(random, -range, range);
                int y = standingY(overworld, destinationX, destinationZ);
                if (y > 0) return new ChunkCoordinates(destinationX, y, destinationZ);
            }
        }
        ChunkCoordinates spawn = overworld.getSpawnPoint();
        int y = standingY(overworld, spawn.posX, spawn.posZ);
        return y > 0 ? new ChunkCoordinates(spawn.posX, y, spawn.posZ) : spawn;
    }

    /** Where a player can stand on the surface at x, z, or -1. */
    public static int standingY(final World world, final int x, final int z) {
        final Chunk chunk = world.getChunkProvider()
            .loadChunk(x >> 4, z >> 4);
        int top = Math.min(chunk.getTopFilledSegment() + 15, world.getHeight() - 3);
        return SurfaceFinder
            .standingY(y -> kind(world, x, y, z), top, world.provider.getAverageGroundLevel() + FLOATING_ABOVE_GROUND);
    }

    private static SurfaceFinder.Kind kind(World world, int x, int y, int z) {
        Block block = world.getBlock(x, y, z);
        Material material = block.getMaterial();
        if (material.isLiquid()) return SurfaceFinder.Kind.LIQUID;
        if (material == Material.leaves || block instanceof BlockLog) return SurfaceFinder.Kind.FOLIAGE;
        return block.getCollisionBoundingBoxFromPool(world, x, y, z) == null ? SurfaceFinder.Kind.PASSABLE
            : SurfaceFinder.Kind.SOLID;
    }

    /** The patch of Unraveled Fabric Limbo used to leave where escapes land. */
    private static void unravel(World world, ChunkCoordinates at, Random random) {
        for (int xc = -3; xc < 4; xc++) {
            for (int zc = -3; zc < 4; zc++) {
                if (Math.abs(xc) + Math.abs(zc) < random.nextInt(3) + 2
                    || Math.abs(xc) + Math.abs(zc) < random.nextInt(3) + 3) {
                    world.setBlock(at.posX + xc, at.posY - 1, at.posZ + zc, DimDoors.blockLimbo);
                }
            }
        }
    }
}
