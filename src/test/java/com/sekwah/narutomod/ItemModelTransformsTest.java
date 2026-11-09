package com.sekwah.narutomod;

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
import java.util.List;
import java.util.stream.Stream;

/**
 * Audit test ensuring that 3D weapons using forge:separate_transforms correctly point
 * to existing 3D and GUI models, and that the 3D models define first-person hand perspectives.
 */
public class ItemModelTransformsTest {

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
    public void testSeparateTransformsModelsIntegrity() throws IOException {
        List<String> errors = new ArrayList<>();
        int separateTransformsCount = 0;

        try (Stream<Path> stream = Files.walk(MODELS_ITEM_DIR, 1)) {
            List<Path> files = stream.filter(p -> p.toString().endsWith(".json")).toList();
            for (Path path : files) {
                try (Reader reader = Files.newBufferedReader(path)) {
                    JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonElement loaderElem = json.get("loader");
                    if (loaderElem != null && "forge:separate_transforms".equals(loaderElem.getAsString())) {
                        separateTransformsCount++;
                        String baseParent = json.getAsJsonObject("base").get("parent").getAsString();
                        Path basePath = resolveModelPath(baseParent);
                        if (basePath == null || !Files.exists(basePath)) {
                            errors.add(path.getFileName() + ": Base parent '" + baseParent + "' does not resolve to an existing file!");
                        } else {
                            try (Reader baseReader = Files.newBufferedReader(basePath)) {
                                JsonObject baseJson = JsonParser.parseReader(baseReader).getAsJsonObject();
                                if (!baseJson.has("display") || !baseJson.getAsJsonObject("display").has("firstperson_righthand")) {
                                    errors.add(path.getFileName() + ": Base 3D model '" + baseParent + "' missing 'firstperson_righthand' display!");
                                }
                            }
                        }

                        JsonObject perspectives = json.getAsJsonObject("perspectives");
                        if (perspectives != null && perspectives.has("gui")) {
                            String guiParent = perspectives.getAsJsonObject("gui").get("parent").getAsString();
                            Path guiPath = resolveModelPath(guiParent);
                            if (guiPath == null || !Files.exists(guiPath)) {
                                errors.add(path.getFileName() + ": GUI parent '" + guiParent + "' does not resolve to an existing file!");
                            }
                        } else {
                            errors.add(path.getFileName() + ": Missing 'perspectives.gui'!");
                        }
                    }
                }
            }
        }

        Assertions.assertTrue(separateTransformsCount >= 8,
                "Expected at least 8 weapon items to use forge:separate_transforms, but found " + separateTransformsCount);
        Assertions.assertTrue(errors.isEmpty(),
                "Found " + errors.size() + " separate_transforms model errors:\n" + String.join("\n", errors));
    }
}
