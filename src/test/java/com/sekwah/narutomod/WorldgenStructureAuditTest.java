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

    @Test
    public void testAllStructuresMeetArchitecturalDetailThresholds() throws IOException {
        List<String> expectedStructures = List.of(
                "konoha_village", "suna_village", "kiri_village", "kumo_village", "iwa_village",
                "valley_of_the_end", "chunin_arena", "myobokuzan_shrine", "uchiha_shrine", "akatsuki_hideout"
        );
        Path structureDir = DATA_DIR.resolve("narutomod/worldgen/structure");
        for (String name : expectedStructures) {
            Path structFile = structureDir.resolve(name + ".json");
            Assertions.assertTrue(Files.isRegularFile(structFile), "Missing worldgen/structure/" + name + ".json");
            JsonObject structJson = JsonParser.parseString(Files.readString(structFile)).getAsJsonObject();
            String startPool = structJson.get("start_pool").getAsString();
            String poolName = startPool.split(":", 2)[1];
            Assertions.assertTrue(Files.isRegularFile(TEMPLATE_POOL_DIR.resolve(poolName + ".json")),
                    "Missing start_pool JSON for " + name + ": " + startPool);
        }

        List<Path> poolFiles;
        try (Stream<Path> walk = Files.walk(TEMPLATE_POOL_DIR)) {
            poolFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        Assertions.assertTrue(poolFiles.size() >= 25, "Expected at least 25 template_pool files across all 10 structures");

        for (Path poolFile : poolFiles) {
            JsonObject root = JsonParser.parseString(Files.readString(poolFile)).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            for (JsonElement elem : elements) {
                JsonObject elementObj = elem.getAsJsonObject().getAsJsonObject("element");
                if (!elementObj.has("location")) {
                    continue;
                }
                String location = elementObj.get("location").getAsString();
                String[] parts = location.split(":", 2);
                Path nbtPath = DATA_DIR.resolve(parts[0]).resolve("structure").resolve(parts[1] + ".nbt");

                net.minecraft.nbt.CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(
                        nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                net.minecraft.nbt.ListTag sizeTag = rootTag.getListOrEmpty("size");
                Assertions.assertEquals(3, sizeTag.size(), "Structure NBT missing 3D size: " + location);
                int sx = sizeTag.getIntOr(0, 0);
                int sy = sizeTag.getIntOr(1, 0);
                int sz = sizeTag.getIntOr(2, 0);
                Assertions.assertTrue(
                        Math.max(sx, sz) >= 15 && Math.min(sx, sz) >= 13 && sy >= 10 && (sx * sy * sz) >= 2000,
                        "Structure piece is too small for architectural detail (" + sx + "x" + sy + "x" + sz + "): " + location);

                net.minecraft.nbt.ListTag paletteTag = rootTag.getListOrEmpty("palette");
                net.minecraft.nbt.ListTag blocksTag = rootTag.getListOrEmpty("blocks");
                java.util.Set<String> nonAirNames = new java.util.HashSet<>();
                int airState = -1;
                for (int i = 0; i < paletteTag.size(); i++) {
                    String blockName = paletteTag.getCompoundOrEmpty(i).getStringOr("Name", "");
                    if ("minecraft:air".equals(blockName)) {
                        airState = i;
                    } else if (!blockName.isEmpty()) {
                        nonAirNames.add(blockName);
                    }
                }
                Assertions.assertTrue(nonAirNames.size() >= 6,
                        "Structure piece lacks material/architectural variety (non-air palette=" + nonAirNames.size() + "): " + location);

                int nonAirCount = 0;
                for (int i = 0; i < blocksTag.size(); i++) {
                    if (blocksTag.getCompoundOrEmpty(i).getIntOr("state", -1) != airState) {
                        nonAirCount++;
                    }
                }
                Assertions.assertTrue(nonAirCount >= 380,
                        "Structure piece has too few blocks (" + nonAirCount + "): " + location);
            }
        }
    }

    @Test
    public void testSecondRevisionStructureLightingEntitiesAndSparsity() throws IOException {
        java.util.Set<String> lightBlocks = java.util.Set.of(
                "minecraft:lantern", "minecraft:soul_lantern", "minecraft:ochre_froglight",
                "minecraft:sea_lantern", "minecraft:shroomlight", "minecraft:glowstone", "minecraft:torch");

        // 1. Every .nbt structure piece in template pools must include illumination blocks
        List<Path> poolFiles;
        try (Stream<Path> walk = Files.walk(TEMPLATE_POOL_DIR)) {
            poolFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .sorted()
                    .toList();
        }
        Assertions.assertTrue(poolFiles.size() >= 30, "Expected 30 template_pool files including Valley & Ichiraku pools");

        for (Path poolFile : poolFiles) {
            JsonObject root = JsonParser.parseString(Files.readString(poolFile)).getAsJsonObject();
            for (JsonElement elem : root.getAsJsonArray("elements")) {
                JsonObject elementObj = elem.getAsJsonObject().getAsJsonObject("element");
                if (!elementObj.has("location")) {
                    continue;
                }
                String location = elementObj.get("location").getAsString();
                String[] parts = location.split(":", 2);
                Path nbtPath = DATA_DIR.resolve(parts[0]).resolve("structure").resolve(parts[1] + ".nbt");
                net.minecraft.nbt.CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(
                        nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                net.minecraft.nbt.ListTag paletteTag = rootTag.getListOrEmpty("palette");
                boolean hasLight = false;
                for (int i = 0; i < paletteTag.size(); i++) {
                    if (lightBlocks.contains(paletteTag.getCompoundOrEmpty(i).getStringOr("Name", ""))) {
                        hasLight = true;
                        break;
                    }
                }
                Assertions.assertTrue(hasLight, "Structure piece lacks illumination blocks: " + location);
            }
        }

        // 2. Single Ichiraku Ramen per Konoha village
        JsonObject uniqueIchiraku = JsonParser.parseString(
                Files.readString(TEMPLATE_POOL_DIR.resolve("konoha_ichiraku_unique.json"))).getAsJsonObject();
        Assertions.assertEquals(1, uniqueIchiraku.getAsJsonArray("elements").size());
        Assertions.assertEquals("narutomod:konoha_ichiraku_house",
                uniqueIchiraku.getAsJsonArray("elements").get(0).getAsJsonObject()
                        .getAsJsonObject("element").get("location").getAsString());

        String konohaAddonsRaw = Files.readString(TEMPLATE_POOL_DIR.resolve("konoha_village_addons.json"));
        Assertions.assertFalse(konohaAddonsRaw.contains("konoha_ichiraku_house"),
                "konoha_village_addons must not contain konoha_ichiraku_house so it spawns only once");

        // 3. Akatsuki Hideout: empty spawn_overrides + exact 2 akatsuki_ninja + 5 rogue_ninja embedded in NBT
        Path structureDir = DATA_DIR.resolve("narutomod/worldgen/structure");
        JsonObject akatsukiStruct = JsonParser.parseString(
                Files.readString(structureDir.resolve("akatsuki_hideout.json"))).getAsJsonObject();
        Assertions.assertEquals(0, akatsukiStruct.getAsJsonObject("spawn_overrides").size(),
                "akatsuki_hideout must not continuously spawn mobs via spawn_overrides");

        net.minecraft.nbt.CompoundTag akatsukiNbt = net.minecraft.nbt.NbtIo.readCompressed(
                DATA_DIR.resolve("narutomod/structure/akatsuki_hideout.nbt"),
                net.minecraft.nbt.NbtAccounter.unlimitedHeap());
        net.minecraft.nbt.ListTag akatsukiEntities = akatsukiNbt.getListOrEmpty("entities");
        int akatsukiCount = 0;
        int rogueCount = 0;
        for (int i = 0; i < akatsukiEntities.size(); i++) {
            String entId = akatsukiEntities.getCompoundOrEmpty(i).getCompoundOrEmpty("nbt").getStringOr("id", "");
            if ("narutomod:akatsuki_ninja".equals(entId)) {
                akatsukiCount++;
            } else if ("narutomod:rogue_ninja".equals(entId)) {
                rogueCount++;
            }
        }
        Assertions.assertEquals(2, akatsukiCount, "akatsuki_hideout.nbt must contain exact 2 akatsuki_ninja");
        Assertions.assertEquals(5, rogueCount, "akatsuki_hideout.nbt must contain exact 5 rogue_ninja");

        // 4. All 5 villages: empty spawn_overrides + village-specific ninja & villager embedded in NBTs
        for (String v : List.of("konoha", "suna", "kiri", "kumo", "iwa")) {
            JsonObject vStruct = JsonParser.parseString(
                    Files.readString(structureDir.resolve(v + "_village.json"))).getAsJsonObject();
            Assertions.assertEquals(0, vStruct.getAsJsonObject("spawn_overrides").size(),
                    v + "_village must rely on one-time NBT entities instead of continuous spawn_overrides");
            String startLoc = JsonParser.parseString(Files.readString(TEMPLATE_POOL_DIR.resolve(v + "_village.json")))
                    .getAsJsonObject().getAsJsonArray("elements").get(0).getAsJsonObject()
                    .getAsJsonObject("element").get("location").getAsString().split(":", 2)[1];
            net.minecraft.nbt.CompoundTag hubNbt = net.minecraft.nbt.NbtIo.readCompressed(
                    DATA_DIR.resolve("narutomod/structure/" + startLoc + ".nbt"),
                    net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            net.minecraft.nbt.ListTag hubEnts = hubNbt.getListOrEmpty("entities");
            int ninjas = 0;
            int villagers = 0;
            for (int i = 0; i < hubEnts.size(); i++) {
                String entId = hubEnts.getCompoundOrEmpty(i).getCompoundOrEmpty("nbt").getStringOr("id", "");
                if (("narutomod:" + v + "_ninja").equals(entId)) ninjas++;
                if (("narutomod:" + v + "_villager").equals(entId)) villagers++;
            }
            Assertions.assertTrue(ninjas >= 1 && villagers >= 1,
                    v + " start piece must embed both " + v + "_ninja and " + v + "_villager");
        }

        // 5. Unified structure_set preventing adjacent structures (separation >= 24 chunks = 384 blocks)
        Path setDir = DATA_DIR.resolve("narutomod/worldgen/structure_set");
        try (Stream<Path> walk = Files.walk(setDir)) {
            List<Path> sets = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).toList();
            Assertions.assertEquals(1, sets.size(), "Expected 1 unified narutomod_structures.json structure_set");
            Path unifiedSet = sets.get(0);
            Assertions.assertEquals("narutomod_structures.json", unifiedSet.getFileName().toString());
            JsonObject setRoot = JsonParser.parseString(Files.readString(unifiedSet)).getAsJsonObject();
            JsonArray structuresArr = setRoot.getAsJsonArray("structures");
            Assertions.assertEquals(10, structuresArr.size(), "Unified structure_set must contain all 10 structures");
            java.util.Set<String> presentIds = new java.util.HashSet<>();
            for (JsonElement elem : structuresArr) {
                presentIds.add(elem.getAsJsonObject().get("structure").getAsString());
            }
            for (String name : List.of("konoha_village", "suna_village", "kiri_village", "kumo_village", "iwa_village",
                    "valley_of_the_end", "chunin_arena", "myobokuzan_shrine", "uchiha_shrine", "akatsuki_hideout")) {
                Assertions.assertTrue(presentIds.contains("narutomod:" + name),
                        "Unified structure_set missing narutomod:" + name);
            }
            JsonObject placement = setRoot.getAsJsonObject("placement");
            Assertions.assertTrue(placement.get("spacing").getAsInt() >= 40,
                    "Unified structure_set spacing must be >= 40");
            Assertions.assertTrue(placement.get("separation").getAsInt() >= 24,
                    "Unified structure_set separation must be >= 24 chunks (384 blocks) to prevent adjacent structures");
        }

        // 6. 3rd revision audits: zero glass_pane blocks in any .nbt, no fountain/plaza in village_addons, and no y>=2 water in fountains
        Path structNbtDir = DATA_DIR.resolve("narutomod/structure");
        try (Stream<Path> walk = Files.walk(structNbtDir)) {
            List<Path> nbtFiles = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".nbt")).toList();
            Assertions.assertFalse(nbtFiles.isEmpty());
            for (Path nbtPath : nbtFiles) {
                net.minecraft.nbt.CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(
                        nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                net.minecraft.nbt.ListTag paletteTag = rootTag.getListOrEmpty("palette");
                for (int i = 0; i < paletteTag.size(); i++) {
                    String blockName = paletteTag.getCompoundOrEmpty(i).getStringOr("Name", "");
                    Assertions.assertFalse(blockName.contains("glass_pane"),
                            "Structure .nbt must use full glass blocks instead of glass_pane (" + blockName + "): " + nbtPath.getFileName());
                }
            }
        }

        for (String bannedPlaza : List.of("konoha_crossroad", "suna_courtyard", "kiri_canal_plaza", "kumo_cloud_plaza", "iwa_pillars")) {
            String villagePrefix = bannedPlaza.split("_", 2)[0];
            String addonsRaw = Files.readString(TEMPLATE_POOL_DIR.resolve(villagePrefix + "_village_addons.json"));
            Assertions.assertFalse(addonsRaw.contains(bannedPlaza),
                    villagePrefix + "_village_addons.json must not contain repeating fountain/plaza " + bannedPlaza);
        }

        for (String fountainPiece : List.of("konoha_crossroad", "suna_courtyard")) {
            net.minecraft.nbt.CompoundTag nbt = net.minecraft.nbt.NbtIo.readCompressed(
                    structNbtDir.resolve(fountainPiece + ".nbt"),
                    net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            net.minecraft.nbt.ListTag palette = nbt.getListOrEmpty("palette");
            int waterState = -1;
            for (int i = 0; i < palette.size(); i++) {
                if ("minecraft:water".equals(palette.getCompoundOrEmpty(i).getStringOr("Name", ""))) {
                    waterState = i;
                    break;
                }
            }
            if (waterState >= 0) {
                net.minecraft.nbt.ListTag blocks = nbt.getListOrEmpty("blocks");
                for (int i = 0; i < blocks.size(); i++) {
                    net.minecraft.nbt.CompoundTag b = blocks.getCompoundOrEmpty(i);
                    if (b.getIntOr("state", -1) == waterState) {
                        int y = b.getListOrEmpty("pos").getIntOr(1, -1);
                        Assertions.assertTrue(y <= 1,
                                fountainPiece + ".nbt must recess fountain water at y<=1 to avoid slab leaks (found y=" + y + ")");
                    }
                }
            }
        }
    }

    @Test
    public void testFourthRevisionTerrainAnchoringAndFoundations() throws IOException {
        Path structureDir = DATA_DIR.resolve("narutomod/worldgen/structure");
        for (String name : List.of(
                "konoha_village", "suna_village", "kiri_village", "kumo_village", "iwa_village",
                "valley_of_the_end", "chunin_arena", "myobokuzan_shrine", "uchiha_shrine", "akatsuki_hideout")) {
            JsonObject root = JsonParser.parseString(
                    Files.readString(structureDir.resolve(name + ".json"))).getAsJsonObject();
            Assertions.assertEquals("narutomod:anchored_jigsaw", root.get("type").getAsString(),
                    name + ".json must use narutomod:anchored_jigsaw for terrain anchoring");
            Assertions.assertTrue(root.get("max_height_variance").getAsInt() >= 4,
                    name + ".json must specify valid max_height_variance");
            Assertions.assertTrue(root.get("max_foundation_depth").getAsInt() >= 16,
                    name + ".json must specify max_foundation_depth >= 16");
        }

        // Verify rigid projections on all path/addon/shore/wall pools (handled by AnchoredJigsawStructure runtime pillars)
        java.util.Map<String, List<String>> expectedRigidPools = java.util.Map.of(
                "konoha_streets", List.of("narutomod:konoha_main_street"),
                "konoha_village_addons", List.of("narutomod:konoha_training_ground"),
                "suna_streets", List.of("narutomod:suna_avenue"),
                "akatsuki_hideout_addons", List.of("narutomod:akatsuki_canyon"),
                "valley_of_the_end_addons", List.of("narutomod:valley_canyon_walls", "narutomod:valley_shore")
        );
        for (var entry : expectedRigidPools.entrySet()) {
            JsonObject pool = JsonParser.parseString(
                    Files.readString(TEMPLATE_POOL_DIR.resolve(entry.getKey() + ".json"))).getAsJsonObject();
            for (String targetLoc : entry.getValue()) {
                boolean matched = false;
                for (JsonElement el : pool.getAsJsonArray("elements")) {
                    JsonObject obj = el.getAsJsonObject().getAsJsonObject("element");
                    if (targetLoc.equals(obj.get("location").getAsString())) {
                        Assertions.assertEquals("rigid", obj.get("projection").getAsString(),
                                targetLoc + " in " + entry.getKey() + " must use rigid projection");
                        matched = true;
                    }
                }
                Assertions.assertTrue(matched, "Expected element " + targetLoc + " in " + entry.getKey());
            }
        }

        // Verify no template_pool element uses terrain_matching
        try (Stream<Path> walk = Files.walk(TEMPLATE_POOL_DIR)) {
            List<Path> allPools = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".json")).sorted().toList();
            for (Path poolFile : allPools) {
                JsonObject pool = JsonParser.parseString(Files.readString(poolFile)).getAsJsonObject();
                for (JsonElement el : pool.getAsJsonArray("elements")) {
                    JsonObject obj = el.getAsJsonObject().getAsJsonObject("element");
                    if (obj.has("projection")) {
                        Assertions.assertEquals("rigid", obj.get("projection").getAsString(),
                                poolFile.getFileName() + " must use rigid projection");
                    }
                }
            }
        }

        // Verify every grounded .nbt piece has y=0 support under every y=1 non-air column
        Path structNbtDir = DATA_DIR.resolve("narutomod/structure");
        try (Stream<Path> walk = Files.walk(structNbtDir)) {
            List<Path> nbtFiles = walk.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".nbt")).sorted().toList();
            for (Path nbtPath : nbtFiles) {
                String fname = nbtPath.getFileName().toString();
                if ("empty.nbt".equals(fname) || "kumo_sky_bridge.nbt".equals(fname)) {
                    continue;
                }
                net.minecraft.nbt.CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(
                        nbtPath, net.minecraft.nbt.NbtAccounter.unlimitedHeap());
                net.minecraft.nbt.ListTag palette = rootTag.getListOrEmpty("palette");
                java.util.Set<Integer> airStates = new java.util.HashSet<>();
                for (int i = 0; i < palette.size(); i++) {
                    String bName = palette.getCompoundOrEmpty(i).getStringOr("Name", "");
                    if ("minecraft:air".equals(bName) || "minecraft:cave_air".equals(bName) || "minecraft:void_air".equals(bName)) {
                        airStates.add(i);
                    }
                }
                java.util.Set<Long> y0 = new java.util.HashSet<>();
                java.util.Set<Long> y1 = new java.util.HashSet<>();
                net.minecraft.nbt.ListTag blocks = rootTag.getListOrEmpty("blocks");
                for (int i = 0; i < blocks.size(); i++) {
                    net.minecraft.nbt.CompoundTag b = blocks.getCompoundOrEmpty(i);
                    if (airStates.contains(b.getIntOr("state", -1))) {
                        continue;
                    }
                    net.minecraft.nbt.ListTag pos = b.getListOrEmpty("pos");
                    int x = pos.getIntOr(0, 0);
                    int y = pos.getIntOr(1, 0);
                    int z = pos.getIntOr(2, 0);
                    long key = (((long) x) << 32) | (z & 0xffffffffL);
                    if (y == 0) y0.add(key);
                    else if (y == 1) y1.add(key);
                }
                y1.removeAll(y0);
                Assertions.assertTrue(y1.isEmpty(),
                        fname + " has " + y1.size() + " column(s) with y=1 blocks lacking y=0 foundation support");
            }
        }
    }

    @Test
    public void testFifthRevisionWaterFramingAndValleyMonumentChest() throws IOException {
        Path structNbtDir = DATA_DIR.resolve("narutomod/structure");

        // 1. Kiri water structures must not leave unframed boundary water at x=0/sx-1/z=0/sz-1
        for (String kiriWaterFile : List.of("kiri_bridge.nbt", "kiri_gate.nbt")) {
            net.minecraft.nbt.CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(
                    structNbtDir.resolve(kiriWaterFile), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            net.minecraft.nbt.ListTag sizeTag = rootTag.getListOrEmpty("size");
            int sx = sizeTag.getIntOr(0, 0);
            int sz = sizeTag.getIntOr(2, 0);
            net.minecraft.nbt.ListTag palette = rootTag.getListOrEmpty("palette");
            int waterState = -1;
            for (int i = 0; i < palette.size(); i++) {
                if ("minecraft:water".equals(palette.getCompoundOrEmpty(i).getStringOr("Name", ""))) {
                    waterState = i;
                    break;
                }
            }
            Assertions.assertTrue(waterState >= 0, kiriWaterFile + " should still contain framed water");
            net.minecraft.nbt.ListTag blocks = rootTag.getListOrEmpty("blocks");
            for (int i = 0; i < blocks.size(); i++) {
                net.minecraft.nbt.CompoundTag b = blocks.getCompoundOrEmpty(i);
                if (b.getIntOr("state", -1) != waterState) continue;
                net.minecraft.nbt.ListTag pos = b.getListOrEmpty("pos");
                int x = pos.getIntOr(0, 0);
                int z = pos.getIntOr(2, 0);
                Assertions.assertTrue(x > 0 && x < sx - 1 && z > 0 && z < sz - 1,
                        kiriWaterFile + " has unframed boundary water at (" + x + "," + z + ")");
            }
        }

        // 2. Hashirama statue uses minimalist palette (no gold/emerald/copper clutter) and includes visible chest
        for (String statueFile : List.of("valley_hashirama_statue.nbt", "valley_madara_statue.nbt")) {
            net.minecraft.nbt.CompoundTag rootTag = net.minecraft.nbt.NbtIo.readCompressed(
                    structNbtDir.resolve(statueFile), net.minecraft.nbt.NbtAccounter.unlimitedHeap());
            net.minecraft.nbt.ListTag palette = rootTag.getListOrEmpty("palette");
            int chestState = -1;
            for (int i = 0; i < palette.size(); i++) {
                String bName = palette.getCompoundOrEmpty(i).getStringOr("Name", "");
                if ("minecraft:chest".equals(bName)) {
                    chestState = i;
                }
                if ("valley_hashirama_statue.nbt".equals(statueFile)) {
                    Assertions.assertFalse(
                            "minecraft:gold_block".equals(bName)
                                    || "minecraft:emerald_block".equals(bName)
                                    || "minecraft:copper_block".equals(bName),
                            "valley_hashirama_statue.nbt should use minimalist palette without " + bName);
                }
            }
            Assertions.assertTrue(chestState >= 0, statueFile + " must contain a visible chest");
            boolean foundLootChest = false;
            net.minecraft.nbt.ListTag blocks = rootTag.getListOrEmpty("blocks");
            for (int i = 0; i < blocks.size(); i++) {
                net.minecraft.nbt.CompoundTag b = blocks.getCompoundOrEmpty(i);
                if (b.getIntOr("state", -1) == chestState) {
                    String loot = b.getCompoundOrEmpty("nbt").getStringOr("LootTable", "");
                    if ("narutomod:chests/valley_of_the_end".equals(loot)) {
                        foundLootChest = true;
                        break;
                    }
                }
            }
            Assertions.assertTrue(foundLootChest, statueFile + " chest must reference narutomod:chests/valley_of_the_end");
        }
    }
}
