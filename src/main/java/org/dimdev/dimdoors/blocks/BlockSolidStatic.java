package org.dimdev.dimdoors.blocks;

import java.util.Arrays;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.Item;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.BrightenedSprite;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** 6.x's TV-static block: what blocks in Limbo decay into first, where they used to turn to stone. */
public class BlockSolidStatic extends Block {

    /** Turned, mirrored and time-shifted copies of the texture, so static next to static doesn't show a grid. */
    private static final int VARIANTS = 8;

    @SideOnly(Side.CLIENT)
    private IIcon[] icons;

    public BlockSolidStatic() {
        super(Material.rock);
        setHardness(7.0F);
        // 6.x's blast resistance of 25
        setResistance(125.0F / 3.0F);
        setStepSound(soundTypeSand);
        setHarvestLevel("pickaxe", 2);
        setCreativeTab(DimDoors.dimDoorsCreativeTab);
    }

    /** Infrangible Fiber, more with Fortune the way ores give more; Silk Touch keeps the block. */
    @Override
    public Item getItemDropped(int meta, Random random, int fortune) {
        return DimDoors.itemInfrangibleFiber;
    }

    @Override
    public int quantityDroppedWithBonus(int fortune, Random random) {
        return fortune > 0 ? Math.max(1, random.nextInt(fortune + 2)) : 1;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        String name = DimDoors.modid + ":" + getUnlocalizedName();
        icons = new IIcon[VARIANTS];
        if (!(iconRegister instanceof TextureMap)) {
            blockIcon = iconRegister.registerIcon(name);
            Arrays.fill(icons, blockIcon);
            return;
        }
        ResourceLocation source = new ResourceLocation(
            DimDoors.modid,
            "textures/blocks/" + getUnlocalizedName() + ".png");
        // As dark as Unraveled Fabric, so under Dark Limbo it's lifted the same way
        float factor = DimDoors.properties.DarkLimbo ? BlockLimbo.UNRAVELED_FABRIC_BRIGHTNESS : 1.0F;
        for (int k = 0; k < VARIANTS; k++) {
            BrightenedSprite sprite = new BrightenedSprite(name + "_" + k, source, factor, k);
            ((TextureMap) iconRegister).setTextureEntry(name + "_" + k, sprite);
            icons[k] = sprite;
        }
        blockIcon = icons[0];
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess world, int x, int y, int z, int side) {
        long hash = x * 3129871L ^ z * 116129781L ^ y;
        hash = hash * hash * 42317861L + hash * 11L + side * 7919L;
        return icons[(int) (hash >>> 16) & VARIANTS - 1];
    }

    @Override
    public IIcon getIcon(int side, int meta) {
        return blockIcon;
    }
}
