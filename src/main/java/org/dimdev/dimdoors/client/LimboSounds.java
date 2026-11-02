package org.dimdev.dimdoors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * Limbo as the player hears it: a dark hum throughout, and near the red Eternal Fabric one of two drones (picked at
 * random each time) that fades in the closer the player comes.
 */
public final class LimboSounds {

    private static final ResourceLocation AMBIENCE = new ResourceLocation(DimDoors.modid, "limbo.ambience");
    private static final ResourceLocation FABRIC = new ResourceLocation(DimDoors.modid, "limbo.fabric");
    private static final float AMBIENCE_VOLUME = 0.8F, FABRIC_VOLUME = 0.9F;
    /** The fabric's drone starts this many blocks from the nearest Eternal Fabric and is loudest on it. */
    private static final int FABRIC_RANGE = 12;
    private static final int SCAN_TICKS = 10, FADE_TICKS = 40;

    private World world;
    private FadingSound ambience, fabric;
    private float nearness;
    private int scan;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != world) {
            stopAll();
            world = mc.theWorld;
        }
        if (world == null || mc.thePlayer == null || mc.isGamePaused()) return;
        if (world.provider.dimensionId != DDProperties.instance().LimboDimensionID) {
            stopAll();
            return;
        }
        if (ambience == null || ambience.isDonePlaying()) {
            ambience = new FadingSound(AMBIENCE, true, AMBIENCE_VOLUME, FADE_TICKS);
            mc.getSoundHandler()
                .playSound(ambience);
        }
        if (--scan <= 0) {
            scan = SCAN_TICKS;
            nearness = nearness(mc.thePlayer);
        }
        float target = FABRIC_VOLUME * nearness;
        if (target > 0) {
            if (fabric == null || fabric.isDonePlaying()) {
                fabric = new FadingSound(FABRIC, true, target, FADE_TICKS);
                mc.getSoundHandler()
                    .playSound(fabric);
            } else fabric.setTarget(target);
        } else if (fabric != null) {
            // Faded out; the next approach picks a drone anew
            fabric.stop();
            fabric = null;
        }
    }

    /** 1 on the Eternal Fabric, down to 0 at FABRIC_RANGE from the nearest. */
    private float nearness(EntityPlayer player) {
        int px = MathHelper.floor_double(player.posX), py = MathHelper.floor_double(player.posY),
            pz = MathHelper.floor_double(player.posZ);
        double nearest = Double.MAX_VALUE;
        for (int y = py - FABRIC_RANGE; y <= py + FABRIC_RANGE; y++) {
            if (y < 0 || y >= world.getHeight()) continue;
            for (int x = px - FABRIC_RANGE; x <= px + FABRIC_RANGE; x++) {
                for (int z = pz - FABRIC_RANGE; z <= pz + FABRIC_RANGE; z++) {
                    if (world.getBlock(x, y, z) != DimDoors.blockDimWallPerm) continue;
                    double dx = x + 0.5 - player.posX, dy = y + 0.5 - player.posY, dz = z + 0.5 - player.posZ;
                    nearest = Math.min(nearest, dx * dx + dy * dy + dz * dz);
                }
            }
        }
        return nearest == Double.MAX_VALUE ? 0 : Math.max(0, 1 - (float) Math.sqrt(nearest) / FABRIC_RANGE);
    }

    private void stopAll() {
        if (ambience != null) ambience.stop();
        if (fabric != null) fabric.stop();
        ambience = fabric = null;
        nearness = 0;
    }
}
