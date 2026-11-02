package org.dimdev.dimdoors.pockets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class PocketGridTest {

    @Test
    public void spiralStartsAtTheOriginAndGrowsInSquares() {
        // Layer 1: (1,0) (1,1) (0,1); layer 2 starts at (2,0)
        int[][] expected = { { 0, 0 }, { 1, 0 }, { 1, 1 }, { 0, 1 }, { 2, 0 }, { 2, 1 }, { 2, 2 }, { 1, 2 }, { 0, 2 } };
        for (int i = 0; i < expected.length; i++) {
            assertEquals("x of cell " + i, expected[i][0], PocketGrid.cellX(i));
            assertEquals("z of cell " + i, expected[i][1], PocketGrid.cellZ(i));
        }
    }

    @Test
    public void indexIsTheInverseAndCellsAreUnique() {
        Set<Long> seen = new HashSet<>();
        for (int i = 0; i < 100_000; i++) {
            int x = PocketGrid.cellX(i), z = PocketGrid.cellZ(i);
            assertTrue("cell " + i + " is negative", x >= 0 && z >= 0);
            assertEquals(i, PocketGrid.index(x, z));
            assertTrue("cell " + i + " repeats", seen.add((long) x << 32 | z));
        }
    }

    @Test
    public void layersStayExactForLargeNumbers() {
        for (int layer : new int[] { 46_340, 30_000, 10_007 }) {
            int first = layer * layer;
            assertEquals(layer, PocketGrid.cellX(first));
            assertEquals(0, PocketGrid.cellZ(first));
            // The layer before ends at x = 0, as layer 1 ends at (0, 1)
            assertEquals(0, PocketGrid.cellX(first - 1));
            assertEquals(layer - 1, PocketGrid.cellZ(first - 1));
        }
    }

    @Test
    public void cellsAreRegionFiles() {
        assertEquals(512, PocketGrid.CELL_BLOCKS);
        assertEquals(0, PocketGrid.cellOfBlock(0));
        assertEquals(0, PocketGrid.cellOfBlock(511));
        assertEquals(1, PocketGrid.cellOfBlock(512));
        assertEquals(-1, PocketGrid.cellOfBlock(-1));
        assertEquals(1024, PocketGrid.cellOrigin(2));
        assertEquals(1280, PocketGrid.cellCenter(2));
        // A region file holds 32 x 32 chunks, as a cell: r.X.Z covers blocks X*512 to X*512+511
        assertEquals("r.3.1.mca", PocketGrid.regionFileName(3, 1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void negativeCellsHaveNoIndex() {
        PocketGrid.index(-1, 0);
    }
}
