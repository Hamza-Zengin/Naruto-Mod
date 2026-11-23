package com.sekwah.narutomod;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.jutsus.*;
import com.sekwah.narutomod.abilities.utility.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Audits all 36 jutsu/utility abilities, their JSON definitions, activation-delay rules,
 * and the 8-slot Jutsu Radial Wheel angle-to-slot mapping:
 * 1. Every JSON file exists and does NOT define a legacy "combo" field.
 * 2. Exactly 3 abilities (chakra_charge, water_walk, leap) are utility abilities.
 * 3. Ability.requiresActivationDelay() is false for utility abilities and true for chakra-costing combat jutsus.
 * 4. JutsuWheelOverlay.computeHoveredSlot maps deadzone and all 8 radial sectors accurately.
 */
public class JutsuComboAuditTest {

    private static final Gson GSON = new Gson();
    private static final Path JUTSU_DIR = Paths.get("src/main/resources/data/narutomod/jutsus");

    private static final Map<String, Ability> ALL_ABILITIES = Map.ofEntries(
            // [A] Basic & Movement
            Map.entry("chakra_charge", new ChakraChargeAbility()),
            Map.entry("water_walk", new WaterWalkAbility()),
            Map.entry("leap", new LeapAbility()),
            Map.entry("kawarimi", new KawarimiJutsuAbility()),
            Map.entry("fireball", new FireballJutsuAbility()),
            Map.entry("water_bullet", new WaterBulletAbility()),
            Map.entry("wind_breaker", new WindBreakerAbility()),
            Map.entry("mud_wall", new MudWallAbility()),
            Map.entry("earth_dragon_bullet", new EarthDragonBulletAbility()),
            Map.entry("vacuum_spheres", new VacuumSpheresAbility()),
            Map.entry("phoenix_flower", new PhoenixFlowerAbility()),
            Map.entry("mystical_palm", new MysticalPalmAbility()),
            Map.entry("water_encampment_wall", new WaterEncampmentWallAbility()),
            Map.entry("chakra_scalpel", new ChakraScalpelAbility()),
            Map.entry("summoning", new SummoningAbility()),
            // [B] High Level
            Map.entry("sharingan", new SharinganAbility()),
            Map.entry("byakugan", new ByakuganAbility()),
            Map.entry("shadow_clone", new ShadowCloneAbility()),
            Map.entry("shadow_possession", new ShadowPossessionAbility()),
            Map.entry("chidori", new ChidoriAbility()),
            Map.entry("chidori_stream", new ChidoriStreamAbility()),
            Map.entry("rasengan", new RasenganAbility()),
            Map.entry("stone_spikes", new StoneSpikesAbility()),
            Map.entry("water_dragon", new WaterDragonAbility()),
            Map.entry("ash_pile_burning", new AshPileBurningAbility()),
            Map.entry("hakke_kusho", new HakkeKushoAbility()),
            Map.entry("bone_dance", new BoneDanceAbility()),
            Map.entry("wood_prison", new WoodPrisonAbility()),
            Map.entry("adamantine_chains", new AdamantineChainsAbility()),
            Map.entry("eight_gates", new EightGatesAbility()),
            // [C] Ultimate
            Map.entry("rasenshuriken", new RasenshurikenAbility()),
            Map.entry("kirin", new KirinAbility()),
            Map.entry("amaterasu", new AmaterasuAbility()),
            Map.entry("tsukuyomi", new TsukuyomiAbility()),
            Map.entry("susanoo_ribcage", new SusanooRibcageAbility()),
            Map.entry("eight_gates_death", new EighthGateOfDeathAbility())
    );

    private static final Set<String> EXPECTED_UTILITIES = Set.of("chakra_charge", "water_walk", "leap");

    @Test
    @DisplayName("Verify all 36 jutsu JSONs omit 'combo', utility count is 3, and requiresActivationDelay matches")
    public void testAllAbilitiesAndJsonAudit() throws IOException {
        assertTrue(Files.isDirectory(JUTSU_DIR), "Jutsu data directory must exist");
        assertEquals(36, ALL_ABILITIES.size(), "Audit map must cover all 36 abilities");

        Set<String> foundUtilities = new HashSet<>();
        int nonUtilityCount = 0;

        for (Map.Entry<String, Ability> entry : ALL_ABILITIES.entrySet()) {
            String name = entry.getKey();
            Ability ability = entry.getValue();

            Path jsonPath = JUTSU_DIR.resolve(name + ".json");
            assertTrue(Files.exists(jsonPath), "Missing JSON file for " + name);

            String content = Files.readString(jsonPath);
            JsonObject obj = GSON.fromJson(content, JsonObject.class);
            assertNotNull(obj, name + ".json must parse as a JSON object");
            assertFalse(obj.has("combo"), name + ".json must NOT contain a 'combo' field");

            if (ability.isUtilityAbility()) {
                foundUtilities.add(name);
                assertFalse(ability.requiresActivationDelay(),
                        "Utility ability " + name + " must not require activation delay");
            } else {
                nonUtilityCount++;
                if (ability.getDefaultChakraCost() > 0) {
                    assertTrue(ability.requiresActivationDelay(),
                            "Chakra-costing combat jutsu " + name + " must require activation delay");
                } else {
                    assertFalse(ability.requiresActivationDelay(),
                            "Zero-chakra-cost ability " + name + " should not require activation delay");
                }
            }
        }

        assertEquals(EXPECTED_UTILITIES, foundUtilities,
                "Exactly chakra_charge, water_walk, and leap must be utility abilities");
        assertEquals(33, nonUtilityCount, "Remaining 33 abilities must have isUtilityAbility() == false");
    }

    @Test
    @DisplayName("Verify JutsuWheelOverlay radial wheel angle-to-slot mapping and deadzone")
    public void testRadialWheelAngleMapping() {
        double centerX = 200.0;
        double centerY = 150.0;
        double deadzone = 30.0;

        // Inside deadzone (distance < 30) returns -1
        assertEquals(-1, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX, centerY, centerX, centerY, deadzone));
        assertEquals(-1, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX + 20.0, centerY - 15.0, centerX, centerY, deadzone));
        assertEquals(-1, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX + 29.0, centerY, centerX, centerY, deadzone));

        // 8 radial sectors starting from Top (slot 0) clockwise to Top-Left (slot 7)
        assertEquals(0, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX, centerY - 60.0, centerX, centerY, deadzone));
        assertEquals(1, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX + 45.0, centerY - 45.0, centerX, centerY, deadzone));
        assertEquals(2, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX + 60.0, centerY, centerX, centerY, deadzone));
        assertEquals(3, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX + 45.0, centerY + 45.0, centerX, centerY, deadzone));
        assertEquals(4, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX, centerY + 60.0, centerX, centerY, deadzone));
        assertEquals(5, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX - 45.0, centerY + 45.0, centerX, centerY, deadzone));
        assertEquals(6, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX - 60.0, centerY, centerX, centerY, deadzone));
        assertEquals(7, com.sekwah.narutomod.client.gui.JutsuWheelOverlay.computeHoveredSlot(
                centerX - 45.0, centerY - 45.0, centerX, centerY, deadzone));
    }
}
