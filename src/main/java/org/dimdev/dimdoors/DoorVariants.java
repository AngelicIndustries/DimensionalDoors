package org.dimdev.dimdoors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.dimdoors.blocks.DoorVariantBlock;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.config.DoorKind;
import org.dimdev.dimdoors.config.DoorVariantList;
import org.dimdev.dimdoors.items.BaseItemDoor;
import org.dimdev.dimdoors.items.ItemDoorVariant;

import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Makes dimensional doors out of other doors, from the config's door variant list. A door whose kind matches one of
 * our own doors (the iron door and the Dimensional Door, for one) leads to that door instead of a new variant. Doors
 * from mods that aren't installed are skipped.
 */
public final class DoorVariants {

    private static final Logger LOG = LogManager.getLogger("DimDoors");

    private DoorVariants() {}

    static void register(DDProperties properties) {
        List<String> problems = new ArrayList<String>();
        Map<String, DoorKind> doors = DoorVariantList.parse(properties.DoorVariants, problems);
        for (String problem : problems) {
            LOG.warn("Door Variants: {}", problem);
        }
        List<String> registered = new ArrayList<String>();
        for (Map.Entry<String, DoorKind> entry : doors.entrySet()) {
            String name = entry.getKey();
            DoorKind kind = entry.getValue();
            int split = name.indexOf(':');
            Block base = GameRegistry.findBlock(name.substring(0, split), name.substring(split + 1));
            if (base == null) {
                continue;
            }
            ItemStack plainDoor = plainDoor(base);
            if (plainDoor == null) {
                LOG.warn("Door Variants: {} drops no door item, so it gets no variant", name);
                continue;
            }
            BaseItemDoor door = ownDoor(name, kind);
            if (door == null) {
                DoorVariantBlock block = new DoorVariantBlock(base, kind, properties);
                String id = DoorVariantList.variantName(name, kind);
                GameRegistry.registerBlock(block, null, id);
                ItemDoorVariant item = new ItemDoorVariant(block, plainDoor);
                GameRegistry.registerItem(item, id + ".item");
                block.setDoorItem(item);
                if (craftable(kind, properties)) {
                    // Warp doors take a pearl like the oak Warp Door, the rest stable fabric
                    Item catalyst = kind == DoorKind.WARP ? Items.ender_pearl : DimDoors.itemStableFabric;
                    GameRegistry.addRecipe(new ItemStack(item), "yxy", 'x', catalyst, 'y', plainDoor.copy());
                }
                door = item;
                registered.add(id);
            }
            // Personal doors are loot, so a plain door under a rift doesn't become one
            if (kind != DoorKind.PERSONAL) {
                BaseItemDoor.mapDoorItem(plainDoor, door);
            }
        }
        LOG.info("Registered {} door variants: {}", registered.size(), registered);
    }

    /** One of our own doors, when the entry describes it. */
    private static BaseItemDoor ownDoor(String name, DoorKind kind) {
        Item door = null;
        if (name.equals("minecraft:iron_door") && kind == DoorKind.POCKET) {
            door = DimDoors.itemDimensionalDoor;
        } else if (name.equals("minecraft:wooden_door") && kind == DoorKind.WARP) {
            door = DimDoors.itemWarpDoor;
        } else if (name.equals("dimdoors:Quartz Door") && kind == DoorKind.PERSONAL) {
            door = DimDoors.itemPersonalDoor;
        } else if (name.equals("dimdoors:Golden Door") && kind == DoorKind.GOLDEN) {
            door = DimDoors.itemGoldenDimensionalDoor;
        }
        return (BaseItemDoor) door;
    }

    private static ItemStack plainDoor(Block base) {
        Item item = base.getItemDropped(0, new Random(0), 0);
        if (item == null) {
            item = Item.getItemFromBlock(base);
        }
        return item == null ? null : new ItemStack(item, 1, base.damageDropped(0));
    }

    private static boolean craftable(DoorKind kind, DDProperties properties) {
        switch (kind) {
            case POCKET:
                return properties.CraftingDimensionalDoorAllowed;
            case WARP:
                return properties.CraftingWarpDoorAllowed;
            case GOLDEN:
                return properties.CraftingGoldenDimensionalDoorAllowed;
            case PERSONAL:
                return properties.CraftingPersonalDimDoorAllowed;
            default:
                return properties.CraftingDungeonDoorsAllowed;
        }
    }
}
