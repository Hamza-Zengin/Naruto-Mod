package com.sekwah.narutomod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit test verifying that all localization JSON files are consistent with en_us.json.
 */
public class LangParityTest {

    private static final Path LANG_DIR = Paths.get("src/main/resources/assets/narutomod/lang");
    private static final Pattern KEY_PATTERN = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\"(.*)\"\\s*,?\\s*$");

    private Map<String, String> parseSimpleJson(Path file) throws Exception {
        Map<String, String> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(file);
        for (String line : lines) {
            Matcher m = KEY_PATTERN.matcher(line);
            if (m.matches()) {
                map.put(m.group(1), m.group(2));
            }
        }
        return map;
    }

    @Test
    public void testLangFilesExistAndMatchEnUs() throws Exception {
        Path enPath = LANG_DIR.resolve("en_us.json");
        Assertions.assertTrue(Files.exists(enPath), "en_us.json must exist!");

        Map<String, String> enKeys = parseSimpleJson(enPath);
        Assertions.assertFalse(enKeys.isEmpty(), "en_us.json should not be empty!");

        try (var stream = Files.list(LANG_DIR)) {
            List<Path> langFiles = stream
                .filter(p -> p.toString().endsWith(".json") && !p.getFileName().toString().equals("en_us.json"))
                .toList();

            Assertions.assertFalse(langFiles.isEmpty(), "At least one additional language file must be present!");

            for (Path langPath : langFiles) {
                Map<String, String> targetKeys = parseSimpleJson(langPath);
                Set<String> missing = new HashSet<>(enKeys.keySet());
                missing.removeAll(targetKeys.keySet());

                Assertions.assertTrue(missing.isEmpty(),
                    "Language file " + langPath.getFileName() + " is missing " + missing.size() + " keys: " + missing);
            }
        }
    }
}
