package org.dimdev.dimdoors.client;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChunkCoordinates;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.tileentities.TileEntityRift;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The sound of open rifts, the nearest few within hearing: a small rift rumbles, and as it grows past
 * TileEntityRift.BIG_RADIUS the rumble gives way to a heavier drone, heard farther. A rift that starts to close falls
 * silent quickly, under its own closing sound (ClosingRifts).
 */
public final class RiftSounds {

    private static final ResourceLocation SMALL = new ResourceLocation(DimDoors.modid, "rift.small");
    private static final ResourceLocation BIG = new ResourceLocation(DimDoors.modid, "rift.big");
    /** The big sound takes over across half a block of radius either side of TileEntityRift.BIG_RADIUS. */
    private static final float CROSSFADE = 0.5F;
    private static final float SMALL_RANGE = 16, BIG_RANGE = 24, SMALL_VOLUME = 0.7F, BIG_VOLUME = 0.9F;
    private static final int MAX_RIFTS = 3, SCAN_TICKS = 10, FADE_TICKS = 20;

    private final Map<ChunkCoordinates, Pair> rifts = new HashMap<>();
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
        List<TileEntityRift> near = new ArrayList<>();
        for (Object object : world.loadedTileEntityList) {
            if (!(object instanceof TileEntityRift)) continue;
            TileEntityRift rift = (TileEntityRift) object;
            if (rift.isInvalid() || rift.shouldClose
                || world.getBlock(rift.xCoord, rift.yCoord, rift.zCoord) != DimDoors.blockRift) continue;
            if (distanceSq(rift, player) <= BIG_RANGE * BIG_RANGE) near.add(rift);
        }
        near.sort(Comparator.comparingDouble(rift -> distanceSq(rift, player)));
        Set<ChunkCoordinates> keep = new HashSet<>();
        for (TileEntityRift rift : near.subList(0, Math.min(MAX_RIFTS, near.size()))) {
            ChunkCoordinates pos = new ChunkCoordinates(rift.xCoord, rift.yCoord, rift.zCoord);
            keep.add(pos);
            Pair pair = rifts.computeIfAbsent(pos, p -> new Pair());
            float big = Math
                .max(0, Math.min(1, (rift.radius() - TileEntityRift.BIG_RADIUS + CROSSFADE) / (2 * CROSSFADE)));
            pair.small = play(mc, pair.small, SMALL, rift, SMALL_RANGE, SMALL_VOLUME * (1 - big));
            pair.big = play(mc, pair.big, BIG, rift, BIG_RANGE, BIG_VOLUME * big);
        }
        for (Iterator<Map.Entry<ChunkCoordinates, Pair>> it = rifts.entrySet()
            .iterator(); it.hasNext();) {
            Map.Entry<ChunkCoordinates, Pair> entry = it.next();
            if (!keep.contains(entry.getKey())) {
                entry.getValue()
                    .stop();
                it.remove();
            }
        }
    }

    /** The sound faded toward the volume: started when it should be heard, stopped when it shouldn't. */
    private static PlacedSound play(Minecraft mc, PlacedSound sound, ResourceLocation name, TileEntityRift rift,
        float range, float volume) {
        if (volume <= 0) {
            if (sound != null) sound.stop();
            return null;
        }
        if (sound == null || sound.isDonePlaying()) {
            sound = new PlacedSound(
                name,
                rift.xCoord + 0.5,
                rift.yCoord + 0.5,
                rift.zCoord + 0.5,
                range,
                volume,
                FADE_TICKS);
            mc.getSoundHandler()
                .playSound(sound);
        } else sound.setTarget(volume);
        return sound;
    }

    private static double distanceSq(TileEntityRift rift, EntityPlayer player) {
        return player.getDistanceSq(rift.xCoord + 0.5, rift.yCoord + 0.5, rift.zCoord + 0.5);
    }

    private void stopAll() {
        for (Pair pair : rifts.values()) pair.stop();
        rifts.clear();
    }

    private static final class Pair {

        PlacedSound small, big;

        void stop() {
            if (small != null) small.stop();
            if (big != null) big.stop();
        }
    }
}
