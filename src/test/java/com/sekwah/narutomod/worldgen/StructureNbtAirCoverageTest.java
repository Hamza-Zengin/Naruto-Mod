package com.sekwah.narutomod.worldgen;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Verifies that all village, shrine, and arena NBT structure files explicitly
 * include block entries (padded with {@code minecraft:air}) for every voxel at {@code y >= 1}.
 */
public class StructureNbtAirCoverageTest {

    private static final Path STRUCTURE_DIR = Path.of("src/main/resources/data/narutomod/structure");

    private static final List<String> TARGET_PREFIXES = List.of(
            "konoha_",
            "suna_",
            "kiri_",
            "iwa_",
            "kumo_",
            "chunin_",
            "akatsuki_",
            "myobokuzan_",
            "uchiha_"
    );

    private static final Set<String> EXCLUDED_FILES = Set.of(
            "empty.nbt",
            "kumo_sky_bridge.nbt"
    );

    private static boolean isTargetStructure(Path path) {
        String name = path.getFileName().toString();
        if (!name.endsWith(".nbt") || EXCLUDED_FILES.contains(name)) {
            return false;
        }
        return TARGET_PREFIXES.stream().anyMatch(name::startsWith);
    }

    @Test
    public void testAllVillageShrineArenaStructuresHaveFullAirCoverageAboveY0() throws IOException {
        Assertions.assertTrue(Files.isDirectory(STRUCTURE_DIR), "Expected structure directory at " + STRUCTURE_DIR);

        List<Path> targetFiles;
        try (Stream<Path> paths = Files.list(STRUCTURE_DIR)) {
            targetFiles = paths.filter(StructureNbtAirCoverageTest::isTargetStructure)
                    .sorted()
                    .toList();
        }

        Assertions.assertFalse(targetFiles.isEmpty(), "Expected matching village/shrine/arena .nbt files");

        for (Path nbtPath : targetFiles) {
            String fileName = nbtPath.getFileName().toString();
            CompoundTag root;
            try (InputStream in = Files.newInputStream(nbtPath)) {
                root = NbtIo.readCompressed(in, NbtAccounter.unlimitedHeap());
            }

            ListTag sizeTag = root.getListOrEmpty("size");
            Assertions.assertEquals(3, sizeTag.size(), "Invalid size tag in " + fileName);
            int sizeX = sizeTag.getIntOr(0, 0);
            int sizeY = sizeTag.getIntOr(1, 0);
            int sizeZ = sizeTag.getIntOr(2, 0);

            ListTag paletteTag = root.getListOrEmpty("palette");
            boolean hasAirInPalette = false;
            for (int i = 0; i < paletteTag.size(); i++) {
                CompoundTag entry = paletteTag.getCompoundOrEmpty(i);
                if ("minecraft:air".equals(entry.getStringOr("Name", ""))) {
                    hasAirInPalette = true;
                    break;
                }
            }
            Assertions.assertTrue(hasAirInPalette, "Missing minecraft:air in palette for " + fileName);

            ListTag blocksTag = root.getListOrEmpty("blocks");
            int expectedLayerSize = sizeX * sizeZ;
            int expectedMinBlocksAboveY0 = sizeX * (sizeY - 1) * sizeZ;

            int[] blocksPerY = new int[sizeY];
            int blocksAtOrAboveY1 = 0;
            for (int i = 0; i < blocksTag.size(); i++) {
                CompoundTag blockTag = blocksTag.getCompoundOrEmpty(i);
                ListTag posTag = blockTag.getListOrEmpty("pos");
                int y = posTag.getIntOr(1, -1);
                if (y >= 0 && y < sizeY) {
                    blocksPerY[y]++;
                }
                if (y >= 1) {
                    blocksAtOrAboveY1++;
                }
            }

            Assertions.assertTrue(
                    blocksAtOrAboveY1 >= expectedMinBlocksAboveY0,
                    fileName + " lacks full y >= 1 coverage: got " + blocksAtOrAboveY1
                            + " blocks for y >= 1, expected >= " + expectedMinBlocksAboveY0
            );

            for (int y = 1; y < sizeY; y++) {
                Assertions.assertTrue(
                        blocksPerY[y] >= expectedLayerSize,
                        fileName + " at y=" + y + " has " + blocksPerY[y]
                                + " blocks, expected >= " + expectedLayerSize
                );
            }
        }
    }
}
