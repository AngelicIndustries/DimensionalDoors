package org.dimdev.dimdoors.pockets;

/**
 * Where pockets lie in a shared pocket dimension: each in a cell of 32 chunks a side, one region file, handed out in a
 * square spiral from the origin as in Dimensional Doors 1.12's PocketLib. Cell 0 is at (0, 0); each next square layer
 * adds a column at x = layer (bottom to top) and then a row at z = layer (right to left), so cells never go negative.
 */
public final class PocketGrid {

    /** A cell is a region file: 32 chunks of 16 blocks. */
    public static final int CELL_CHUNKS = 32;
    public static final int CELL_BLOCKS = CELL_CHUNKS * 16;

    private PocketGrid() {}

    /** The cell's x of the index-th cell. */
    public static int cellX(int index) {
        int layer = layer(index);
        int step = index - layer * layer;
        return step <= layer ? layer : layer - (step - layer);
    }

    /** The cell's z of the index-th cell. */
    public static int cellZ(int index) {
        int layer = layer(index);
        return Math.min(index - layer * layer, layer);
    }

    /** The index of the cell at (cellX, cellZ), which must not be negative. */
    public static int index(int cellX, int cellZ) {
        if (cellX < 0 || cellZ < 0) throw new IllegalArgumentException("No cell at " + cellX + ", " + cellZ);
        long index = cellX >= cellZ ? (long) cellX * cellX + cellZ : (long) (cellZ + 1) * cellZ + cellZ - cellX;
        if (index > Integer.MAX_VALUE)
            throw new IllegalArgumentException("Cell " + cellX + ", " + cellZ + " is too far out");
        return (int) index;
    }

    /** The cell a block coordinate lies in (negative outside the grid). */
    public static int cellOfBlock(int block) {
        return Math.floorDiv(block, CELL_BLOCKS);
    }

    /** The lowest block coordinate of a cell. */
    public static int cellOrigin(int cell) {
        return cell * CELL_BLOCKS;
    }

    /** The block coordinate at the middle of a cell. */
    public static int cellCenter(int cell) {
        return cell * CELL_BLOCKS + CELL_BLOCKS / 2;
    }

    /** The region file that holds a cell, in its dimension's region folder. */
    public static String regionFileName(int cellX, int cellZ) {
        return "r." + cellX + "." + cellZ + ".mca";
    }

    /** The square layer an index is on: the largest whole number whose square is at most the index. */
    private static int layer(int index) {
        if (index < 0) throw new IllegalArgumentException("No cell number " + index);
        long layer = (long) Math.sqrt(index);
        // Square roots of large numbers can be off by one either way; longs keep the squares from overflowing
        while (layer * layer > index) layer--;
        while ((layer + 1) * (layer + 1) <= index) layer++;
        return (int) layer;
    }
}
