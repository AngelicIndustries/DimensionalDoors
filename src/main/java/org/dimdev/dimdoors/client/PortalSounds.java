package org.dimdev.dimdoors.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.BaseDimDoor;
import org.dimdev.dimdoors.blocks.TransTrapdoor;
import org.dimdev.dimdoors.tileentities.TileEntityDimDoor;
import org.dimdev.dimdoors.tileentities.TileEntityTransTrapdoor;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The hum of portals: every dimensional door or trapdoor on a rift hums, the nearest few within hearing, muffled while
 * it is shut (a low-passed copy of the hum, crossfaded as it opens and shuts).
 */
public final class PortalSounds {

    private static final ResourceLocation HUM = new ResourceLocation(DimDoors.modid, "portal.hum");
    private static final ResourceLocation MUFFLED = new ResourceLocation(DimDoors.modid, "portal.hum.muffled");
    private static final int RANGE = 16;
    private static final float VOLUME = 0.8F;
    private static final int MAX_HUMS = 3, SCAN_TICKS = 10, FADE_TICKS = 10;

    private final Map<ChunkCoordinates, Hum> hums = new HashMap<>();
    private World world;
    private int scan;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != world) {
            stopAll();
            world = mc.theWorld;
        }
        if (world == null || mc.thePlayer == null || mc.isGamePaused() || --scan > 0) return;
        scan = SCAN_TICKS;
        EntityPlayer player = mc.thePlayer;
        List<Portal> portals = new ArrayList<>();
        // Doors and trapdoors don't tick, so they aren't in the world's list of loaded tile entities, only their
        // chunks'
        int px = MathHelper.floor_double(player.posX), pz = MathHelper.floor_double(player.posZ);
        for (int cx = (px - RANGE) >> 4; cx <= (px + RANGE) >> 4; cx++) {
            for (int cz = (pz - RANGE) >> 4; cz <= (pz + RANGE) >> 4; cz++) {
                if (!world.getChunkProvider()
                    .chunkExists(cx, cz)) continue;
                Chunk chunk = world.getChunkFromChunkCoords(cx, cz);
                for (Object object : chunk.chunkTileEntityMap.values()) {
                    Portal portal = portal((TileEntity) object);
                    if (portal != null && portal.distanceSq(player) <= RANGE * RANGE) portals.add(portal);
                }
            }
        }
        portals.sort(Comparator.comparingDouble(portal -> portal.distanceSq(player)));
        Set<ChunkCoordinates> keep = new HashSet<>();
        for (Portal portal : portals.subList(0, Math.min(MAX_HUMS, portals.size()))) {
            keep.add(portal.pos);
            Hum hum = hums.computeIfAbsent(portal.pos, pos -> new Hum());
            hum.clear = play(mc, hum.clear, HUM, portal, portal.open ? VOLUME : 0);
            hum.muffled = play(mc, hum.muffled, MUFFLED, portal, portal.open ? 0 : VOLUME);
        }
        for (Iterator<Map.Entry<ChunkCoordinates, Hum>> it = hums.entrySet()
            .iterator(); it.hasNext();) {
            Map.Entry<ChunkCoordinates, Hum> entry = it.next();
            if (!keep.contains(entry.getKey())) {
                entry.getValue()
                    .stop();
                it.remove();
            }
        }
    }

    /** The sound faded toward the volume: started when it should be heard, stopped when it shouldn't. */
    private static PlacedSound play(Minecraft mc, PlacedSound sound, ResourceLocation name, Portal portal,
        float volume) {
        if (volume <= 0) {
            if (sound != null) sound.stop();
            return null;
        }
        if (sound == null || sound.isDonePlaying()) {
            sound = new PlacedSound(name, portal.x, portal.y, portal.z, RANGE, volume, FADE_TICKS);
            mc.getSoundHandler()
                .playSound(sound);
        } else sound.setTarget(volume);
        return sound;
    }

    /** The portal a tile entity holds, or null: the upper half of a door on a rift, or a trapdoor on one. */
    private Portal portal(TileEntity tile) {
        if (tile.isInvalid()) return null;
        int x = tile.xCoord, y = tile.yCoord, z = tile.zCoord;
        Block block = world.getBlock(x, y, z);
        if (tile instanceof TileEntityDimDoor && block instanceof BaseDimDoor) {
            BaseDimDoor door = (BaseDimDoor) block;
            if (!door.isUpperDoorBlock(world.getBlockMetadata(x, y, z)) || !door.isDoorOnRift(world, x, y, z))
                return null;
            // The middle of the doorway, where the upper half starts
            return new Portal(x, y, z, x + 0.5, y, z + 0.5, door.isDoorOpen(door.func_150012_g(world, x, y, z)));
        }
        if (tile instanceof TileEntityTransTrapdoor && block instanceof TransTrapdoor) {
            if (!((TransTrapdoor) block).isDoorOnRift(world, x, y, z)) return null;
            return new Portal(
                x,
                y,
                z,
                x + 0.5,
                y + 0.5,
                z + 0.5,
                BlockTrapDoor.func_150118_d(world.getBlockMetadata(x, y, z)));
        }
        return null;
    }

    private void stopAll() {
        for (Hum hum : hums.values()) hum.stop();
        hums.clear();
    }

    private static final class Hum {

        PlacedSound clear, muffled;

        void stop() {
            if (clear != null) clear.stop();
            if (muffled != null) muffled.stop();
        }
    }

    private static final class Portal {

        final ChunkCoordinates pos;
        final double x, y, z;
        final boolean open;

        Portal(int bx, int by, int bz, double x, double y, double z, boolean open) {
            this.pos = new ChunkCoordinates(bx, by, bz);
            this.x = x;
            this.y = y;
            this.z = z;
            this.open = open;
        }

        double distanceSq(EntityPlayer player) {
            return player.getDistanceSq(x, y, z);
        }
    }
}
