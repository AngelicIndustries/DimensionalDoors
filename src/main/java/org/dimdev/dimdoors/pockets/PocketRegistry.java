package org.dimdev.dimdoors.pockets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final TreeSet<Integer> resets = new TreeSet<>();
    private int nextId;
    private boolean modified;

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
        Pocket pocket = new Pocket(this, id, parent, rootDimension);
        pockets.put(id, pocket);
        modified = true;
        return pocket;
    }

    /** A saved pocket, hanging from nothing until its parent is restored too. */
    Pocket restore(int id, int rootDimension) {
        if (id < 0 || pockets.containsKey(id)) throw new IllegalArgumentException("Cell " + id + " is taken twice");
        Pocket pocket = new Pocket(this, id, null, rootDimension);
        pockets.put(id, pocket);
        nextId = Math.max(nextId, id + 1);
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

    /** The personal pocket a player owns, or null. */
    public Pocket ownedBy(String owner) {
        for (Pocket pocket : pockets.values()) if (owner.equals(pocket.owner())) return pocket;
        return null;
    }

    /** Marks a pocket to be reset at the next server start, when its dimension isn't loaded. */
    public void markForReset(Pocket pocket) {
        if (pockets.get(pocket.id()) == pocket && resets.add(pocket.id())) modified = true;
    }

    /** The cells marked for reset. */
    public Set<Integer> resets() {
        return Collections.unmodifiableSet(resets);
    }

    /**
     * Takes out the pockets marked for reset and lets their cells be used again, calling deleteCell on each first (to
     * delete its region file). Run at server start, before the dimension loads.
     */
    public List<Pocket> applyResets(CellDeleter deleteCell) throws IOException {
        List<Pocket> reset = new ArrayList<>();
        for (int id : new ArrayList<>(resets)) {
            Pocket pocket = pockets.get(id);
            deleteCell.delete(type, PocketGrid.cellX(id), PocketGrid.cellZ(id));
            remove(pocket);
            freeCell(id);
            reset.add(pocket);
        }
        return reset;
    }

    public interface CellDeleter {

        void delete(DimensionType type, int cellX, int cellZ) throws IOException;
    }

    /**
     * Takes a pocket out (reset dungeons). Its children hang from its parent instead; its cell stays taken until
     * freeCell says its region file is gone.
     */
    public void remove(Pocket pocket) {
        if (pockets.get(pocket.id()) != pocket) return;
        pockets.remove(pocket.id());
        resets.remove(pocket.id());
        for (Pocket child : pocket.children()
            .toArray(new Pocket[0])) child.setParent(pocket.parent());
        pocket.setParent(null);
        modified = true;
    }

    /** Lets a removed pocket's cell be used again, once nothing of it is left on disk. */
    public void freeCell(int id) {
        if (id >= 0 && id < nextId && !pockets.containsKey(id) && freeCells.add(id)) modified = true;
    }

    /** The next cell of the spiral no pocket has had yet. */
    int nextId() {
        return nextId;
    }

    void setNextId(int nextId) {
        this.nextId = Math.max(this.nextId, nextId);
    }

    Set<Integer> freeCells() {
        return Collections.unmodifiableSet(freeCells);
    }

    void restoreReset(int id) {
        if (pockets.containsKey(id)) resets.add(id);
    }

    void markModified() {
        modified = true;
    }

    /** Whether anything changed since the last save. */
    public boolean isModified() {
        return modified;
    }

    public void clearModified() {
        modified = false;
    }
}
