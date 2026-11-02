package org.dimdev.dimdoors.items;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.dimdev.dimdoors.DimDoors;

import com.gtnewhorizons.angelica.api.IDynamicLightProducer;

import cpw.mods.fml.common.Optional;

/**
 * A carried Fadebloom lights more around its carrier than a planted one does (Fadebloom Held Light, through Angelica's
 * dynamic lights). Eyes in the Shadows judges it by the planted light, so it still never keeps the eyes away.
 */
@Optional.Interface(iface = "com.gtnewhorizons.angelica.api.IDynamicLightProducer", modid = "angelica")
public class ItemFadebloom extends ItemBlock implements IDynamicLightProducer {

    public ItemFadebloom(Block block) {
        super(block);
    }

    @Override
    @Optional.Method(modid = "angelica")
    public int getLuminance(ItemStack stack) {
        return DimDoors.properties.FadebloomHeldLight;
    }
}
