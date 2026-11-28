package com.sekwah.narutomod;

import com.sekwah.narutomod.block.MissionBoardBlock;
import com.sekwah.narutomod.entity.projectile.FlashBombEntity;
import com.sekwah.narutomod.item.weapons.FlashBombItem;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

public class FlashBombAndMissionBoardTest {

    @Test
    public void testRegistryAndAssetWiring() throws Exception {
        String itemsSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/item/NarutoItems.java"));
        String blocksSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/block/NarutoBlocks.java"));
        String entitiesSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/entity/NarutoEntities.java"));
        String renderSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/client/renderer/NarutoRenderEvents.java"));

        assertTrue(itemsSrc.contains("FLASH_BOMB"));
        assertTrue(blocksSrc.contains("MISSION_BOARD"));
        assertTrue(entitiesSrc.contains("FLASH_BOMB"));
        assertTrue(renderSrc.contains("FlashBombRenderer"));

        assertTrue(Files.exists(Paths.get("src/main/resources/data/narutomod/recipe/flash_bomb.json")));
        assertTrue(Files.exists(Paths.get("src/main/resources/data/narutomod/recipe/mission_board.json")));
        assertTrue(Files.exists(Paths.get("src/main/resources/data/narutomod/loot_table/blocks/mission_board.json")));
    }

    @Test
    public void testClassContracts() {
        assertTrue(FlashBombItem.class.getSuperclass().getSimpleName().equals("Item"));
        assertTrue(FlashBombEntity.class.getSuperclass().getSimpleName().equals("ThrowableItemProjectile"));
        assertTrue(MissionBoardBlock.class.getSuperclass().getSimpleName().equals("RotatableDecorativeBlock"));
        assertEquals(9, MissionBoardBlock.BoardPart.values().length);
    }

    @Test
    public void testMissionBoardMultiBlockAssets() throws Exception {
        String[] parts = {
                "bottom_left", "bottom_center", "bottom_right",
                "mid_left", "mid_center", "mid_right",
                "top_left", "top_center", "top_right"
        };
        String[] facings = {"north", "south", "east", "west"};

        String blockstateJson = Files.readString(Paths.get("src/main/resources/assets/narutomod/blockstates/mission_board.json"));
        for (String part : parts) {
            assertTrue(Files.exists(Paths.get("src/main/resources/assets/narutomod/models/block/mission_board_" + part + ".json")),
                    "Missing 3x3 block model for part: " + part);
            for (String facing : facings) {
                assertTrue(blockstateJson.contains("facing=" + facing + ",part=" + part),
                        "Missing blockstate variant for facing=" + facing + ",part=" + part);
            }
        }
        assertFalse(Files.exists(Paths.get("src/main/resources/assets/narutomod/models/block/mission_board_lower.json")));
        assertFalse(Files.exists(Paths.get("src/main/resources/assets/narutomod/models/block/mission_board_upper.json")));

        String lootJson = Files.readString(Paths.get("src/main/resources/data/narutomod/loot_table/blocks/mission_board.json"));
        assertTrue(lootJson.contains("\"part\": \"bottom_center\""));
        assertFalse(lootJson.contains("\"half\": \"lower\""));
    }
}
