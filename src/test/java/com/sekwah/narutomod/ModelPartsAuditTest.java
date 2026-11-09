package com.sekwah.narutomod;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Audit test preventing crashes such as NoSuchElementException from getChild calls on model parts.
 */
public class ModelPartsAuditTest {

    private static final Path SRC_MAIN_JAVA = Paths.get("src/main/java");
    private static final Pattern GET_CHILD_PATTERN = Pattern.compile("\\.getChild\\(\"([^\"]+)\"\\)");

    @Test
    public void testAllGetChildCallsHaveMatchingAddOrReplaceChild() throws IOException {
        List<String> errors = new ArrayList<>();

        try (Stream<Path> stream = Files.walk(SRC_MAIN_JAVA)) {
            stream.filter(p -> p.toString().endsWith(".java")).forEach(path -> {
                try {
                    String content = Files.readString(path);
                    List<String> lines = Files.readAllLines(path);
                    for (int i = 0; i < lines.size(); i++) {
                        String line = lines.get(i);
                        Matcher matcher = GET_CHILD_PATTERN.matcher(line);
                        while (matcher.find()) {
                            String part = matcher.group(1);
                            if (!content.contains("addOrReplaceChild(\"" + part + "\"")) {
                                errors.add(path.getFileName() + ":" + (i + 1) + " - ModelPart '" + part
                                        + "' called via getChild() but not defined with addOrReplaceChild()!");
                            }
                        }
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        Assertions.assertTrue(errors.isEmpty(),
                "Found " + errors.size() + " model part inconsistency errors:\n" + String.join("\n", errors));
    }
}
