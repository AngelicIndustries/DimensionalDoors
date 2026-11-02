package org.dimdev.dimdoors.pockets;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import org.dimdev.dimdoors.core.DimensionType;

/**
 * The pockets of one shared pocket dimension, by cell. New pockets take the cells reset dungeons left once their
 * region files are gone, the lowest first, and otherwise the next cell of the spiral.
 */
public final class PocketRegistry {

    private final int dimension;
    private final DimensionType type;
    private final Map<Integer, Pocket> pockets = new HashMap<>();
    private final TreeSet<Integer> freeCells = new TreeSet<>();
    private int nextId;

    public PocketRegistry(int dimension, DimensionType type) {
        if (!type.isPocketDimension()) throw new IllegalArgumentException(type + " isn't a pocket type");
        this.dimension = dimension;
        this.type = type;
    }

    public int dimension() {
        return dimension;
    }

    public DimensionType type() {
        return type;
    }

    /** A new pocket in the next free cell, hanging from a parent pocket or (null) from a real dimension. */
    public Pocket newPocket(Pocket parent, int rootDimension) {
        Integer free = freeCells.pollFirst();
        int id = free != null ? free : nextId++;
        Pocket pocket = new Pocket(id, dimension, type, parent, rootDimension);
        pockets.put(id, pocket);
        return pocket;
    }

    public Pocket get(int id) {
        return pockets.get(id);
    }

    /** The pocket whose cell holds this block column, or null. */
    public Pocket pocketAt(int x, int z) {
        int cellX = PocketGrid.cellOfBlock(x);
        int cellZ = PocketGrid.cellOfBlock(z);
        if (cellX < 0 || cellZ < 0) return null;
        return pockets.get(PocketGrid.index(cellX, cellZ));
    }

    public Collection<Pocket> pockets() {
        return Collections.unmodifiableCollection(pockets.values());
    }

    /**
     * Takes a pocket out (reset dungeons). Its children hang from its parent instead; its cell stays taken until
     * freeCell says its region file is gone.
     */
    public void remove(Pocket pocket) {
        if (pockets.get(pocket.id()) != pocket) return;
        pockets.remove(pocket.id());
        for (Pocket child : pocket.children()
            .toArray(new Pocket[0])) child.setParent(pocket.parent());
        pocket.setParent(null);
    }

    /** Lets a removed pocket's cell be used again, once nothing of it is left on disk. */
    public void freeCell(int id) {
        if (id >= 0 && id < nextId && !pockets.containsKey(id)) freeCells.add(id);
    }
}
