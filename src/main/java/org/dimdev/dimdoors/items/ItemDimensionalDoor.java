package org.dimdev.dimdoors.items;

import java.util.List;

import net.minecraft.block.material.Material;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemDoor;
import net.minecraft.item.ItemStack;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.blocks.BaseDimDoor;

public class ItemDimensionalDoor extends BaseItemDoor {

    public ItemDimensionalDoor(Material material, ItemDoor door) {
        super(material, door);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public void addInformation(ItemStack par1ItemStack, EntityPlayer par2EntityPlayer, List par3List, boolean par4) {
        DimDoors.translateAndAdd("info.dimDoor", par3List);
    }

    @Override
    protected BaseDimDoor getDoorBlock() {
        return DimDoors.dimensionalDoor;
    }
}
