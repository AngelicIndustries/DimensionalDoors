package org.dimdev.dimdoors.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class DoorVariantListTest {

    @Test
    public void readsDoorsAndKinds() {
        List<String> problems = new ArrayList<String>();
        Map<String, DoorKind> doors = DoorVariantList.parse(
            new String[] { "etfuturum:door_spruce=warp", " etfuturum:crimson_door = Nether_Dungeon ",
                "dimdoors:Quartz Door=personal", "Growthcraft|Bamboo:grc.bambooDoor=pocket" },
            problems);
        assertEquals(Arrays.asList(), problems);
        assertEquals(
            Arrays.asList(
                "etfuturum:door_spruce",
                "etfuturum:crimson_door",
                "dimdoors:Quartz Door",
                "Growthcraft|Bamboo:grc.bambooDoor"),
            new ArrayList<String>(doors.keySet()));
        assertEquals(DoorKind.WARP, doors.get("etfuturum:door_spruce"));
        assertEquals(DoorKind.NETHER_DUNGEON, doors.get("etfuturum:crimson_door"));
        assertEquals(DoorKind.PERSONAL, doors.get("dimdoors:Quartz Door"));
        assertEquals(DoorKind.POCKET, doors.get("Growthcraft|Bamboo:grc.bambooDoor"));
    }

    @Test
    public void skipsBadEntries() {
        List<String> problems = new ArrayList<String>();
        Map<String, DoorKind> doors = DoorVariantList.parse(
            new String[] { "", "door_spruce=warp", "etfuturum:door_spruce", "etfuturum:door_birch=portal",
                "etfuturum:=warp", "etfuturum:door_jungle=dungeon", "etfuturum:door_jungle=warp" },
            problems);
        assertEquals(1, doors.size());
        assertEquals(DoorKind.DUNGEON, doors.get("etfuturum:door_jungle"));
        assertEquals(5, problems.size());
        assertTrue(
            problems.get(4)
                .contains("keeps dungeon"));
    }

    @Test
    public void defaultsAreValid() {
        List<String> problems = new ArrayList<String>();
        Map<String, DoorKind> doors = DoorVariantList.parse(DoorVariantList.DEFAULTS, problems);
        assertEquals(Arrays.asList(), problems);
        assertEquals(DoorVariantList.DEFAULTS.length, doors.size());
        assertEquals(DoorKind.POCKET, doors.get("minecraft:iron_door"));
        assertEquals(DoorKind.WARP, doors.get("minecraft:wooden_door"));
        assertEquals(DoorKind.PERSONAL, doors.get("dimdoors:Quartz Door"));
        assertEquals(DoorKind.GOLDEN, doors.get("dimdoors:Golden Door"));
        assertEquals(DoorKind.DUNGEON, doors.get("etfuturum:waxed_oxidized_copper_door"));
        assertEquals(DoorKind.NETHER_DUNGEON, doors.get("etfuturum:warped_door"));
    }

    @Test
    public void variantNamesAreSafeRegistryNames() {
        assertEquals(
            "warp_door.etfuturum.door_spruce",
            DoorVariantList.variantName("etfuturum:door_spruce", DoorKind.WARP));
        assertEquals(
            "nether_dungeon_door.Growthcraft_Bamboo.grc.bambooDoor",
            DoorVariantList.variantName("Growthcraft|Bamboo:grc.bambooDoor", DoorKind.NETHER_DUNGEON));
        assertEquals(
            "pocket_door.dimdoors.Quartz_Door",
            DoorVariantList.variantName("dimdoors:Quartz Door", DoorKind.POCKET));
    }
}
