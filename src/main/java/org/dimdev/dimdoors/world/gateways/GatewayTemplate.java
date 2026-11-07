package org.dimdev.dimdoors.world.gateways;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.block.Block;
import net.minecraft.block.BlockBush;
import net.minecraft.block.material.Material;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

/**
 * A gateway's shape, converted from Dimensional Doors 6.x's structures: each position holds a role (base, column,
 * accent, stairs...) that a {@link GatewayStyle} fills with its blocks. The template turns around its door, which
 * stands where 6.x has its portal.
 */
public final class GatewayTemplate {

    static final int VOID = 0, AIR = 1, BASE = 2, COLUMN = 3, ACCENT = 4, STAIRS = 5, MUD = 6, LIGHT = 7;

    /** How far the ground under a template may stray from the door's ground, up or down. */
    private static final int MAX_UNEVENNESS = 1;
    /** How far a template's floor is propped up where the ground falls away under it. */
    private static final int MAX_FILL = 8;

    private static final Map<String, GatewayTemplate> CACHE = new HashMap<String, GatewayTemplate>();

    private final int width, height, length;
    private final byte[] roles;
    private final byte[] data;
    private final int doorX, doorY, doorZ, doorOrientation;
    private final int sink;
    /** Per column: 0 nothing, 1 only air to clear, 2 blocks. */
    private final byte[] columns;

    private GatewayTemplate(NBTTagCompound tag) {
        width = tag.getShort("Width");
        height = tag.getShort("Height");
        length = tag.getShort("Length");
        roles = tag.getByteArray("Roles");
        data = tag.getByteArray("Data");
        doorX = tag.getInteger("DoorX");
        doorY = tag.getInteger("DoorY");
        doorZ = tag.getInteger("DoorZ");
        doorOrientation = tag.getInteger("DoorOrientation");
        sink = tag.getInteger("Sink");
        columns = new byte[width * length];
        for (int x = 0; x < width; x++) {
            for (int z = 0; z < length; z++) {
                for (int y = 0; y < height; y++) {
                    int role = role(x, y, z);
                    if (role == AIR) columns[z * width + x] = (byte) Math.max(columns[z * width + x], 1);
                    else if (role != VOID) columns[z * width + x] = 2;
                }
            }
        }
    }

    /** A template from schematics/gateways, loaded once. */
    public static synchronized GatewayTemplate get(String name) {
        GatewayTemplate template = CACHE.get(name);
        if (template == null) {
            String path = "/schematics/gateways/" + name + ".nbt";
            InputStream stream = GatewayTemplate.class.getResourceAsStream(path);
            if (stream == null) throw new IllegalStateException("Missing gateway template " + path);
            try {
                template = new GatewayTemplate(CompressedStreamTools.readCompressed(stream));
            } catch (IOException e) {
                throw new IllegalStateException("Could not read gateway template " + path, e);
            }
            CACHE.put(name, template);
        }
        return template;
    }

    private int role(int x, int y, int z) {
        return roles[(y * length + z) * width + x];
    }

    /** The world y of the template's bottom layer, for a door whose ground block is at groundY. */
    private int baseY(int groundY) {
        return groundY + 1 - sink;
    }

    /** Where the door's lower half goes. */
    public int doorY(int groundY) {
        return baseY(groundY) + doorY;
    }

    /** The door's direction (0 east, 1 south, 2 west, 3 north) once the template is turned. */
    public int doorOrientation(int turns) {
        return (doorOrientation + turns) & 3;
    }

    private int worldX(int doorWorldX, int x, int z, int turns) {
        int dx = x - doorX, dz = z - doorZ;
        switch (turns & 3) {
            case 1:
                return doorWorldX - dz;
            case 2:
                return doorWorldX - dx;
            case 3:
                return doorWorldX + dz;
            default:
                return doorWorldX + dx;
        }
    }

    private int worldZ(int doorWorldZ, int x, int z, int turns) {
        int dx = x - doorX, dz = z - doorZ;
        switch (turns & 3) {
            case 1:
                return doorWorldZ + dx;
            case 2:
                return doorWorldZ - dz;
            case 3:
                return doorWorldZ - dx;
            default:
                return doorWorldZ + dz;
        }
    }

    /**
     * Whether the ground is even enough: under the template's blocks within a block of the door's ground, dry on land
     * and under water for an underwater gateway; where it only clears air, not much higher than that. And no tree
     * trunk stands where it builds or clears.
     */
    public boolean fits(World world, int x, int groundY, int z, int turns, boolean underwater) {
        int baseY = baseY(groundY);
        for (int ty = 0; ty < height; ty++) {
            for (int tz = 0; tz < length; tz++) {
                for (int tx = 0; tx < width; tx++) {
                    if (role(tx, ty, tz) == VOID) continue;
                    int wx = worldX(x, tx, tz, turns), wy = baseY + ty, wz = worldZ(z, tx, tz, turns);
                    if (world.getBlock(wx, wy, wz)
                        .isWood(world, wx, wy, wz)) return false;
                }
            }
        }
        for (int tx = 0; tx < width; tx++) {
            for (int tz = 0; tz < length; tz++) {
                int column = columns[tz * width + tx];
                if (column == 0) continue;
                int wx = worldX(x, tx, tz, turns), wz = worldZ(z, tx, tz, turns);
                int ground = groundAt(world, wx, wz, underwater);
                if (column == 1) {
                    if (ground > groundY + MAX_UNEVENNESS) return false;
                    continue;
                }
                if (ground < 0 || Math.abs(ground - groundY) > MAX_UNEVENNESS) return false;
                boolean wet = world.getBlock(wx, ground + 1, wz)
                    .getMaterial()
                    .isLiquid();
                if (wet != underwater) return false;
            }
        }
        return true;
    }

    /** The top block that bears weight in a column: under water for an underwater gateway, or -1 if none. */
    static int groundAt(World world, int x, int z, boolean underwater) {
        for (int y = world.getHeightValue(x, z); y > 0; y--) {
            Material material = world.getBlock(x, y, z)
                .getMaterial();
            if (material.isLiquid()) {
                if (underwater) continue;
                return -1;
            }
            // Skip what stands on the ground: plants, snow layers, trees
            if (!material.blocksMovement() || material == Material.leaves || material == Material.wood) continue;
            return y;
        }
        return -1;
    }

    /** Builds the template around a door at (x, z) whose ground block is at groundY, turned clockwise. */
    public void place(World world, int x, int groundY, int z, int turns, GatewayStyle.Materials materials,
        boolean underwater) {
        int baseY = baseY(groundY);
        for (int ty = 0; ty < height; ty++) {
            for (int tz = 0; tz < length; tz++) {
                for (int tx = 0; tx < width; tx++) {
                    int role = role(tx, ty, tz);
                    if (role == VOID) continue;
                    int wx = worldX(x, tx, tz, turns), wy = baseY + ty, wz = worldZ(z, tx, tz, turns);
                    if (role == AIR) {
                        // Under water the gateway fills with water, as on 6.x; leaves overhead stay
                        if (!underwater && !world.getBlock(wx, wy, wz)
                            .isLeaves(world, wx, wy, wz)) world.setBlock(wx, wy, wz, Blocks.air, 0, 2);
                        continue;
                    }
                    Block block = materials.block(role);
                    int meta = role == STAIRS && materials.hasStairs()
                        ? rotateStairs(data[(ty * length + tz) * width + tx], turns)
                        : materials.meta(role);
                    world.setBlock(wx, wy, wz, block, meta, 2);
                    if (ty == 0) prop(world, wx, wy - 1, wz, materials, underwater);
                }
            }
        }
    }

    /**
     * Quietly removes plants, petals, leaf litter and other decorations around the gateway that can no longer stand
     * where they are, now that it covers their ground. Left there, the next block update pops them off as items.
     */
    public void tidy(World world, int x, int groundY, int z) {
        int reach = Math.max(width, length);
        for (int dx = -reach; dx <= reach; dx++) {
            for (int dz = -reach; dz <= reach; dz++) {
                for (int y = groundY; y <= groundY + height + 2; y++) {
                    Block block = world.getBlock(x + dx, y, z + dz);
                    Material material = block.getMaterial();
                    if ((material == Material.plants || material == Material.vine || block instanceof BlockBush)
                        && !block.canBlockStay(world, x + dx, y, z + dz)) {
                        world.setBlock(x + dx, y, z + dz, Blocks.air, 0, 2);
                    }
                }
            }
        }
    }

    /** Props the floor up where the ground falls away, with the style's base block. */
    private static void prop(World world, int x, int y, int z, GatewayStyle.Materials materials, boolean underwater) {
        for (int n = 0; n < MAX_FILL && y > 0; n++, y--) {
            Block block = world.getBlock(x, y, z);
            Material material = block.getMaterial();
            boolean open = material == Material.air || material.isLiquid() && underwater
                || block.isReplaceable(world, x, y, z);
            if (!open) return;
            world.setBlock(x, y, z, materials.block(BASE), materials.meta(BASE), 2);
        }
    }

    /** Legacy stairs metadata turned clockwise: east, south, west, north. The upside-down bit stays. */
    private static int rotateStairs(int meta, int turns) {
        for (int n = 0; n < (turns & 3); n++) {
            int facing = meta & 3;
            int next = facing == 0 ? 2 : facing == 2 ? 1 : facing == 1 ? 3 : 0;
            meta = meta & 4 | next;
        }
        return meta;
    }
}
