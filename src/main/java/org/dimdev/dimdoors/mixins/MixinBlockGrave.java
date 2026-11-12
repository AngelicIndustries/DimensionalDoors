package org.dimdev.dimdoors.mixins;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import org.dimdev.dimdoors.dungeon.DungeonGrave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * OpenBlocks' grave block: punching a dungeon's grave (MixinTileEntityGrave) does nothing, not even its owner check.
 */
@Pseudo
@Mixin(targets = "openblocks.common.block.BlockGrave", remap = false)
public abstract class MixinBlockGrave {

    @Inject(method = { "onBlockClicked", "func_149699_a" }, at = @At("HEAD"), cancellable = true, require = 0)
    private void dimdoors$ignorePunches(World world, int x, int y, int z, EntityPlayer player, CallbackInfo ci) {
        TileEntity tile = world.getTileEntity(x, y, z);
        if (tile instanceof DungeonGrave && ((DungeonGrave) tile).dimdoors$isDungeonGrave()) {
            ci.cancel();
        }
    }
}
