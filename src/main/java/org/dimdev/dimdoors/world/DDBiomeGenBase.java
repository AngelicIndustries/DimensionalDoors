package org.dimdev.dimdoors.world;

import java.util.AbstractList;
import java.util.List;

import net.minecraft.entity.EnumCreatureType;
import net.minecraft.world.biome.BiomeGenBase;

public class DDBiomeGenBase extends BiomeGenBase {

    public DDBiomeGenBase(int biomeID, String name) {
        super(biomeID);
        this.setBiomeName(name);
        this.theBiomeDecorator.treesPerChunk = 0;
        this.theBiomeDecorator.flowersPerChunk = 0;
        this.theBiomeDecorator.grassPerChunk = 0;
        this.setDisableRain();

        this.spawnableMonsterList.clear();
        this.spawnableCreatureList.clear();
        this.spawnableWaterCreatureList.clear();
        this.spawnableCaveCreatureList.clear();
    }

    /**
     * Nothing spawns here from biome lists, whatever other mods add to them later (Mo' Creatures and Project Zulu
     * add their animals to biomes after these are made). Monoliths come from their own spawner.
     */
    @Override
    public List<BiomeGenBase.SpawnListEntry> getSpawnableList(EnumCreatureType type) {
        return NO_SPAWNS;
    }

    /** Always empty: mods add spawns through this list, so it takes their entries and drops them. */
    private static final List<BiomeGenBase.SpawnListEntry> NO_SPAWNS = new AbstractList<BiomeGenBase.SpawnListEntry>() {

        @Override
        public BiomeGenBase.SpawnListEntry get(int index) {
            throw new IndexOutOfBoundsException("No spawns here");
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void add(int index, BiomeGenBase.SpawnListEntry entry) {}
    };

    public static void checkBiomes(int[] biomes) {
        for (int k = 0; k < biomes.length; k++) {
            if (getBiomeGenArray()[biomes[k]] != null && !(getBiomeGenArray()[biomes[k]] instanceof DDBiomeGenBase)) {
                // Crash Minecraft to avoid having people complain to us about strange things
                // that are really the result of silent biome ID conflicts.
                throw new IllegalStateException(
                    "There is a biome ID conflict between a biome from Dimensional Doors and another biome type. Fix your configuration!");
            }
        }
    }
}
