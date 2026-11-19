package com.sekwah.narutomod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Audit test verifying that all Phase C-1 village building blocks have required
 * blockstate, client item, item model, block model, texture, loot table, recipe,
 * and localization assets.
 */
public class BuildingBlocksAuditTest {

    private static final Path RESOURCES_DIR = Paths.get("src/main/resources");
    private static final Path ASSETS_DIR = RESOURCES_DIR.resolve("assets/narutomod");
    private static final Path DATA_DIR = RESOURCES_DIR.resolve("data/narutomod");

    private static final List<String> BUILDING_BLOCKS = List.of(
            "tatami",
            "tatami_slab",
            "shoji_panel",
            "stone_lantern"
    );

    private static final List<String> REQUIRED_TEXTURES = List.of(
            "tatami_top.png",
            "tatami_side.png",
            "shoji_panel.png",
            "shoji_panel_top.png",
            "stone_lantern.png"
    );

    @Test
    public void testBuildingBlockAssetsAndDataExist() throws IOException {
        JsonObject enUs = JsonParser.parseString(
                Files.readString(ASSETS_DIR.resolve("lang/en_us.json"))).getAsJsonObject();

        for (String id : BUILDING_BLOCKS) {
            Assertions.assertTrue(
                    Files.isRegularFile(ASSETS_DIR.resolve("blockstates/" + id + ".json")),
                    "Missing blockstate for " + id);
            Assertions.assertTrue(
                    Files.isRegularFile(ASSETS_DIR.resolve("items/" + id + ".json")),
                    "Missing 26.3 client item for " + id);
            Assertions.assertTrue(
                    Files.isRegularFile(ASSETS_DIR.resolve("models/item/" + id + ".json")),
                    "Missing item model for " + id);
            Assertions.assertTrue(
                    Files.isRegularFile(DATA_DIR.resolve("loot_table/blocks/" + id + ".json")),
                    "Missing block loot table for " + id);
            Assertions.assertTrue(
                    Files.isRegularFile(DATA_DIR.resolve("recipe/" + id + ".json")),
                    "Missing recipe for " + id);
            Assertions.assertTrue(
                    enUs.has("block.narutomod." + id),
                    "Missing en_us.json localization key block.narutomod." + id);
        }

        for (String tex : REQUIRED_TEXTURES) {
            Assertions.assertTrue(
                    Files.isRegularFile(ASSETS_DIR.resolve("textures/block/deco/" + tex)),
                    "Missing deco texture " + tex);
        }
    }
}
