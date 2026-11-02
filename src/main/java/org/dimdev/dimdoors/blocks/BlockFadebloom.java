package org.dimdev.dimdoors.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.IIcon;
import net.minecraft.world.World;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.client.FadebloomSprite;
import org.dimdev.dimdoors.config.DDProperties;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * A pale orchid that grows on Limbo's Unraveled Fabric and glows faintly, enough to see a few blocks by. Picked up,
 * it lights a little around whoever carries it (as a dynamic light), never enough to keep the eyes away.
 */
public class BlockFadebloom extends BlockBush {

    /** Its renderer (stem, then glowing petals), set by the client proxy. Crossed squares until then. */
    public static int renderType = 1;

    @SideOnly(Side.CLIENT)
    private IIcon stem, petals, whole;

    public BlockFadebloom(DDProperties properties) {
        super(Material.plants);
        this.setLightLevel(properties.FadebloomLight / 15.0F);
        this.setStepSound(soundTypeGrass);
        this.setHardness(0.0F);
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
        this.setBlockName("fadebloom");
    }

    @Override
    protected boolean canPlaceBlockOn(Block ground) {
        return ground == DimDoors.blockLimbo || ground == Blocks.grass || ground == Blocks.dirt;
    }

    /** Forge asks the ground whether it sustains plants, which only grass, dirt and farmland do for flowers. */
    @Override
    public boolean canBlockStay(World world, int x, int y, int z) {
        return canPlaceBlockOn(world.getBlock(x, y - 1, z));
    }

    @Override
    public int getRenderType() {
        return renderType;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister register) {
        stem = sprite(register, "dimdoors:fadebloom_stem", FadebloomSprite.Part.STEM);
        petals = sprite(register, "dimdoors:fadebloom_petals", FadebloomSprite.Part.PETALS);
        whole = sprite(register, "dimdoors:fadebloom", FadebloomSprite.Part.WHOLE);
    }

    @SideOnly(Side.CLIENT)
    private static IIcon sprite(IIconRegister register, String name, FadebloomSprite.Part part) {
        if (!(register instanceof TextureMap)) return register.registerIcon("flower_blue_orchid");
        FadebloomSprite sprite = new FadebloomSprite(name, part);
        ((TextureMap) register).setTextureEntry(name, sprite);
        return sprite;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return whole;
    }

    @SideOnly(Side.CLIENT)
    public IIcon stemIcon() {
        return stem;
    }

    @SideOnly(Side.CLIENT)
    public IIcon petalIcon() {
        return petals;
    }
}
