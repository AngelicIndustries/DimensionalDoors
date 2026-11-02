package org.dimdev.dimdoors.pockets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.dimdev.dimdoors.core.DimensionType;
import org.dimdev.dimdoors.dungeon.DungeonData;
import org.dimdev.dimdoors.dungeon.pack.DungeonPack;
import org.dimdev.dimdoors.util.Point4D;

/**
 * A pocket: one cell of a shared pocket dimension, with its place in the pocket tree. Legacy kept this per pocket
 * dimension (NewDimData); the pocket's links stay with its dimension's data.
 */
public final class Pocket {

    private final PocketRegistry registry;
    private final int id;
    private final int dimension;
    private final DimensionType type;
    private final int rootDimension;
    private final List<Pocket> children = new ArrayList<>();
    private Pocket parent;
    private int depth;
    private int packDepth;
    private Point4D origin;
    private int orientation;
    private DungeonData dungeon;
    private boolean built;
    private String owner;
    private VirtualLocation virtualLocation;

    /** Made by PocketRegistry, which hands out the IDs (cell numbers). */
    Pocket(PocketRegistry registry, int id, Pocket parent, int rootDimension) {
        this.registry = registry;
        this.id = id;
        this.dimension = registry.dimension();
        this.type = registry.type();
        this.rootDimension = rootDimension;
        setParent(parent);
    }

    /** The cell number in its dimension. */
    public int id() {
        return id;
    }

    /** The shared pocket dimension it is in. */
    public int dimension() {
        return dimension;
    }

    public DimensionType type() {
        return type;
    }

    /** The real dimension this pocket's tree hangs from. */
    public int rootDimension() {
        return rootDimension;
    }

    /** The pocket it was entered from, or null for one entered from a real dimension. */
    public Pocket parent() {
        return parent;
    }

    public List<Pocket> children() {
        return Collections.unmodifiableList(children);
    }

    /** 1 for a pocket entered from a real dimension, one more for each pocket inside it. */
    public int depth() {
        return depth;
    }

    /** Hangs this pocket and its subtree from another pocket, or from its real dimension (null). */
    public void setParent(Pocket parent) {
        if (this.parent != null) this.parent.children.remove(this);
        this.parent = parent;
        if (parent != null) parent.children.add(this);
        updateDepth();
        registry.markModified();
    }

    private void updateDepth() {
        depth = parent == null ? 1 : parent.depth + 1;
        // Legacy kept a dungeon's place in its pack's chain in step with its parent
        if (dungeon != null) packDepth = packDepthOf(parent, dungeon);
        for (Pocket child : children) child.updateDepth();
    }

    public int packDepth() {
        return packDepth;
    }

    public void setPackDepth(int packDepth) {
        this.packDepth = packDepth;
        registry.markModified();
    }

    public int cellX() {
        return PocketGrid.cellX(id);
    }

    public int cellZ() {
        return PocketGrid.cellZ(id);
    }

    /** Whether a block column lies in this pocket's cell. */
    public boolean contains(int x, int z) {
        return PocketGrid.cellOfBlock(x) == cellX() && PocketGrid.cellOfBlock(z) == cellZ();
    }

    /** Where it was entered, once built. */
    public Point4D origin() {
        return origin;
    }

    public int orientation() {
        return orientation;
    }

    public void setOrigin(Point4D origin, int orientation) {
        this.origin = origin;
        this.orientation = orientation;
        registry.markModified();
    }

    public DungeonData dungeon() {
        return dungeon;
    }

    /**
     * How deep a dungeon would be in its pack's chain under a parent pocket (null for a real dimension): one more than
     * the parent's if both are dungeons of the same pack, otherwise 1.
     */
    public static int packDepthOf(Pocket parent, DungeonData dungeon) {
        if (dungeon == null) throw new IllegalArgumentException("dungeon cannot be null.");
        DungeonData predecessor = parent != null ? parent.dungeon : null;
        if (predecessor == null) return 1;
        DungeonPack owner = dungeon.dungeonType().Owner;
        if (owner == null || predecessor.dungeonType().Owner != owner) return 1;
        return parent.packDepth + 1;
    }

    public void setDungeon(DungeonData dungeon) {
        this.dungeon = dungeon;
        registry.markModified();
    }

    public boolean isBuilt() {
        return built;
    }

    public void setBuilt(boolean built) {
        this.built = built;
        registry.markModified();
    }

    /** The player a personal pocket belongs to, or null. */
    public String owner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
        registry.markModified();
    }

    public VirtualLocation virtualLocation() {
        return virtualLocation;
    }

    public void setVirtualLocation(VirtualLocation virtualLocation) {
        this.virtualLocation = virtualLocation;
        registry.markModified();
    }

    /**
     * The real x a block x in this pocket stands for, through its virtual location (its origin, or its cell's middle).
     */
    public int toVirtualX(int x) {
        return virtualLocation.x() + x - (origin != null ? origin.getX() : PocketGrid.cellCenter(cellX()));
    }

    /** The real z a block z in this pocket stands for, as toVirtualX. */
    public int toVirtualZ(int z) {
        return virtualLocation.z() + z - (origin != null ? origin.getZ() : PocketGrid.cellCenter(cellZ()));
    }

    @Override
    public String toString() {
        return type + " pocket " + id + " in dimension " + dimension;
    }
}
