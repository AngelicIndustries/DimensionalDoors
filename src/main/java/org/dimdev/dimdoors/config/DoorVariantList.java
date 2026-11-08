package org.dimdev.dimdoors.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The door variant list from the config. Each entry maps a door's block registry name to a kind, like
 * "etfuturum:door_spruce=warp". A door takes the first kind it is given.
 */
public final class DoorVariantList {

    public static final String[] DEFAULTS = { "minecraft:iron_door=pocket", "minecraft:wooden_door=warp",
        "dimdoors:Quartz Door=personal", "dimdoors:Golden Door=golden",

        "etfuturum:door_spruce=warp", "etfuturum:door_birch=warp", "etfuturum:door_jungle=warp",
        "etfuturum:door_acacia=warp", "etfuturum:door_dark_oak=warp", "etfuturum:mangrove_door=warp",
        "etfuturum:cherry_door=warp", "etfuturum:bamboo_door=warp", "etfuturum:crimson_door=nether_dungeon",
        "etfuturum:warped_door=nether_dungeon", "etfuturum:copper_door=dungeon",
        "etfuturum:exposed_copper_door=dungeon", "etfuturum:weathered_copper_door=dungeon",
        "etfuturum:oxidized_copper_door=dungeon", "etfuturum:waxed_copper_door=dungeon",
        "etfuturum:waxed_exposed_copper_door=dungeon", "etfuturum:waxed_weathered_copper_door=dungeon",
        "etfuturum:waxed_oxidized_copper_door=dungeon",

        "etfuturum:bop_bamboo_door=warp", "etfuturum:bop_cherry_door=warp", "etfuturum:bop_dark_door=warp",
        "etfuturum:bop_ethereal_door=warp", "etfuturum:bop_fir_door=warp", "etfuturum:bop_hellbark_door=nether_dungeon",
        "etfuturum:bop_jacaranda_door=warp", "etfuturum:bop_magic_door=warp", "etfuturum:bop_mahogany_door=warp",
        "etfuturum:bop_mangrove_door=warp", "etfuturum:bop_palm_door=warp", "etfuturum:bop_pine_door=warp",
        "etfuturum:bop_redwood_door=warp", "etfuturum:bop_sacredoak_door=warp", "etfuturum:bop_willow_door=warp",

        "Natura:door.bloodwood=nether_dungeon", "Natura:door.ghostwood=nether_dungeon", "Natura:door.eucalyptus=warp",
        "Natura:door.hopseed=warp", "Natura:door.redwood=warp", "Natura:door.redwoodbark=warp",
        "Natura:door.sakura=warp",

        "TwilightForest:tile.TFDoorTwilight=warp", "TwilightForest:tile.TFDoorCanopy=warp",
        "TwilightForest:tile.TFDoorMangrove=warp", "TwilightForest:tile.TFDoorDarkwood=warp",
        "TwilightForest:tile.TFDoorTime=warp", "TwilightForest:tile.TFDoorTrans=warp",
        "TwilightForest:tile.TFDoorMine=warp", "TwilightForest:tile.TFDoorSort=warp",

        "netherlicious:doorfoxfire=nether_dungeon", "aether_legacy:skyroot_door=warp",
        "Growthcraft|Bamboo:grc.bambooDoor=warp",

        "plantmegapack:bambooAsperDoor=warp", "plantmegapack:bambooFargesiaRobustaDoor=warp",
        "plantmegapack:bambooGiantTimberDoor=warp", "plantmegapack:bambooGoldenDoor=warp",
        "plantmegapack:bambooMosoDoor=warp", "plantmegapack:bambooShortTassledDoor=warp",
        "plantmegapack:bambooTimorBlackDoor=warp", "plantmegapack:bambooTropicalBlueDoor=warp",
        "plantmegapack:bambooWetForestDoor=warp",

        "biomic:baobab_door=warp", "biomic:blackwood_door=warp", "biomic:blue_bioshroom_door=warp",
        "biomic:brimwood_door=nether_dungeon", "biomic:cobalt_door=nether_dungeon", "biomic:cypress_door=warp",
        "biomic:dead_door=warp", "biomic:eucalyptus_door=warp", "biomic:green_bioshroom_door=warp",
        "biomic:joshua_door=warp", "biomic:kapok_door=warp", "biomic:larch_door=warp", "biomic:magnolia_door=warp",
        "biomic:maple_door=warp", "biomic:palm_door=warp", "biomic:pine_door=warp", "biomic:pink_bioshroom_door=warp",
        "biomic:redwood_door=warp", "biomic:socotra_door=warp", "biomic:willow_door=warp", "biomic:wisteria_door=warp",
        "biomic:yellow_bioshroom_door=nether_dungeon" };

    private DoorVariantList() {}

    /** Door registry names to kinds, in list order. Bad entries are skipped and described in problems. */
    public static Map<String, DoorKind> parse(String[] entries, List<String> problems) {
        Map<String, DoorKind> doors = new LinkedHashMap<String, DoorKind>();
        for (String entry : entries) {
            if (entry == null || entry.trim()
                .isEmpty()) {
                continue;
            }
            int split = entry.lastIndexOf('=');
            String door = split < 0 ? ""
                : entry.substring(0, split)
                    .trim();
            DoorKind kind = split < 0 ? null : DoorKind.fromId(entry.substring(split + 1));
            if (door.indexOf(':') <= 0 || door.endsWith(":")) {
                problems.add("\"" + entry + "\" needs a door's registry name, like etfuturum:door_spruce=warp");
            } else if (kind == null) {
                problems.add("\"" + entry + "\" has an unknown kind; the kinds are " + kindIds());
            } else if (doors.containsKey(door)) {
                problems.add(
                    "\"" + entry
                        + "\" repeats "
                        + door
                        + ", which keeps "
                        + doors.get(door)
                            .id());
            } else {
                doors.put(door, kind);
            }
        }
        return doors;
    }

    /** The registry name of a door's variant, like "warp_door.etfuturum.door_spruce". */
    public static String variantName(String door, DoorKind kind) {
        return kind.id() + "_door."
            + door.replace(':', '.')
                .replaceAll("[^A-Za-z0-9_.]", "_");
    }

    private static String kindIds() {
        List<String> ids = new ArrayList<String>();
        for (DoorKind kind : DoorKind.values()) {
            ids.add(kind.id());
        }
        return String.join(", ", ids);
    }
}
