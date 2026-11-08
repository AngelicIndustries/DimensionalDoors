package org.dimdev.dimdoors.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.PortalSprite;
import org.dimdev.dimdoors.core.DDTeleporter;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.items.ItemDDKey;
import org.dimdev.dimdoors.tileentities.TileEntityTransTrapdoor;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class TransTrapdoor extends BlockTrapDoor implements IDimDoor, ITileEntityProvider {

    /** The item's icon: the trapdoor with the portal showing through, as its item draws it in 3D from block icons. */
    @SideOnly(Side.CLIENT)
    private PortalSprite portalIcon;

    public TransTrapdoor(Material material) {
        super(material);
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        this.blockIcon = iconRegister.registerIcon(DimDoors.modid + ":" + this.getUnlocalizedName());
        registerItemIcon(iconRegister, DimDoors.modid + ":trapdoor_portal/dimHatch");
    }

    /** Draws the item's icon from the plain one when the atlas loads. */
    @SideOnly(Side.CLIENT)
    protected void registerItemIcon(IIconRegister iconRegister, String name) {
        if (iconRegister instanceof TextureMap map) {
            portalIcon = PortalSprite.trapdoor(name, this);
            map.setTextureEntry(name, portalIcon);
        }
    }

    /** The trapdoor's own icon, without the portal. */
    @SideOnly(Side.CLIENT)
    public IIcon plainIcon(int side, int metadata) {
        return blockIcon;
    }

    /** Asked for by the item and particles; the trapdoor in the world is drawn plain, the portal showing below it. */
    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return portalIcon != null && !portalIcon.failed() ? portalIcon : plainIcon(side, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return plainIcon(side, blockAccess.getBlockMetadata(x, y, z));
    }

    // Teleports the player to the exit link of that dimension, assuming it is a pocket
    @Override
    public void onEntityCollidedWithBlock(World world, int x, int y, int z, Entity entity) {
        enterDimDoor(world, x, y, z, entity);
    }

    public boolean checkCanOpen(World world, int x, int y, int z) {
        return this.checkCanOpen(world, x, y, z, null);
    }

    public boolean checkCanOpen(World world, int x, int y, int z, EntityPlayer player) {
        DimLink link = PocketManager.getLink(x, y, z, world);
        if (link == null || player == null) {
            return link == null;
        }
        if (!link.getLockState()) {
            return true;
        }

        for (ItemStack item : player.inventory.mainInventory) {
            if (item != null) {
                if (item.getItem() instanceof ItemDDKey) {
                    if (link.tryToOpen(item)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean onBlockActivated(World par1World, int par2, int par3, int par4, EntityPlayer par5EntityPlayer,
        int par6, float par7, float par8, float par9) {
        if (this.checkCanOpen(par1World, par2, par3, par4, par5EntityPlayer)) {
            return super.onBlockActivated(par1World, par2, par3, par4, par5EntityPlayer, par6, par7, par8, par9);
        }
        return false;
    }

    public void onPoweredBlockChange(World par1World, int par2, int par3, int par4, boolean par5) {
        if (this.checkCanOpen(par1World, par2, par3, par4)) {
            super.func_150120_a(par1World, par2, par3, par4, par5);
        }
    }

    @Override
    public void enterDimDoor(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && func_150118_d(world.getBlockMetadata(x, y, z))) {
            DimLink link = PocketManager.getLink(x, y, z, world);
            if (link != null && (link.linkType() != LinkType.PERSONAL || entity instanceof EntityPlayer)) {
                DDTeleporter.traverseDimDoor(world, link, entity, this);
            }
            super.func_150120_a(world, x, y, z, false);
        }
    }

    @Override
    public void onBlockAdded(World world, int x, int y, int z) {
        this.placeLink(world, x, y, z);
        world.setTileEntity(x, y, z, this.createNewTileEntity(world, world.getBlockMetadata(x, y, z)));
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return new TileEntityTransTrapdoor();
    }

    @Override
    public void placeLink(World world, int x, int y, int z) {
        if (!world.isRemote) {
            NewDimData dimension = PocketManager.createDimensionData(world);
            DimLink link = dimension.getLink(x, y, z);
            if (link == null && dimension.isPocketDimension()) {
                dimension.createLink(x, y, z, LinkType.UNSAFE_EXIT, 0);
            }
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemStack getPickBlock(MovingObjectPosition target, World world, int x, int y, int z, EntityPlayer player) {
        return new ItemStack(this.getDoorItem(), 1, 0);
    }

    /** Breaking it gives back the plain trapdoor it was made from. */
    @Override
    public Item getItemDropped(int metadata, Random random, int fortuneLevel) {
        return Item.getItemFromBlock(Blocks.trapdoor);
    }

    @Override
    public Item getDoorItem() {
        return Item.getItemFromBlock(this);
    }

    public static boolean isTrapdoorSetLow(int metadata) {
        return (metadata & 8) == 0;
    }

    @Override
    public TileEntity initDoorTE(World world, int x, int y, int z) {
        TileEntity te = this.createNewTileEntity(world, world.getBlockMetadata(x, y, z));
        world.setTileEntity(x, y, z, te);
        return te;
    }

    @Override
    public boolean isDoorOnRift(World world, int x, int y, int z) {
        return PocketManager.getLink(x, y, z, world) != null;
    }

    @Override
    public void breakBlock(World world, int x, int y, int z, Block oldBlock, int oldMeta) {
        // This function runs on the server side after a block is replaced
        // We MUST call super.breakBlock() since it involves removing tile entities
        super.breakBlock(world, x, y, z, oldBlock, oldMeta);

        // Schedule rift regeneration for this block if it was replaced
        if (world.getBlock(x, y, z) != oldBlock) {
            DimDoors.riftRegenerator.scheduleFastRegeneration(x, y, z, world);
        }
    }
}
