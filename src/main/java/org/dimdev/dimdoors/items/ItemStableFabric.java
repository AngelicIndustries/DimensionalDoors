package org.dimdev.dimdoors.items;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.item.Item;

import org.dimdev.dimdoors.DimDoors;

public class ItemStableFabric extends Item {

    public ItemStableFabric(int par2) {
        super();
        this.setCreativeTab(DimDoors.dimDoorsCreativeTab);
    }

    @Override
    public void registerIcons(IIconRegister par1IconRegister) {
        this.itemIcon = par1IconRegister.registerIcon(
            DimDoors.modid + ":"
                + this.getUnlocalizedName()
                    .replace("item.", ""));
    }
}
