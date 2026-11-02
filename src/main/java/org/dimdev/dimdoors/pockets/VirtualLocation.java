package org.dimdev.dimdoors.pockets;

/**
 * Where in a real dimension a pocket stands: the x and z its origin stands for. Legacy built every pocket at its
 * entrance door's x and z (dungeons farther ahead with depth) in a dimension of its own, and its exits and sends to
 * Limbo kept a door's x and z; pockets in shared dimensions translate their coordinates through this instead.
 */
public final class VirtualLocation {

    private final int dimension;
    private final int x;
    private final int z;

    public VirtualLocation(int dimension, int x, int z) {
        this.dimension = dimension;
        this.x = x;
        this.z = z;
    }

    public int dimension() {
        return dimension;
    }

    public int x() {
        return x;
    }

    public int z() {
        return z;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof VirtualLocation)) return false;
        VirtualLocation location = (VirtualLocation) other;
        return dimension == location.dimension && x == location.x && z == location.z;
    }

    @Override
    public int hashCode() {
        return (dimension * 31 + x) * 31 + z;
    }

    @Override
    public String toString() {
        return x + ", " + z + " in dimension " + dimension;
    }
}
