package com.sekwah.narutomod;

import com.sekwah.narutomod.block.AmaterasuFireBlock;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.FireBlock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AmaterasuFireBlock, NarutoConfig duration/permission settings,
 * NarutoBlocks registration, and AmaterasuAbility integration.
 */
public class AmaterasuFireBlockTest {

    @Test
    @DisplayName("AmaterasuFireBlock extends BaseFireBlock and NOT FireBlock so it never spreads")
    public void testAmaterasuFireBlockHierarchy() {
        assertTrue(BaseFireBlock.class.isAssignableFrom(AmaterasuFireBlock.class),
                "AmaterasuFireBlock must extend BaseFireBlock");
        assertFalse(FireBlock.class.isAssignableFrom(AmaterasuFireBlock.class),
                "AmaterasuFireBlock must NOT extend FireBlock to avoid uncontrolled fire spread");
    }

    @Test
    @DisplayName("NarutoConfig defines amaterasuDurationTicks (200) and block placement permissions")
    public void testNarutoConfigDefaults() {
        assertEquals(200, NarutoConfig.amaterasuDurationTicks,
                "Default Amaterasu duration should be 200 ticks (10 seconds)");
        assertTrue(NarutoConfig.jutsuWorldDamage,
                "jutsuWorldDamage should default to true");
        assertTrue(NarutoConfig.amaterasuPlaceFire,
                "amaterasuPlaceFire should default to true");
    }

    @Test
    @DisplayName("NarutoBlocks registers AMATERASU_FIRE and AmaterasuAbility places it with config checks")
    public void testRegistrationAndAbilityIntegration() throws IOException {
        String blocksSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/block/NarutoBlocks.java"));
        assertTrue(blocksSrc.contains("AMATERASU_FIRE = BLOCKS.registerBlock(\"amaterasu_fire\""),
                "NarutoBlocks must register amaterasu_fire");

        String abilitySrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/abilities/jutsus/AmaterasuAbility.java"));
        assertFalse(abilitySrc.contains("Blocks.SOUL_FIRE"),
                "AmaterasuAbility must no longer place vanilla SOUL_FIRE");
        assertTrue(abilitySrc.contains("NarutoBlocks.AMATERASU_FIRE"),
                "AmaterasuAbility must place NarutoBlocks.AMATERASU_FIRE");
        assertTrue(abilitySrc.contains("NarutoConfig.jutsuWorldDamage")
                        && abilitySrc.contains("NarutoConfig.amaterasuPlaceFire"),
                "AmaterasuAbility must check NarutoConfig world damage & fire placement permissions");

        String fireBlockSrc = Files.readString(Paths.get("src/main/java/com/sekwah/narutomod/block/AmaterasuFireBlock.java"));
        assertTrue(fireBlockSrc.contains("scheduleTick"),
                "AmaterasuFireBlock must use scheduleTick to self-extinguish");
        assertTrue(fireBlockSrc.contains("NarutoConfig.amaterasuDurationTicks"),
                "AmaterasuFireBlock must read NarutoConfig.amaterasuDurationTicks");
    }
}
