package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.jutsus.HiraishinAbility;
import com.sekwah.narutomod.capabilities.HiraishinMarkerTracker;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class HiraishinTest {

    @Test
    public void testHiraishinAbilityMetadataAndJson() throws Exception {
        HiraishinAbility ability = new HiraishinAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(NinjaRank.JONIN, ability.getRequiredRank());
        assertTrue(ability.isWheelEquippable());
        assertTrue(ability.playsHandSealSounds());
        assertEquals(30.0f, ability.getDefaultChakraCost());
        assertEquals(50, ability.getDefaultCooldown());
        assertEquals(50, ability.getCooldown(0));
        assertEquals(35, ability.getCooldown(500));
        assertEquals(20, ability.getCooldown(1000));
        assertEquals(96.0D, HiraishinMarkerTracker.getMaxTeleportRange(0), 0.001D);
        assertEquals(144.0D, HiraishinMarkerTracker.getMaxTeleportRange(500), 0.001D);
        assertEquals(192.0D, HiraishinMarkerTracker.getMaxTeleportRange(1000), 0.001D);
        assertEquals(1200L, HiraishinMarkerTracker.getMarkerDurationTicks(0));
        assertEquals(3000L, HiraishinMarkerTracker.getMarkerDurationTicks(1000));

        assertTrue(Files.exists(Paths.get("src/main/resources/data/narutomod/jutsus/hiraishin.json")));
        assertTrue(Files.exists(Paths.get("src/main/resources/data/narutomod/recipe/hiraishin_kunai.json")));
    }

    @Test
    public void testMarkerEvaluationRangeExpiryAndDimension() {
        UUID kunaiId = UUID.randomUUID();
        ResourceKey<Level> overworld = Level.OVERWORLD;
        ResourceKey<Level> nether = Level.NETHER;

        HiraishinMarkerTracker.MarkerEntry entry = new HiraishinMarkerTracker.MarkerEntry(
                kunaiId, overworld, 100L + HiraishinMarkerTracker.MARKER_DURATION_TICKS);

        assertEquals(HiraishinMarkerTracker.MarkerStatus.NO_MARKER,
                HiraishinMarkerTracker.evaluateMarker(null, overworld, Vec3.ZERO, Vec3.ZERO, true, 200L));

        assertEquals(HiraishinMarkerTracker.MarkerStatus.ACTIVE,
                HiraishinMarkerTracker.evaluateMarker(entry, overworld, new Vec3(0, 64, 0), new Vec3(50, 64, 0), true, 500L));

        assertEquals(HiraishinMarkerTracker.MarkerStatus.OUT_OF_RANGE,
                HiraishinMarkerTracker.evaluateMarker(entry, overworld, new Vec3(0, 64, 0), new Vec3(120, 64, 0), true, 500L));

        // Mastery-scaled max range allows 120 blocks at high mastery
        assertEquals(HiraishinMarkerTracker.MarkerStatus.ACTIVE,
                HiraishinMarkerTracker.evaluateMarker(entry, overworld, new Vec3(0, 64, 0), new Vec3(120, 64, 0), true, 500L, 192.0D));

        assertEquals(HiraishinMarkerTracker.MarkerStatus.WRONG_DIMENSION,
                HiraishinMarkerTracker.evaluateMarker(entry, nether, new Vec3(0, 64, 0), new Vec3(10, 64, 0), true, 500L));

        assertEquals(HiraishinMarkerTracker.MarkerStatus.EXPIRED,
                HiraishinMarkerTracker.evaluateMarker(entry, overworld, new Vec3(0, 64, 0), new Vec3(10, 64, 0), true, 2000L));

        NinjaData geninData = new NinjaData(true);
        geninData.setRank(NinjaRank.GENIN.name());
        assertFalse(new HiraishinAbility().canActivate(null, geninData), "Genin cannot activate Jonin-rank Hiraishin");

        NinjaData joninData = new NinjaData(true);
        joninData.setRank(NinjaRank.JONIN.name());
        assertTrue(new HiraishinAbility().canActivate(null, joninData), "Jonin passes rank gating for Hiraishin");
        assertEquals(HiraishinMarkerTracker.MarkerStatus.NO_MARKER,
                HiraishinMarkerTracker.resolveMarkerStatus(null, 0L).status());
    }
}
