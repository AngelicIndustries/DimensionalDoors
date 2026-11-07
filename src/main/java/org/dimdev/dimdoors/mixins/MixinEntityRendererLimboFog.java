package org.dimdev.dimdoors.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.dimdev.dimdoors.client.LimboFog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Limbo's fog, set right after the event that ends vanilla's plain fog (the second post in setupFog), so it comes after
 * every handler of that event. Blindness, water and lava fog stay as they are.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererLimboFog {

    @Shadow
    private Minecraft mc;

    @Shadow
    private float farPlaneDistance;

    @Inject(
        method = "setupFog",
        at = @At(
            value = "INVOKE",
            target = "Lcpw/mods/fml/common/eventhandler/EventBus;post(Lcpw/mods/fml/common/eventhandler/Event;)Z",
            ordinal = 1,
            shift = At.Shift.AFTER))
    private void dimdoors$limboFog(int fogMode, float partialTicks, CallbackInfo ci) {
        LimboFog.apply(mc.renderViewEntity, farPlaneDistance);
    }
}
