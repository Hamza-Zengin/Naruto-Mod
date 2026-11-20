package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Unit tests for Paket A FAZ 2: Fuma Shuriken Hitbox +50% (1.35F x 0.6F),
 * renderer vertical alignment, and safe launch offset.
 */
public class FumaShurikenSizeTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("FUMA_SHURIKEN hitbox dimensions are scaled +50% to 1.35F x 0.6F")
    public void testFumaShurikenDimensions() throws IOException {
        Assertions.assertEquals(1.35F, NarutoEntities.FUMA_SHURIKEN_WIDTH, 1.0E-5F);
        Assertions.assertEquals(0.6F, NarutoEntities.FUMA_SHURIKEN_HEIGHT, 1.0E-5F);

        if (NarutoEntities.FUMA_SHURIKEN.isBound()) {
            var dimensions = NarutoEntities.FUMA_SHURIKEN.get().getDimensions();
            Assertions.assertEquals(1.35F, dimensions.width(), 1.0E-5F);
            Assertions.assertEquals(0.6F, dimensions.height(), 1.0E-5F);
        } else {
            String entitiesSource = Files.readString(
                    Paths.get("src/main/java/com/sekwah/narutomod/entity/NarutoEntities.java"));
            Assertions.assertTrue(
                    entitiesSource.contains("builder.sized(FUMA_SHURIKEN_WIDTH, FUMA_SHURIKEN_HEIGHT)")
                            || entitiesSource.contains("builder.sized(1.35F, 0.6F)"),
                    "FUMA_SHURIKEN must be registered with 1.35F x 0.6F dimensions");
        }
    }

    @Test
    @DisplayName("FumaShurikenRenderer vertical alignment matches 0.6F hitbox height")
    public void testFumaShurikenRendererOffset() throws IOException {
        String rendererSource = Files.readString(
                Paths.get("src/main/java/com/sekwah/narutomod/client/renderer/entity/FumaShurikenRenderer.java"));
        Assertions.assertTrue(rendererSource.contains("poseStack.translate(0.0D, 0.22D, 0.0D);"),
                "FumaShurikenRenderer must translate by 0.22D on Y axis");
    }

    @Test
    @DisplayName("FumaShurikenEntity safe forward launch offset clears shooter and hitbox radius")
    public void testSafeSpawnForwardOffset() {
        Assertions.assertTrue(FumaShurikenEntity.SAFE_SPAWN_FORWARD_OFFSET >= NarutoEntities.FUMA_SHURIKEN_WIDTH * 0.5D,
                "Forward launch offset should clear half of the 1.35F hitbox width");

        Vec3 spawn = FumaShurikenEntity.computeSafeSpawnPosition(
                10.0D, 65.5D, 20.0D,
                new Vec3(0.0D, 0.0D, 2.0D),
                FumaShurikenEntity.SAFE_SPAWN_FORWARD_OFFSET);
        Assertions.assertEquals(10.0D, spawn.x, 1.0E-5D);
        Assertions.assertEquals(65.5D, spawn.y, 1.0E-5D);
        Assertions.assertEquals(20.0D + FumaShurikenEntity.SAFE_SPAWN_FORWARD_OFFSET, spawn.z, 1.0E-5D);
    }
}
