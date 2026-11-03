package org.dimdev.dimdoors.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.ItemStack;

/** 6.x's Transcendent: arrows from the bow hit Endermen instead of them teleporting away. Loot only. */
public class EnchantmentTranscendent extends Enchantment {

    public EnchantmentTranscendent(int id) {
        super(id, 2, EnumEnchantmentType.bow);
        setName("dimdoors.transcendent");
    }

    @Override
    public int getMinEnchantability(int level) {
        return 15 + (level - 1) * 10;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 30;
    }

    @Override
    public int getMaxLevel() {
        return 1;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack) {
        return false;
    }

    @Override
    public boolean isAllowedOnBooks() {
        return false;
    }
}
