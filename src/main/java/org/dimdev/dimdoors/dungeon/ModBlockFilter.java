package org.dimdev.dimdoors.dungeon;

import java.util.List;

import net.minecraft.block.Block;

import org.dimdev.dimdoors.schematic.SchematicFilter;

public class ModBlockFilter extends SchematicFilter {

    private final List<Block> exceptions;
    private final List<String> allowedMods;
    private final Block replacementBlock;
    private final byte replacementMetadata;

    /** Keeps vanilla blocks, the exceptions and every block of the allowed mods; the rest are replaced. */
    public ModBlockFilter(List<Block> exceptions, List<String> allowedMods, Block replacementBlock,
        byte replacementMetadata) {
        super("ModBlockFilter");
        this.exceptions = exceptions;
        this.allowedMods = allowedMods;
        this.replacementBlock = replacementBlock;
        this.replacementMetadata = replacementMetadata;
    }

    @Override
    protected boolean applyToBlock(int index, Block[] blocks, byte[] metadata) {
        int k;
        Block current = blocks[index];
        String name = Block.blockRegistry.getNameForObject(current);
        if (!name.startsWith("minecraft:") && !allowedMods.contains(name.substring(0, name.indexOf(':')))) {
            // This might be a mod block. Check if an exception exists.
            for (k = 0; k < exceptions.size(); k++) {
                if (current == exceptions.get(k)) {
                    // Exception found, not considered a mod block
                    return false;
                }
            }
            // No matching exception found. Replace the block.
            blocks[index] = replacementBlock;
            metadata[index] = replacementMetadata;
            return true;
        }
        return false;
    }

    @Override
    protected boolean terminates() {
        return false;
    }
}
