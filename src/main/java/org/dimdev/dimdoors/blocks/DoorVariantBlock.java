package org.dimdev.dimdoors.blocks;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.dimdev.dimdoors.client.GoggleRiftFX;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.config.DoorKind;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.tileentities.TileEntityDimDoorGold;
import org.dimdev.dimdoors.world.PersonalPocketProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A dimensional door made from another mod's door, drawn exactly like that door, as on modern versions. */
public class DoorVariantBlock extends BaseDimDoor {

    private final Block base;
    private final DoorKind kind;
    private Item item;

    public DoorVariantBlock(Block base, DoorKind kind, DDProperties properties) {
        super(base.getMaterial(), properties);
        this.base = base;
        this.kind = kind;
        this.setHardness(1.0F);
        this.setStepSound(base.stepSound);
        this.setBlockName("dimdoors.variant");
    }

    public DoorKind kind() {
        return kind;
    }

    public Block base() {
        return base;
    }

    public void setDoorItem(Item item) {
        this.item = item;
    }

    @Override
    public Item getDoorItem() {
        return item;
    }

    @Override
    public void placeLink(World world, int x, int y, int z) {
        if (world.isRemote || world.getBlock(x, y - 1, z) != this) {
            return;
        }
        NewDimData dimension = PocketManager.createDimensionData(world);
        if (dimension.getLink(x, y, z) != null) {
            return;
        }
        int orientation = world.getBlockMetadata(x, y - 1, z);
        switch (kind) {
            case POCKET:
            case GOLDEN:
                dimension.createLink(x, y, z, LinkType.POCKET, orientation);
                break;
            case WARP:
                if (dimension.isPocketDimension()) {
                    dimension.createLink(x, y, z, LinkType.SAFE_EXIT, orientation);
                }
                break;
            case DUNGEON:
            case NETHER_DUNGEON:
                dimension.createLink(x, y, z, LinkType.DUNGEON, orientation);
                break;
            case PERSONAL:
                dimension.createLink(
                    x,
                    y,
                    z,
                    world.provider instanceof PersonalPocketProvider ? LinkType.LIMBO : LinkType.PERSONAL,
                    orientation);
                break;
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int metadata) {
        return kind == DoorKind.GOLDEN ? new TileEntityDimDoorGold() : super.createNewTileEntity(world, metadata);
    }

    @Override
    public String getLocalizedName() {
        return StatCollector.translateToLocalFormatted("dimdoors.variant." + kind.id(), base.getLocalizedName());
    }

    /** An unlinked warp variant looks just like its plain door, so it gives off a rift particle now and then. */
    @Override
    @SideOnly(Side.CLIENT)
    public void randomDisplayTick(World world, int x, int y, int z, Random rand) {
        if (kind != DoorKind.WARP || rand.nextInt(2) != 0
            || !isUpperDoorBlock(world.getBlockMetadata(x, y, z))
            || isDoorOnRift(world, x, y, z)) {
            return;
        }
        // Off either face of the door's panel, which sits at one edge of its block
        setBlockBoundsBasedOnState(world, x, y, z);
        double offset = rand.nextBoolean() ? -0.05 : 0.05;
        double px, pz;
        if (maxX - minX < maxZ - minZ) {
            px = x + (offset < 0 ? minX : maxX) + offset;
            pz = z + minZ + rand.nextDouble() * (maxZ - minZ);
        } else {
            px = x + minX + rand.nextDouble() * (maxX - minX);
            pz = z + (offset < 0 ? minZ : maxZ) + offset;
        }
        EffectRenderer effects = Minecraft.getMinecraft().effectRenderer;
        effects.addEffect(
            new GoggleRiftFX(
                world,
                px,
                y - 1 + rand.nextDouble() * 2,
                pz,
                rand.nextGaussian() * 0.01,
                rand.nextGaussian() * 0.01,
                rand.nextGaussian() * 0.01,
                effects));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister iconRegister) {
        // The base door registers the textures this one draws.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int side, int metadata) {
        return base.getIcon(side, metadata);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(IBlockAccess blockAccess, int x, int y, int z, int side) {
        return base.getIcon(blockAccess, x, y, z, side);
    }
}
