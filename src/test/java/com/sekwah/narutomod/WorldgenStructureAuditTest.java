package com.sekwah.narutomod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

/**
 * Verifies that every template_pool location under data/narutomod/worldgen/template_pool/
 * resolves to a valid Gzip-compressed Minecraft Structure .nbt file.
 */
public class WorldgenStructureAuditTest {

    private static final Path TEMPLATE_POOL_DIR = Paths.get("src/main/resources/data/narutomod/worldgen/template_pool");
    private static final Path DATA_DIR = Paths.get("src/main/resources/data");

    @Test
    public void testAllTemplatePoolLocationsHaveValidNbtFiles() throws IOException {
        Assertions.assertTrue(Files.isDirectory(TEMPLATE_POOL_DIR), "template_pool directory must exist!");

        List<Path> poolFiles;
        try (Stream<Path> walk = Files.walk(TEMPLATE_POOL_DIR)) {
            poolFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }

        Assertions.assertFalse(poolFiles.isEmpty(), "At least one template_pool JSON must exist!");

        List<String> checkedLocations = new ArrayList<>();
        for (Path poolFile : poolFiles) {
            JsonObject root = JsonParser.parseString(Files.readString(poolFile)).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            Assertions.assertNotNull(elements, "template_pool must contain 'elements': " + poolFile);
            Assertions.assertFalse(elements.isEmpty(), "template_pool 'elements' must not be empty: " + poolFile);

            for (JsonElement elem : elements) {
                JsonObject elementObj = elem.getAsJsonObject().getAsJsonObject("element");
                Assertions.assertNotNull(elementObj, "Missing 'element' object in " + poolFile);
                if (!elementObj.has("location")) {
                    continue;
                }
                String location = elementObj.get("location").getAsString();
                String[] parts = location.split(":", 2);
                Assertions.assertEquals(2, parts.length, "Invalid ResourceLocation: " + location);

                Path nbtPath = DATA_DIR.resolve(parts[0]).resolve("structure").resolve(parts[1] + ".nbt");
                Assertions.assertTrue(Files.isRegularFile(nbtPath),
                        "Missing structure .nbt file for location '" + location + "' at " + nbtPath);

                byte[] raw = Files.readAllBytes(nbtPath);
                Assertions.assertTrue(raw.length >= 10, "Structure .nbt file is too small: " + nbtPath);
                Assertions.assertEquals((byte) 0x1F, raw[0], "Invalid Gzip magic byte 0 in " + nbtPath);
                Assertions.assertEquals((byte) 0x8B, raw[1], "Invalid Gzip magic byte 1 in " + nbtPath);

                try (GZIPInputStream gzis = new GZIPInputStream(new ByteArrayInputStream(raw))) {
                    int rootTag = gzis.read();
                    Assertions.assertEquals(10, rootTag, "Decompressed NBT must start with TAG_Compound (10): " + nbtPath);
                    byte[] rest = gzis.readAllBytes();
                    Assertions.assertTrue(rest.length > 0, "Decompressed NBT payload must not be empty: " + nbtPath);
                }

                checkedLocations.add(location);
            }
        }

        Assertions.assertFalse(checkedLocations.isEmpty(), "Expected at least one structure location to be audited!");
    }

    @Test
    public void testAllStructureSpawnOverridesUseCountSchema() throws IOException {
        Path structureDir = DATA_DIR.resolve("narutomod/worldgen/structure");
        Assertions.assertTrue(Files.isDirectory(structureDir), "worldgen/structure directory must exist!");

        try (Stream<Path> walk = Files.walk(structureDir)) {
            List<Path> structureFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
            Assertions.assertFalse(structureFiles.isEmpty(), "At least one structure JSON must exist!");

            for (Path structureFile : structureFiles) {
                JsonObject root = JsonParser.parseString(Files.readString(structureFile)).getAsJsonObject();
                if (!root.has("spawn_overrides")) {
                    continue;
                }
                JsonObject overrides = root.getAsJsonObject("spawn_overrides");
                for (String category : overrides.keySet()) {
                    JsonObject catObj = overrides.getAsJsonObject(category);
                    if (!catObj.has("spawns")) {
                        continue;
                    }
                    JsonArray spawns = catObj.getAsJsonArray("spawns");
                    for (JsonElement elem : spawns) {
                        JsonObject spawn = elem.getAsJsonObject();
                        Assertions.assertFalse(spawn.has("minCount") || spawn.has("maxCount"),
                                structureFile.getFileName() + " uses legacy 1.20.1 minCount/maxCount instead of 26.3 'count'");
                        Assertions.assertTrue(spawn.has("count"),
                                structureFile.getFileName() + " spawn entry missing required 26.3 'count' field");
                        Assertions.assertTrue(spawn.has("weight"),
                                structureFile.getFileName() + " spawn entry missing 'weight' field");
                    }
                }
            }
        }
    }
}
