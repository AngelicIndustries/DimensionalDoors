package org.dimdev.dimdoors.enchantment;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;

import org.dimdev.dimdoors.DimDoors;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Marks arrows shot from a Transcendent bow; MixinEntityEndermanTranscendent lets them hit. */
public final class Transcendent {

    private static final String TAG = "dimdoorsTranscendent";

    @SubscribeEvent
    public void onEntityJoinWorld(EntityJoinWorldEvent event) {
        if (event.world.isRemote || DimDoors.transcendent == null || !(event.entity instanceof EntityArrow)) return;
        EntityArrow arrow = (EntityArrow) event.entity;
        // Only while it's being shot: reloaded arrows have no shooter and keep their mark
        if (!(arrow.shootingEntity instanceof EntityLivingBase)) return;
        ItemStack bow = ((EntityLivingBase) arrow.shootingEntity).getHeldItem();
        if (bow != null && EnchantmentHelper.getEnchantmentLevel(DimDoors.transcendent.effectId, bow) > 0) {
            arrow.getEntityData()
                .setBoolean(TAG, true);
        }
    }

    /** Whether the damage comes from a marked arrow. */
    public static boolean marks(DamageSource source) {
        Entity projectile = source.getSourceOfDamage();
        return projectile != null && projectile.getEntityData()
            .getBoolean(TAG);
    }
}
