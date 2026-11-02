package org.dimdev.dimdoors.world.gateways;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.DimensionManager;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.DimLink;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.NewDimData;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.world.LimboProvider;
import org.dimdev.dimdoors.world.PocketProvider;

import cpw.mods.fml.common.IWorldGenerator;

public class GatewayGenerator implements IWorldGenerator {

    public static final int MAX_GATEWAY_GENERATION_CHANCE = 10000;
    public static final int MAX_CLUSTER_GENERATION_CHANCE = 10000;
    private static final int CLUSTER_GROWTH_CHANCE = 80;
    private static final int MAX_CLUSTER_GROWTH_CHANCE = 100;
    private static final int MIN_RIFT_Y = 4;
    // Rifts stay this far below the top of the world, so up to 240 in a 256 high world
    private static final int RIFT_CEILING_GAP = 16;
    private static final int CHUNK_LENGTH = 16;
    private static final int GATEWAY_RADIUS = 4;
    private static final int MAX_GATEWAY_GENERATION_ATTEMPTS = 10;
    private static final int OVERWORLD_DIMENSION_ID = 0;
    private static final int NETHER_DIMENSION_ID = -1;
    private static final int END_DIMENSION_ID = 1;
    private static final String SPIRIT_WORLD_NAME = "Spirit World";

    private BaseGateway limboGateway;

    private final DDProperties properties;

    public GatewayGenerator(DDProperties properties) {
        this.properties = properties;
        this.initialize();
    }

    private void initialize() {
        limboGateway = new GatewayLimbo(properties);
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkProvider chunkGenerator,
        IChunkProvider chunkProvider) {
        // Don't generate rifts or gateways if the current world is a pocket dimension or the world is remote.
        // Also don't generate anything in the Nether, The End, or in Witchery's Spirit World.
        // We only match against Spirit World using hashing to speed up the process a little (hopefully).
        int dimensionID = world.provider.dimensionId;
        if (world.isRemote || (world.provider instanceof PocketProvider)
            || (dimensionID == END_DIMENSION_ID)
            || (dimensionID == NETHER_DIMENSION_ID)
            || (world.provider.getDimensionName()
                .hashCode() == SPIRIT_WORLD_NAME.hashCode())) {
            return;
        }
        // This check prevents a crash related to superflat worlds not loading World 0
        if (DimensionManager.getWorld(OVERWORLD_DIMENSION_ID) == null) {
            return;
        }

        int x, y, z;
        int attempts;
        DimLink link;
        NewDimData dimension;

        // Check if we're allowed to generate rift clusters in this dimension.
        // If so, randomly decide whether to one.
        if (DimDoors.worldProperties.RiftClusterDimensions.isAccepted(dimensionID)
            && random.nextInt(MAX_CLUSTER_GENERATION_CHANCE) < properties.ClusterGenerationChance) {
            link = null;
            dimension = null;
            do {
                // Pick a random point on the surface of the chunk
                x = chunkX * CHUNK_LENGTH + random.nextInt(CHUNK_LENGTH);
                z = chunkZ * CHUNK_LENGTH + random.nextInt(CHUNK_LENGTH);
                y = world.getHeightValue(x, z);

                // If the point is within the acceptable altitude range, the block above is empty, and we're
                // not building on bedrock, then generate a rift there
                if (y >= MIN_RIFT_Y && y <= maxRiftY(world)
                    && world.isAirBlock(x, y + 1, z)
                    && world.getBlock(x, y, z) != Blocks.bedrock
                    && // <-- Stops Nether roof spawning. DO NOT REMOVE!
                    world.getBlock(x, y - 1, z) != Blocks.bedrock
                    && world.getBlock(x, y - 2, z) != Blocks.bedrock
                    && !world.getBlock(x, y - 1, z)
                        .getMaterial()
                        .isLiquid()) {
                    // Create a link. If this is not the first time, create a child link and connect it to the first
                    // link.
                    if (link == null) {
                        dimension = PocketManager.getDimensionData(world);
                        link = dimension.createLink(x, y + 1, z, LinkType.DUNGEON, 0);
                    } else {
                        dimension.createChildLink(x, y + 1, z, link);
                    }
                }
            }
            // Randomly decide whether to repeat the process and add another rift to the cluster
            while (random.nextInt(MAX_CLUSTER_GROWTH_CHANCE) < CLUSTER_GROWTH_CHANCE);
        }

        // Check if we can place a Rift Gateway in this dimension, then randomly decide whether to place one.
        // This only happens if a rift cluster was NOT generated.
        else if (DimDoors.worldProperties.RiftGatewayDimensions.isAccepted(dimensionID)
            && random.nextInt(MAX_GATEWAY_GENERATION_CHANCE) < properties.GatewayGenerationChance) {
                // Check locations for the gateway until one is built or we run out of attempts.
                for (attempts = 0; attempts < MAX_GATEWAY_GENERATION_ATTEMPTS; attempts++) {
                    // Pick a random point on the surface of the chunk and check its materials. Like vanilla's
                    // decorations, the point is offset by half a chunk so the gateway stays in generated chunks.
                    x = chunkX * CHUNK_LENGTH + CHUNK_LENGTH / 2 + random.nextInt(CHUNK_LENGTH);
                    z = chunkZ * CHUNK_LENGTH + CHUNK_LENGTH / 2 + random.nextInt(CHUNK_LENGTH);
                    y = world.getHeightValue(x, z);

                    if (world.provider instanceof LimboProvider) {
                        if (checkGatewayLocation(world, x, y, z) && limboGateway.generate(world, x, y - 1, z)) {
                            break;
                        }
                    } else if (isSeaSurface(world, x, y, z)) {
                        // 6.x builds its prismarine gateway on the sea floor, at any depth
                        int floor = GatewayTemplate.groundAt(world, x, z, true);
                        if (floor >= MIN_RIFT_Y && StyledGateways.generate(world, random, x, floor, z, true)) {
                            break;
                        }
                    } else if (checkGatewayLocation(world, x, y, z)
                        && StyledGateways.generate(world, random, x, y - 1, z, false)) {
                            break;
                        }
                }
            }
    }

    /** Water at the surface of an ocean biome. */
    private static boolean isSeaSurface(World world, int x, int y, int z) {
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);
        return biome != null && BiomeDictionary.isBiomeOfType(biome, BiomeDictionary.Type.OCEAN)
            && world.getBlock(x, y - 1, z)
                .getMaterial() == Material.water;
    }

    private static boolean checkGatewayLocation(World world, int x, int y, int z) {
        // Check if the point is within the acceptable altitude range, the block above that point is empty,
        // and the block two levels down is opaque and has a reasonable material. Plus that we're not building
        // on top of bedrock.
        return (y >= MIN_RIFT_Y && y <= maxRiftY(world)
            && world.isAirBlock(x, y + 1, z)
            && world.getBlock(x, y, z) != Blocks.bedrock
            && // <-- Stops Nether roof spawning. DO NOT REMOVE!
            world.getBlock(x, y - 1, z) != Blocks.bedrock
            && checkFoundationMaterial(world, x, y - 2, z)
            && isDry(world, x, y, z));
    }

    /** No water or lava at the gateway's floor or doorway, so it doesn't stand in a pond or a marsh. */
    private static boolean isDry(World world, int x, int y, int z) {
        for (int dx = -GATEWAY_RADIUS; dx <= GATEWAY_RADIUS; dx++) {
            for (int dz = -GATEWAY_RADIUS; dz <= GATEWAY_RADIUS; dz++) {
                for (int dy = -1; dy <= 0; dy++) {
                    if (world.getBlock(x + dx, y + dy, z + dz)
                        .getMaterial()
                        .isLiquid()) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static int maxRiftY(World world) {
        return world.getHeight() - RIFT_CEILING_GAP;
    }

    private static boolean checkFoundationMaterial(World world, int x, int y, int z) {
        // We check the material and opacity to prevent generating gateways on top of trees or houses,
        // or on top of strange things like tall grass, water, slabs, or torches.
        // We also want to avoid generating things on top of the Nether's bedrock!
        Material material = world.getBlock(x, y, z)
            .getMaterial();
        return (material != Material.leaves && material != Material.wood
            && material != Material.gourd
            && world.isBlockNormalCubeDefault(x, y, z, false)
            && world.getBlock(x, y, z) != Blocks.bedrock);
    }
}
