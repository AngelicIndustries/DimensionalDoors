package org.dimdev.dimdoors.ticking;

import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.MathHelper;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.core.PocketManager;
import org.dimdev.dimdoors.helpers.yCoordHelper;
import org.dimdev.dimdoors.pockets.Pocket;
import org.dimdev.dimdoors.pockets.SharedPockets;
import org.dimdev.dimdoors.util.ChunkLocation;

public class CustomLimboPopulator implements IRegularTickReceiver {

    public static final int MAX_MONOLITH_SPAWNING_CHANCE = 100;
    private static final String MOB_SPAWNING_RULE = "doMobSpawning";
    private static final int MAX_MONOLITH_SPAWN_Y = 245;
    /** A standing player's eye height. */
    private static final double EYE_LEVEL = 1.62;
    /** The percentage of Limbo's Monoliths that hover low. */
    private static final int LOW_MONOLITH_CHANCE = 40;
    private static final int CHUNK_SIZE = 16;
    private static final int MONOLITH_SPAWNING_INTERVAL = 1;

    private final DDProperties properties;
    private final ConcurrentLinkedQueue<ChunkLocation> locations;

    public CustomLimboPopulator(IRegularTickSender sender, DDProperties properties) {
        this.properties = properties;
        this.locations = new ConcurrentLinkedQueue<ChunkLocation>();
        sender.registerReceiver(this, MONOLITH_SPAWNING_INTERVAL, false);
    }

    @Override
    public void notifyTick() {

        World limboWorld = null;

        // Check if any new spawning requests have come in
        if (!locations.isEmpty()) {
            // Check if mob spawning is allowed
            if (isMobSpawningAllowed()) {
                // Loop over the locations and call the appropriate function depending
                // on whether the request is for Limbo or for a pocket dimension.
                for (ChunkLocation location : locations) {
                    if (location.DimensionID == properties.LimboDimensionID) {
                        // Limbo chunk

                        // SenseiKiwi: Check if we haven't loaded Limbo for another request in this request
                        // cycle. If so, try to load Limbo up. This solves a strange issue with ChickenChunks
                        // where CC somehow forces chunks to generate in Limbo if LimboProvider.canRespawnHere()
                        // is true, yet when execution reaches this point, Limbo isn't loaded anymore! My theory
                        // is that CC force-loads a chunk for some reason, but since there are no players around,
                        // Limbo immediately unloads after standard world gen runs, and before this code can run.

                        if (limboWorld == null) {
                            limboWorld = PocketManager.loadDimension(properties.LimboDimensionID);
                        }
                        placeMonolithsInLimbo(limboWorld, location.ChunkX, location.ChunkZ);
                        DimDoors.gatewayGenerator.generate(
                            limboWorld.rand,
                            location.ChunkX,
                            location.ChunkZ,
                            limboWorld,
                            limboWorld.getChunkProvider(),
                            limboWorld.getChunkProvider());
                    } else {
                        // Pocket dimension chunk
                        placeMonolithsInPocket(location.DimensionID, location.ChunkX, location.ChunkZ);
                    }
                }
            }

            locations.clear();
        }
    }

    public void registerChunkForPopulation(int dimensionID, int chunkX, int chunkZ) {
        ChunkLocation location = new ChunkLocation(dimensionID, chunkX, chunkZ);
        locations.add(location);
    }

    private void placeMonolithsInPocket(int dimensionID, int chunkX, int chunkZ) {
        Pocket dungeon = SharedPockets
            .pocketAt(dimensionID, chunkX * CHUNK_SIZE + CHUNK_SIZE / 2, chunkZ * CHUNK_SIZE + CHUNK_SIZE / 2);
        World pocket = DimensionManager.getWorld(dimensionID);

        if (pocket == null || dungeon == null
            || dungeon.dungeon() == null
            || dungeon.dungeon()
                .isOpen()) {
            return;
        }

        int sanity = 0;
        boolean didSpawn = false;

        // The following initialization code is based on code from ChunkProviderGenerate.
        // It makes our generation depend on the world seed.
        Random random = new Random(pocket.getSeed() ^ 0xA210FE65F20017D6L);
        long factorA = random.nextLong() / 2L * 2L + 1L;
        long factorB = random.nextLong() / 2L * 2L + 1L;
        random.setSeed(chunkX * factorA + chunkZ * factorB ^ pocket.getSeed());

        int x, z;
        do {
            // Select a random column within the chunk
            x = chunkX * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
            z = chunkZ * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
            int floor = roomFloor(pocket, x, z);
            if (floor > 0) {
                MobMonolith monolith = new MobMonolith(pocket);
                monolith.setLocationAndAngles(x + 0.5, floor + 1 + EYE_LEVEL - monolith.getEyeHeight(), z + 0.5, 1, 1);
                pocket.spawnEntityInWorld(monolith);
                didSpawn = true;
            }
            sanity++;
        } while (sanity < 5 && !didSpawn);
    }

    /**
     * The floor of the first room down a column: under its roof, with headroom for a player. Legacy took the first
     * floor it reached past the Fabric of Reality shell its rooms have, which in 6.x's rooms, built without one, was
     * their roof.
     */
    private static int roomFloor(World pocket, int x, int z) {
        int y = MAX_MONOLITH_SPAWN_Y;
        while (y > 0 && pocket.isAirBlock(x, y, z)) y--;
        while (y > 0 && !pocket.isAirBlock(x, y, z)) y--;
        int top = y;
        while (y > 0 && pocket.isAirBlock(x, y, z)) y--;
        return y > 0 && top - y >= 2 ? y : -1;
    }

    private void placeMonolithsInLimbo(World limbo, int chunkX, int chunkZ) {
        // The following initialization code is based on code from ChunkProviderGenerate.
        // It makes our generation depend on the world seed.
        Random random = new Random(limbo.getSeed() ^ 0xB5130C4ACC71A822L);
        long factorA = random.nextLong() / 2L * 2L + 1L;
        long factorB = random.nextLong() / 2L * 2L + 1L;
        random.setSeed(chunkX * factorA + chunkZ * factorB ^ limbo.getSeed());

        // Okay, the following code is full of magic constants and makes little sense. =/ ~SenseiKiwi
        if (random.nextInt(MAX_MONOLITH_SPAWNING_CHANCE) < properties.MonolithSpawningChance) {
            int y = 0;
            int yTest;
            do {
                int x = chunkX * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);
                int z = chunkZ * CHUNK_SIZE + random.nextInt(CHUNK_SIZE);

                while (limbo.getBlock(x, y, z)
                    .isAir(limbo, x, y, z) && y < 255) {
                    y++;
                }
                y = yCoordHelper.getFirstUncovered(limbo, x, y + 2, z);
                yTest = yCoordHelper.getFirstUncovered(limbo, x, y + 5, z);
                if (yTest > 245) {
                    return;
                }

                int jumpSanity = 0;
                int jumpHeight = 0;
                do {
                    jumpHeight = y + random.nextInt(25);
                    jumpSanity++;
                } while (!limbo.isAirBlock(x, jumpHeight + 6, z) && jumpSanity < 20);

                Entity monolith = new MobMonolith(limbo);
                // Some hover low, their eye at a player's eye level, to open in your face out of the fog. y is above
                // the two air blocks getFirstUncovered found, so one stands two lower.
                double posY = random.nextInt(100) < LOW_MONOLITH_CHANCE ? y - 2 + EYE_LEVEL - monolith.getEyeHeight()
                    : jumpHeight;
                // An eye inside a block is never seen, so it can't aggro
                if (!limbo.isAirBlock(x, MathHelper.floor_double(posY + monolith.getEyeHeight()), z)) continue;
                monolith.setLocationAndAngles(x + 0.5, posY, z + 0.5, 1, 1);
                limbo.spawnEntityInWorld(monolith);
            } while (yTest > y);
        }
    }

    public static boolean isMobSpawningAllowed() {
        // This function is used to retrieve the value of doMobSpawning. The code is the same
        // as the code used by Minecraft. Jaitsu requested this to make testing easier. ~SenseiKiwi

        GameRules rules = MinecraftServer.getServer()
            .worldServerForDimension(0)
            .getGameRules();
        return rules.getGameRuleBooleanValue(MOB_SPAWNING_RULE);
    }
}
