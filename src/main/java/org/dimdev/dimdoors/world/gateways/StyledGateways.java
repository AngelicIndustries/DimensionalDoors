package org.dimdev.dimdoors.world.gateways;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemDoor;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import org.dimdev.dimdoors.DimDoors;
import org.dimdev.dimdoors.core.LinkType;
import org.dimdev.dimdoors.core.PocketManager;

/**
 * 6.x's gateways, built in the style the ground and the biome call for: an enclosed ring of pillars most of the time,
 * otherwise (or where the ground is too uneven for the ring) a pair of pillars. Under the sea they are prismarine.
 */
public final class StyledGateways {

    /** One in this many gateways is a pair of pillars rather than the enclosed ring. */
    private static final int PILLARS_CHANCE = 4;
    private static final int GROUND_PATCH_RADIUS = 4;

    private StyledGateways() {}

    /**
     * Builds a gateway around a door at (x, z) whose ground block (or sea floor) is at groundY. Returns false if
     * neither shape fits the ground there.
     */
    public static boolean generate(World world, Random random, int x, int groundY, int z, boolean underwater) {
        GatewayStyle style = choose(world, x, groundY, z, underwater);
        int turns = random.nextInt(4);
        boolean enclosed = random.nextInt(PILLARS_CHANCE) != 0;
        if (enclosed && build(world, style, true, x, groundY, z, turns, underwater)) return true;
        return build(world, style, false, x, groundY, z, turns, underwater);
    }

    private static boolean build(World world, GatewayStyle style, boolean enclosed, int x, int groundY, int z,
        int turns, boolean underwater) {
        GatewayTemplate template = style.template(enclosed);
        GatewayStyle.Materials materials = (enclosed ? style : style.pillarStyle()).materials();
        if (materials == null || !template.fits(world, x, groundY, z, turns, underwater)) return false;

        if (!enclosed && materials.block(GatewayTemplate.BASE) == Blocks.stonebrick && !underwater) {
            patchGround(world, x, groundY, z);
        }
        template.place(world, x, groundY, z, turns, materials, underwater);
        template.tidy(world, x, groundY, z);

        // The door and its dungeon link, as Legacy's gateways have
        int doorY = template.doorY(groundY);
        int orientation = template.doorOrientation(turns);
        // The ring's door faces its wide open side, with the arch and its steps behind; 6.x faced the arch
        if (enclosed) orientation = (orientation + 2) & 3;
        PocketManager.createDimensionData(world)
            .createLink(x, doorY + 1, z, LinkType.DUNGEON, orientation);
        ItemDoor.placeDoorBlock(world, x, doorY, z, orientation, DimDoors.transientDoor);
        return true;
    }

    /** The style for a gateway: from the ground block, what lies on it, and the biome's Forge types. */
    static GatewayStyle choose(World world, int x, int groundY, int z, boolean underwater) {
        if (world.provider.getClass()
            .getName()
            .startsWith("twilightforest.")) {
            return twilightStyle(world.getBiomeGenForCoords(x, z), underwater);
        }
        if (underwater) return GatewayStyle.PRISMARINE;
        Block ground = world.getBlock(x, groundY, z);
        int meta = world.getBlockMetadata(x, groundY, z);
        Block above = world.getBlock(x, groundY + 1, z);
        String name = String.valueOf(Block.blockRegistry.getNameForObject(ground));
        BiomeGenBase biome = world.getBiomeGenForCoords(x, z);

        // What the ground is made of first, then the biome's types, then bare rock
        if (name.startsWith("aether_legacy:")) return GatewayStyle.AETHER;
        if (ground == Blocks.obsidian || ground == Blocks.netherrack
            || name.equals("etfuturum:blackstone")
            || name.equals("etfuturum:basalt")
            || name.equals("etfuturum:magma")) {
            return GatewayStyle.BLACKSTONE;
        }
        if (ground == Blocks.sand && meta == 1 || ground == Blocks.hardened_clay
            || ground == Blocks.stained_hardened_clay
            || name.equals("etfuturum:red_sandstone")) {
            return GatewayStyle.RED_SANDSTONE;
        }
        if (above == Blocks.snow_layer || ground == Blocks.snow
            || ground == Blocks.ice
            || ground == Blocks.packed_ice) {
            return GatewayStyle.QUARTZ;
        }
        if (name.equals("etfuturum:mud") || name.equals("BiomesOPlenty:mud")) return GatewayStyle.MUD;
        if ((ground == Blocks.sand || ground == Blocks.sandstone) && (is(biome, Type.HOT) || is(biome, Type.SANDY))) {
            return GatewayStyle.SANDSTONE;
        }
        if (is(biome, Type.MESA)) return GatewayStyle.RED_SANDSTONE;
        if (is(biome, Type.SNOWY)) return GatewayStyle.QUARTZ;
        if (is(biome, Type.SWAMP)) return GatewayStyle.MUD;
        if (is(biome, Type.JUNGLE) || is(biome, Type.LUSH)) return GatewayStyle.MOSSY;
        if (ground == Blocks.stone || ground == Blocks.cobblestone
            || ground == Blocks.gravel
            || name.equals("etfuturum:tuff")
            || name.equals("etfuturum:stone")
            || name.equals("etfuturum:calcite")) {
            return GatewayStyle.TUFF;
        }
        return GatewayStyle.STONE_BRICK;
    }

    /**
     * In the Twilight Forest, the stone of the biome's landmark: aurora blocks on the glacier and in the snowy forest,
     * castle brick in the highlands, deadrock in the thornlands, towerwood in the dark forest, mossy mazestone over
     * the swamp's labyrinth (and in its lakes), mazestone everywhere else.
     */
    private static GatewayStyle twilightStyle(BiomeGenBase biome, boolean underwater) {
        String name = biome != null ? biome.biomeName : "";
        if (underwater || name.contains("Swamp")) return GatewayStyle.MOSSY_MAZESTONE;
        if (name.equals("Snowy Forest") || name.equals("Twilight Glacier")) return GatewayStyle.AURORA;
        if (name.equals("Twilight Highlands") || name.equals("Highlands Center")) return GatewayStyle.CASTLE_BRICK;
        if (name.equals("Thornlands")) return GatewayStyle.DEADROCK;
        if (name.startsWith("Dark Forest")) return GatewayStyle.TOWERWOOD;
        return GatewayStyle.MAZESTONE;
    }

    private static boolean is(BiomeGenBase biome, Type type) {
        return biome != null && BiomeDictionary.isBiomeOfType(biome, type);
    }

    /** Legacy's stone brick and cracked brick patch around the plain pillars. */
    private static void patchGround(World world, int x, int y, int z) {
        for (int dx = -GROUND_PATCH_RADIUS; dx <= GROUND_PATCH_RADIUS; dx++) {
            for (int dz = -GROUND_PATCH_RADIUS; dz <= GROUND_PATCH_RADIUS; dz++) {
                // Only on supported ground, so not over a cliff or on a frozen lake
                if (!world.isBlockNormalCubeDefault(x + dx, y - 1, z + dz, false)) continue;
                // Less likely the farther from the gateway
                int distance = Math.abs(dx) + Math.abs(dz);
                // Without block updates, so plants on it don't pop off as items (tidy clears them)
                if (distance < world.rand.nextInt(2) + 3) {
                    world.setBlock(x + dx, y, z + dz, Blocks.stonebrick, 0, 2);
                } else if (distance < world.rand.nextInt(3) + 3) {
                    world.setBlock(x + dx, y, z + dz, Blocks.stonebrick, 2, 2);
                }
            }
        }
    }
}
