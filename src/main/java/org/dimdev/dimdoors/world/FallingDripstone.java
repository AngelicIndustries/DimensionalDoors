package org.dimdev.dimdoors.world;

import net.minecraft.block.Block;
import net.minecraftforge.event.world.BlockEvent;

import org.dimdev.dimdoors.ticking.EntityFallingDripstone;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Et Futurum's pointed dripstone only drops as an item when what holds it goes; a stalactite falls instead, as in 6.x,
 * so traps built on it work. Without Et Futurum this does nothing.
 */
public class FallingDripstone {

    /**
     * Et Futurum's metadata: the thickness from tip to base (tip, frustum, middle, base, merged tip), plus 5 pointing
     * up.
     */
    private static final int STATES = 5;

    private Block dripstone;
    private boolean looked;

    @SubscribeEvent
    public void onHarvestDrops(BlockEvent.HarvestDropsEvent event) {
        if (event.world.isRemote || event.harvester != null || event.block != dripstone()) return;
        int meta = event.blockMetadata;
        if (meta >= STATES) return;
        // Broken some other way while still held, by an explosion say: it drops as usual
        Block above = event.world.getBlock(event.x, event.y + 1, event.z);
        if (above == dripstone || above.isNormalCube(event.world, event.x, event.y + 1, event.z)) return;
        event.drops.clear();
        boolean tip = meta == 0 || meta == 4;
        event.world.spawnEntityInWorld(
            new EntityFallingDripstone(event.world, event.x, event.y, event.z, event.block, meta, tip));
    }

    private Block dripstone() {
        if (!looked) {
            dripstone = GameRegistry.findBlock("etfuturum", "pointed_dripstone");
            looked = true;
        }
        return dripstone;
    }
}
