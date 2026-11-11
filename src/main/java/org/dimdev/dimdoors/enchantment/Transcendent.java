package org.dimdev.dimdoors.enchantment;

import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;

import org.dimdev.dimdoors.DimDoors;

import cpw.mods.fml.common.eventhandler.Event;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/** Marks arrows shot from a Transcendent bow; MixinEntityEndermanTranscendent lets them hit. */
public final class Transcendent {

    private static final String TAG = "dimdoorsTranscendent";
    private static final String DRAGONAPI_ENDER_TELEPORT = "Reika.DragonAPI.Instantiable.Event.EnderAttackTPEvent";

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

    /**
     * DragonAPI turns the Enderman's check for indirect damage, which MixinEntityEndermanTranscendent wraps otherwise,
     * into its EnderAttackTPEvent, a LivingAttackEvent with a result: denied, the Enderman takes the hit.
     */
    @SubscribeEvent
    public void onLivingAttack(LivingAttackEvent event) {
        if (event.getClass()
            .getName()
            .equals(DRAGONAPI_ENDER_TELEPORT) && marks(event.source)) {
            event.setResult(Event.Result.DENY);
        }
    }

    /** Whether the damage comes from a marked arrow. */
    public static boolean marks(DamageSource source) {
        Entity projectile = source.getSourceOfDamage();
        return projectile != null && projectile.getEntityData()
            .getBoolean(TAG);
    }
}
