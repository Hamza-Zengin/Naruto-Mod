package com.sekwah.narutomod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.sekwah.narutomod.abilities.JutsuDataManager;
import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.loot.AddNarutoLootModifier;
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
            } else if ("minecraft:smelting".equals(type) || "minecraft:blasting".equals(type)) {
                Assertions.assertTrue(root.has("ingredient"), fileName + " missing 'ingredient'");
                assertValid263Ingredient(fileName + " ingredient", root.get("ingredient"));
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
                Assertions.assertFalse(pool.has("conditions"),
                        fileName + " pool[" + pIdx + "] uses legacy 1.20.1 'conditions' array instead of 26.3 'condition'");
                if (pool.has("condition")) {
                    JsonElement condElem = pool.get("condition");
                    if (condElem.isJsonObject()) {
                        JsonObject condObj = condElem.getAsJsonObject();
                        Assertions.assertFalse(condObj.has("condition"),
                                fileName + " pool[" + pIdx + "].condition uses legacy 'condition' instead of 26.3 'type'");
                        Assertions.assertTrue(condObj.has("type"),
                                fileName + " pool[" + pIdx + "].condition missing 26.3 'type'");
                    } else if (!condElem.isJsonPrimitive()) {
                        Assertions.fail(fileName + " pool[" + pIdx + "].condition must be object or predicate id");
                    }
                }

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
                DataResult<JutsuDataManager.JutsuStats> result = JutsuDataManager.JutsuStats.CODEC.parse(JsonOps.INSTANCE, json);
                Assertions.assertTrue(result.result().isPresent(),
                        "Failed to decode JutsuStats " + file.getFileName() + ": " + result.error().map(DataResult.Error::message).orElse(""));
            }
        }
    }

    @Test
    public void testEquipmentAssetsExistForAllArmorMaterials() throws IOException {
        Path equipmentDir = Path.of("src/main/resources/assets/narutomod/equipment");
        Assertions.assertTrue(Files.isDirectory(equipmentDir), "assets/narutomod/equipment directory must exist!");
        for (String assetName : List.of("anbu", "headband", "chunin", "akatsuki", "jonin", "warring_states", "kage", "sage")) {
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

    @Test
    public void testGlobalLootModifiersDecodeCleanly() throws IOException {
        Path legacyGlobalFile = Paths.get("src/main/resources/data/neoforge/loot_modifiers/global_loot_modifiers.json");
        Assertions.assertFalse(Files.exists(legacyGlobalFile),
                "Legacy global_loot_modifiers.json must not exist in NeoForge 26.3 (causes LootModifierManager parse error)");

        Path modifiersDir = DATA_DIR.resolve("loot_modifiers");
        Assertions.assertTrue(Files.isDirectory(modifiersDir), "loot_modifiers directory must exist!");

        List<Path> modifierFiles;
        try (Stream<Path> walk = Files.list(modifiersDir)) {
            modifierFiles = walk.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        Assertions.assertEquals(16, modifierFiles.size(), "Expected 16 Global Loot Modifier JSON files!");

        for (Path modifierFile : modifierFiles) {
            JsonObject modObj = JsonParser.parseString(Files.readString(modifierFile)).getAsJsonObject();
            Assertions.assertTrue(modObj.has("type"), modifierFile.getFileName() + " missing 'type'");
            Assertions.assertEquals("narutomod:add_naruto_loot", modObj.get("type").getAsString(),
                    modifierFile.getFileName() + " must use narutomod:add_naruto_loot");
            Assertions.assertFalse(modObj.has("conditions"),
                    modifierFile.getFileName() + " should not use legacy 'conditions' array in NeoForge 26.3");
            DataResult<AddNarutoLootModifier> result = AddNarutoLootModifier.CODEC.codec().parse(JsonOps.INSTANCE, modObj);
            Assertions.assertTrue(result.result().isPresent(),
                    "Failed to decode AddNarutoLootModifier " + modifierFile.getFileName() + ": "
                            + result.error().map(DataResult.Error::message).orElse(""));
        }
    }

    @Test
    public void testAdvancementsAndTriggersDecodeCleanly() throws IOException {
        Path advDir = DATA_DIR.resolve("advancement").resolve("shinobi");
        Assertions.assertTrue(Files.isDirectory(advDir), "advancement/shinobi directory must exist!");

        List<Path> files;
        try (Stream<Path> walk = Files.walk(advDir)) {
            files = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).sorted().toList();
        }
        Assertions.assertEquals(27, files.size(), "Expected 27 Shinobi advancement JSONs!");

        for (Path file : files) {
            JsonObject root = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
            String fileName = file.getFileName().toString();
            Assertions.assertTrue(root.has("display") && root.get("display").isJsonObject(),
                    fileName + " missing 'display'");
            Assertions.assertTrue(root.has("criteria") && root.get("criteria").isJsonObject(),
                    fileName + " missing 'criteria'");

            JsonObject criteria = root.getAsJsonObject("criteria");
            for (String key : criteria.keySet()) {
                JsonObject crit = criteria.getAsJsonObject(key);
                if ("narutomod:ninja_progress".equals(crit.get("trigger").getAsString())) {
                    JsonObject cond = crit.getAsJsonObject("conditions");
                    Assertions.assertNotNull(cond, fileName + " criterion '" + key + "' missing 'conditions'");
                    DataResult<NinjaProgressTrigger.Instance> res =
                            NinjaProgressTrigger.Instance.CODEC.parse(JsonOps.INSTANCE, cond);
                    Assertions.assertTrue(res.result().isPresent(),
                            "Failed to decode NinjaProgressTrigger in " + fileName + ": "
                                    + res.error().map(DataResult.Error::message).orElse(""));
                }
            }
        }
    }

    @Test
    public void testPhase2WorldgenAndBlockTagsValid() throws IOException {
        Path legacyConfigured = DATA_DIR.resolve("worldgen/configured_feature/ore_chakra_metal.json");
        Assertions.assertFalse(Files.exists(legacyConfigured),
                "Legacy worldgen/configured_feature/ore_chakra_metal.json must not exist in MC 26.3!");

        Path feature = DATA_DIR.resolve("worldgen/feature/ore_chakra_metal.json");
        Assertions.assertTrue(Files.isRegularFile(feature), "worldgen/feature/ore_chakra_metal.json must exist!");
        JsonObject featObj = JsonParser.parseString(Files.readString(feature)).getAsJsonObject();
        Assertions.assertEquals("minecraft:ore", featObj.get("type").getAsString());
        Assertions.assertFalse(featObj.has("config"),
                "MC 26.3 worldgen/feature JSON must inline fields at root instead of nesting 'config'");
        Assertions.assertTrue(featObj.get("size").getAsInt() > 0);
        JsonArray targets = featObj.getAsJsonArray("targets");
        Assertions.assertEquals(2, targets.size());
        for (JsonElement tElem : targets) {
            JsonObject stateObj = tElem.getAsJsonObject().getAsJsonObject("state");
            Assertions.assertFalse(stateObj.has("Name"), "MC 26.3 BlockState uses 'id' instead of 'Name'");
            Assertions.assertTrue(stateObj.has("id"), "MC 26.3 BlockState missing 'id'");
        }

        Path placed = DATA_DIR.resolve("worldgen/placed_feature/ore_chakra_metal_placed.json");
        Assertions.assertTrue(Files.isRegularFile(placed), "ore_chakra_metal_placed.json must exist!");
        JsonObject placedObj = JsonParser.parseString(Files.readString(placed)).getAsJsonObject();
        Assertions.assertEquals("narutomod:ore_chakra_metal", placedObj.get("feature").getAsString());
        Assertions.assertFalse(placedObj.getAsJsonArray("placement").isEmpty());

        Path modifier = DATA_DIR.resolve("neoforge/biome_modifier/add_chakra_metal_ore.json");
        Assertions.assertTrue(Files.isRegularFile(modifier), "add_chakra_metal_ore.json must exist!");
        JsonObject modObj = JsonParser.parseString(Files.readString(modifier)).getAsJsonObject();
        Assertions.assertEquals("neoforge:add_features", modObj.get("type").getAsString());
        Assertions.assertEquals("underground_ores", modObj.get("step").getAsString());

        Path mcTagsDir = Paths.get("src/main/resources/data/minecraft/tags/block");
        for (String tagRel : List.of("mineable/pickaxe.json", "needs_iron_tool.json", "mineable/axe.json")) {
            Path tagPath = mcTagsDir.resolve(tagRel);
            Assertions.assertTrue(Files.isRegularFile(tagPath), "Missing block tag: " + tagRel);
            JsonObject tagObj = JsonParser.parseString(Files.readString(tagPath)).getAsJsonObject();
            Assertions.assertTrue(tagObj.has("values") && !tagObj.getAsJsonArray("values").isEmpty(),
                    tagRel + " must have non-empty values array");
        }
    }
}
