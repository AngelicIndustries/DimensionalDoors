package org.dimdev.dimdoors.items;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;

import org.dimdev.dimdoors.DimDoors;

/** A plain crafting material, its texture named after it. */
public class ItemMaterial extends Item {

    public ItemMaterial() {
        setCreativeTab(DimDoors.dimDoorsCreativeTab);
    }

    @Override
    public void registerIcons(IIconRegister iconRegister) {
        itemIcon = iconRegister.registerIcon(DimDoors.modid + ":" + getUnlocalizedName().replace("item.", ""));
    }
}
