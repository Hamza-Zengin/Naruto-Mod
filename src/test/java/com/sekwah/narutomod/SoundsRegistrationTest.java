package com.sekwah.narutomod;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Unit test verifying that all SoundEvents declared in NarutoSounds.java are properly mapped in sounds.json.
 */
public class SoundsRegistrationTest {

    private static final Path SOUNDS_JAVA = Paths.get("src/main/java/com/sekwah/narutomod/sounds/NarutoSounds.java");
    private static final Path SOUNDS_JSON = Paths.get("src/main/resources/assets/narutomod/sounds.json");
    private static final Pattern REGISTER_PATTERN = Pattern.compile("register\\(\"([^\"]+)\"\\)");
    private static final Pattern JSON_KEY_PATTERN = Pattern.compile("^\\s*\"([^\"]+)\"\\s*:\\s*\\{");

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
}
