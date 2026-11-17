package com.sekwah.narutomod;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
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
 * Audits all 26 jutsu/utility abilities and their JSON definitions to guarantee:
 * 1. 100% unique, collision-free combos across all abilities.
 * 2. Exact parity between JSON "combo" array and Java Ability.defaultCombo().
 * 3. Tiered combo key lengths: 2-3 keys for basic, >=4 for high-level, 5 for ultimate jutsus.
 */
public class JutsuComboAuditTest {

    private static final Gson GSON = new Gson();
    private static final Path JUTSU_DIR = Paths.get("src/main/resources/data/narutomod/jutsus");

    private static final Map<String, Ability> ALL_ABILITIES = Map.ofEntries(
            // [A] Basic & Movement (2-3 keys)
            Map.entry("chakra_charge", new ChakraChargeAbility()),
            Map.entry("water_walk", new WaterWalkAbility()),
            Map.entry("leap", new LeapAbility()),
            Map.entry("kawarimi", new KawarimiJutsuAbility()),
            Map.entry("fireball", new FireballJutsuAbility()),
            Map.entry("water_bullet", new WaterBulletAbility()),
            Map.entry("wind_breaker", new WindBreakerAbility()),
            Map.entry("mud_wall", new MudWallAbility()),
            Map.entry("summoning", new SummoningAbility()),
            // [B] High Level (4 keys)
            Map.entry("sharingan", new SharinganAbility()),
            Map.entry("byakugan", new ByakuganAbility()),
            Map.entry("shadow_clone", new ShadowCloneAbility()),
            Map.entry("shadow_possession", new ShadowPossessionAbility()),
            Map.entry("chidori", new ChidoriAbility()),
            Map.entry("rasengan", new RasenganAbility()),
            Map.entry("hakke_kusho", new HakkeKushoAbility()),
            Map.entry("bone_dance", new BoneDanceAbility()),
            Map.entry("wood_prison", new WoodPrisonAbility()),
            Map.entry("eight_gates", new EightGatesAbility()),
            // [C] Ultimate (5 keys)
            Map.entry("rasenshuriken", new RasenshurikenAbility()),
            Map.entry("kirin", new KirinAbility()),
            Map.entry("amaterasu", new AmaterasuAbility()),
            Map.entry("tsukuyomi", new TsukuyomiAbility()),
            Map.entry("susanoo_ribcage", new SusanooRibcageAbility()),
            Map.entry("eight_gates_death", new EighthGateOfDeathAbility())
    );

    private static final Set<String> HIGH_LEVEL = Set.of(
            "shadow_clone", "shadow_possession", "chidori",
            "rasengan", "hakke_kusho", "bone_dance", "wood_prison", "eight_gates"
    );

    private static final Set<String> ULTIMATE = Set.of(
            "rasenshuriken", "kirin", "amaterasu", "tsukuyomi", "susanoo_ribcage", "eight_gates_death"
    );

    @Test
    @DisplayName("Verify all 26 jutsu combos are 100% unique, match JSON, and obey tier key lengths")
    public void testAllJutsuCombosUniqueAndTiered() throws IOException {
        assertTrue(Files.isDirectory(JUTSU_DIR), "Jutsu data directory must exist");

        Set<List<Integer>> uniqueCombos = new HashSet<>();
        Set<Long> uniqueJavaCombos = new HashSet<>();

        for (Map.Entry<String, Ability> entry : ALL_ABILITIES.entrySet()) {
            String name = entry.getKey();
            Ability ability = entry.getValue();

            Path jsonPath = JUTSU_DIR.resolve(name + ".json");
            assertTrue(Files.exists(jsonPath), "Missing JSON file for " + name);

            String content = Files.readString(jsonPath);
            JsonObject obj = GSON.fromJson(content, JsonObject.class);
            assertTrue(obj.has("combo"), name + ".json must define a 'combo' array");

            JsonArray arr = obj.getAsJsonArray("combo");
            List<Integer> jsonCombo = new ArrayList<>();
            long reconstructedCombo = 0;
            for (int i = 0; i < arr.size(); i++) {
                int key = arr.get(i).getAsInt();
                assertTrue(key >= 1 && key <= 3, "Combo key must be 1, 2, or 3 in " + name);
                jsonCombo.add(key);
                reconstructedCombo = reconstructedCombo * 10L + key;
            }

            long javaCombo = ability.defaultCombo();
            assertEquals(reconstructedCombo, javaCombo,
                    "Java defaultCombo() must match JSON combo for " + name);

            assertTrue(uniqueCombos.add(jsonCombo),
                    "Duplicate combo detected for " + name + ": " + jsonCombo);
            assertTrue(uniqueJavaCombos.add(javaCombo),
                    "Duplicate Java defaultCombo() detected for " + name + ": " + javaCombo);

            if (ULTIMATE.contains(name)) {
                assertEquals(5, jsonCombo.size(), "Ultimate jutsu " + name + " must have 5 keys");
            } else if (HIGH_LEVEL.contains(name)) {
                assertTrue(jsonCombo.size() >= 4, "High-level jutsu " + name + " must have >= 4 keys");
            } else if ("chakra_charge".equals(name) || "water_walk".equals(name)) {
                assertEquals(1, jsonCombo.size(), "Starter utility " + name + " must have 1 key");
            } else {
                assertTrue(jsonCombo.size() >= 2 && jsonCombo.size() <= 3,
                        "Basic/movement ability " + name + " must have 2-3 keys");
            }
        }

        assertEquals(ALL_ABILITIES.size(), uniqueCombos.size());
        assertEquals(ALL_ABILITIES.size(), uniqueJavaCombos.size());
    }
}
