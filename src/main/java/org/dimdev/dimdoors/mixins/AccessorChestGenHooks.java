package org.dimdev.dimdoors.mixins;

import java.util.HashMap;

import net.minecraftforge.common.ChestGenHooks;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Forge's loot categories, to look one up without ChestGenHooks.getInfo making an empty one. */
@Mixin(value = ChestGenHooks.class, remap = false)
public interface AccessorChestGenHooks {

    @Accessor("chestInfo")
    static HashMap<String, ChestGenHooks> getChestInfo() {
        throw new AssertionError();
    }
}
