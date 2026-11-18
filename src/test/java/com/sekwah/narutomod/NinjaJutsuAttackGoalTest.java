package com.sekwah.narutomod;

import com.sekwah.narutomod.entity.ai.NinjaJutsuAttackGoal;
import com.sekwah.narutomod.entity.ai.NinjaJutsuCooldowns;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.ai.NinjaJutsuType;
import com.sekwah.narutomod.entity.ai.NinjaJutsuUser;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for NinjaJutsuAttackGoal tactical decision matrix,
 * shared global + per-ability cooldown locking, and Akatsuki/Rogue NPC profiles.
 */
public class NinjaJutsuAttackGoalTest {

    @Test
    @DisplayName("Global and per-ability cooldowns prevent same-tick jutsu spam")
    public void testCooldownsPreventSameTickSpam() {
        NinjaJutsuCooldowns cooldowns = new NinjaJutsuCooldowns();
        assertTrue(cooldowns.isReady(NinjaJutsuType.FIREBALL));
        assertTrue(cooldowns.isReady(NinjaJutsuType.WATER_BULLET));
        assertTrue(cooldowns.isReady(NinjaJutsuType.KAWARIMI));

        cooldowns.trigger(NinjaJutsuType.FIREBALL, 60, 20);

        assertEquals(20, cooldowns.getGlobalJutsuCooldown());
        assertEquals(60, cooldowns.getAbilityCooldown(NinjaJutsuType.FIREBALL));
        assertFalse(cooldowns.isReady(NinjaJutsuType.FIREBALL));
        assertFalse(cooldowns.isReady(NinjaJutsuType.WATER_BULLET),
                "Other abilities must be locked while globalJutsuCooldown is active");
        assertFalse(cooldowns.isReady(NinjaJutsuType.KAWARIMI),
                "Defensive abilities must also respect globalJutsuCooldown");

        for (int i = 0; i < 20; i++) {
            cooldowns.tick();
        }
        assertEquals(0, cooldowns.getGlobalJutsuCooldown());
        assertEquals(40, cooldowns.getAbilityCooldown(NinjaJutsuType.FIREBALL));
        assertTrue(cooldowns.isReady(NinjaJutsuType.WATER_BULLET),
                "Unrelated abilities should become ready once global cooldown expires");
        assertFalse(cooldowns.isReady(NinjaJutsuType.FIREBALL),
                "Triggered ability must stay locked until its own cooldown expires");
    }

    @Test
    @DisplayName("Ranged tactical decision triggers Fireball/WaterBullet only at 10-24 blocks with Line of Sight")
    public void testRangedTacticalSelection() {
        NinjaJutsuProfile akatsuki = NinjaJutsuProfile.akatsukiPreset();
        NinjaJutsuCooldowns cooldowns = new NinjaJutsuCooldowns();

        // Within 10-24 blocks with LoS -> cycles FIREBALL, WATER_BULLET, EARTH_BULLET, VACUUM_SPHERES
        NinjaJutsuType first = NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 15.0D, true, 1.0F, 0);
        NinjaJutsuType second = NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 15.0D, true, 1.0F, 1);
        NinjaJutsuType third = NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 15.0D, true, 1.0F, 2);
        NinjaJutsuType fourth = NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 15.0D, true, 1.0F, 3);
        assertEquals(NinjaJutsuType.FIREBALL, first);
        assertEquals(NinjaJutsuType.WATER_BULLET, second);
        assertEquals(NinjaJutsuType.EARTH_BULLET, third);
        assertEquals(NinjaJutsuType.VACUUM_SPHERES, fourth);

        // No Line of Sight -> null
        assertNull(NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 15.0D, false, 1.0F, 0));

        // Too close (< 10 blocks) at healthy HP -> null
        assertNull(NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 8.0D, true, 1.0F, 0));

        // Too far (> 24 blocks) -> null
        assertNull(NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 26.0D, true, 1.0F, 0));
    }

    @Test
    @DisplayName("Critical health (<35%) under close threat triggers Kawarimi first, then MudWall fallback for Akatsuki")
    public void testCriticalHealthDefensiveReflexes() {
        NinjaJutsuProfile akatsuki = NinjaJutsuProfile.akatsukiPreset();
        NinjaJutsuCooldowns cooldowns = new NinjaJutsuCooldowns();

        // Critical health + close threat -> KAWARIMI first
        assertEquals(NinjaJutsuType.KAWARIMI,
                NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 4.0D, true, 0.30F, 0));

        // Put Kawarimi on cooldown and expire global cooldown -> MUD_WALL fallback
        cooldowns.setAbilityCooldown(NinjaJutsuType.KAWARIMI, 100);
        assertEquals(NinjaJutsuType.MUD_WALL,
                NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 4.0D, true, 0.30F, 0));

        // Healthy (> 35%) at close range -> no defensive reflex
        cooldowns.setAbilityCooldown(NinjaJutsuType.KAWARIMI, 0);
        assertNull(NinjaJutsuAttackGoal.selectTacticalAction(akatsuki, cooldowns, 4.0D, true, 0.60F, 0));
    }

    @Test
    @DisplayName("Rogue Ninja profile is basic WaterBullet + Kawarimi evasion focused with longer cooldowns than Akatsuki")
    public void testAkatsukiVsRogueProfiles() {
        NinjaJutsuProfile akatsuki = NinjaJutsuProfile.akatsukiPreset();
        NinjaJutsuProfile rogue = NinjaJutsuProfile.roguePreset();

        assertTrue(akatsuki.getGlobalCooldownTicks() < rogue.getGlobalCooldownTicks(),
                "Akatsuki should have shorter global cooldown than Rogue");
        assertTrue(akatsuki.getWaterBulletCooldownTicks() < rogue.getWaterBulletCooldownTicks(),
                "Akatsuki should cast ranged jutsu more frequently than Rogue");
        assertTrue(akatsuki.getKawarimiCooldownTicks() < rogue.getKawarimiCooldownTicks());

        assertTrue(akatsuki.isFireballEnabled());
        assertTrue(akatsuki.isMudWallEnabled());

        assertFalse(rogue.isFireballEnabled(), "Rogue should use basic WaterBullet rather than Fireball");
        assertTrue(rogue.isWaterBulletEnabled());
        assertTrue(rogue.isKawarimiEnabled());
        assertFalse(rogue.isMudWallEnabled(), "Rogue should focus on Kawarimi evasion rather than MudWall");

        assertTrue(NinjaJutsuUser.class.isAssignableFrom(AkatsukiNinjaEntity.class));
        assertTrue(NinjaJutsuUser.class.isAssignableFrom(RogueNinjaEntity.class));
    }
}
