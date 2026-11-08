package org.dimdev.dimdoors.client;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The hiss of Solid Static. Decayed Limbo can be full of it, so one loop plays, from the nearest block within hearing,
 * and follows to whichever block is nearest as the player moves.
 */
public final class StaticSounds {

    private static final ResourceLocation STATIC = new ResourceLocation(DimDoors.modid, "static");
    private static final float VOLUME = 0.5F;
    /** Heard up to this far, and looked for this far around the player. */
    private static final int RANGE = 8;
    private static final int SCAN_TICKS = 10, FADE_TICKS = 20;

    private World world;
    private PlacedSound hiss;
    private int scan;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld != world) {
            stop();
            world = mc.theWorld;
        }
        if (world == null || mc.thePlayer == null || mc.isGamePaused() || --scan > 0) return;
        scan = SCAN_TICKS;
        double[] nearest = nearest(mc.thePlayer);
        if (nearest == null) {
            stop();
            return;
        }
        if (hiss == null || hiss.isDonePlaying()) {
            hiss = new PlacedSound(STATIC, nearest[0], nearest[1], nearest[2], RANGE, VOLUME, FADE_TICKS);
            mc.getSoundHandler()
                .playSound(hiss);
        } else hiss.moveTo(nearest[0], nearest[1], nearest[2]);
    }

    /** The middle of the nearest Solid Static within RANGE, or null. */
    private double[] nearest(EntityPlayer player) {
        int px = MathHelper.floor_double(player.posX), py = MathHelper.floor_double(player.posY),
            pz = MathHelper.floor_double(player.posZ);
        double best = RANGE * RANGE;
        double[] found = null;
        for (int y = Math.max(0, py - RANGE); y <= Math.min(world.getHeight() - 1, py + RANGE); y++) {
            for (int x = px - RANGE; x <= px + RANGE; x++) {
                for (int z = pz - RANGE; z <= pz + RANGE; z++) {
                    if (world.getBlock(x, y, z) != DimDoors.blockSolidStatic) continue;
                    double dx = x + 0.5 - player.posX, dy = y + 0.5 - player.posY, dz = z + 0.5 - player.posZ;
                    double d = dx * dx + dy * dy + dz * dz;
                    if (d < best) {
                        best = d;
                        found = new double[] { x + 0.5, y + 0.5, z + 0.5 };
                    }
                }
            }
        }
        return found;
    }

    private void stop() {
        if (hiss != null) hiss.stop();
        hiss = null;
    }
}
