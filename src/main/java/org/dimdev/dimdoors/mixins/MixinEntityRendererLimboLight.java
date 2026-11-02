package org.dimdev.dimdoors.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.ShaderPacks;
import org.dimdev.dimdoors.world.LimboProvider;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Dark Limbo's ambient light: every color of the lightmap is lifted evenly toward white, so unlit places turn a dim
 * grey (light from the light levels comes out warm or blue and flickers). It follows the player's brightness setting,
 * and shader packs light Limbo their own way.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererLimboLight {

    @Shadow
    private Minecraft mc;

    @Shadow
    @Final
    private int[] lightmapColors;

    @Inject(
        method = "updateLightmap",
        at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/renderer/EntityRenderer;lightmapTexture:Lnet/minecraft/client/renderer/texture/DynamicTexture;",
            opcode = Opcodes.GETFIELD))
    private void dimdoors$liftLimbo(float partialTicks, CallbackInfo ci) {
        float ambient = DimDoors.properties.LimboAmbientLight;
        if (ambient <= 0 || !DimDoors.properties.DarkLimbo
            || mc.theWorld == null
            || !(mc.theWorld.provider instanceof LimboProvider)
            || ShaderPacks.inUse()) {
            return;
        }
        // The gamma curve vanilla gives the lightmap, applied to the lift
        float gamma = mc.gameSettings.gammaSetting;
        float dark = 1.0F - ambient;
        float lift = ambient * (1.0F - gamma) + (1.0F - dark * dark * dark * dark) * gamma;
        for (int i = 0; i < lightmapColors.length; i++) {
            int color = lightmapColors[i];
            int r = color >> 16 & 255, g = color >> 8 & 255, b = color & 255;
            r += Math.round((255 - r) * lift);
            g += Math.round((255 - g) * lift);
            b += Math.round((255 - b) * lift);
            lightmapColors[i] = color & 0xFF000000 | r << 16 | g << 8 | b;
        }
    }
}
