package org.dimdev.dimdoors.config;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraftforge.common.config.Configuration;

import org.dimdev.dimdoors.blocks.BlockRift;
import org.dimdev.dimdoors.helpers.LimboEscape;
import org.dimdev.dimdoors.ticking.CustomLimboPopulator;
import org.dimdev.dimdoors.world.fortresses.FortressGateways;
import org.dimdev.dimdoors.world.gateways.GatewayGenerator;

public class DDProperties {

    /**
     * Other IDs
     */

    public final int LimboBiomeID;
    public final int PocketBiomeID;
    public final int LimboDimensionID;
    public final int PublicPocketDimensionID;
    public final int DungeonPocketDimensionID;
    public final int PersonalPocketDimensionID;
    public final int LimboProviderID;
    public final int PocketProviderID;
    public final int PersonalPocketProviderID;
    public final int DoorRenderEntityID;
    public final int MonolithEntityID;
    public final int FallingDripstoneEntityID;
    public final int TranscendentEnchantmentID;
    public final int TrepidationEnchantmentID;
    public final String[] TrepidationMobs;
    public final int LootEnchantmentBookWeight;
    public final int MusicDiscLootWeight;

    /**
     * Crafting Flags
     */

    public final boolean CraftingDimensionalDoorAllowed;
    public final boolean CraftingWarpDoorAllowed;
    public final boolean CraftingRiftSignatureAllowed;
    public final boolean CraftingRiftRemoverAllowed;
    public final boolean CraftingUnstableDoorAllowed;
    public final boolean CraftingRiftBladeAllowed;
    public final boolean CraftingTransTrapdoorAllowed;
    public final boolean CraftingStabilizedRiftSignatureAllowed;
    public final boolean CraftingRiftStabilizerAllowed;
    public final boolean CraftingLimboMaterialsAllowed;
    public final boolean CraftingStableFabricAllowed;
    public final boolean CraftingGoldenDimensionalDoorAllowed;
    public final boolean CraftingGoldenDoorAllowed;
    public final boolean CraftingDDKeysAllowed;
    public final boolean CraftingQuartzDoorAllowed;
    public final boolean CraftingPersonalDimDoorAllowed;
    public final boolean CraftingDungeonDoorsAllowed;

    /**
     * Loot Flags
     */

    public final boolean RiftBladeLootEnabled;
    public final boolean FabricOfRealityLootEnabled;
    public final boolean WorldThreadLootEnabled;
    public final int RiftSignatureLootWeight;
    public final int StabilizedRiftSignatureLootWeight;
    public final int PersonalDimDoorLootWeight;

    /**
     * Other Flags
     */

    public final boolean RiftSpreadEnabled;
    public final boolean RiftGriefingEnabled;
    public final double RiftGrowthSpeed;
    public final int MaxRiftRadius;
    public final boolean RiftsSpawnEndermenEnabled;
    public final boolean LimboEnabled;
    public final boolean HardcoreLimboEnabled;
    public final boolean LimboDropsInventory;
    public final boolean SeamlessLimboDeath;
    public final boolean HungerInLimbo;
    public final boolean DarkLimbo;
    public final boolean ShowRiftCore;
    public final int HighlightRiftCoreFor;
    public final double RiftSize;
    public final double RiftJitter;
    public final double RiftCoreJitter;
    public final float LimboAmbientLight;
    public final boolean FadebloomsEnabled;
    public final int FadebloomLight;
    public final int FadebloomHeldLight;
    public final int FadebloomGlow;
    public final int FadebloomPatches;
    public final boolean RebuildDoorsAtRifts;
    public final int MonolithAggroRange;
    public final int MonolithNoticeRange;
    public final double MonolithAggroScreenEffect;
    public final double LimboFogDensity;
    public static final String LIMBO_SHADER_FOG = "Limbo Shader Fog";
    /** Limbo's fog strength under each shader pack, by pack name; /limbofog adds to it. */
    public final Map<String, Double> LimboShaderFog = new LinkedHashMap<String, Double>();
    public static final String LIMBO_SHADER_LIGHT = "Limbo Shader Light";
    /** Limbo's even light under each shader pack, by pack name; /limbofog adds to it. */
    public final Map<String, Double> LimboShaderLight = new LinkedHashMap<String, Double>();
    /** These settings as loaded, comments and all, for the in-game tuners that save to them. */
    public final Configuration Config;
    public final boolean LimboReturnsInventoryEnabled;
    public final boolean DoorRenderingEnabled;
    public final double PortalRoundness;
    public final double PortalEdgeRoughness;
    public final double PortalSpinSpeed;
    public final boolean TNFREAKINGT_Enabled;
    public final boolean MonolithTeleportationEnabled;
    public final boolean DangerousLimboMonolithsDisabled;
    public final boolean UniversalLimboInNewWorlds;
    public final String LimboEscapeDestination;
    public final boolean LimboEscapeUnravelsLanding;
    public final int ExitSearchRadius;

    /**
     * Other
     */

    public final int NonTntWeight;
    public final int ClusterGenerationChance;
    public final int GatewayGenerationChance;
    public final int FortressGatewayGenerationChance;
    public final String[] DoorVariants;
    public final int MonolithSpawningChance;
    public final int WorldThreadDropChance;
    public final int LimboEntryRange;
    public final int LimboReturnRange;
    public final int WorldThreadRequirementLevel;
    public final String CustomSchematicDirectory;

    // Singleton instance
    private static DDProperties instance = null;
    // Path for custom dungeons within configuration directory
    private final String CUSTOM_SCHEMATIC_SUBDIRECTORY = "/DimDoors_Custom_schematics";
    // Names of categories
    private final String CATEGORY_CRAFTING = "crafting";
    private final String CATEGORY_ENTITY = "entity";
    private final String CATEGORY_DIMENSION = "dimension";
    private final String CATEGORY_PROVIDER = "provider";
    private final String CATEGORY_BIOME = "biome";
    private final String CATEGORY_LOOT = "loot";
    private final String CATEGORY_DOOR_VARIANTS = "door_variants";
    private final String CATEGORY_ENCHANTMENTS = "enchantments";

    /** Reads a list of pack name=value lines into values, leaving out lines that aren't. */
    private static void readPackValues(Configuration config, String key, String comment, Map<String, Double> values) {
        for (String entry : config.get(Configuration.CATEGORY_GENERAL, key, new String[0], comment)
            .getStringList()) {
            int split = entry.lastIndexOf('=');
            if (split <= 0) continue;
            try {
                values.put(entry.substring(0, split), Double.parseDouble(entry.substring(split + 1)));
            } catch (NumberFormatException e) {
                // Left out
            }
        }
    }

    private int lootWeight(Configuration config, String item, int weight) {
        return config
            .get(
                CATEGORY_LOOT,
                item + " Loot Weight",
                weight,
                "How often dungeon chests hold a " + item
                    + ", against 750 for all other loot together. 0 leaves it out. The default is "
                    + weight
                    + ".",
                0,
                1000)
            .getInt();
    }

    private DDProperties(File configFile) {
        // Load the configuration. This must be done in the constructor, even though I'd rather have a separate
        // function, because "blank final" variables must be initialized within the constructor.

        CustomSchematicDirectory = configFile.getParent() + CUSTOM_SCHEMATIC_SUBDIRECTORY;
        Configuration config = new Configuration(configFile);
        Config = config;
        config.load();

        CraftingDimensionalDoorAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Dimensional Door", true)
            .getBoolean(true);
        CraftingWarpDoorAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Warp Door", true)
            .getBoolean(true);
        CraftingUnstableDoorAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Unstable Door", true)
            .getBoolean(true);
        CraftingTransTrapdoorAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Transdimensional Trapdoor", true)
            .getBoolean(true);
        CraftingRiftSignatureAllowed = config
            .get(CATEGORY_CRAFTING, "Allow Crafting Rift Signature", false, "Off by default: Rift Signatures are loot.")
            .getBoolean(false);
        CraftingRiftRemoverAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Rift Remover", true)
            .getBoolean(true);
        CraftingRiftStabilizerAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Rift Stabilizer", true)
            .getBoolean(true);
        CraftingLimboMaterialsAllowed = config
            .get(
                CATEGORY_CRAFTING,
                "Allow Crafting Limbo Materials",
                true,
                "Liminal Lint, Enduring Fibers, Fabric of Finality")
            .getBoolean(true);
        CraftingStabilizedRiftSignatureAllowed = config
            .get(
                CATEGORY_CRAFTING,
                "Allow Crafting Stabilized Rift Signature",
                false,
                "Off by default: Stabilized Rift Signatures are loot.")
            .getBoolean(false);
        CraftingRiftBladeAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Rift Blade", true)
            .getBoolean(true);
        CraftingStableFabricAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Stable Fabric", true)
            .getBoolean(true);
        CraftingGoldenDoorAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Golden Door", true)
            .getBoolean(true);
        CraftingGoldenDimensionalDoorAllowed = config
            .get(CATEGORY_CRAFTING, "Allow Crafting Golden Dimensional Door", true)
            .getBoolean(true);
        CraftingDDKeysAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Rift Keys", true)
            .getBoolean(true);
        CraftingQuartzDoorAllowed = config.get(CATEGORY_CRAFTING, "Allow Crafting Quartz Doors", true)
            .getBoolean(true);
        CraftingPersonalDimDoorAllowed = config
            .get(
                CATEGORY_CRAFTING,
                "Allow Crafting Personal Dim Doors",
                false,
                "Off by default: Personal Dimensional Doors are loot.")
            .getBoolean(false);
        CraftingDungeonDoorsAllowed = config
            .get(
                CATEGORY_CRAFTING,
                "Allow Crafting Dungeon Doors",
                true,
                "Lets door variants of the dungeon and nether_dungeon kinds be crafted.")
            .getBoolean(true);

        WorldThreadRequirementLevel = config.get(
            CATEGORY_CRAFTING,
            "World Thread Requirement Level",
            4,
            "Controls the amount of World Thread needed to craft Stable Fabric. The number must be an "
                + "integer from 1 to 4. The levels change the recipe to use 1, 2, 4, or 8 threads, respectively. The default level is 4.")
            .getInt();

        RiftBladeLootEnabled = config.get(CATEGORY_LOOT, "Enable Rift Blade Loot", true)
            .getBoolean(true);
        FabricOfRealityLootEnabled = config.get(CATEGORY_LOOT, "Enable Fabric of Reality Loot", true)
            .getBoolean(true);
        WorldThreadLootEnabled = config.get(CATEGORY_LOOT, "Enable World Thread Loot", true)
            .getBoolean(true);
        RiftSignatureLootWeight = lootWeight(config, "Rift Signature", 20);
        StabilizedRiftSignatureLootWeight = lootWeight(config, "Stabilized Rift Signature", 6);
        PersonalDimDoorLootWeight = lootWeight(config, "Personal Dimensional Door", 10);
        LootEnchantmentBookWeight = lootWeight(config, "Transcendent or Trepidation Book", 9);
        MusicDiscLootWeight = lootWeight(config, "Music Disc", 9);

        RiftGriefingEnabled = config.get(
            Configuration.CATEGORY_GENERAL,
            "Enable Rift Griefing",
            true,
            "Sets whether growing rifts unravel blocks around them, step by step (stone to cobblestone to gravel to sand to nothing). World Thread can drop when a block goes.")
            .getBoolean(true);
        RiftGrowthSpeed = config.get(
            Configuration.CATEGORY_GENERAL,
            "Rift Growth Speed",
            1.0,
            "How fast open rifts grow while their chunk is loaded, until stabilized, closed or a door is placed on them. The bigger a rift, the farther and more often it unravels blocks. 1 is the pace of the 1.12 rewrite: about 3 blocks around after an hour, about 15 after a day. 0 stops growth.",
            0.0,
            100.0)
            .getDouble(1.0);
        MaxRiftRadius = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Max Rift Radius",
                0,
                "How far around them open rifts can grow to unravel blocks, in blocks. 0 for no limit.",
                0,
                1000)
            .getInt(0);
        RiftSpreadEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Rift Spread",
                true,
                "Sets whether rifts create more rifts when they are near other rifts")
            .getBoolean(true);
        RiftsSpawnEndermenEnabled = config.get(
            Configuration.CATEGORY_GENERAL,
            "Enable Endermen Spawning from Rifts",
            true,
            "Sets whether groups of connected rifts, and single rifts that have grown for about an hour, spawn Endermen")
            .getBoolean(true);

        LimboEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Limbo",
                true,
                "Sets whether players are teleported to Limbo when they die in any pocket dimension")
            .getBoolean(true);
        LimboReturnsInventoryEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Limbo Returns Inventory",
                true,
                "Sets whether players keep their inventories upon dying and respawning in Limbo")
            .getBoolean(true);
        HardcoreLimboEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Hardcore Limbo",
                false,
                "Sets whether players that die in Limbo will respawn there")
            .getBoolean(false);
        LimboDropsInventory = config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Drops Inventory",
            true,
            "When Limbo doesn't return inventories: whether a player's items fall where they died, as in a normal "
                + "death (graves and similar mods get them), or are destroyed (false, as Dimensional Doors always did).")
            .getBoolean(true);
        SeamlessLimboDeath = config.get(
            Configuration.CATEGORY_GENERAL,
            "Seamless Limbo Death",
            true,
            "With Hardcore Limbo: dying in Limbo wakes the player up again in Limbo's sky, without the death screen. "
                + "Items are kept, dropped or destroyed as on the way into Limbo.")
            .getBoolean(true);
        HungerInLimbo = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Hunger in Limbo",
                false,
                "Sets whether players get hungry in Limbo. Off: food and saturation stay as they were on arrival, and "
                    + "health still regenerates.")
            .getBoolean(false);
        DarkLimbo = config.get(
            Configuration.CATEGORY_GENERAL,
            "Dark Limbo",
            true,
            "Sets whether Limbo is dark: a midnight without stars where unlit places are black and the Eternal Fabric "
                + "glows. Off: Limbo is evenly bright, as Dimensional Doors always had it.")
            .getBoolean(true);
        ShowRiftCore = config.get(
            Configuration.CATEGORY_GENERAL,
            "Show Rift Cores",
            false,
            "Sets whether open rifts always show their spinning core. Off: they show it for a while after a Rift Remover "
                + "or Rift Stabilizer misses, to help find them.")
            .getBoolean(false);
        HighlightRiftCoreFor = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Highlight Rift Cores For",
                15000,
                "How long open rifts show their core after a rift tool misses, in milliseconds.",
                0,
                600000)
            .getInt(15000);
        RiftSize = config.get(
            Configuration.CATEGORY_GENERAL,
            "Rift Crack Size",
            1.0,
            "How big the black cracks of open rifts are drawn; 1 reaches a block past the hollow a rift eats, 0 draws none.",
            0.0,
            10.0)
            .getDouble(1.0);
        RiftJitter = config.get(
            Configuration.CATEGORY_GENERAL,
            "Rift Crack Shaking",
            1.0,
            "How much the cracks of open rifts shake, in step with their size; 1 is a few percent of it, 0 keeps them still.",
            0.0,
            10.0)
            .getDouble(1.0);
        RiftCoreJitter = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Rift Core Shaking",
                1.0,
                "How much the spinning cores of open rifts shake; 1 is a little, 0 keeps them still.",
                0.0,
                10.0)
            .getDouble(1.0);
        LimboAmbientLight = (float) config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Ambient Light",
            0.0,
            "With Dark Limbo and no shader pack: how far all light in Limbo is lifted toward white, evenly in grey, from 0 "
                + "(unlit places are black) to 1 (full light everywhere). Shader packs light Limbo their own way.",
            0.0,
            1.0)
            .getDouble();
        LimboFogDensity = config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Fog Density",
            0.1,
            "How thick Limbo's black fog is: at 0.1 everything past 30 blocks is black, at 0.2 past 15. 0 for the old light fog.",
            0.0,
            1.0)
            .getDouble();
        readPackValues(
            config,
            LIMBO_SHADER_FOG,
            "How strong Limbo's fog is under each shader pack, as pack name=strength (/limbofog sets it), since packs "
                + "draw their own fog. At 1 the sky turns black and the fog closes at about 20 blocks in Complementary "
                + "and 15 in BSL; up to 2 it gets thicker, below 1 the sky shows through, 0 is the pack's own Limbo. "
                + "A name can be the start of the pack's, with more of it after a +: ComplementaryUnbound+EuphoriaPatches. "
                + "Packs not listed get 1.6.",
            LimboShaderFog);
        readPackValues(
            config,
            LIMBO_SHADER_LIGHT,
            "How much even light Limbo gets under each shader pack, as pack name=light (/limbofog sets it): the shader "
                + "side of Limbo Ambient Light, given to the pack as faint night vision. 0 to 1; packs not listed get "
                + "0.3. Names as for Limbo Shader Fog.",
            LimboShaderLight);
        FadebloomsEnabled = config.get(
            Configuration.CATEGORY_GENERAL,
            "Enable Fadeblooms",
            false,
            "Sets whether Fadeblooms exist: faintly glowing flowers that grow in Limbo and light up a little around "
                + "players who carry them. Off: they aren't registered or generated.")
            .getBoolean(false);
        FadebloomLight = config.get(
            Configuration.CATEGORY_GENERAL,
            "Fadebloom Light",
            1,
            "The light level a planted Fadebloom gives off. Eyes in the Shadows judges Fadeblooms by it, carried ones "
                + "too, so keep it low enough that it never keeps the eyes away.",
            0,
            15)
            .getInt();
        FadebloomHeldLight = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Fadebloom Held Light",
                14,
                "The light level around a player carrying a Fadebloom (Angelica's dynamic lights).",
                0,
                15)
            .getInt();
        FadebloomGlow = config.get(
            Configuration.CATEGORY_GENERAL,
            "Fadebloom Glow",
            8,
            "How brightly a Fadebloom's petals are drawn even in the dark, as a light level from 0 (lit like any plant) "
                + "to 15 (full brightness).",
            0,
            15)
            .getInt();
        FadebloomPatches = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Fadebloom Patches",
                2,
                "How many patches of one to four Fadeblooms a newly generated Limbo chunk tries to grow.",
                0,
                16)
            .getInt();
        RebuildDoorsAtRifts = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Rebuild Doors At Rifts",
                false,
                "Sets whether coming back to a bare rift (a gateway whose one-use door has vanished, for one) builds a "
                    + "matching door there, as Dimensional Doors always did. Off: the rift stays a rift, as on modern "
                    + "versions; place a door in it to use it again.")
            .getBoolean(false);
        LimboEntryRange = config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Entry Range",
            500,
            "Sets the farthest distance that players may be moved at random when sent to Limbo. Must be greater than or equal to 0.")
            .getInt();
        UniversalLimboInNewWorlds = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Universal Limbo In New Worlds",
                true,
                "The Enable Universal Limbo setting a new world starts with: whether dying in any dimension sends "
                    + "players to Limbo. Each world keeps its own setting in DimensionalDoors/DimDoorsWorld.cfg.")
            .getBoolean(true);
        LimboEscapeDestination = config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Escape Destination",
            LimboEscape.SURFACE,
            "Where walking on Eternal Fabric sends players: surface (the surface near their Limbo position, "
                + "within the Limbo Return Range), bed (their bed, or world spawn without one) or spawn (world spawn).",
            new String[] { LimboEscape.SURFACE, LimboEscape.BED, LimboEscape.SPAWN })
            .getString();
        LimboEscapeUnravelsLanding = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Limbo Escape Unravels Landing",
                false,
                "Sets whether a patch of Unraveled Fabric is left under players where they land after escaping Limbo")
            .getBoolean(false);
        ExitSearchRadius = config.get(
            Configuration.CATEGORY_GENERAL,
            "Exit Search Radius",
            8,
            "How far around its own column, in blocks, a dungeon or pocket exit looks for solid ground before it builds "
                + "a Fabric of Reality platform under its door. 0 searches the column only.",
            0,
            32)
            .getInt();
        LimboReturnRange = config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Return Range",
            500,
            "Sets the farthest distance that players may be moved at random when sent from Limbo to the Overworld. Must be greater than or equal to 0.")
            .getInt();
        DoorRenderingEnabled = config.get(Configuration.CATEGORY_GENERAL, "Enable Door Rendering", true)
            .getBoolean(true);
        PortalRoundness = config.get(
            Configuration.CATEGORY_GENERAL,
            "Portal Roundness",
            0.25,
            "The shape of door and trapdoor portals, from 0 (a rectangle) through rectangles with ever rounder corners "
                + "to 1 (an oval; a circle in a trapdoor). Client only.",
            0.0,
            1.0)
            .getDouble();
        PortalEdgeRoughness = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Portal Edge Roughness",
                1.0,
                "How fractal the edge of door and trapdoor portals is: 0 is smooth, 1 the usual bulges on bulges, more "
                    + "makes them bigger. Client only.",
                0.0,
                3.0)
            .getDouble();
        PortalSpinSpeed = config.get(
            Configuration.CATEGORY_GENERAL,
            "Portal Spin Speed",
            1.0,
            "How fast the edge of door and trapdoor portals turns, as a multiple of the usual pace (a turn in about "
                + "8 seconds); 0 keeps it still. Client only.",
            0.0,
            5.0)
            .getDouble();

        TNFREAKINGT_Enabled = config.get(Configuration.CATEGORY_GENERAL, "EXPLOSIONS!!???!!!?!?!!", false)
            .getBoolean(false);
        NonTntWeight = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "HOWMUCHTNT",
                25,
                "Weighs the chance that a block will not be TNT. Must be greater than or equal to 0. "
                    + "EXPLOSIONS must be set to true for this to have any effect.")
            .getInt();

        DoorRenderEntityID = config.get(CATEGORY_ENTITY, "Door Render Entity ID", 89)
            .getInt();
        MonolithEntityID = config.get(CATEGORY_ENTITY, "Monolith Entity ID", 125)
            .getInt();
        FallingDripstoneEntityID = config.get(CATEGORY_ENTITY, "Falling Dripstone Entity ID", 126)
            .getInt();

        config.setCategoryComment(
            CATEGORY_ENCHANTMENTS,
            "Transcendent (bows: arrows hit Endermen instead of them teleporting away) and Trepidation (chestplates: a "
                + "heartbeat when a hostile mob is within 5, 10 or 15 blocks). Both come only as books in dungeon chests.");
        TranscendentEnchantmentID = config
            .get(CATEGORY_ENCHANTMENTS, "Transcendent ID", 170, "Enchantment ID; -1 leaves Transcendent out.")
            .getInt();
        TrepidationEnchantmentID = config
            .get(CATEGORY_ENCHANTMENTS, "Trepidation ID", 171, "Enchantment ID; -1 leaves Trepidation out.")
            .getInt();
        TrepidationMobs = config
            .get(
                CATEGORY_ENCHANTMENTS,
                "Trepidation Mobs",
                new String[] { "*" },
                "Mobs Trepidation senses, by entity name: * is every mob the game counts as hostile, a name adds one "
                    + "and -name takes one out.")
            .getStringList();

        LimboDimensionID = config.get(CATEGORY_DIMENSION, "Limbo Dimension ID", -23)
            .getInt();
        PublicPocketDimensionID = config
            .get(
                CATEGORY_DIMENSION,
                "Public Pocket Dimension ID",
                -24,
                "The dimension that holds pockets, each in a cell of its own.")
            .getInt();
        DungeonPocketDimensionID = config
            .get(CATEGORY_DIMENSION, "Dungeon Pocket Dimension ID", -25, "The dimension that holds dungeon pockets.")
            .getInt();
        PersonalPocketDimensionID = config
            .get(CATEGORY_DIMENSION, "Personal Pocket Dimension ID", -26, "The dimension that holds personal pockets.")
            .getInt();
        PocketProviderID = config.get(CATEGORY_PROVIDER, "Pocket Provider ID", 124)
            .getInt();
        LimboProviderID = config.get(CATEGORY_PROVIDER, "Limbo Provider ID", 113)
            .getInt();
        PersonalPocketProviderID = config.get(CATEGORY_PROVIDER, "Personal Pocket Provider ID", 125)
            .getInt();

        MonolithTeleportationEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Monolith Teleportation",
                true,
                "Sets whether Monoliths can teleport players")
            .getBoolean(true);

        DangerousLimboMonolithsDisabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Docile Monoliths in Limbo",
                true,
                "Sets whether monoliths in Limbo stare at the player rather than attack")
            .getBoolean(true);

        MonolithAggroRange = config
            .get(
                CATEGORY_ENTITY,
                "Monolith Aggro Range",
                35,
                "How far, in blocks, a Monolith that has opened its eye keeps watching a player before letting go; its "
                    + "sounds reach about as far.",
                8,
                128)
            .getInt();
        MonolithNoticeRange = config.get(
            CATEGORY_ENTITY,
            "Monolith Notice Range",
            8,
            "How close, in blocks, a player in sight has to come for a Monolith to open its eye. Once its eye is open "
                + "it follows them out to the Monolith Aggro Range.",
            2,
            128)
            .getInt();
        MonolithAggroScreenEffect = config.get(
            CATEGORY_ENTITY,
            "Monolith Aggro Screen Effect",
            1.0,
            "How strongly the screen of a player a Monolith is about to take pulses red at the edges and breaks up in "
                + "red glitches, from 0 (not at all) to 1. It worsens the longer the Monolith has them. Client only.",
            0.0,
            1.0)
            .getDouble();
        MonolithSpawningChance = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Monolith Spawning Chance",
                28,
                "Sets the chance (out of " + CustomLimboPopulator.MAX_MONOLITH_SPAWNING_CHANCE
                    + ") that Monoliths will "
                    + "spawn in a given Limbo chunk. The default chance is 28.")
            .getInt();

        ClusterGenerationChance = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Cluster Generation Chance",
                2,
                "Sets the chance (out of " + GatewayGenerator.MAX_CLUSTER_GENERATION_CHANCE
                    + ") that a cluster of rifts will "
                    + "generate in a given chunk. The default chance is 2.")
            .getInt();

        GatewayGenerationChance = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Gateway Generation Chance",
                15,
                "Sets the chance (out of " + GatewayGenerator.MAX_GATEWAY_GENERATION_CHANCE
                    + ") that a Rift Gateway will "
                    + "generate in a given chunk. The default chance is 15.")
            .getInt();

        FortressGatewayGenerationChance = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Fortress Gateway Generation Chance",
                33,
                "Sets the chance (out of " + FortressGateways.MAX_GATEWAY_GENERATION_CHANCE
                    + ") that a Rift Gateway will "
                    + "generate as part of a Nether Fortress. The default chance is 33.")
            .getInt();

        DoorVariants = config.get(
            CATEGORY_DOOR_VARIANTS,
            "Door Variants",
            DoorVariantList.DEFAULTS,
            "Doors that get a dimensional variant, as block registry name=kind. The kinds are pocket (like the "
                + "Dimensional Door), warp (like the Warp Door), dungeon (opens a new dungeon), nether_dungeon (a "
                + "new dungeon from the Nether pack), personal (like the Personal Dimensional Door) and golden "
                + "(like the Golden Dimensional Door). A variant is crafted from door, Stable Fabric (an Ender Pearl "
                + "for warp ones), door when "
                + "the crafting option of its kind allows it, so personal ones aren't crafted by default. A plain door "
                + "placed under a rift becomes its variant, except for personal ones. The iron, wooden, quartz "
                + "and golden doors with their own kinds stand for our existing doors. Doors from mods that "
                + "aren't installed are skipped.")
            .getStringList();

        WorldThreadDropChance = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "World Thread Drop Chance",
                50,
                "Sets the chance (out of " + BlockRift.MAX_WORLD_THREAD_DROP_CHANCE
                    + ") that a rift will "
                    + "drop World Thread when it destroys a block. The default chance is 50.")
            .getInt();

        LimboBiomeID = config.get(CATEGORY_BIOME, "Limbo Biome ID", 148)
            .getInt();
        PocketBiomeID = config.get(CATEGORY_BIOME, "Pocket Biome ID", 147)
            .getInt();

        config.save();
    }

    public static DDProperties initialize(File configFile) {
        if (instance == null) instance = new DDProperties(configFile);
        else throw new IllegalStateException("Cannot initialize DDProperties twice");

        return instance;
    }

    public static DDProperties instance() {
        if (instance == null) {
            // This is to prevent some frustrating bugs that could arise when classes
            // are loaded in the wrong order. Trust me, I had to squash a few...
            throw new IllegalStateException("Instance of DDProperties requested before initialization");
        }
        return instance;
    }
}
