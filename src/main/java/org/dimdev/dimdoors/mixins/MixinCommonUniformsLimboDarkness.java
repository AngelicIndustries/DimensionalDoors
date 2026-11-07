package org.dimdev.dimdoors.mixins;

import net.coderbot.iris.gl.uniform.FloatSupplier;
import net.coderbot.iris.gl.uniform.UniformHolder;
import net.coderbot.iris.gl.uniform.UniformUpdateFrequency;

import org.dimdev.dimdoors.client.LimboFog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Limbo's fog and light under a shader pack. Packs draw their own fog and sky and ignore vanilla's, but they turn both
 * black for the darkness effect, which 1.7.10 lacks, so Angelica sets its uniform once to 0. Here it's read every frame
 * and raised in Limbo, with blindness on top for thicker fog and faint night vision for Limbo's even light. Skipped
 * without Angelica.
 */
@Pseudo
@Mixin(targets = "net.coderbot.iris.uniforms.CommonUniforms", remap = false)
public abstract class MixinCommonUniformsLimboDarkness {

    @Redirect(
        method = "generalCommonUniforms",
        at = @At(
            value = "INVOKE",
            target = "Lnet/coderbot/iris/gl/uniform/UniformHolder;uniform1f(Lnet/coderbot/iris/gl/uniform/UniformUpdateFrequency;Ljava/lang/String;Lnet/coderbot/iris/gl/uniform/FloatSupplier;)Lnet/coderbot/iris/gl/uniform/UniformHolder;"),
        require = 0)
    private static UniformHolder dimdoors$limboDarkness(UniformHolder holder, UniformUpdateFrequency frequency,
        String name, FloatSupplier value) {
        if (!"darknessFactor".equals(name)) return holder.uniform1f(frequency, name, value);
        return holder.uniform1f(
            UniformUpdateFrequency.PER_FRAME,
            name,
            () -> Math.max(value.getAsFloat(), LimboFog.shaderDarkness()));
    }

    @Inject(method = "getNightVision", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dimdoors$limboLight(CallbackInfoReturnable<Float> cir) {
        float limbo = LimboFog.shaderNightVision();
        if (limbo > cir.getReturnValue()) cir.setReturnValue(limbo);
    }

    @Inject(method = "getBlindness", at = @At("RETURN"), cancellable = true, require = 0)
    private static void dimdoors$limboBlindness(CallbackInfoReturnable<Float> cir) {
        float limbo = LimboFog.shaderBlindness();
        if (limbo > cir.getReturnValue()) cir.setReturnValue(limbo);
    }
}
