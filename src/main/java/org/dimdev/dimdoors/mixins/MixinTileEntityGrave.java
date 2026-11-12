package org.dimdev.dimdoors.mixins;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IChatComponent;

import org.dimdev.dimdoors.dungeon.DungeonGrave;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;

/**
 * OpenBlocks' grave, for the graves dungeons place (OpenBlocksGraves): those are saved with a mark, spawn no
 * skeletons or bats, call up no storm, equip nobody and spill their loot on the ground when dug. Clicking one only
 * shows its death message. Graves without the mark, players' own, are left alone. OpenBlocks is optional, and the
 * methods it overrides from Minecraft go by both their dev and their runtime names.
 */
@Pseudo
@Mixin(targets = "openblocks.common.tileentity.TileEntityGrave", remap = false)
public abstract class MixinTileEntityGrave extends TileEntity implements DungeonGrave {

    @Unique
    private static final String dimdoors$TAG = "DimDoorsGrave";

    @Shadow
    private IChatComponent deathMessage;

    @Unique
    private boolean dimdoors$dungeonGrave;

    @Override
    public boolean dimdoors$isDungeonGrave() {
        return dimdoors$dungeonGrave;
    }

    @Override
    public void dimdoors$markDungeonGrave() {
        dimdoors$dungeonGrave = true;
        markDirty();
    }

    @Inject(method = { "writeToNBT", "func_145841_b" }, at = @At("TAIL"), require = 0)
    private void dimdoors$saveMark(NBTTagCompound tag, CallbackInfo ci) {
        if (dimdoors$dungeonGrave) {
            tag.setBoolean(dimdoors$TAG, true);
        }
    }

    @Inject(method = { "readFromNBT", "func_145839_a" }, at = @At("TAIL"), require = 0)
    private void dimdoors$loadMark(NBTTagCompound tag, CallbackInfo ci) {
        dimdoors$dungeonGrave = tag.getBoolean(dimdoors$TAG);
    }

    @ModifyExpressionValue(
        method = { "updateEntity", "func_145845_h" },
        at = @At(value = "FIELD", target = "Lopenblocks/Config;spawnSkeletons:Z"),
        require = 0)
    private boolean dimdoors$spawnNothing(boolean spawnSkeletons) {
        return spawnSkeletons && !dimdoors$dungeonGrave;
    }

    @WrapOperation(
        method = "robGrave",
        at = @At(
            value = "INVOKE",
            target = "Lopenblocks/common/GraveAutoEquip;tryEquipOrDrop(Lnet/minecraft/entity/player/EntityPlayer;Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;"),
        require = 0)
    private ItemStack dimdoors$spillLoot(EntityPlayer player, ItemStack stack, Operation<ItemStack> original) {
        // What isn't equipped is dropped where the grave is
        return dimdoors$dungeonGrave ? stack : original.call(player, stack);
    }

    @Inject(method = "ohNoes", at = @At("HEAD"), cancellable = true, require = 0)
    private void dimdoors$noStorm(EntityPlayer player, CallbackInfo ci) {
        if (dimdoors$dungeonGrave) {
            ci.cancel();
        }
    }

    @Inject(method = "autoEquipAll", at = @At("HEAD"), cancellable = true, require = 0)
    private void dimdoors$equipNobody(EntityPlayer player, CallbackInfo ci) {
        if (dimdoors$dungeonGrave) {
            ci.cancel();
        }
    }

    @Inject(method = "onBlockActivated", at = @At("HEAD"), cancellable = true, require = 0)
    private void dimdoors$onlyRead(EntityPlayer player, int side, float hitX, float hitY, float hitZ,
        CallbackInfoReturnable<Boolean> cir) {
        if (!dimdoors$dungeonGrave || player.worldObj.isRemote) {
            return;
        }
        ItemStack held = player.getHeldItem();
        if (held != null && held.getItem()
            .getToolClasses(held)
            .contains("shovel")) {
            return;
        }
        if (deathMessage != null) {
            player.addChatMessage(deathMessage.createCopy());
        }
        cir.setReturnValue(true);
    }
}
