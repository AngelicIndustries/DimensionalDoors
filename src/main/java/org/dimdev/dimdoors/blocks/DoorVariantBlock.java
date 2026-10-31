package org.dimdev.dimdoors.blocks;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.config.DoorKind;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.tileentities.TileEntityDimDoorGold;
import org.dimdev.dimdoors.world.PersonalPocketProvider;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** A dimensional door made from another mod's door, drawn with that door's textures. */
public class DoorVariantBlock extends BaseDimDoor {

    /** The tinted door renderer, set by the client proxy. Vanilla's door renderer until then. */
    public static int renderType = 7;

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

    @Override
    public int getRenderType() {
        return renderType;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int colorMultiplier(IBlockAccess blockAccess, int x, int y, int z) {
        return properties.DoorVariantTint;
    }
}
