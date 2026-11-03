package org.dimdev.dimdoors.client;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.enchantment.EnchantmentTrepidation;
import org.dimdev.dimdoors.enchantment.TrepidationMobs;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * The wearer of a Trepidation chestplate hears a heartbeat while a sensed mob is in range, faster the closer it is.
 * The two halves and their spacing come from Enhanced Visuals' low-health heartbeat.
 */
public final class TrepidationHeartbeat {

    private static final ResourceLocation LUB = new ResourceLocation(DimDoors.modid, "trepidation.in");
    private static final ResourceLocation DUB = new ResourceLocation(DimDoors.modid, "trepidation.out");
    private static final float VOLUME = 0.5F;
    /** Ticks between beats with the mob right next to you and at the edge of the range. */
    private static final int FASTEST = 12, SLOWEST = 40;

    private int countdown;

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayer player = mc.thePlayer;
        if (player == null || mc.theWorld == null || mc.isGamePaused() || DimDoors.trepidation == null) {
            countdown = 0;
            return;
        }
        int level = EnchantmentHelper.getEnchantmentLevel(DimDoors.trepidation.effectId, player.getCurrentArmor(2));
        int range = EnchantmentTrepidation.range(level);
        double nearest = level > 0 ? nearest(mc.theWorld, player, range) : -1;
        if (nearest < 0) {
            countdown = 0;
            return;
        }
        if (--countdown <= 0) {
            countdown = FASTEST + (int) Math.round((SLOWEST - FASTEST) * nearest / range);
            play(mc, DUB);
        } else if (countdown == 5) {
            play(mc, LUB);
        }
    }

    /** Distance to the nearest sensed mob within range, or -1. */
    private static double nearest(World world, EntityPlayer player, int range) {
        @SuppressWarnings("unchecked")
        List<EntityLiving> mobs = world
            .getEntitiesWithinAABB(EntityLiving.class, player.boundingBox.expand(range, range, range));
        double nearest = -1;
        for (EntityLiving mob : mobs) {
            if (mob.isDead || !TrepidationMobs.senses(mob)) continue;
            double distance = player.getDistanceToEntity(mob);
            if (distance <= range && (nearest < 0 || distance < nearest)) nearest = distance;
        }
        return nearest;
    }

    private static void play(Minecraft mc, ResourceLocation sound) {
        mc.getSoundHandler()
            .playSound(new PositionedSoundRecord(sound, VOLUME, 1.0F, 0, 0, 0) {

                @Override
                public AttenuationType getAttenuationType() {
                    return AttenuationType.NONE;
                }
            });
    }
}
