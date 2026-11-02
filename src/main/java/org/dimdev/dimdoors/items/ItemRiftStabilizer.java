package org.dimdev.dimdoors.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.tileentities.TileEntityRift;

import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Stops a rift growing, unravelling blocks and spawning Endermen, for good. From the 1.12 rewrite, with its six uses
 * and recipe. Works like the Rift Remover: the client finds the rift and passes the click to the server.
 */
public class ItemRiftStabilizer extends Item {

    private static final int USES = 6;

    public ItemRiftStabilizer() {
        super();
        this.setMaxStackSize(1);
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
        this.setMaxDamage(USES - 1);
    }

    @Override
    public void registerIcons(IIconRegister register) {
        this.itemIcon = register.registerIcon(
            DimDoors.modid + ":"
                + this.getUnlocalizedName()
                    .replace("item.", ""));
    }

    @Override
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
        if (!world.isRemote) {
            return stack;
        }
        MovingObjectPosition hit = this.getMovingObjectPositionFromPlayer(world, player, true);
        if (hit != null && findRift(world, hit.blockX, hit.blockY, hit.blockZ) != null
            && player.canPlayerEdit(hit.blockX, hit.blockY, hit.blockZ, hit.sideHit, stack)) {
            // Invoke onPlayerRightClick() so onItemUseFirst() runs on the server too
            FMLClientHandler.instance()
                .getClient().playerController.onPlayerRightClick(
                    player,
                    world,
                    stack,
                    hit.blockX,
                    hit.blockY,
                    hit.blockZ,
                    hit.sideHit,
                    hit.hitVec);
        }
        return stack;
    }

    @Override
    public boolean onItemUseFirst(ItemStack stack, EntityPlayer player, World world, int x, int y, int z, int side,
        float hitX, float hitY, float hitZ) {
        MovingObjectPosition hit = this.getMovingObjectPositionFromPlayer(world, player, true);
        if (hit == null) {
            return true;
        }
        x = hit.blockX;
        y = hit.blockY;
        z = hit.blockZ;
        TileEntityRift rift = findRift(world, x, y, z);
        if (rift == null || !player.canPlayerEdit(x, y, z, side, stack)) {
            return true;
        }
        if (rift.isStabilized() || rift.shouldClose) {
            if (world.isRemote) {
                player.addChatComponentMessage(new ChatComponentTranslation("info.riftStabilizer.already"));
            }
            return true;
        }
        rift.stabilize();
        if (world.isRemote) {
            // Tell the server about this
            return false;
        }
        if (!player.capabilities.isCreativeMode) {
            stack.damageItem(1, player);
        }
        world.playSoundAtEntity(player, DimDoors.modid + ":riftClose", 0.6f, 1.5f);
        player.addChatComponentMessage(new ChatComponentTranslation("info.riftStabilizer.done"));
        return true;
    }

    private static TileEntityRift findRift(World world, int x, int y, int z) {
        if (world.getBlock(x, y, z) != DimDoors.blockRift || PocketManager.createDimensionData(world)
            .getLink(x, y, z) == null) {
            return null;
        }
        TileEntity tile = world.getTileEntity(x, y, z);
        return tile instanceof TileEntityRift ? (TileEntityRift) tile : null;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @SideOnly(Side.CLIENT)
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        DimDoors.translateAndAdd("info.riftStabilizer", list);
    }
}
