package org.dimdev.dimdoors.pockets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.util.Point4D;
import org.junit.Test;

public class PocketRegistryTest {

    private static final int POCKETS = -40, DUNGEONS = -41, OVERWORLD = 0;

    @Test
    public void pocketsTakeTheSpiralsCellsInOrder() {
        PocketRegistry registry = new PocketRegistry(POCKETS, DimensionType.POCKET);
        for (int i = 0; i < 10; i++) {
            Pocket pocket = registry.newPocket(null, OVERWORLD);
            assertEquals(i, pocket.id());
            assertEquals(PocketGrid.cellX(i), pocket.cellX());
            assertEquals(PocketGrid.cellZ(i), pocket.cellZ());
            assertEquals(POCKETS, pocket.dimension());
        }
    }

    @Test
    public void pocketAtFindsThePocketWhoseCellHoldsTheColumn() {
        PocketRegistry registry = new PocketRegistry(DUNGEONS, DimensionType.DUNGEON);
        Pocket first = registry.newPocket(null, OVERWORLD);
        Pocket second = registry.newPocket(null, OVERWORLD); // cell (1, 0)
        assertSame(first, registry.pocketAt(0, 0));
        assertSame(first, registry.pocketAt(511, 511));
        assertSame(second, registry.pocketAt(512, 0));
        assertSame(second, registry.pocketAt(1023, 511));
        assertTrue(second.contains(700, 300));
        // Cell (1, 1) has no pocket yet, and nothing is at negative coordinates
        assertNull(registry.pocketAt(600, 600));
        assertNull(registry.pocketAt(-1, 5));
    }

    @Test
    public void depthFollowsTheTree() {
        PocketRegistry pockets = new PocketRegistry(POCKETS, DimensionType.POCKET);
        PocketRegistry dungeons = new PocketRegistry(DUNGEONS, DimensionType.DUNGEON);
        Pocket top = pockets.newPocket(null, OVERWORLD);
        Pocket middle = dungeons.newPocket(top, OVERWORLD);
        Pocket bottom = dungeons.newPocket(middle, OVERWORLD);
        assertEquals(1, top.depth());
        assertEquals(2, middle.depth());
        assertEquals(3, bottom.depth());
        assertEquals(
            1,
            top.children()
                .size());

        // Taking out the middle hangs its child from the top one
        dungeons.remove(middle);
        assertSame(top, bottom.parent());
        assertEquals(2, bottom.depth());
        assertNull(dungeons.get(middle.id()));
        assertNull(middle.parent());
    }

    @Test
    public void freedCellsAreReusedLowestFirst() {
        PocketRegistry registry = new PocketRegistry(DUNGEONS, DimensionType.DUNGEON);
        Pocket[] made = new Pocket[5];
        for (int i = 0; i < made.length; i++) made[i] = registry.newPocket(null, OVERWORLD);
        registry.remove(made[3]);
        registry.remove(made[1]);
        // Until its region file is gone, a removed pocket's cell isn't handed out
        assertEquals(
            5,
            registry.newPocket(null, OVERWORLD)
                .id());
        registry.freeCell(3);
        registry.freeCell(1);
        assertEquals(
            1,
            registry.newPocket(null, OVERWORLD)
                .id());
        assertEquals(
            3,
            registry.newPocket(null, OVERWORLD)
                .id());
        assertEquals(
            6,
            registry.newPocket(null, OVERWORLD)
                .id());
        // A cell still in use, or never handed out, can't be freed
        registry.freeCell(2);
        registry.freeCell(50);
        assertEquals(
            7,
            registry.newPocket(null, OVERWORLD)
                .id());
    }

    @Test
    public void coordinatesTranslateThroughTheVirtualLocation() {
        PocketRegistry registry = new PocketRegistry(POCKETS, DimensionType.POCKET);
        registry.newPocket(null, OVERWORLD);
        Pocket pocket = registry.newPocket(null, OVERWORLD); // cell (1, 0): blocks 512 to 1023
        pocket.setVirtualLocation(new VirtualLocation(OVERWORLD, 2000, -300));
        // Before it's built, its cell's middle stands for the virtual location
        assertEquals(2000, pocket.toVirtualX(768));
        assertEquals(-300, pocket.toVirtualZ(256));
        pocket.setOrigin(new Point4D(760, 70, 250, POCKETS), 0);
        assertEquals(2000, pocket.toVirtualX(760));
        assertEquals(2010, pocket.toVirtualX(770));
        assertEquals(-305, pocket.toVirtualZ(245));
    }

    @Test(expected = IllegalArgumentException.class)
    public void realDimensionsHaveNoRegistry() {
        new PocketRegistry(OVERWORLD, DimensionType.ROOT);
    }
}
