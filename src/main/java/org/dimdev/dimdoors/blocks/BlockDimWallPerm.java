package org.dimdev.dimdoors.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.helpers.LimboEscape;

public class BlockDimWallPerm extends Block {

    private static final Random random = new Random();
    private static DDProperties properties = null;

    public BlockDimWallPerm(int j, Material par2Material) {
        super(Material.ground);
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
        if (properties == null) properties = DDProperties.instance();
    }

    @Override
    public void registerBlockIcons(IIconRegister par1IconRegister) {
        this.blockIcon = par1IconRegister.registerIcon(DimDoors.modid + ":" + this.getUnlocalizedName());
    }

    @Override
    public int quantityDropped(Random par1Random) {
        return 0;
    }

    @Override
    public void onBlockDestroyedByPlayer(World par1World, int par2, int par3, int par4, int par5) {}

    /**
     * Only matters if the player is in limbo, acts to teleport the player from limbo back to dim 0
     */
    @Override
    public void onEntityWalking(World world, int x, int y, int z, Entity entity) {
        if (!world.isRemote && world.provider.dimensionId == properties.LimboDimensionID
            && DimDoors.worldProperties.LimboEscapeEnabled) {
            World overworld = DimensionManager.getWorld(0);
            if (overworld != null && entity instanceof EntityPlayerMP) {
                LimboEscape.escape((EntityPlayerMP) entity, overworld, x, z, properties, random);
            }
        }
    }
}
