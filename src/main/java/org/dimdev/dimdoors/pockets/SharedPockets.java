package org.dimdev.dimdoors.pockets;

import java.util.EnumMap;
import java.util.Map;

import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DimensionType;

/**
 * The three shared pocket dimensions (public pockets, dungeons and personal pockets) and, while a world is loaded on
 * the server, their registries.
 */
public final class SharedPockets {

    private static Map<DimensionType, PocketRegistry> registries;

    private SharedPockets() {}

    /** The pocket type a dimension holds, or null if it isn't a shared pocket dimension. */
    public static DimensionType typeOf(int dimension) {
        DDProperties properties = DDProperties.instance();
        if (dimension == properties.PublicPocketDimensionID) return DimensionType.POCKET;
        if (dimension == properties.DungeonPocketDimensionID) return DimensionType.DUNGEON;
        if (dimension == properties.PersonalPocketDimensionID) return DimensionType.PERSONAL;
        return null;
    }

    public static boolean isShared(int dimension) {
        return typeOf(dimension) != null;
    }

    /** The shared dimension that holds pockets of a type. */
    public static int dimensionOf(DimensionType type) {
        DDProperties properties = DDProperties.instance();
        switch (type) {
            case POCKET:
                return properties.PublicPocketDimensionID;
            case DUNGEON:
                return properties.DungeonPocketDimensionID;
            case PERSONAL:
                return properties.PersonalPocketDimensionID;
            default:
                throw new IllegalArgumentException(type + " pockets have no shared dimension");
        }
    }

    /** Its folder in the save, under DimensionalDoors. */
    public static String folderOf(DimensionType type) {
        switch (type) {
            case POCKET:
                return "pockets";
            case DUNGEON:
                return "dungeons";
            case PERSONAL:
                return "personal";
            default:
                throw new IllegalArgumentException(type + " pockets have no shared dimension");
        }
    }

    /** Its name, as the game shows it (and shader packs see it). */
    public static String nameOf(DimensionType type) {
        switch (type) {
            case POCKET:
                return "Pockets";
            case DUNGEON:
                return "Dungeon Pockets";
            case PERSONAL:
                return "Personal Pockets";
            default:
                throw new IllegalArgumentException(type + " pockets have no shared dimension");
        }
    }

    /** Starts the registries for a world (server side). */
    public static void load() {
        registries = new EnumMap<>(DimensionType.class);
        for (DimensionType type : new DimensionType[] { DimensionType.POCKET, DimensionType.DUNGEON,
            DimensionType.PERSONAL }) registries.put(type, new PocketRegistry(dimensionOf(type), type));
    }

    public static void unload() {
        registries = null;
    }

    public static boolean isLoaded() {
        return registries != null;
    }

    /** The registry of a pocket type, while a world is loaded on the server. */
    public static PocketRegistry registry(DimensionType type) {
        if (registries == null) throw new IllegalStateException("No world is loaded");
        PocketRegistry registry = registries.get(type);
        if (registry == null) throw new IllegalArgumentException(type + " pockets have no shared dimension");
        return registry;
    }

    /** The pocket at a block column of a dimension, or null (also outside the shared dimensions). */
    public static Pocket pocketAt(int dimension, int x, int z) {
        DimensionType type = typeOf(dimension);
        if (type == null || registries == null) return null;
        return registry(type).pocketAt(x, z);
    }
}
