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
 * Unit tests for Fuma Shuriken 3D Model-Matched Hitbox (2.35F x 0.35F),
 * renderer vertical center alignment (0.175D), and safe launch offset (1.25D).
 */
public class FumaShurikenSizeTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("FUMA_SHURIKEN hitbox dimensions match 3D model at 2.35F x 0.35F")
    public void testFumaShurikenDimensions() throws IOException {
        Assertions.assertEquals(2.35F, NarutoEntities.FUMA_SHURIKEN_WIDTH, 1.0E-5F);
        Assertions.assertEquals(0.35F, NarutoEntities.FUMA_SHURIKEN_HEIGHT, 1.0E-5F);

        if (NarutoEntities.FUMA_SHURIKEN.isBound()) {
            var dimensions = NarutoEntities.FUMA_SHURIKEN.get().getDimensions();
            Assertions.assertEquals(2.35F, dimensions.width(), 1.0E-5F);
            Assertions.assertEquals(0.35F, dimensions.height(), 1.0E-5F);
        } else {
            String entitiesSource = Files.readString(
                    Paths.get("src/main/java/com/sekwah/narutomod/entity/NarutoEntities.java"));
            Assertions.assertTrue(
                    entitiesSource.contains("builder.sized(FUMA_SHURIKEN_WIDTH, FUMA_SHURIKEN_HEIGHT)")
                            || entitiesSource.contains("builder.sized(2.35F, 0.35F)"),
                    "FUMA_SHURIKEN must be registered with 2.35F x 0.35F dimensions");
        }
    }

    @Test
    @DisplayName("FumaShurikenRenderer vertical alignment matches exact center of 0.35F hitbox height (0.175D)")
    public void testFumaShurikenRendererOffset() throws IOException {
        String rendererSource = Files.readString(
                Paths.get("src/main/java/com/sekwah/narutomod/client/renderer/entity/FumaShurikenRenderer.java"));
        Assertions.assertTrue(rendererSource.contains("poseStack.translate(0.0D, 0.175D, 0.0D);"),
                "FumaShurikenRenderer must translate by 0.175D on Y axis");
    }

    @Test
    @DisplayName("FumaShurikenEntity safe forward launch offset is 1.25D and clears 1.175F hitbox radius")
    public void testSafeSpawnForwardOffset() {
        Assertions.assertEquals(1.25D, FumaShurikenEntity.SAFE_SPAWN_FORWARD_OFFSET, 1.0E-5D);
        Assertions.assertTrue(FumaShurikenEntity.SAFE_SPAWN_FORWARD_OFFSET >= NarutoEntities.FUMA_SHURIKEN_WIDTH * 0.5D,
                "Forward launch offset should clear half of the 2.35F hitbox width (1.175F)");

        Vec3 spawn = FumaShurikenEntity.computeSafeSpawnPosition(
                10.0D, 65.5D, 20.0D,
                new Vec3(0.0D, 0.0D, 2.0D),
                FumaShurikenEntity.SAFE_SPAWN_FORWARD_OFFSET);
        Assertions.assertEquals(10.0D, spawn.x, 1.0E-5D);
        Assertions.assertEquals(65.5D, spawn.y, 1.0E-5D);
        Assertions.assertEquals(21.25D, spawn.z, 1.0E-5D);
    }
}
