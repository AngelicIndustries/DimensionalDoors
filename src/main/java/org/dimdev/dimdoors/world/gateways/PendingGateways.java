package org.dimdev.dimdoors.world.gateways;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.storage.MapStorage;

/**
 * Gateways wait until the land around them is finished. A chunk's trees, leaf litter and other decorations reach into
 * its neighbours, which are often decorated later, so a gateway built right away got trees grown through it and litter
 * on its stairs. Each chunk that rolls a gateway waits here, saved with its dimension, until every chunk within two of
 * it is loaded and populated; only then is its spot chosen and the gateway built.
 */
public class PendingGateways extends WorldSavedData {

    private static final String NAME = "dimdoors_pending_gateways";
    private static final int REACH = 2;
    /** How many waiting gateways one tick may build. */
    private static final int PER_TICK = 2;
    private static final Map<World, PendingGateways> LOADED = new WeakHashMap<World, PendingGateways>();

    private final List<int[]> chunks = new ArrayList<int[]>();

    public PendingGateways(String name) {
        super(name);
    }

    /** This dimension's waiting gateways, read from its save the first time. */
    public static PendingGateways get(World world) {
        PendingGateways pending = LOADED.get(world);
        if (pending == null) {
            MapStorage storage = world.perWorldStorage;
            pending = (PendingGateways) storage.loadData(PendingGateways.class, NAME);
            if (pending == null) {
                pending = new PendingGateways(NAME);
                storage.setData(NAME, pending);
            }
            LOADED.put(world, pending);
        }
        return pending;
    }

    public void add(int chunkX, int chunkZ) {
        chunks.add(new int[] { chunkX, chunkZ });
        markDirty();
    }

    /** Builds a few of the gateways whose surroundings are finished. */
    public void build(World world, GatewayGenerator generator) {
        if (chunks.isEmpty()) return;
        int built = 0;
        for (int[] chunk : new ArrayList<int[]>(chunks)) {
            if (built == PER_TICK) return;
            if (!ready(world, chunk[0], chunk[1])) continue;
            chunks.remove(chunk);
            markDirty();
            generator.buildGateway(world, chunk[0], chunk[1]);
            built++;
        }
    }

    /** Whether every chunk within reach is loaded and populated; never loads or generates one. */
    private static boolean ready(World world, int chunkX, int chunkZ) {
        IChunkProvider provider = world.getChunkProvider();
        for (int dx = -REACH; dx <= REACH; dx++) {
            for (int dz = -REACH; dz <= REACH; dz++) {
                if (!provider.chunkExists(chunkX + dx, chunkZ + dz)
                    || !world.getChunkFromChunkCoords(chunkX + dx, chunkZ + dz).isTerrainPopulated) return false;
            }
        }
        return true;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        chunks.clear();
        int[] coords = tag.getIntArray("Chunks");
        for (int i = 0; i + 1 < coords.length; i += 2) chunks.add(new int[] { coords[i], coords[i + 1] });
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        int[] coords = new int[chunks.size() * 2];
        for (int i = 0; i < chunks.size(); i++) {
            coords[i * 2] = chunks.get(i)[0];
            coords[i * 2 + 1] = chunks.get(i)[1];
        }
        tag.setIntArray("Chunks", coords);
    }
}
