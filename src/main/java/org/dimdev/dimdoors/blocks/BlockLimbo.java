package org.dimdev.dimdoors.blocks;

import java.util.ArrayList;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.BrightenedSprite;
import org.dimdev.dimdoors.world.LimboDecay;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

public class BlockLimbo extends Block {

    /** How much brighter than its file Unraveled Fabric is drawn under Dark Limbo. */
    static final float UNRAVELED_FABRIC_BRIGHTNESS = 2.0F;

    private final int limboDimensionID;
    private final LimboDecay decay;

    public BlockLimbo(int j, Material par2Material, int limboDimensionID, LimboDecay decay) {
        super(Material.ground);
        this.limboDimensionID = limboDimensionID;
        this.decay = decay;
        this.setTickRandomly(true);
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
    }

    /**
     * Retrieves the block texture to use based on the display side. Args: iBlockAccess, x, y, z, side
     */
    @SideOnly(Side.CLIENT)
    @Override
    public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return this.getIcon(side, blockAccess.getBlockMetadata(x, y, z));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        String name = DimDoors.modid + ":" + this.getUnlocalizedName();
        // Nearly black as drawn: under Dark Limbo's real lighting it shows a little of the light that falls on it
        if (DimDoors.properties.DarkLimbo && iconRegister instanceof TextureMap) {
            BrightenedSprite sprite = new BrightenedSprite(
                name + "_lit",
                new ResourceLocation(DimDoors.modid, "textures/blocks/" + this.getUnlocalizedName() + ".png"),
                UNRAVELED_FABRIC_BRIGHTNESS);
            ((TextureMap) iconRegister).setTextureEntry(name + "_lit", sprite);
            this.blockIcon = sprite;
        } else {
            this.blockIcon = iconRegister.registerIcon(name);
        }
    }

    @Override
    public IIcon getIcon(int par1, int par2) {
        return this.blockIcon;
    }

    /**
     * Frayed Filament instead of the fabric itself as in 6.x: one in ten, then a seventh, a quarter, always with
     * Fortune.
     */
    @Override
    public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
        final float[] filamentChance = { 0.1F, 1 / 7F, 0.25F, 1.0F };
        ArrayList<ItemStack> drops = new ArrayList<ItemStack>();
        if (world.rand.nextFloat() < filamentChance[Math.min(fortune, 3)]) {
            drops.add(new ItemStack(DimDoors.itemFrayedFilament));
        } else {
            drops.add(new ItemStack(this));
        }
        return drops;
    }

    /**
     * If the block is in Limbo, attempt to decay surrounding blocks upon receiving a random update tick.
     */
    @Override
    public void updateTick(World world, int x, int y, int z, Random random) {
        // Make sure this block is in Limbo
        if (world.provider.dimensionId == limboDimensionID) {
            decay.applySpreadDecay(world, x, y, z);
        }
    }
}
