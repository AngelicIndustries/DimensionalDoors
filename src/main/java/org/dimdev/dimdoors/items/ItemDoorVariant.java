package org.dimdev.dimdoors.items;

import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.BaseDimDoor;
import org.dimdev.dimdoors.blocks.DoorVariantBlock;
import org.dimdev.dimdoors.client.PortalSprite;
import org.dimdev.dimdoors.config.DoorKind;

import cpw.mods.fml.common.registry.GameData;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/** The item of a door variant, drawn as its base door with the portal through its windows (PortalSprite). */
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
    @SideOnly(Side.CLIENT)
    public void registerIcons(IIconRegister iconRegister) {
        // The base door registers its own icon; ours is drawn from it when the atlas loads
        if (iconRegister instanceof TextureMap map) {
            String name = "dimdoors:door_variant/" + GameData.getItemRegistry()
                .getNameForObject(this)
                .replace(':', '/');
            PortalSprite sprite = PortalSprite
                .door(name, base, map.getTextureType() == 0 ? "textures/blocks" : "textures/items");
            map.setTextureEntry(name, sprite);
            itemIcon = sprite;
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconFromDamage(int damage) {
        return drawnIcon() != null ? drawnIcon()
            : base.getItem()
                .getIconFromDamage(base.getItemDamage());
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIconIndex(ItemStack stack) {
        return drawnIcon() != null ? drawnIcon()
            : base.getItem()
                .getIconIndex(base);
    }

    /** Our icon, or null to draw the base door's. */
    @SideOnly(Side.CLIENT)
    private IIcon drawnIcon() {
        return itemIcon instanceof PortalSprite sprite && !sprite.failed() ? itemIcon : null;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public int getSpriteNumber() {
        return base.getItem()
            .getSpriteNumber();
    }
}
