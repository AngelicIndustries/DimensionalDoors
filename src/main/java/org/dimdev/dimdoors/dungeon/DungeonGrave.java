package org.dimdev.dimdoors.dungeon;

/**
 * An OpenBlocks grave that a dungeon placed (OpenBlocksGraves), as MixinTileEntityGrave marks it. Such graves spawn
 * nothing, call up no storm and spill their loot when dug; players' own graves are left alone.
 */
public interface DungeonGrave {

    boolean dimdoors$isDungeonGrave();

    void dimdoors$markDungeonGrave();
}
