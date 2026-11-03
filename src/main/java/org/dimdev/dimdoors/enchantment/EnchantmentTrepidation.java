package org.dimdev.dimdoors.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnumEnchantmentType;
import net.minecraft.item.ItemStack;

/** 6.x's Trepidation: the wearer hears a heartbeat when a hostile mob comes within 5 blocks a level. Loot only. */
public class EnchantmentTrepidation extends Enchantment {

    public EnchantmentTrepidation(int id) {
        super(id, 2, EnumEnchantmentType.armor_torso);
        setName("dimdoors.trepidation");
    }

    /** How far the wearer senses mobs. */
    public static int range(int level) {
        return 5 * level;
    }

    @Override
    public int getMinEnchantability(int level) {
        return 5 + (level - 1) * 8;
    }

    @Override
    public int getMaxEnchantability(int level) {
        return getMinEnchantability(level) + 20;
    }

    @Override
    public int getMaxLevel() {
        return 3;
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
