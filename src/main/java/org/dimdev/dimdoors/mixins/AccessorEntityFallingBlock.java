package org.dimdev.dimdoors.mixins;

import net.minecraft.block.Block;
import net.minecraft.entity.item.EntityFallingBlock;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(EntityFallingBlock.class)
public interface AccessorEntityFallingBlock {

    /** The block that falls. */
    @Accessor("field_145811_e")
    void dimdoors$setBlock(Block block);

    /** Set, the falling block neither lands as a block nor drops as an item (vanilla uses it for broken anvils). */
    @Accessor("field_145808_f")
    void dimdoors$setVanishes(boolean vanishes);
}
