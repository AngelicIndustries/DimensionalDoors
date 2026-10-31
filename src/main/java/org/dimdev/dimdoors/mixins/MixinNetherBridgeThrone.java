package org.dimdev.dimdoors.mixins;

import java.util.Random;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.StructureComponent;
import net.minecraft.world.gen.structure.StructureNetherBridgePieces;

import org.dimdev.dimdoors.world.fortresses.ComponentNetherGateway;
import org.dimdev.dimdoors.world.fortresses.FortressGateways;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StructureNetherBridgePieces.Throne.class)
public abstract class MixinNetherBridgeThrone implements FortressGateways.GatewayRoom {

    @Unique
    private boolean dimdoors$gateway;

    @Override
    public boolean dimdoors$isGateway() {
        return dimdoors$gateway;
    }

    @Override
    public void dimdoors$setGateway(boolean gateway) {
        dimdoors$gateway = gateway;
    }

    @Inject(method = "func_143012_a", at = @At("TAIL"))
    private void dimdoors$writeGateway(NBTTagCompound tag, CallbackInfo ci) {
        if (dimdoors$gateway) {
            tag.setBoolean("DDGateway", true);
        }
    }

    @Inject(method = "func_143011_b", at = @At("TAIL"))
    private void dimdoors$readGateway(NBTTagCompound tag, CallbackInfo ci) {
        dimdoors$gateway = tag.getBoolean("DDGateway");
    }

    @Inject(method = "addComponentParts", at = @At("HEAD"), cancellable = true)
    private void dimdoors$buildGateway(World world, Random random, StructureBoundingBox bounds,
        CallbackInfoReturnable<Boolean> cir) {
        if (dimdoors$gateway) {
            cir.setReturnValue(
                ComponentNetherGateway.createFromComponent((StructureComponent) (Object) this, random)
                    .addComponentParts(world, random, bounds));
        }
    }
}
