package org.dimdev.dimdoors.mixins;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.gen.structure.MapGenNetherBridge;
import net.minecraft.world.gen.structure.StructureStart;

import org.dimdev.dimdoors.world.fortresses.FortressGateways;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapGenNetherBridge.Start.class)
public abstract class MixinMapGenNetherBridgeStart {

    @Inject(method = "<init>(Lnet/minecraft/world/World;Ljava/util/Random;II)V", at = @At("RETURN"))
    private void dimdoors$pickGatewayRoom(World world, Random random, int chunkX, int chunkZ, CallbackInfo ci) {
        FortressGateways.pickRoom(((StructureStart) (Object) this).getComponents(), chunkX, chunkZ);
    }
}
