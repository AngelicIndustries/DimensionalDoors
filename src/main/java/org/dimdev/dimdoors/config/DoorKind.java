package org.dimdev.dimdoors.config;

import java.util.Locale;

/** What a dimensional variant of a door does. */
public enum DoorKind {

    /** Opens a new pocket, like the Dimensional Door. */
    POCKET,
    /** Leads out of a pocket, like the Warp Door. */
    WARP,
    /** Opens a new dungeon. */
    DUNGEON,
    /** Opens a new dungeon from the Nether pack. */
    NETHER_DUNGEON,
    /** Leads to the player's own pocket, like the Personal Dimensional Door. */
    PERSONAL,
    /** Opens a pocket and keeps it loaded, like the Golden Dimensional Door. */
    GOLDEN;

    /** The name used in the config, like "nether_dungeon". */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** The kind with this config name, or null. */
    public static DoorKind fromId(String id) {
        for (DoorKind kind : values()) {
            if (kind.id()
                .equals(
                    id.trim()
                        .toLowerCase(Locale.ROOT))) {
                return kind;
            }
        }
        return null;
    }
}
