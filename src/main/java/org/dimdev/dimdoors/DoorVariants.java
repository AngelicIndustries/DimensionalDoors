package org.dimdev.dimdoors;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dimdev.dimdoors.blocks.DoorVariantBlock;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.config.DoorKind;
import org.dimdev.dimdoors.config.DoorVariantList;
import org.dimdev.dimdoors.items.BaseItemDoor;
import org.dimdev.dimdoors.items.ItemDoorVariant;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Makes dimensional doors out of other doors, from the config's door variant list. A door whose kind matches one of
 * our own doors (the iron door and the Dimensional Door, for one) leads to that door instead of a new variant. Doors
 * from mods that aren't installed are skipped; doors of installed mods that can't be found are logged.
 */
public final class DoorVariants {

    private static final Logger LOG = LogManager.getLogger("DimDoors");

    /** Doors whose blocks drop something else and whose items hold no block, with the items that place them. */
    private static final Map<String, String> KNOWN_ITEMS = new HashMap<String, String>();

    static {
        // Twilight Forest's door blocks drop oak doors
        for (String wood : new String[] { "Twilight", "Canopy", "Mangrove", "Darkwood", "Time", "Trans", "Mine",
            "Sort" }) {
            KNOWN_ITEMS.put("TwilightForest:tile.TFDoor" + wood, "TwilightForest:item.door" + wood);
        }
        KNOWN_ITEMS.put("witchery:icedoor", "witchery:ingredient:152");
    }

    private DoorVariants() {}

    static void register(DDProperties properties) {
        List<String> problems = new ArrayList<String>();
        Map<String, DoorKind> doors = DoorVariantList.parse(properties.DoorVariants, problems);
        List<String> registered = new ArrayList<String>();
        Set<Block> bases = new HashSet<Block>();
        for (Map.Entry<String, DoorKind> entry : doors.entrySet()) {
            String name = entry.getKey();
            DoorKind kind = entry.getValue();
            Block base;
            ItemStack plainDoor;
            int plus = name.indexOf('+');
            if (plus >= 0) {
                base = findBlock(name.substring(0, plus));
                plainDoor = findItem(name.substring(plus + 1));
                if (base == null || plainDoor == null) {
                    if (installed(name)) {
                        problems.add(
                            "no " + (base == null ? "block " + name.substring(0, plus)
                                : "item " + name.substring(plus + 1)));
                    }
                    continue;
                }
            } else if ((base = findBlock(name)) != null) {
                plainDoor = KNOWN_ITEMS.containsKey(name) ? findItem(KNOWN_ITEMS.get(name)) : plainDoor(base);
                if (plainDoor == null) {
                    problems.add(
                        name + " drops no door item of its own; name its item too, as " + name + "+item=" + kind.id());
                    continue;
                }
            } else if ((plainDoor = findItem(name)) != null) {
                base = KNOWN_ITEMS.containsValue(name) ? findBlock(knownBlock(name)) : placedBlock(plainDoor.getItem());
                if (base == null) {
                    problems.add(
                        "can't tell which block " + name
                            + " places; name the block too, as block+"
                            + name
                            + "="
                            + kind.id());
                    continue;
                }
            } else {
                if (installed(name)) {
                    problems.add("no door block or item named " + name);
                }
                continue;
            }
            // Variants are named after their block, so naming a door by its item keeps the same variant
            String blockName = Block.blockRegistry.getNameForObject(base);
            if (!bases.add(base)) {
                problems.add(
                    (name.equals(blockName) ? name : name + " (" + blockName + ")")
                        + " is listed twice and keeps its first kind");
                continue;
            }
            BaseItemDoor door = ownDoor(blockName, kind);
            if (door == null) {
                DoorVariantBlock block = new DoorVariantBlock(base, kind, properties);
                String id = DoorVariantList.variantName(blockName, kind);
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
        for (String problem : problems) {
            LOG.warn("Door Variants: {}", problem);
        }
        LOG.info("Registered {} door variants: {}", registered.size(), registered);
    }

    private static Block findBlock(String name) {
        int split = name.indexOf(':');
        return GameRegistry.findBlock(name.substring(0, split), name.substring(split + 1));
    }

    /** An item by registry name, with its damage when the name ends in one, like witchery:ingredient:152. */
    private static ItemStack findItem(String name) {
        int split = name.indexOf(':');
        int damage = 0;
        int last = name.lastIndexOf(':');
        if (last > split && name.substring(last + 1)
            .matches("\\d+")) {
            damage = Integer.parseInt(name.substring(last + 1));
            name = name.substring(0, last);
        }
        Item item = GameRegistry.findItem(name.substring(0, split), name.substring(split + 1));
        return item == null ? null : new ItemStack(item, 1, damage);
    }

    private static String knownBlock(String item) {
        for (Map.Entry<String, String> known : KNOWN_ITEMS.entrySet()) {
            if (known.getValue()
                .equals(item)) {
                return known.getKey();
            }
        }
        return null;
    }

    private static boolean installed(String name) {
        String mod = name.substring(0, name.indexOf(':'));
        return mod.equals("minecraft") || Loader.isModLoaded(mod);
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

    /**
     * The item that places a door block: what it drops, unless that's a plain vanilla door it isn't, as with Twilight
     * Forest's doors; then an item that holds the block, like most mods' door items do; then the block's own item.
     */
    private static ItemStack plainDoor(Block base) {
        Item item = base.getItemDropped(0, new Random(0), 0);
        boolean vanilla = item == Items.wooden_door && base != Blocks.wooden_door
            || item == Items.iron_door && base != Blocks.iron_door;
        if (item != null && !vanilla) {
            return new ItemStack(item, 1, base.damageDropped(0));
        }
        for (Object other : Item.itemRegistry) {
            if (!(other instanceof ItemBlock) && heldBlocks((Item) other).contains(base)) {
                return new ItemStack((Item) other);
            }
        }
        Item own = item == null ? Item.getItemFromBlock(base) : null;
        return own == null ? null : new ItemStack(own);
    }

    /** The door block an item places: the one door block it holds, or else the one door block that drops it. */
    private static Block placedBlock(Item item) {
        if (item instanceof ItemBlock) {
            return Block.getBlockFromItem(item);
        }
        Block found = null;
        for (Block block : heldBlocks(item)) {
            if (isDoor(block)) {
                if (found != null) {
                    return null;
                }
                found = block;
            }
        }
        if (found != null) {
            return found;
        }
        for (Object other : Block.blockRegistry) {
            Block block = (Block) other;
            if (isDoor(block) && block.getItemDropped(0, new Random(0), 0) == item) {
                if (found != null) {
                    return null;
                }
                found = block;
            }
        }
        return found;
    }

    private static boolean isDoor(Block block) {
        return block instanceof BlockDoor || block.getRenderType() == 7;
    }

    /** The blocks an item keeps in its fields. */
    private static Set<Block> heldBlocks(Item item) {
        Set<Block> blocks = new LinkedHashSet<Block>();
        for (Class<?> c = item.getClass(); c != Item.class && c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!Block.class.isAssignableFrom(field.getType()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                try {
                    field.setAccessible(true);
                    Object block = field.get(item);
                    if (block != null) {
                        blocks.add((Block) block);
                    }
                } catch (Exception e) {
                    // A field we can't read holds nothing we can use
                }
            }
        }
        return blocks;
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
