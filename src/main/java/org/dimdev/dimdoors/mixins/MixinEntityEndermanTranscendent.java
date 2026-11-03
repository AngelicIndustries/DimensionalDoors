package org.dimdev.dimdoors.mixins;

import net.minecraft.entity.monster.EntityEnderman;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

import org.dimdev.dimdoors.enchantment.Transcendent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/** Transcendent arrows hit Endermen: the hit counts as direct, so the Enderman doesn't teleport away. */
@Mixin(EntityEnderman.class)
public abstract class MixinEntityEndermanTranscendent {

    @WrapOperation(method = "attackEntityFrom", constant = @Constant(classValue = EntityDamageSourceIndirect.class))
    private boolean dimdoors$transcendentArrowsHit(Object source, Operation<Boolean> original) {
        return original.call(source) && !Transcendent.marks((DamageSource) source);
    }

    /** Arrows fly on through Endermen, which always dodged them; one that hit has to stop. */
    @Inject(method = "attackEntityFrom", at = @At("RETURN"))
    private void dimdoors$transcendentArrowsStop(DamageSource source, float amount,
        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && Transcendent.marks(source)) source.getSourceOfDamage()
            .setDead();
    }
}
