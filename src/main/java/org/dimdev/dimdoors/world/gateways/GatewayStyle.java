package org.dimdev.dimdoors.world.gateways;

import net.minecraft.block.Block;

/**
 * The block sets gateways are built in: 6.x's styles, plus some from other mods of the pack. A style whose blocks
 * aren't there (the mod is missing or has them switched off) falls back to another, in the end to stone bricks.
 */
public enum GatewayStyle {

    STONE_BRICK("enclosed_west", "two_pillars", null, "minecraft:stonebrick", null, "minecraft:stonebrick@3",
        "minecraft:stone_brick_stairs", null, null),
    MOSSY("enclosed_west", "two_pillars", STONE_BRICK, "minecraft:stonebrick@1", null, "minecraft:stonebrick@3",
        "etfuturum:mossy_stone_brick_stairs", null, null),
    SANDSTONE("enclosed_east", "pillars_torches", STONE_BRICK, "minecraft:sandstone", null, "minecraft:sandstone@1",
        "minecraft:sandstone_stairs", null, "minecraft:torch@5"),
    RED_SANDSTONE("enclosed_east", "pillars_lanterns", SANDSTONE, "etfuturum:red_sandstone", null,
        "etfuturum:red_sandstone@1", "etfuturum:red_sandstone_stairs", null, "etfuturum:lantern"),
    MUD_BOP("enclosed_mud", "two_pillars", STONE_BRICK, "BiomesOPlenty:mudBricks", null, null,
        "BiomesOPlenty:mudBricksStairs", "BiomesOPlenty:mud", null),
    MUD("enclosed_mud", "two_pillars", MUD_BOP, "etfuturum:packed_mud@1", null, null, "etfuturum:mud_brick_stairs",
        "etfuturum:mud", null),
    PACKED_ICE("two_pillars", "two_pillars", STONE_BRICK, "minecraft:packed_ice", null, null, null, null, null),
    ICE("two_pillars", "two_pillars", PACKED_ICE, "etfuturum:blue_ice", null, null, null, null, null),
    QUARTZ("enclosed_east", "two_pillars", STONE_BRICK, "minecraft:quartz_block", "minecraft:quartz_block@2",
        "minecraft:quartz_block@1", "minecraft:quartz_stairs", null, null),
    PRISMARINE("enclosed_east", "two_pillars", STONE_BRICK, "etfuturum:prismarine_block@1", null,
        "etfuturum:prismarine_block@2", "etfuturum:prismarine_stairs_brick", null, null),
    TUFF("enclosed_west", "two_pillars", STONE_BRICK, "etfuturum:tuff@2", null, "etfuturum:tuff@4",
        "etfuturum:tuff_brick_stairs", null, null),
    BLACKSTONE("enclosed_west", "two_pillars", STONE_BRICK, "etfuturum:blackstone@2", null, "etfuturum:blackstone@4",
        "etfuturum:polished_blackstone_brick_stairs", null, null),
    AETHER("enclosed_west", "two_pillars", STONE_BRICK, "aether_legacy:holystone_brick", "aether_legacy:pillar",
        "aether_legacy:pillar_top", "aether_legacy:holystone_brick_stairs", null, null),
    // The Twilight Forest's own stones. It has no stairs for them, so the steps are full blocks.
    MAZESTONE("enclosed_west", "two_pillars", STONE_BRICK, "TwilightForest:tile.TFMazestone@1", null,
        "TwilightForest:tile.TFMazestone@2", null, null, null),
    MOSSY_MAZESTONE("enclosed_west", "two_pillars", MAZESTONE, "TwilightForest:tile.TFMazestone@5", null,
        "TwilightForest:tile.TFMazestone@2", null, null, null),
    CASTLE_BRICK("enclosed_east", "two_pillars", STONE_BRICK, "TwilightForest:tile.CastleBrick",
        "TwilightForest:tile.CastleBrick@1", "TwilightForest:tile.CastleBrick@3", null, null, null),
    DEADROCK("enclosed_east", "two_pillars", STONE_BRICK, "TwilightForest:tile.TFDeadrock@2",
        "TwilightForest:tile.TFDeadrock", "TwilightForest:tile.TFDeadrock@1", null, null, null),
    TOWERWOOD("enclosed_west", "two_pillars", STONE_BRICK, "TwilightForest:tile.TFTowerStone",
        "TwilightForest:tile.TFTowerStone@1", "TwilightForest:tile.TFTowerStone@1", null, null, null),
    AURORA("enclosed_east", "two_pillars", QUARTZ, "TwilightForest:tile.TFAuroraBrick",
        "TwilightForest:tile.AuroraPillar", "TwilightForest:tile.AuroraPillar", null, null, null);

    private final String enclosed;
    private final String pillars;
    private final GatewayStyle fallback;
    private final String[] blocks = new String[8];
    private Materials resolved;
    private boolean checked;

    GatewayStyle(String enclosed, String pillars, GatewayStyle fallback, String base, String column, String accent,
        String stairs, String mud, String light) {
        this.enclosed = enclosed;
        this.pillars = pillars;
        this.fallback = fallback;
        blocks[GatewayTemplate.BASE] = base;
        blocks[GatewayTemplate.COLUMN] = column != null ? column : base;
        blocks[GatewayTemplate.ACCENT] = accent != null ? accent : base;
        blocks[GatewayTemplate.STAIRS] = stairs;
        blocks[GatewayTemplate.MUD] = mud != null ? mud : base;
        blocks[GatewayTemplate.LIGHT] = light != null ? light : "minecraft:torch@5";
    }

    /** The snowy enclosed gateway is quartz, as on 6.x; its pillars are 6.x's blue ice ones. */
    public GatewayStyle pillarStyle() {
        return this == QUARTZ ? ICE : this;
    }

    public GatewayTemplate template(boolean enclosed) {
        return GatewayTemplate.get(enclosed ? this.enclosed : pillarStyle().pillars);
    }

    /** This style's blocks, or its fallback's when some are missing. */
    public synchronized Materials materials() {
        if (!checked) {
            resolved = Materials.resolve(blocks);
            checked = true;
        }
        if (resolved != null) return resolved;
        return fallback != null ? fallback.materials() : null;
    }

    /** The blocks for each role of a template. */
    public static final class Materials {

        private final Block[] blocks = new Block[8];
        private final int[] metas = new int[8];

        private Materials() {}

        static Materials resolve(String[] names) {
            Materials materials = new Materials();
            for (int role = 0; role < names.length; role++) {
                if (names[role] == null) continue;
                String[] parts = names[role].split("@");
                if (!Block.blockRegistry.containsKey(parts[0])) return null;
                materials.blocks[role] = (Block) Block.blockRegistry.getObject(parts[0]);
                materials.metas[role] = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            }
            return materials;
        }

        public Block block(int role) {
            // A style without stairs (ice) uses its base block for them
            return blocks[role] != null ? blocks[role] : blocks[GatewayTemplate.BASE];
        }

        public boolean hasStairs() {
            return blocks[GatewayTemplate.STAIRS] != null;
        }

        public int meta(int role) {
            return blocks[role] != null ? metas[role] : metas[GatewayTemplate.BASE];
        }
    }
}
