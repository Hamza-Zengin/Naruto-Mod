package com.sekwah.narutomod;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Audit test ensuring that 3D weapons use Minecraft 26.3 Client Item definitions
 * (minecraft:select with minecraft:display_context) pointing to existing 3D and GUI models,
 * that the 3D models define first-person hand perspectives, and that no legacy
 * neoforge:separate_transforms loaders remain in models/item.
 */
public class ItemModelTransformsTest {

    private static final Path ITEMS_DIR = Paths.get("src/main/resources/assets/narutomod/items");
    private static final Path MODELS_ITEM_DIR = Paths.get("src/main/resources/assets/narutomod/models/item");

    private Path resolveModelPath(String modelId) {
        if (modelId == null) return null;
        if (modelId.startsWith("narutomod:")) {
            String path = modelId.substring("narutomod:".length());
            if (path.startsWith("item/")) {
                path = path.substring("item/".length());
            }
            return MODELS_ITEM_DIR.resolve(path + ".json");
        }
        return null;
    }

    @Test
    public void testClientItemModelsIntegrity() throws IOException {
        List<String> errors = new ArrayList<>();
        int selectDisplayContextCount = 0;

        Assertions.assertTrue(Files.isDirectory(ITEMS_DIR),
                "Client items directory must exist: " + ITEMS_DIR);

        try (Stream<Path> stream = Files.walk(ITEMS_DIR, 1)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path path : files) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject modelObj = json.getAsJsonObject("model");
                    if (modelObj == null) {
                        errors.add(path.getFileName() + ": Missing 'model' object!");
                        continue;
                    }
                    String type = modelObj.has("type") ? modelObj.get("type").getAsString() : "";
                    String property = modelObj.has("property") ? modelObj.get("property").getAsString() : "";
                    if ("minecraft:select".equals(type) && "minecraft:display_context".equals(property)) {
                        selectDisplayContextCount++;

                        // Validate fallback (3D model)
                        JsonObject fallback = modelObj.getAsJsonObject("fallback");
                        if (fallback == null || !fallback.has("model")) {
                            errors.add(path.getFileName() + ": Missing 'fallback.model'!");
                        } else {
                            String fallbackModel = fallback.get("model").getAsString();
                            Path fallbackPath = resolveModelPath(fallbackModel);
                            if (fallbackPath == null || !Files.exists(fallbackPath)) {
                                errors.add(path.getFileName() + ": Fallback 3D model '" + fallbackModel + "' does not resolve to an existing file!");
                            } else {
                                try (Reader baseReader = Files.newBufferedReader(fallbackPath)) {
                                    JsonObject baseJson = JsonParser.parseReader(baseReader).getAsJsonObject();
                                    if (!baseJson.has("display") || !baseJson.getAsJsonObject("display").has("firstperson_righthand")) {
                                        errors.add(path.getFileName() + ": Fallback 3D model '" + fallbackModel + "' missing 'firstperson_righthand' display!");
                                    }
                                }
                            }
                        }

                        // Validate cases (gui, ground, fixed -> _gui model)
                        JsonArray cases = modelObj.getAsJsonArray("cases");
                        if (cases == null || cases.isEmpty()) {
                            errors.add(path.getFileName() + ": Missing 'cases' array!");
                        } else {
                            Set<String> coveredContexts = new HashSet<>();
                            for (JsonElement caseElem : cases) {
                                JsonObject caseObj = caseElem.getAsJsonObject();
                                JsonArray whenArr = caseObj.getAsJsonArray("when");
                                if (whenArr != null) {
                                    for (JsonElement w : whenArr) {
                                        coveredContexts.add(w.getAsString());
                                    }
                                }
                                JsonObject caseModel = caseObj.getAsJsonObject("model");
                                if (caseModel == null || !caseModel.has("model")) {
                                    errors.add(path.getFileName() + ": Case missing 'model.model'!");
                                } else {
                                    String guiModel = caseModel.get("model").getAsString();
                                    Path guiPath = resolveModelPath(guiModel);
                                    if (guiPath == null || !Files.exists(guiPath)) {
                                        errors.add(path.getFileName() + ": GUI model '" + guiModel + "' does not resolve to an existing file!");
                                    }
                                }
                            }
                            if (!coveredContexts.contains("gui")) {
                                errors.add(path.getFileName() + ": Missing display_context 'gui' in cases!");
                            }
                        }
                    }
                }
            }
        }

        // Ensure no legacy neoforge:separate_transforms remain in models/item and every item model has a client item definition
        try (Stream<Path> stream = Files.walk(MODELS_ITEM_DIR, 1)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path path : files) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonElement loaderElem = json.get("loader");
                    if (loaderElem != null && "neoforge:separate_transforms".equals(loaderElem.getAsString())) {
                        errors.add(path.getFileName() + ": Still uses removed 'neoforge:separate_transforms' loader!");
                    }
                }
                Path clientItemPath = ITEMS_DIR.resolve(path.getFileName());
                if (!Files.exists(clientItemPath)) {
                    errors.add(path.getFileName() + ": Missing client item definition at " + clientItemPath);
                }
            }
        }

        // Ensure all 3D weapon models declare a particle texture
        Path weaponsDir = MODELS_ITEM_DIR.resolve("weapons");
        if (Files.isDirectory(weaponsDir)) {
            try (Stream<Path> stream = Files.walk(weaponsDir, 1)) {
                List<Path> files = stream.filter(p -> p.toString().endsWith("_3d.json")).toList();
                for (Path path : files) {
                    try (Reader reader = Files.newBufferedReader(path)) {
                        JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                        JsonObject textures = json.getAsJsonObject("textures");
                        if (textures == null || !textures.has("particle")) {
                            errors.add(path.getFileName() + ": Missing 'textures.particle' reference!");
                        }
                    }
                }
            }
        }

        // Ensure conflicting 1.20.1 blocks.json atlas override does not exist
        Path conflictingAtlas = Paths.get("src/main/resources/assets/minecraft/atlases/blocks.json");
        Assertions.assertFalse(Files.exists(conflictingAtlas),
                "Conflicting blocks.json atlas override must be removed for 26.3: " + conflictingAtlas);

        Assertions.assertTrue(selectDisplayContextCount >= 8,
                "Expected at least 8 weapon items to use minecraft:select + minecraft:display_context, but found " + selectDisplayContextCount);
        Assertions.assertTrue(errors.isEmpty(),
                "Found " + errors.size() + " item model errors:\n" + String.join("\n", errors));
    }

    @Test
    public void testRedesigned3DWeaponModelsAndTextures() throws IOException {
        Path texDir = Paths.get("src/main/resources/assets/narutomod/textures/item/weapons");
        for (String weapon : List.of("anbu_tanto", "chakra_blades", "kunai", "explosive_kunai", "samehada", "gunbai_uchiwa", "shuriken", "kusanagi_sword")) {
            Path modelPath = MODELS_ITEM_DIR.resolve("weapons").resolve(weapon + "_3d.json");
            Assertions.assertTrue(Files.isRegularFile(modelPath), "Missing 3D weapon model: " + modelPath);
            JsonObject root = JsonParser.parseString(Files.readString(modelPath)).getAsJsonObject();
            JsonArray elements = root.getAsJsonArray("elements");
            Assertions.assertTrue(elements != null && elements.size() >= 24,
                    weapon + "_3d.json must contain >= 24 sculpted elements for realistic geometry, found " + (elements == null ? 0 : elements.size()));

            double minThickness = Double.MAX_VALUE;
            for (JsonElement el : elements) {
                JsonObject obj = el.getAsJsonObject();
                JsonArray from = obj.getAsJsonArray("from");
                JsonArray to = obj.getAsJsonArray("to");
                for (int axis = 0; axis < 3; axis++) {
                    double dim = Math.abs(to.get(axis).getAsDouble() - from.get(axis).getAsDouble());
                    if (dim > 0.001 && dim < minThickness) {
                        minThickness = dim;
                    }
                }
            }
            Assertions.assertTrue(minThickness <= 0.16,
                    weapon + "_3d.json must have tapered razor/detail edge <= 0.16 thickness (found " + minThickness + ")");

            Path texPath = texDir.resolve(weapon + "_3d.png");
            Assertions.assertTrue(Files.isRegularFile(texPath), "Missing 3D weapon texture: " + texPath);
            java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(texPath.toFile());
            Assertions.assertNotNull(img);
            Assertions.assertEquals(64, img.getWidth(), weapon + "_3d.png width must be 64px");
            Assertions.assertEquals(64, img.getHeight(), weapon + "_3d.png height must be 64px");
        }
    }
}
