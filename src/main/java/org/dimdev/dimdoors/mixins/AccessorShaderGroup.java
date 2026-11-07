package org.dimdev.dimdoors.mixins;

import java.util.List;

import net.minecraft.client.shader.ShaderGroup;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** A post shader's passes, to set the uniforms of MonolithAggroEffect's each frame. */
@Mixin(ShaderGroup.class)
public interface AccessorShaderGroup {

    @Accessor("listShaders")
    List<?> getListShaders();
}
