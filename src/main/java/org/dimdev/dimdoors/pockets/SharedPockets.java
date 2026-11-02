package org.dimdev.dimdoors.pockets;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.dungeon.DungeonData;
import org.dimdev.dimdoors.util.Point4D;

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

    /** The three pocket types that have a shared dimension. */
    public static final DimensionType[] TYPES = { DimensionType.POCKET, DimensionType.DUNGEON, DimensionType.PERSONAL };

    /** Starts the registries for a world (server side), from its pockets file if it has one. */
    public static void load(File file, Function<String, DungeonData> dungeons) throws IOException {
        registries = new EnumMap<>(DimensionType.class);
        for (DimensionType type : TYPES) registries.put(type, new PocketRegistry(dimensionOf(type), type));
        if (file != null && file.exists()) PocketsFile.read(registries, file, dungeons);
    }

    /** Writes the pockets file, if anything changed or checkModified is false. */
    public static void save(File file, boolean checkModified) throws IOException {
        if (registries == null) return;
        boolean modified = !checkModified;
        for (PocketRegistry registry : registries.values()) modified |= registry.isModified();
        if (!modified) return;
        PocketsFile.write(registries, file);
        for (PocketRegistry registry : registries.values()) registry.clearModified();
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

    public static Pocket pocketAt(Point4D point) {
        return pocketAt(point.getDimension(), point.getX(), point.getZ());
    }

    /** The personal pocket a player owns, or null. */
    public static Pocket personalPocketOf(String owner) {
        return registries == null ? null : registry(DimensionType.PERSONAL).ownedBy(owner);
    }

    /**
     * Where a point stands in a real dimension: through the virtual location of the pocket it is in, or the point
     * itself outside the shared dimensions. Points in a shared dimension outside any pocket stand at the overworld's
     * origin.
     */
    public static Point4D toVirtual(Point4D point) {
        if (!isShared(point.getDimension())) return point;
        Pocket pocket = pocketAt(point);
        if (pocket == null || pocket.virtualLocation() == null) return new Point4D(0, point.getY(), 0, 0);
        return new Point4D(
            pocket.toVirtualX(point.getX()),
            point.getY(),
            pocket.toVirtualZ(point.getZ()),
            pocket.virtualLocation()
                .dimension());
    }

    /** The real dimension the pocket at a point hangs from, or the point's own dimension outside the shared ones. */
    public static int rootDimensionOf(Point4D point) {
        if (!isShared(point.getDimension())) return point.getDimension();
        Pocket pocket = pocketAt(point);
        return pocket != null ? pocket.rootDimension() : 0;
    }

    /** Resets the pockets marked for it in every registry (see PocketRegistry.applyResets). */
    public static List<Pocket> applyResets(PocketRegistry.CellDeleter deleteCell) throws IOException {
        List<Pocket> reset = new ArrayList<>();
        for (PocketRegistry registry : registries.values()) reset.addAll(registry.applyResets(deleteCell));
        return reset;
    }
}
