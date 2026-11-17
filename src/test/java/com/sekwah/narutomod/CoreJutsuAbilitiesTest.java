package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganAbility;
import com.sekwah.narutomod.abilities.jutsus.ShadowCloneAbility;
import com.sekwah.narutomod.capabilities.NinjaRank;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying default parameters, mastery scaling, and rank/combo
 * metadata for core jutsu abilities.
 */
public class CoreJutsuAbilitiesTest {

    @Test
    @DisplayName("Rasengan defaults and mastery hold-duration scaling")
    void testRasenganDefaultsAndScaling() {
        RasenganAbility ability = new RasenganAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(NinjaRank.CHUNIN, ability.getRequiredRank());
        assertEquals(2123L, ability.defaultCombo());
        assertEquals(40.0f, ability.getDefaultChakraCost(), 0.001f);
        assertEquals(160, ability.getDefaultCooldown());
        assertEquals(22.0f, ability.getDefaultDamage(), 0.001f);

        assertEquals(100, RasenganAbility.getScaledHoldDuration(0));
        assertEquals(100, RasenganAbility.getScaledHoldDuration(-10));
        assertEquals(120, RasenganAbility.getScaledHoldDuration(100));
    }

    @Test
    @DisplayName("Chidori defaults, element, and mastery hold-duration scaling")
    void testChidoriDefaultsAndScaling() {
        ChidoriAbility ability = new ChidoriAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(ChakraElement.LIGHTNING, ability.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, ability.getRequiredRank());
        assertEquals(1312L, ability.defaultCombo());
        assertEquals(35.0f, ability.getDefaultChakraCost(), 0.001f);
        assertEquals(120, ability.getDefaultCooldown());
        assertEquals(26.0f, ability.getDefaultDamage(), 0.001f);

        assertEquals(100, ChidoriAbility.getScaledHoldDuration(0));
        assertEquals(120, ChidoriAbility.getScaledHoldDuration(100));
    }

    @Test
    @DisplayName("ShadowClone defaults, clone count, and lifespan scaling by mastery")
    void testShadowCloneDefaultsAndScaling() {
        ShadowCloneAbility ability = new ShadowCloneAbility();
        assertEquals(Ability.ActivationType.INSTANT, ability.activationType());
        assertEquals(NinjaRank.CHUNIN, ability.getRequiredRank());
        assertEquals(3123L, ability.defaultCombo());
        assertEquals(30.0f, ability.getDefaultChakraCost(), 0.001f);
        assertEquals(240, ability.getDefaultCooldown());

        assertEquals(1, ShadowCloneAbility.getCloneCount(0));
        assertEquals(2, ShadowCloneAbility.getCloneCount(100));
        assertEquals(4, ShadowCloneAbility.getCloneCount(300));
        assertEquals(8, ShadowCloneAbility.getCloneCount(700));

        assertEquals(400, ShadowCloneAbility.getCloneLifespan(0));
        assertEquals(600, ShadowCloneAbility.getCloneLifespan(100));
        assertEquals(900, ShadowCloneAbility.getCloneLifespan(300));
        assertEquals(1200, ShadowCloneAbility.getCloneLifespan(700));
    }
}
