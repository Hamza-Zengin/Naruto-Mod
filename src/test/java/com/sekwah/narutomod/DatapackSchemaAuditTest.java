package com.sekwah.narutomod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.sekwah.narutomod.abilities.JutsuDataManager;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.damagesource.DamageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Stream;

/**
 * Audits all Naruto-Mod datapack JSON files against Minecraft 26.3 schemas and codecs
 * to prevent RegistryDataLoader errors when loading or creating worlds.
 */
public class DatapackSchemaAuditTest {

    private static final Path DATA_DIR = Paths.get("src/main/resources/data/narutomod");

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    public void testAllRecipesUse263Schema() throws IOException {
        Path recipeDir = DATA_DIR.resolve("recipe");
        Assertions.assertTrue(Files.isDirectory(recipeDir), "recipe directory must exist!");

        List<Path> recipeFiles;
        try (Stream<Path> walk = Files.walk(recipeDir)) {
            recipeFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        Assertions.assertFalse(recipeFiles.isEmpty(), "At least one recipe JSON must exist!");

        for (Path recipeFile : recipeFiles) {
            JsonObject root = JsonParser.parseString(Files.readString(recipeFile)).getAsJsonObject();
            String fileName = recipeFile.getFileName().toString();
            String type = root.has("type") ? root.get("type").getAsString() : "";
            Assertions.assertFalse(type.isEmpty(), fileName + " missing recipe 'type'");

            Assertions.assertTrue(root.has("result") && root.get("result").isJsonObject(),
                    fileName + " missing 'result' object");
            JsonObject result = root.getAsJsonObject("result");
            Assertions.assertFalse(result.has("item"),
                    fileName + " uses legacy 1.20.1 'result.item' instead of 26.3 'result.id'");
            Assertions.assertTrue(result.has("id") && result.get("id").isJsonPrimitive(),
                    fileName + " missing required 26.3 'result.id'");

            if ("minecraft:crafting_shaped".equals(type)) {
                Assertions.assertTrue(root.has("pattern") && root.get("pattern").isJsonArray(),
                        fileName + " missing 'pattern'");
                Assertions.assertTrue(root.has("key") && root.get("key").isJsonObject(),
                        fileName + " missing 'key'");
                JsonObject keyObj = root.getAsJsonObject("key");
                for (String keyChar : keyObj.keySet()) {
                    assertValid263Ingredient(fileName + " key['" + keyChar + "']", keyObj.get(keyChar));
                }
            } else if ("minecraft:crafting_shapeless".equals(type)) {
                Assertions.assertTrue(root.has("ingredients") && root.get("ingredients").isJsonArray(),
                        fileName + " missing 'ingredients'");
                JsonArray ingredients = root.getAsJsonArray("ingredients");
                Assertions.assertFalse(ingredients.isEmpty(), fileName + " 'ingredients' must not be empty");
                for (int i = 0; i < ingredients.size(); i++) {
                    assertValid263Ingredient(fileName + " ingredients[" + i + "]", ingredients.get(i));
                }
            }
        }
    }

    private static void assertValid263Ingredient(String context, JsonElement elem) {
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isString()) {
            String val = elem.getAsString();
            Assertions.assertFalse(val.isBlank(), context + " ingredient string must not be blank");
            return;
        }
        if (elem.isJsonArray()) {
            JsonArray arr = elem.getAsJsonArray();
            Assertions.assertFalse(arr.isEmpty(), context + " ingredient array must not be empty");
            for (JsonElement child : arr) {
                Assertions.assertTrue(child.isJsonPrimitive() && child.getAsJsonPrimitive().isString(),
                        context + " compound ingredient entry must be a string, found: " + child);
            }
            return;
        }
        Assertions.fail(context + " uses legacy 1.20.1 object ingredient instead of 26.3 string/array: " + elem);
    }

    @Test
    public void testAllLootTablesUse263Schema() throws IOException {
        Path lootDir = DATA_DIR.resolve("loot_table");
        Assertions.assertTrue(Files.isDirectory(lootDir), "loot_table directory must exist!");

        List<Path> lootFiles;
        try (Stream<Path> walk = Files.walk(lootDir)) {
            lootFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        Assertions.assertFalse(lootFiles.isEmpty(), "At least one loot_table JSON must exist!");

        for (Path lootFile : lootFiles) {
            JsonObject root = JsonParser.parseString(Files.readString(lootFile)).getAsJsonObject();
            String fileName = lootFile.getFileName().toString();
            JsonArray pools = root.getAsJsonArray("pools");
            Assertions.assertNotNull(pools, fileName + " missing 'pools'");

            for (int pIdx = 0; pIdx < pools.size(); pIdx++) {
                JsonObject pool = pools.get(pIdx).getAsJsonObject();
                Assertions.assertTrue(pool.has("rolls"), fileName + " pool[" + pIdx + "] missing 'rolls'");
                assertValid263NumberProvider(fileName + " pool[" + pIdx + "].rolls", pool.get("rolls"));

                JsonArray entries = pool.getAsJsonArray("entries");
                Assertions.assertNotNull(entries, fileName + " pool[" + pIdx + "] missing 'entries'");
                for (int eIdx = 0; eIdx < entries.size(); eIdx++) {
                    JsonObject entry = entries.get(eIdx).getAsJsonObject();
                    Assertions.assertFalse(entry.has("functions"),
                            fileName + " entry[" + eIdx + "] uses legacy 1.20.1 'functions' array instead of 26.3 'modifier'");
                    if (entry.has("modifier")) {
                        JsonElement modElem = entry.get("modifier");
                        if (modElem.isJsonObject()) {
                            assertValid263LootModifier(fileName + " entry[" + eIdx + "].modifier", modElem.getAsJsonObject());
                        } else if (modElem.isJsonArray()) {
                            for (JsonElement m : modElem.getAsJsonArray()) {
                                assertValid263LootModifier(fileName + " entry[" + eIdx + "].modifier[]", m.getAsJsonObject());
                            }
                        } else {
                            Assertions.fail(fileName + " entry[" + eIdx + "].modifier must be object or array");
                        }
                    }
                }
            }
        }
    }

    private static void assertValid263LootModifier(String context, JsonObject mod) {
        Assertions.assertFalse(mod.has("function"), context + " uses legacy 'function' instead of 26.3 'type'");
        Assertions.assertTrue(mod.has("type"), context + " missing 'type'");
        if ("minecraft:set_count".equals(mod.get("type").getAsString())) {
            Assertions.assertTrue(mod.has("count"), context + " missing 'count'");
            assertValid263NumberProvider(context + ".count", mod.get("count"));
        }
    }

    private static void assertValid263NumberProvider(String context, JsonElement elem) {
        if (elem.isJsonPrimitive() && elem.getAsJsonPrimitive().isNumber()) {
            return;
        }
        Assertions.assertTrue(elem.isJsonObject(), context + " must be a number or NumberProvider object");
        JsonObject obj = elem.getAsJsonObject();
        Assertions.assertTrue(obj.has("type") && obj.get("type").isJsonPrimitive(),
                context + " NumberProvider object missing required 26.3 'type' (e.g. 'minecraft:uniform'): " + obj);
    }

    @Test
    public void testJukeboxSongsUse263Schema() throws IOException {
        Path jukeboxDir = DATA_DIR.resolve("jukebox_song");
        Assertions.assertTrue(Files.isDirectory(jukeboxDir), "jukebox_song directory must exist!");
        Path lonelyMarch = jukeboxDir.resolve("lonely_march.json");
        Assertions.assertTrue(Files.isRegularFile(lonelyMarch), "lonely_march.json must exist!");

        JsonObject root = JsonParser.parseString(Files.readString(lonelyMarch)).getAsJsonObject();
        Assertions.assertTrue(root.has("comparator_output") && root.get("comparator_output").getAsInt() >= 0);
        Assertions.assertTrue(root.has("description") && root.get("description").isJsonObject());
        Assertions.assertTrue(root.has("length_in_seconds") && root.get("length_in_seconds").getAsFloat() > 0.0f);
        Assertions.assertEquals("narutomod:lonely_march", root.get("sound_event").getAsString());
    }

    @Test
    public void testAllDamageTypesDecodeCleanly() throws IOException {
        Path dmgDir = DATA_DIR.resolve("damage_type");
        Assertions.assertTrue(Files.isDirectory(dmgDir), "damage_type directory must exist!");

        try (Stream<Path> walk = Files.walk(dmgDir)) {
            List<Path> files = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
            Assertions.assertFalse(files.isEmpty());
            for (Path file : files) {
                JsonElement json = JsonParser.parseString(Files.readString(file));
                DataResult<DamageType> result = DamageType.DIRECT_CODEC.parse(JsonOps.INSTANCE, json);
                Assertions.assertTrue(result.result().isPresent(),
                        "Failed to decode DamageType " + file.getFileName() + ": " + result.error().map(DataResult.Error::message).orElse(""));
            }
        }
    }

    @Test
    public void testAllJutsuDataDefinitionsDecodeCleanly() throws IOException {
        Path jutsuDir = DATA_DIR.resolve("jutsus");
        Assertions.assertTrue(Files.isDirectory(jutsuDir), "jutsus directory must exist!");

        try (Stream<Path> walk = Files.walk(jutsuDir)) {
            List<Path> files = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
            Assertions.assertFalse(files.isEmpty());
            for (Path file : files) {
                JsonElement json = JsonParser.parseString(Files.readString(file));
                DataResult<JutsuDataManager.JutsuData> result = JutsuDataManager.JutsuData.CODEC.parse(JsonOps.INSTANCE, json);
                Assertions.assertTrue(result.result().isPresent(),
                        "Failed to decode JutsuData " + file.getFileName() + ": " + result.error().map(DataResult.Error::message).orElse(""));
            }
        }
    }

    @Test
    public void testEquipmentAssetsExistForAllArmorMaterials() throws IOException {
        Path equipmentDir = Path.of("src/main/resources/assets/narutomod/equipment");
        Assertions.assertTrue(Files.isDirectory(equipmentDir), "assets/narutomod/equipment directory must exist!");
        for (String assetName : List.of("anbu", "headband", "chunin", "akatsuki")) {
            Path file = equipmentDir.resolve(assetName + ".json");
            Assertions.assertTrue(Files.isRegularFile(file), "Missing equipment asset JSON: " + file);
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            Assertions.assertTrue(root.has("layers") && root.get("layers").isJsonObject(),
                    file + " missing 'layers' object");
            JsonObject layers = root.getAsJsonObject("layers");
            Assertions.assertTrue(layers.has("humanoid") && layers.get("humanoid").isJsonArray()
                            && !layers.getAsJsonArray("humanoid").isEmpty(),
                    file + " must have non-empty 'layers.humanoid' array");
        }
    }
}
