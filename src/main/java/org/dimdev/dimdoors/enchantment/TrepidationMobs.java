package org.dimdev.dimdoors.enchantment;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.monster.IMob;

/** Which mobs Trepidation senses, from the Trepidation Mobs config list. */
public final class TrepidationMobs {

    private static boolean allHostile;
    private static final Set<String> added = new HashSet<String>(), removed = new HashSet<String>();

    private TrepidationMobs() {}

    /** * is every mob the game counts as hostile, a name adds one and -name takes one out. */
    public static void configure(String[] entries) {
        allHostile = false;
        added.clear();
        removed.clear();
        for (String entry : entries) {
            entry = entry.trim();
            if (entry.equals("*")) allHostile = true;
            else if (entry.startsWith("-")) removed.add(
                entry.substring(1)
                    .trim());
            else if (!entry.isEmpty()) added.add(entry);
        }
    }

    public static boolean senses(Entity entity) {
        String name = EntityList.getEntityString(entity);
        if (name != null && removed.contains(name)) return false;
        return name != null && added.contains(name) || allHostile && entity instanceof IMob;
    }
}
