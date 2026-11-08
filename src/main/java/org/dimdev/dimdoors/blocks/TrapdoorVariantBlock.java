package org.dimdev.dimdoors.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;

import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A Transdimensional Trapdoor made from another mod's trapdoor, drawn like that trapdoor. An iron one only opens by
 * redstone, as vanilla trapdoors of iron do.
 */
public class TrapdoorVariantBlock extends TransTrapdoor {

    private final Block base;
    private final ItemStack plain;

    /** @param plain the item the base trapdoor drops, which this one is crafted from and drops */
    public TrapdoorVariantBlock(Block base, ItemStack plain) {
        super(base.getMaterial());
        this.base = base;
        this.plain = plain;
        this.setHardness(1.0F);
        this.setStepSound(base.stepSound);
        this.setBlockName("dimdoors.trapdoorVariant");
    }

    @Override
    public String getLocalizedName() {
        return StatCollector.translateToLocalFormatted("dimdoors.variant.trapdoor", plain.getDisplayName());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        // The base trapdoor registers the textures this one draws
        registerItemIcon(iconRegister, "dimdoors:trapdoor_portal/" + GameRegistry.findUniqueIdentifierFor(this).name);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon plainIcon(int side, int metadata) {
        return base.getIcon(side, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return base.getIcon(blockAccess, x, y, z, side);
    }

    @Override
    public Item getItemDropped(int metadata, Random random, int fortuneLevel) {
        return plain.getItem();
    }

    @Override
    public int damageDropped(int metadata) {
        return plain.getItemDamage();
    }
}
