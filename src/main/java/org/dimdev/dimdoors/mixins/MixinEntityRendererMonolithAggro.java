package org.dimdev.dimdoors.mixins;

import net.minecraft.client.renderer.EntityRenderer;

import org.dimdev.dimdoors.client.MonolithAggroEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the Monolith aggro screen effect right after the world, past any shader pack and before the HUD. */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererMonolithAggro {

    @Inject(
        method = "updateCameraAndRender",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;renderWorld(FJ)V",
            shift = At.Shift.AFTER),
        require = 0)
    private void dimdoors$monolithAggro(float partialTicks, CallbackInfo ci) {
        MonolithAggroEffect.render(partialTicks);
    }
}
