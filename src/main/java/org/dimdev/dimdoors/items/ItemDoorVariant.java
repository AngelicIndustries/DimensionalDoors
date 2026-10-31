package org.dimdev.dimdoors.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.BaseDimDoor;
import org.dimdev.dimdoors.blocks.DoorVariantBlock;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.config.DoorKind;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The item of a door variant, drawn with the base door's item icon. */
public class ItemDoorVariant extends BaseItemDoor {

    private final DoorVariantBlock door;
    private final ItemStack base;

    public ItemDoorVariant(DoorVariantBlock door, ItemStack base) {
        super(door.getMaterial(), null);
        this.door = door;
        this.base = base;
        this.setUnlocalizedName("dimdoors.variant");
    }

    public DoorKind kind() {
        return door.kind();
    }

    /** The plain door this one is made from. */
    public ItemStack base() {
        return base.copy();
    }

    @Override
    protected BaseDimDoor getDoorBlock() {
        return door;
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return StatCollector.translateToLocalFormatted("dimdoors.variant." + kind().id(), base.getDisplayName());
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
        DimDoors.translateAndAdd(infoKey(kind()), list);
    }

    /** Variants share their tooltip with our door of the same kind. */
    private static String infoKey(DoorKind kind) {
        switch (kind) {
            case POCKET:
                return "info.dimDoor";
            case WARP:
                return "info.warpDoor";
            case GOLDEN:
                return "info.goldDimDoor";
            case PERSONAL:
                return "info.personalDimDoor";
            case NETHER_DUNGEON:
                return "info.netherDungeonDoor";
            default:
                return "info.dungeonDoor";
        }
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        // The base door registers the icon this one draws.
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return base.getItem()
            .getIconFromDamage(base.getItemDamage());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        return base.getItem()
            .getIconIndex(base);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return base.getItem()
            .getSpriteNumber();
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getColorFromItemStack(ItemStack stack, int pass) {
        return DDProperties.instance().DoorVariantTint;
    }
}
