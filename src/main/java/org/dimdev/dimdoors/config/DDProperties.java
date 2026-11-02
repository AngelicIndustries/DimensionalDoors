package org.dimdev.dimdoors.config;

import java.io.File;

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
    public final boolean RiftsSpawnEndermenEnabled;
    public final boolean LimboEnabled;
    public final boolean HardcoreLimboEnabled;
    public final boolean LimboDropsInventory;
    public final boolean SeamlessLimboDeath;
    public final boolean HungerInLimbo;
    public final boolean DarkLimbo;
    public final float LimboAmbientLight;
    public final boolean FadebloomsEnabled;
    public final int FadebloomLight;
    public final int FadebloomHeldLight;
    public final int FadebloomGlow;
    public final int FadebloomPatches;
    public final boolean RebuildDoorsAtRifts;
    public final int MonolithAggroRange;
    public final boolean LimboReturnsInventoryEnabled;
    public final boolean DoorRenderingEnabled;
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

        RiftGriefingEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Rift Griefing",
                true,
                "Sets whether rifts destroy blocks around them or not")
            .getBoolean(true);
        RiftSpreadEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Rift Spread",
                true,
                "Sets whether rifts create more rifts when they are near other rifts")
            .getBoolean(true);
        RiftsSpawnEndermenEnabled = config
            .get(
                Configuration.CATEGORY_GENERAL,
                "Enable Endermen Spawning from Rifts",
                true,
                "Sets whether groups of connected rifts will spawn Endermen")
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
        LimboAmbientLight = (float) config.get(
            Configuration.CATEGORY_GENERAL,
            "Limbo Ambient Light",
            0.0,
            "With Dark Limbo and no shader pack: how far all light in Limbo is lifted toward white, evenly in grey, from 0 "
                + "(unlit places are black) to 1 (full light everywhere). Shader packs light Limbo their own way.",
            0.0,
            1.0)
            .getDouble();
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
                "How close, in blocks, a player has to be for a Monolith to notice them.",
                8,
                128)
            .getInt();
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
