package org.dimdev.dimdoors.pockets;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.dungeon.DungeonData;
import org.dimdev.dimdoors.util.Point4D;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

/** The pockets of the shared dimensions, saved as one JSON file next to the per-dimension link data. */
public final class PocketsFile {

    private static final int VERSION = 1;

    private PocketsFile() {}

    public static void write(Map<DimensionType, PocketRegistry> registries, File file) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("version", VERSION);
        JsonObject all = new JsonObject();
        for (PocketRegistry registry : registries.values()) {
            JsonObject saved = new JsonObject();
            saved.addProperty("next", registry.nextId());
            saved.add("free", numbers(registry.freeCells()));
            saved.add("reset", numbers(registry.resets()));
            JsonArray pockets = new JsonArray();
            for (Pocket pocket : registry.pockets()) pockets.add(write(pocket));
            saved.add("pockets", pockets);
            all.add(
                registry.type()
                    .name(),
                saved);
        }
        root.add("registries", all);

        File temp = new File(file.getPath() + ".tmp");
        try (Writer writer = Files.newBufferedWriter(temp.toPath(), StandardCharsets.UTF_8)) {
            new GsonBuilder().setPrettyPrinting()
                .create()
                .toJson(root, writer);
        }
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    private static JsonObject write(Pocket pocket) {
        JsonObject saved = new JsonObject();
        saved.addProperty("id", pocket.id());
        saved.addProperty("root", pocket.rootDimension());
        if (pocket.parent() != null) {
            JsonObject parent = new JsonObject();
            parent.addProperty(
                "type",
                pocket.parent()
                    .type()
                    .name());
            parent.addProperty(
                "id",
                pocket.parent()
                    .id());
            saved.add("parent", parent);
        }
        saved.addProperty("packDepth", pocket.packDepth());
        if (pocket.origin() != null) {
            JsonArray origin = new JsonArray();
            origin.add(
                number(
                    pocket.origin()
                        .getX()));
            origin.add(
                number(
                    pocket.origin()
                        .getY()));
            origin.add(
                number(
                    pocket.origin()
                        .getZ()));
            saved.add("origin", origin);
            saved.addProperty("orientation", pocket.orientation());
        }
        if (pocket.dungeon() != null) saved.addProperty(
            "dungeon",
            pocket.dungeon()
                .schematicName());
        saved.addProperty("built", pocket.isBuilt());
        if (pocket.owner() != null) saved.addProperty("owner", pocket.owner());
        VirtualLocation virtual = pocket.virtualLocation();
        if (virtual != null) {
            JsonArray location = new JsonArray();
            location.add(number(virtual.dimension()));
            location.add(number(virtual.x()));
            location.add(number(virtual.z()));
            saved.add("virtual", location);
        }
        return saved;
    }

    /**
     * Fills empty registries from a file. Dungeons are looked up by schematic name; one that is gone leaves its pocket
     * without a dungeon.
     */
    public static void read(Map<DimensionType, PocketRegistry> registries, File file,
        Function<String, DungeonData> dungeons) throws IOException {
        JsonObject root;
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            root = new JsonParser().parse(reader)
                .getAsJsonObject();
        }
        int version = root.get("version")
            .getAsInt();
        if (version > VERSION) throw new IOException(file + " was written by a newer version (" + version + ")");
        JsonObject all = root.getAsJsonObject("registries");

        // Pockets first, then the tree, since parents can be in other registries
        Map<Pocket, JsonObject> parents = new HashMap<>();
        for (Map.Entry<String, JsonElement> entry : all.entrySet()) {
            PocketRegistry registry = registries.get(DimensionType.valueOf(entry.getKey()));
            if (registry == null) continue;
            JsonObject saved = entry.getValue()
                .getAsJsonObject();
            for (JsonElement element : saved.getAsJsonArray("pockets")) {
                JsonObject data = element.getAsJsonObject();
                Pocket pocket = registry.restore(
                    data.get("id")
                        .getAsInt(),
                    data.get("root")
                        .getAsInt());
                read(pocket, data, dungeons);
                if (data.has("parent")) parents.put(pocket, data.getAsJsonObject("parent"));
            }
            registry.setNextId(
                saved.get("next")
                    .getAsInt());
            for (JsonElement free : saved.getAsJsonArray("free")) registry.freeCell(free.getAsInt());
            for (JsonElement reset : saved.getAsJsonArray("reset")) registry.restoreReset(reset.getAsInt());
        }
        for (Map.Entry<Pocket, JsonObject> entry : parents.entrySet()) {
            JsonObject parent = entry.getValue();
            PocketRegistry registry = registries.get(
                DimensionType.valueOf(
                    parent.get("type")
                        .getAsString()));
            Pocket found = registry != null ? registry.get(
                parent.get("id")
                    .getAsInt())
                : null;
            // A pocket whose parent is gone hangs from its real dimension, as Legacy's orphans did
            if (found != null) entry.getKey()
                .setParent(found);
        }
        for (PocketRegistry registry : registries.values()) registry.clearModified();
    }

    private static void read(Pocket pocket, JsonObject data, Function<String, DungeonData> dungeons) {
        if (data.has("origin")) {
            JsonArray origin = data.getAsJsonArray("origin");
            pocket.setOrigin(
                new Point4D(
                    origin.get(0)
                        .getAsInt(),
                    origin.get(1)
                        .getAsInt(),
                    origin.get(2)
                        .getAsInt(),
                    pocket.dimension()),
                data.get("orientation")
                    .getAsInt());
        }
        if (data.has("dungeon")) pocket.setDungeon(
            dungeons.apply(
                data.get("dungeon")
                    .getAsString()));
        pocket.setBuilt(
            data.get("built")
                .getAsBoolean());
        if (data.has("owner")) pocket.setOwner(
            data.get("owner")
                .getAsString());
        if (data.has("virtual")) {
            JsonArray virtual = data.getAsJsonArray("virtual");
            pocket.setVirtualLocation(
                new VirtualLocation(
                    virtual.get(0)
                        .getAsInt(),
                    virtual.get(1)
                        .getAsInt(),
                    virtual.get(2)
                        .getAsInt()));
        }
        pocket.setPackDepth(
            data.get("packDepth")
                .getAsInt());
    }

    private static JsonArray numbers(Iterable<Integer> values) {
        JsonArray array = new JsonArray();
        for (int value : values) array.add(number(value));
        return array;
    }

    private static JsonElement number(int value) {
        return new JsonPrimitive(value);
    }
}
