package org.dimdev.dimdoors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentData;
import net.minecraft.init.Items;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MathHelper;
import net.minecraft.util.WeightedRandom;
import net.minecraft.util.WeightedRandomChestContent;
import net.minecraftforge.common.ChestGenHooks;

import org.dimdev.dimdoors.config.DDProperties;
import org.dimdev.dimdoors.dungeon.DungeonData;
import org.dimdev.dimdoors.mixins.AccessorChestGenHooks;

/**
 * Dungeon chest loot, from Forge loot categories that TooMuchLoot or MineTweaker can change: dimensionalDungeonChest,
 * and dimensionalGraveChest for the remains of someone who died there. Either can be set for one dungeon pack or one
 * dungeon by adding a category named after it, and the most specific one that exists is used. The loot section of the
 * config tells packmakers the same.
 */
public class DDLoot {

    public static final String DUNGEON_CHEST = "dimensionalDungeonChest";
    public static final String GRAVE_CHEST = "dimensionalGraveChest";
    /** An NBT tag on a loot item that stands for something made as the chest is filled. */
    public static final String LOOT_TAG = "DimDoorsLoot";
    /** A Transcendent or Trepidation book: Forge would turn an enchanted book entry into a random enchantment. */
    private static final String ENCHANTED_BOOK = "enchantedBook";
    /** The head of a player who died in a pocket dimension, or of a Dimensional Doors author. */
    private static final String GRAVE_HEAD = "graveHead";

    private static final String[] SPECIAL_SKULL_OWNERS = new String[] { "stevenrs11", "kamikazekiwi3", "fbt", "Jaitsu",
        "XCompWiz", "skyboy026", "Wylker" };
    private static final int PLAYER_SKULL_METADATA = 3;

    private static final double MIN_ITEM_DAMAGE = 0.3;
    private static final double MAX_ITEM_DAMAGE = 0.9;
    private static final int ITEM_ENCHANTMENT_CHANCE = 50;
    private static final int MAX_ITEM_ENCHANTMENT_CHANCE = 100;
    private static final int SPECIAL_SKULL_CHANCE = 20;
    private static final int MAX_SPECIAL_SKULL_CHANCE = 100;

    private static final int DUNGEON_CHEST_DRAWS = 6;
    private static final int MIN_GRAVE_CHEST_DRAWS = 7, MAX_GRAVE_CHEST_DRAWS = 10;

    private DDLoot() {}

    /** Sets up the base categories; other mods and loot tools change them from here. */
    public static void registerInfo(DDProperties properties) {
        ChestGenHooks dungeon = ChestGenHooks.getInfo(DUNGEON_CHEST);
        dungeon.setMin(DUNGEON_CHEST_DRAWS);
        dungeon.setMax(DUNGEON_CHEST_DRAWS);
        add(dungeon, true, Items.iron_ingot, 160, 1, 3);
        add(dungeon, true, Items.coal, 120, 1, 3);
        add(dungeon, true, Items.quartz, 120, 1, 3);
        add(dungeon, true, Items.book, 100, 1, 1);
        add(dungeon, true, Items.gold_ingot, 80, 1, 3);
        add(dungeon, true, Items.diamond, 40, 1, 2);
        add(dungeon, true, Items.emerald, 20, 1, 2);
        add(dungeon, true, Items.golden_apple, 10, 1, 1);
        add(dungeon, properties.FabricOfRealityLootEnabled, Item.getItemFromBlock(DimDoors.blockDimWall), 20, 16, 64);
        add(dungeon, properties.WorldThreadLootEnabled, DimDoors.itemWorldThread, 80, 2, 12);
        addWeighted(dungeon, DimDoors.itemRiftSignature, properties.RiftSignatureLootWeight);
        addWeighted(dungeon, DimDoors.itemStabilizedRiftSignature, properties.StabilizedRiftSignatureLootWeight);
        addWeighted(dungeon, DimDoors.itemPersonalDoor, properties.PersonalDimDoorLootWeight);
        // One of the three discs, the weight split between them
        int discWeight = (properties.MusicDiscLootWeight + 2) / 3;
        addWeighted(dungeon, DimDoors.itemRecordCreepy, discWeight);
        addWeighted(dungeon, DimDoors.itemRecordTheyStareBack, discWeight);
        addWeighted(dungeon, DimDoors.itemRecordWhiteVoid, discWeight);
        if (properties.LootEnchantmentBookWeight > 0
            && (DimDoors.transcendent != null || DimDoors.trepidation != null)) {
            dungeon.addItem(
                new WeightedRandomChestContent(
                    special(new ItemStack(Items.book), ENCHANTED_BOOK),
                    1,
                    1,
                    properties.LootEnchantmentBookWeight));
        }

        // What someone who died here left: their remains, a few to each draw so they lie scattered, and some of their
        // gear and belongings. Weights out of about 875 give graves the odds their fixed rolls used to have.
        ChestGenHooks grave = ChestGenHooks.getInfo(GRAVE_CHEST);
        grave.setMin(MIN_GRAVE_CHEST_DRAWS);
        grave.setMax(MAX_GRAVE_CHEST_DRAWS);
        add(grave, true, Items.bone, 250, 1, 2);
        add(grave, true, Items.rotten_flesh, 200, 1, 2);
        add(grave, true, Items.iron_pickaxe, 30, 1, 1);
        add(grave, true, Items.bow, 30, 1, 1);
        add(grave, true, Items.arrow, 30, 8, 32);
        add(grave, true, Items.iron_sword, 20, 1, 1);
        add(grave, true, Items.stone_sword, 10, 1, 1);
        add(grave, properties.RiftBladeLootEnabled, DimDoors.itemRiftBlade, 10, 1, 1);
        add(grave, true, Items.iron_helmet, 25, 1, 1);
        add(grave, true, Items.chainmail_helmet, 10, 1, 1);
        add(grave, true, Items.iron_chestplate, 25, 1, 1);
        add(grave, true, Items.chainmail_chestplate, 10, 1, 1);
        add(grave, true, Items.iron_leggings, 25, 1, 1);
        add(grave, true, Items.chainmail_leggings, 10, 1, 1);
        add(grave, true, Items.iron_boots, 25, 1, 1);
        add(grave, true, Items.chainmail_boots, 10, 1, 1);
        add(grave, true, Items.name_tag, 40, 1, 1);
        add(grave, true, Items.glass_bottle, 35, 1, 1);
        add(grave, true, Items.ender_pearl, 30, 1, 1);
        add(grave, true, Items.ghast_tear, 30, 1, 1);
        add(grave, true, Items.record_11, 5, 1, 1);
        grave.addItem(
            new WeightedRandomChestContent(
                special(new ItemStack(Items.skull, 1, PLAYER_SKULL_METADATA), GRAVE_HEAD),
                1,
                1,
                5));
    }

    private static void addWeighted(ChestGenHooks category, Item item, int weight) {
        add(category, weight > 0, item, weight, 1, 1);
    }

    private static void add(ChestGenHooks category, boolean include, Item item, int weight, int min, int max) {
        if (include) category.addItem(new WeightedRandomChestContent(item, 0, min, max, weight));
    }

    private static ItemStack special(ItemStack stack, String kind) {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString(LOOT_TAG, kind);
        stack.setTagCompound(tag);
        return stack;
    }

    /**
     * The loot category for a chest in a dungeon: the dungeon's own, else its pack's, else the base one. One that
     * exists is used even if it holds nothing, so a packmaker can leave a dungeon's chests empty.
     */
    static ChestGenHooks category(String base, DungeonData dungeon) {
        if (dungeon != null && dungeon.dungeonType().Owner != null) {
            String pack = base + "."
                + dungeon.dungeonType().Owner.getName()
                    .toLowerCase(Locale.ROOT);
            Map<String, ChestGenHooks> categories = AccessorChestGenHooks.getChestInfo();
            ChestGenHooks own = categories.get(pack + "." + dungeon.schematicName());
            if (own != null) return own;
            ChestGenHooks packs = categories.get(pack);
            if (packs != null) return packs;
        }
        return ChestGenHooks.getInfo(base);
    }

    /** Fills an empty chest in a dungeon (null for one built outside any): a grave chest some of the time. */
    public static void fillChest(IInventory inventory, Random random, DDProperties properties, DungeonData dungeon) {
        boolean grave = random.nextInt(100) < properties.GraveChestChance;
        ChestGenHooks loot = category(grave ? GRAVE_CHEST : DUNGEON_CHEST, dungeon);
        WeightedRandomChestContent[] content = loot.getItems(random);
        List<ItemStack> stacks = new ArrayList<ItemStack>();
        if (content.length > 0) {
            int draws = loot.getCount(random);
            for (int k = 0; k < draws; k++) {
                WeightedRandomChestContent draw = (WeightedRandomChestContent) WeightedRandom
                    .getRandomItem(random, content);
                // Asked again for each draw, so that enchanted books don't all come out alike
                draw = draw.theItemId.getItem()
                    .getChestGenBase(loot, random, draw);
                if (draw == null) continue;
                for (ItemStack stack : ChestGenHooks.generateStacks(
                    random,
                    draw.theItemId,
                    draw.theMinimumChanceToGenerateItem,
                    draw.theMaximumChanceToGenerateItem)) {
                    ItemStack made = make(stack, random, grave);
                    if (made != null) stacks.add(made);
                }
            }
        }
        scatter(stacks, inventory, random);
    }

    /** A drawn stack as it goes in the chest: special items made, and a grave's gear worn. Null leaves it out. */
    private static ItemStack make(ItemStack stack, Random random, boolean grave) {
        NBTTagCompound tag = stack.getTagCompound();
        String kind = tag != null ? tag.getString(LOOT_TAG) : "";
        if (kind.equals(ENCHANTED_BOOK)) return enchantedBook(random);
        if (kind.equals(GRAVE_HEAD)) return graveHead(random);
        if (grave && stack.isItemStackDamageable()) wear(stack, random);
        return stack;
    }

    /** Puts the stacks in the chest in a spread-out order, without putting one on another. */
    private static void scatter(List<ItemStack> stacks, IInventory inventory, Random random) {
        // The prime number below is used for choosing chest slots in a seemingly-random pattern. Its value
        // was selected specifically to achieve a spread-out distribution for chests with up to 104 slots.
        // Choosing a prime number ensures that our increments are relatively-prime to the chest size, which
        // means we'll cover all the slots before repeating any. This is mathematically guaranteed.
        final int primeOffset = 239333;

        int size = inventory.getSizeInventory();
        for (ItemStack item : stacks) {
            int limit = size;
            int index = random.nextInt(size);

            while (limit > 0 && inventory.getStackInSlot(index) != null) {
                limit--;
                index = (index + primeOffset) % size;
            }
            // A full chest takes no more, rather than losing what it holds
            if (limit == 0) return;

            inventory.setInventorySlotContents(index, item);
        }
    }

    /** Wears gear down, and half the time gives it one enchantment it could have, at the lowest level. */
    private static void wear(ItemStack stack, Random random) {
        stack.setItemDamage(
            (int) (stack.getMaxDamage() * MathHelper.getRandomDoubleInRange(random, MIN_ITEM_DAMAGE, MAX_ITEM_DAMAGE)));
        if (stack.isItemEnchanted() || random.nextInt(MAX_ITEM_ENCHANTMENT_CHANCE) >= ITEM_ENCHANTMENT_CHANCE) return;
        List<Enchantment> fitting = new ArrayList<Enchantment>();
        for (Enchantment enchantment : Enchantment.enchantmentsList) {
            // Transcendent and Trepidation only come as books
            if (enchantment != null && enchantment.canApply(stack)
                && enchantment != DimDoors.transcendent
                && enchantment != DimDoors.trepidation) {
                fitting.add(enchantment);
            }
        }
        if (!fitting.isEmpty()) stack.addEnchantment(fitting.get(random.nextInt(fitting.size())), 1);
    }

    private static ItemStack enchantedBook(Random random) {
        List<Enchantment> pool = new ArrayList<Enchantment>();
        if (DimDoors.transcendent != null) pool.add(DimDoors.transcendent);
        if (DimDoors.trepidation != null) pool.add(DimDoors.trepidation);
        if (pool.isEmpty()) return null;
        Enchantment enchantment = pool.get(random.nextInt(pool.size()));
        ItemStack book = new ItemStack(Items.enchanted_book);
        Items.enchanted_book
            .addEnchantment(book, new EnchantmentData(enchantment, 1 + random.nextInt(enchantment.getMaxLevel())));
        return book;
    }

    private static ItemStack graveHead(Random random) {
        DeathTracker deathTracker = DimDoors.deathTracker;
        String skullOwner;
        if (deathTracker.isEmpty() || (random.nextInt(MAX_SPECIAL_SKULL_CHANCE) < SPECIAL_SKULL_CHANCE)) {
            skullOwner = SPECIAL_SKULL_OWNERS[random.nextInt(SPECIAL_SKULL_OWNERS.length)];
        } else {
            skullOwner = deathTracker.getRandomUsername(random);
        }
        ItemStack skull = new ItemStack(Items.skull, 1, PLAYER_SKULL_METADATA);
        skull.stackTagCompound = new NBTTagCompound();
        skull.stackTagCompound.setString("SkullOwner", skullOwner);
        return skull;
    }
}
