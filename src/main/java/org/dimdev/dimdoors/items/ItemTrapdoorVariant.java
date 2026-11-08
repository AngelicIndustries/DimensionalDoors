package org.dimdev.dimdoors.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

/** A trapdoor variant's item, named after its base trapdoor like the block. */
public class ItemTrapdoorVariant extends ItemBlock {

    public ItemTrapdoorVariant(Block block) {
        super(block);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        return field_150939_a.getLocalizedName();
    }
}
