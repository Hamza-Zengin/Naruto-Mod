package com.sekwah.narutomod.capabilities;

import com.sekwah.narutomod.events.PlayerEvents;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RankPhysicalPassivesTest {

    @Test
    void testRankPhysicalBonusesScaleMonotonicallyWithinBalancedBounds() {
        NinjaRank[] ranks = NinjaRank.values();
        for (int i = 0; i < ranks.length; i++) {
            NinjaRank r = ranks[i];
            if (i == 0) {
                assertEquals(NinjaRank.ACADEMY_STUDENT, r);
                assertEquals(0.0F, r.getMovementSpeedBonus(), 1e-6F);
                assertEquals(0.0D, r.getJumpStrengthBonus(), 1e-6D);
                assertEquals(0.0D, r.getSafeFallDistanceBonus(), 1e-6D);
                assertEquals(0.0F, r.getFallDamageReduction(), 1e-6F);
                assertEquals(0.0F, r.getUnarmedDamageBonus(), 1e-6F);
                assertEquals(0.0F, r.getUnarmoredDamageReduction(), 1e-6F);
            } else {
                NinjaRank prev = ranks[i - 1];
                assertTrue(r.getMovementSpeedBonus() > prev.getMovementSpeedBonus(),
                        r + " speed bonus should exceed " + prev);
                assertTrue(r.getJumpStrengthBonus() > prev.getJumpStrengthBonus(),
                        r + " jump bonus should exceed " + prev);
                assertTrue(r.getSafeFallDistanceBonus() > prev.getSafeFallDistanceBonus(),
                        r + " safe fall bonus should exceed " + prev);
                assertTrue(r.getFallDamageReduction() > prev.getFallDamageReduction(),
                        r + " fall reduction should exceed " + prev);
                assertTrue(r.getUnarmedDamageBonus() > prev.getUnarmedDamageBonus(),
                        r + " unarmed damage bonus should exceed " + prev);
                assertTrue(r.getUnarmoredDamageReduction() > prev.getUnarmoredDamageReduction(),
                        r + " unarmored reduction should exceed " + prev);
            }
        }

        // Kage balanced caps
        assertEquals(0.15F, NinjaRank.KAGE.getMovementSpeedBonus(), 1e-6F);
        assertEquals(0.10D, NinjaRank.KAGE.getJumpStrengthBonus(), 1e-6D);
        assertEquals(4.0D, NinjaRank.KAGE.getSafeFallDistanceBonus(), 1e-6D);
        assertEquals(0.55F, NinjaRank.KAGE.getFallDamageReduction(), 1e-6F);
        assertEquals(5.0F, NinjaRank.KAGE.getUnarmedDamageBonus(), 1e-6F);
        assertEquals(0.35F, NinjaRank.KAGE.getUnarmoredDamageReduction(), 1e-6F);
    }

    @Test
    void testUnarmedDamageBonusAppliesOnlyWhenMainHandEmptyAndDirectMelee() {
        // Empty hand + direct melee
        assertEquals(1.0F, PlayerEvents.applyRankUnarmedDamageBonus(1.0F, NinjaRank.ACADEMY_STUDENT, true, true), 1e-5F);
        assertEquals(2.0F, PlayerEvents.applyRankUnarmedDamageBonus(1.0F, NinjaRank.GENIN, true, true), 1e-5F);
        assertEquals(3.0F, PlayerEvents.applyRankUnarmedDamageBonus(1.0F, NinjaRank.CHUNIN, true, true), 1e-5F);
        assertEquals(6.0F, PlayerEvents.applyRankUnarmedDamageBonus(1.0F, NinjaRank.KAGE, true, true), 1e-5F);

        // Holding a weapon/item -> no unarmed bonus
        assertEquals(7.0F, PlayerEvents.applyRankUnarmedDamageBonus(7.0F, NinjaRank.KAGE, false, true), 1e-5F);

        // Indirect attack (projectile/jutsu) even with empty hand -> no unarmed bonus
        assertEquals(10.0F, PlayerEvents.applyRankUnarmedDamageBonus(10.0F, NinjaRank.KAGE, true, false), 1e-5F);
    }

    @Test
    void testUnarmoredDamageReductionAppliesOnlyWhenCompletelyUnarmoredAndNotBypassing() {
        assertEquals(20.0F, PlayerEvents.applyRankUnarmoredDamageReduction(20.0F, NinjaRank.ACADEMY_STUDENT, true, false), 1e-5F);
        assertEquals(18.4F, PlayerEvents.applyRankUnarmoredDamageReduction(20.0F, NinjaRank.GENIN, true, false), 1e-5F);
        assertEquals(17.0F, PlayerEvents.applyRankUnarmoredDamageReduction(20.0F, NinjaRank.CHUNIN, true, false), 1e-5F);
        assertEquals(13.0F, PlayerEvents.applyRankUnarmoredDamageReduction(20.0F, NinjaRank.KAGE, true, false), 1e-5F);

        // Any armor equipped -> no reduction
        assertEquals(20.0F, PlayerEvents.applyRankUnarmoredDamageReduction(20.0F, NinjaRank.KAGE, false, false), 1e-5F);

        // Bypass invulnerability (/kill, void) -> no reduction
        assertEquals(20.0F, PlayerEvents.applyRankUnarmoredDamageReduction(20.0F, NinjaRank.KAGE, true, true), 1e-5F);
    }

    @Test
    void testFallDistanceReductionScalesWithRank() {
        assertEquals(10.0D, PlayerEvents.applyRankFallDistanceReduction(10.0D, NinjaRank.ACADEMY_STUDENT), 1e-5D);
        assertEquals(8.8D, PlayerEvents.applyRankFallDistanceReduction(10.0D, NinjaRank.GENIN), 1e-5D);
        assertEquals(7.6D, PlayerEvents.applyRankFallDistanceReduction(10.0D, NinjaRank.CHUNIN), 1e-5D);
        assertEquals(4.5D, PlayerEvents.applyRankFallDistanceReduction(10.0D, NinjaRank.KAGE), 1e-5D);
    }
}
