package org.dimdev.dimdoors.pockets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.util.Point4D;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class PocketsFileTest {

    private static final int POCKETS = -40, DUNGEONS = -41, PERSONAL = -42, OVERWORLD = 0, NETHER = -1;

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private static Map<DimensionType, PocketRegistry> registries() {
        Map<DimensionType, PocketRegistry> registries = new EnumMap<>(DimensionType.class);
        registries.put(DimensionType.POCKET, new PocketRegistry(POCKETS, DimensionType.POCKET));
        registries.put(DimensionType.DUNGEON, new PocketRegistry(DUNGEONS, DimensionType.DUNGEON));
        registries.put(DimensionType.PERSONAL, new PocketRegistry(PERSONAL, DimensionType.PERSONAL));
        return registries;
    }

    @Test
    public void pocketsSurviveARoundTrip() throws IOException {
        Map<DimensionType, PocketRegistry> saved = registries();
        PocketRegistry pockets = saved.get(DimensionType.POCKET);
        PocketRegistry dungeons = saved.get(DimensionType.DUNGEON);
        Pocket top = pockets.newPocket(null, NETHER);
        top.setOrigin(new Point4D(256, 70, 256, POCKETS), 2);
        top.setVirtualLocation(new VirtualLocation(NETHER, -100, 40));
        top.setBuilt(true);
        Pocket deep = dungeons.newPocket(top, NETHER);
        deep.setPackDepth(3);
        Pocket unbuilt = dungeons.newPocket(deep, NETHER);
        Pocket home = saved.get(DimensionType.PERSONAL)
            .newPocket(null, OVERWORLD);
        home.setOwner("8667ba71-b85a-4004-af54-457a9734eed7");
        dungeons.remove(dungeons.newPocket(null, OVERWORLD)); // cell 2 taken, not free yet
        dungeons.newPocket(null, OVERWORLD); // cell 3
        dungeons.remove(dungeons.get(3));
        dungeons.freeCell(3);
        dungeons.markForReset(unbuilt);

        File file = folder.newFile("pockets.json");
        PocketsFile.write(saved, file);
        Map<DimensionType, PocketRegistry> loaded = registries();
        PocketsFile.read(loaded, file, name -> null);

        Pocket loadedTop = loaded.get(DimensionType.POCKET)
            .get(0);
        assertEquals(NETHER, loadedTop.rootDimension());
        assertEquals(new Point4D(256, 70, 256, POCKETS), loadedTop.origin());
        assertEquals(2, loadedTop.orientation());
        assertEquals(new VirtualLocation(NETHER, -100, 40), loadedTop.virtualLocation());
        assertTrue(loadedTop.isBuilt());
        assertNull(loadedTop.parent());

        PocketRegistry loadedDungeons = loaded.get(DimensionType.DUNGEON);
        Pocket loadedDeep = loadedDungeons.get(0);
        assertSame(loadedTop, loadedDeep.parent());
        assertEquals(2, loadedDeep.depth());
        assertEquals(3, loadedDeep.packDepth());
        assertSame(
            loadedDeep,
            loadedDungeons.get(1)
                .parent());
        assertEquals(
            3,
            loadedDungeons.get(1)
                .depth());
        assertFalse(
            loadedDungeons.get(1)
                .isBuilt());
        assertNull(
            loadedDungeons.get(1)
                .origin());
        assertTrue(
            loadedDungeons.resets()
                .contains(1));

        assertSame(
            loaded.get(DimensionType.PERSONAL)
                .get(0),
            loaded.get(DimensionType.PERSONAL)
                .ownedBy("8667ba71-b85a-4004-af54-457a9734eed7"));

        // Cell 2 stays taken until its region file is gone; cell 3 is free, then the spiral goes on at 4
        assertEquals(
            3,
            loadedDungeons.newPocket(null, OVERWORLD)
                .id());
        assertEquals(
            4,
            loadedDungeons.newPocket(null, OVERWORLD)
                .id());
        assertFalse(
            loaded.get(DimensionType.POCKET)
                .isModified());
    }

    @Test
    public void resetsDeleteTheirCellsAndFreeThem() throws IOException {
        PocketRegistry dungeons = new PocketRegistry(DUNGEONS, DimensionType.DUNGEON);
        Pocket first = dungeons.newPocket(null, OVERWORLD);
        Pocket second = dungeons.newPocket(first, OVERWORLD); // cell (1, 0)
        Pocket third = dungeons.newPocket(second, OVERWORLD);
        dungeons.markForReset(second);

        List<String> deleted = new ArrayList<>();
        List<Pocket> reset = dungeons
            .applyResets((type, cellX, cellZ) -> deleted.add(type + " " + PocketGrid.regionFileName(cellX, cellZ)));

        assertEquals(1, reset.size());
        assertSame(second, reset.get(0));
        assertEquals("[DUNGEON r.1.0.mca]", deleted.toString());
        assertNull(dungeons.get(1));
        assertNull(dungeons.pocketAt(600, 10));
        assertTrue(
            dungeons.resets()
                .isEmpty());
        // Its child hangs from its parent, and its cell is handed out again
        assertSame(first, third.parent());
        assertEquals(
            1,
            dungeons.newPocket(null, OVERWORLD)
                .id());
    }
}
