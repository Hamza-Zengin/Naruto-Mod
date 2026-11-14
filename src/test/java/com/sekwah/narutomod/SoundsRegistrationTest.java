package com.sekwah.narutomod;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Unit test verifying that all SoundEvents declared in NarutoSounds.java are properly mapped in sounds.json
 * and that no duplicate placeholder .ogg files exist.
 */
public class SoundsRegistrationTest {

    private static final Path SOUNDS_JAVA = Paths.get("src/main/java/com/sekwah/narutomod/sounds/NarutoSounds.java");
    private static final Path SOUNDS_JSON = Paths.get("src/main/resources/assets/narutomod/sounds.json");
    private static final Path SOUNDS_DIR = Paths.get("src/main/resources/assets/narutomod/sounds");
    private static final Pattern REGISTER_PATTERN = Pattern.compile("register\\(\"([^\"]+)\"\\)");
    private static final Pattern JSON_KEY_PATTERN = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\\{");
    private static final Set<String> ALLOWED_DUPLICATES = Set.of();

    @Test
    public void testAllRegisteredSoundsExistInSoundsJson() throws IOException {
        Assertions.assertTrue(Files.exists(SOUNDS_JAVA), "NarutoSounds.java must exist!");
        Assertions.assertTrue(Files.exists(SOUNDS_JSON), "sounds.json must exist!");

        String javaContent = Files.readString(SOUNDS_JAVA);
        Set<String> javaSounds = new HashSet<>();
        Matcher matcher = REGISTER_PATTERN.matcher(javaContent);
        while (matcher.find()) {
            javaSounds.add(matcher.group(1));
        }

        Assertions.assertFalse(javaSounds.isEmpty(), "At least one SoundEvent must be registered!");

        Set<String> jsonKeys = new HashSet<>();
        List<String> lines = Files.readAllLines(SOUNDS_JSON);
        for (String line : lines) {
            Matcher m = JSON_KEY_PATTERN.matcher(line);
            if (m.find()) {
                jsonKeys.add(m.group(1));
            }
        }

        Set<String> missing = new HashSet<>(javaSounds);
        missing.removeAll(jsonKeys);

        Assertions.assertTrue(missing.isEmpty(),
                "SoundEvents declared in NarutoSounds.java but missing in sounds.json: " + missing);
    }

    @Test
    public void testNoDuplicatePlaceholderSoundFiles() throws IOException, NoSuchAlgorithmException {
        Assertions.assertTrue(Files.isDirectory(SOUNDS_DIR), "Sounds directory must exist!");

        Map<String, List<String>> hashToFiles = new LinkedHashMap<>();
        MessageDigest digest = MessageDigest.getInstance("SHA-256");

        try (Stream<Path> walk = Files.walk(SOUNDS_DIR)) {
            List<Path> oggFiles = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".ogg"))
                    .sorted()
                    .toList();

            Assertions.assertFalse(oggFiles.isEmpty(), "At least one .ogg sound file must exist!");

            for (Path ogg : oggFiles) {
                String relPath = SOUNDS_DIR.relativize(ogg).toString().replace('\\', '/');
                if (ALLOWED_DUPLICATES.contains(relPath)) {
                    continue;
                }
                byte[] bytes = Files.readAllBytes(ogg);
                byte[] hashBytes = digest.digest(bytes);
                StringBuilder hex = new StringBuilder(hashBytes.length * 2);
                for (byte b : hashBytes) {
                    hex.append(String.format("%02x", b));
                }
                hashToFiles.computeIfAbsent(hex.toString(), k -> new ArrayList<>()).add(relPath);
            }
        }

        List<String> duplicateGroups = new ArrayList<>();
        for (Map.Entry<String, List<String>> entry : hashToFiles.entrySet()) {
            if (entry.getValue().size() > 1) {
                duplicateGroups.add(entry.getKey().substring(0, 12) + "... -> " + entry.getValue());
            }
        }

        Assertions.assertTrue(duplicateGroups.isEmpty(),
                "Duplicate placeholder .ogg files detected (identical SHA-256): " + duplicateGroups);
    }
}
