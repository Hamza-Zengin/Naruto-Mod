package com.sekwah.narutomod;

import com.sekwah.narutomod.gamerules.NarutoGameRules;
import com.sekwah.narutomod.util.WorldProtectionHelper;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link NarutoGameRules} and {@link WorldProtectionHelper} wiring and safety defaults.
 */
public class WorldProtectionHelperTest {

    @Test
    @DisplayName("WorldProtectionHelper returns false when level is null")
    public void testNullLevelReturnsFalse() {
        assertFalse(WorldProtectionHelper.canModifyBlock(null, BlockPos.ZERO, null));
    }

    @Test
    @DisplayName("WorldProtectionHelper and NarutoGameRules wire naruto_jutsu_griefing, NarutoConfig, and BreakBlockEvent")
    public void testProtectionWiring() throws IOException {
        String gameRulesSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/gamerules/NarutoGameRules.java"));
        assertTrue(gameRulesSrc.contains("naruto_jutsu_griefing"), "NarutoGameRules must register naruto_jutsu_griefing");
        assertTrue(gameRulesSrc.contains("Boolean.TRUE"), "naruto_jutsu_griefing must default to true");

        String helperSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/util/WorldProtectionHelper.java"));
        assertTrue(helperSrc.contains("NARUTO_JUTSU_GRIEFING"), "WorldProtectionHelper must check NARUTO_JUTSU_GRIEFING");
        assertTrue(helperSrc.contains("NarutoConfig.jutsuWorldDamage"), "WorldProtectionHelper must check NarutoConfig.jutsuWorldDamage");
        assertTrue(helperSrc.contains("BreakBlockEvent"), "WorldProtectionHelper must post BreakBlockEvent for ServerPlayer");
        assertTrue(helperSrc.contains("isCanceled()"), "WorldProtectionHelper must respect BreakBlockEvent cancellation");

        for (String path : new String[]{
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/FireballJutsuEntity.java",
                "src/main/java/com/sekwah/narutomod/entity/jutsuprojectile/RasenshurikenEntity.java",
                "src/main/java/com/sekwah/narutomod/block/weapons/PaperBombBlock.java",
                "src/main/java/com/sekwah/narutomod/entity/item/PaperBombEntity.java",
                "src/main/java/com/sekwah/narutomod/abilities/jutsus/AmaterasuAbility.java"
        }) {
            String src = Files.readString(Paths.get(path));
            assertTrue(src.contains("WorldProtectionHelper."), path + " must route world modifications through WorldProtectionHelper");
        }
    }
}
