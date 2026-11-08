package org.dimdev.dimdoors.api;

import org.dimdev.dimdoors.DimDoors;

/** Dimensional Doors client effects for other mods. Calls on a server do nothing. */
public final class DimDoorsClient {

    private DimDoorsClient() {}

    /** Shows the cores of nearby rifts for a while, as the Rift Remover and Rift Stabilizer do on a miss. */
    public static void highlightRiftCores() {
        DimDoors.proxy.highlightRiftCores();
    }
}
