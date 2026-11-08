package org.dimdev.dimdoors;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.dimdoors.blocks.TrapdoorVariantBlock;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.items.ItemTrapdoorVariant;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Makes Transdimensional Trapdoors out of other trapdoors, from the config's trapdoor variant list. Each works like
 * ours and is crafted the same way from its own trapdoor. Trapdoors from mods that aren't installed are skipped.
 */
public final class TrapdoorVariants {

    private static final Logger LOG = LogManager.getLogger("DimDoors");

    /** Wooden trapdoors and iron, by block registry name. */
    public static final String[] DEFAULTS = { "etfuturum:trapdoor_spruce", "etfuturum:trapdoor_birch",
        "etfuturum:trapdoor_jungle", "etfuturum:trapdoor_acacia", "etfuturum:trapdoor_dark_oak",
        "etfuturum:mangrove_trapdoor", "etfuturum:cherry_trapdoor", "etfuturum:bamboo_trapdoor",
        "etfuturum:crimson_trapdoor", "etfuturum:warped_trapdoor", "etfuturum:iron_trapdoor",

        "etfuturum:bop_bamboo_trapdoor", "etfuturum:bop_cherry_trapdoor", "etfuturum:bop_dark_trapdoor",
        "etfuturum:bop_ethereal_trapdoor", "etfuturum:bop_fir_trapdoor", "etfuturum:bop_hellbark_trapdoor",
        "etfuturum:bop_jacaranda_trapdoor", "etfuturum:bop_magic_trapdoor", "etfuturum:bop_mahogany_trapdoor",
        "etfuturum:bop_mangrove_trapdoor", "etfuturum:bop_palm_trapdoor", "etfuturum:bop_pine_trapdoor",
        "etfuturum:bop_redwood_trapdoor", "etfuturum:bop_sacredoak_trapdoor", "etfuturum:bop_willow_trapdoor",

        "Natura:trapdoor.amaranth", "Natura:trapdoor.bloodwood", "Natura:trapdoor.darkwood",
        "Natura:trapdoor.eucalyptus", "Natura:trapdoor.fusewood", "Natura:trapdoor.ghostwood",
        "Natura:trapdoor.hopseed", "Natura:trapdoor.maple", "Natura:trapdoor.redwood", "Natura:trapdoor.sakura",
        "Natura:trapdoor.silverbell", "Natura:trapdoor.tiger", "Natura:trapdoor.willow",

        "TwilightForest:tile.TFTrapDoorTwilight", "TwilightForest:tile.TFTrapDoorCanopy",
        "TwilightForest:tile.TFTrapDoorMangrove", "TwilightForest:tile.TFTrapDoorDarkwood",
        "TwilightForest:tile.TFTrapDoorTime", "TwilightForest:tile.TFTrapDoorTrans",
        "TwilightForest:tile.TFTrapDoorMine", "TwilightForest:tile.TFTrapDoorSort",

        "aether_legacy:skyroot_trapdoor", "aether_legacy:golden_oak_trapdoor", "aether_legacy:greatroot_trapdoor",
        "aether_legacy:wisproot_trapdoor", "aether_legacy:void_trapdoor",

        "biomic:baobab_trapdoor", "biomic:blackwood_trapdoor", "biomic:blue_bioshroom_trapdoor",
        "biomic:brimwood_trapdoor", "biomic:cobalt_trapdoor", "biomic:cypress_trapdoor", "biomic:dead_trapdoor",
        "biomic:eucalyptus_trapdoor", "biomic:green_bioshroom_trapdoor", "biomic:joshua_trapdoor",
        "biomic:kapok_trapdoor", "biomic:larch_trapdoor", "biomic:magnolia_trapdoor", "biomic:maple_trapdoor",
        "biomic:palm_trapdoor", "biomic:pine_trapdoor", "biomic:pink_bioshroom_trapdoor", "biomic:redwood_trapdoor",
        "biomic:socotra_trapdoor", "biomic:willow_trapdoor", "biomic:wisteria_trapdoor",
        "biomic:yellow_bioshroom_trapdoor",

        "netherlicious:foxfire_trapdoor" };

    private TrapdoorVariants() {}

    static void register(DDProperties properties) {
        List<String> registered = new ArrayList<String>();
        for (String entry : properties.TrapdoorVariants) {
            String name = entry.trim();
            int split = name.indexOf(':');
            if (split <= 0 || split == name.length() - 1) {
                if (!name.isEmpty()) LOG.warn("Trapdoor Variants: \"{}\" needs a trapdoor's registry name", entry);
                continue;
            }
            Block base = GameRegistry.findBlock(name.substring(0, split), name.substring(split + 1));
            if (base == null) {
                continue;
            }
            ItemStack plain = plainItem(base);
            if (plain == null) {
                LOG.warn("Trapdoor Variants: {} drops no item, so it gets no variant", name);
                continue;
            }
            String id = "transdimensional_trapdoor." + name.replace(':', '.')
                .replaceAll("[^A-Za-z0-9_.]", "_");
            TrapdoorVariantBlock block = new TrapdoorVariantBlock(base, plain);
            GameRegistry.registerBlock(block, ItemTrapdoorVariant.class, id);
            if (properties.CraftingTransTrapdoorAllowed) {
                GameRegistry.addRecipe(new ItemStack(block), "y", "x", "y", 'x', Items.ender_pearl, 'y', plain.copy());
            }
            registered.add(id);
        }
        LOG.info("Registered {} trapdoor variants: {}", registered.size(), registered);
    }

    private static ItemStack plainItem(Block base) {
        Item item = base.getItemDropped(0, new Random(0), 0);
        if (item == null) {
            item = Item.getItemFromBlock(base);
        }
        return item == null ? null : new ItemStack(item, 1, base.damageDropped(0));
    }
}
