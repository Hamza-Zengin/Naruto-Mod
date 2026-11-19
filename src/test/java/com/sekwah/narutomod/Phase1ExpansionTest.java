package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.jutsus.AdamantineChainsAbility;
import com.sekwah.narutomod.block.TrainingDummyBlock;
import com.sekwah.narutomod.capabilities.ChakraStaminaLogic;
import com.sekwah.narutomod.capabilities.JutsuUnlockRules;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.events.PlayerEvents;
import com.sekwah.narutomod.item.armor.NarutoArmorItem;
import com.sekwah.narutomod.item.armor.NarutoArmorMaterial;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Phase 1 Content Expansion Batch:
 * - Uzumaki Kongō Fūsa (Adamantine Sealing Chains) metadata & clan/rank rules
 * - Declarative NarutoArmorItem passive stats & PlayerEvents / ChakraStaminaLogic math helpers
 * - Shinobi Training Dummy cooldown gating, melee damage, & projectile estimation helpers
 */
public class Phase1ExpansionTest {

    @Test
    @DisplayName("Verify AdamantineChainsAbility metadata and Uzumaki clan + Chunin unlock rules")
    void testAdamantineChainsMetadataAndUnlock() {
        AdamantineChainsAbility chains = new AdamantineChainsAbility();
        Identifier chainsId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "adamantine_chains");

        assertEquals(Ability.ActivationType.INSTANT, chains.activationType());
        assertEquals(ChakraElement.NONE, chains.getChakraElement());
        assertEquals(NinjaRank.CHUNIN, chains.getRequiredRank());
        assertEquals(1321L, chains.defaultCombo());
        assertEquals(42.0F, chains.getDefaultChakraCost(), 0.001F);
        assertEquals(220, chains.getDefaultCooldown());
        assertEquals(16.0F, chains.getDefaultDamage(), 0.001F);
        assertTrue(JutsuUnlockRules.isClanRestrictedJutsu(chainsId));

        // Non-Uzumaki Chunin cannot unlock or activate
        NinjaData uchihaChunin = new NinjaData(true);
        uchihaChunin.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha"));
        uchihaChunin.setRank(NinjaRank.CHUNIN.name());
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(uchihaChunin, chains, chainsId));
        assertFalse(chains.canActivate(null, uchihaChunin));

        // Uzumaki Genin cannot unlock yet (requires Chunin), even though bloodline matches
        NinjaData uzumakiGenin = new NinjaData(true);
        uzumakiGenin.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uzumaki"));
        uzumakiGenin.setRank(NinjaRank.GENIN.name());
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(uzumakiGenin, chains, chainsId));
        assertTrue(chains.canActivate(null, uzumakiGenin));

        // Uzumaki Chunin unlocks naturally
        NinjaData uzumakiChunin = new NinjaData(true);
        uzumakiChunin.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uzumaki"));
        uzumakiChunin.setRank(NinjaRank.CHUNIN.name());
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(uzumakiChunin, chains, chainsId));
        assertTrue(chains.canActivate(null, uzumakiChunin));
    }

    @Test
    @DisplayName("Verify declarative armor passives and PlayerEvents / ChakraStaminaLogic math helpers")
    void testDeclarativeArmorPassivesAndMath() {
        NarutoArmorItem dummyBoot = new NarutoArmorItem(NarutoArmorMaterial.ANBU_MAT, ArmorType.BOOTS, new Item.Properties())
                .setChakraRegenBonus(0.10F)
                .setStaminaRegenBonus(0.15F)
                .setFallDamageReduction(1.20F)
                .setKnockbackResistanceBonus(1.20F)
                .setJutsuDamageReduction(1.20F)
                .setMovementSpeedBonus(0.04F)
                .setStealthSpeedBonus(0.15F)
                .setAnbuStealthSynergy(true);

        assertEquals(0.10F, dummyBoot.getChakraRegenBonus(), 0.001F);
        assertEquals(0.15F, dummyBoot.getStaminaRegenBonus(), 0.001F);
        assertEquals(0.95F, dummyBoot.getFallDamageReduction(), 0.001F);
        assertEquals(0.95F, dummyBoot.getKnockbackResistanceBonus(), 0.001F);
        assertEquals(0.95F, dummyBoot.getJutsuDamageReduction(), 0.001F);
        assertEquals(0.04F, dummyBoot.getMovementSpeedBonus(), 0.001F);
        assertEquals(0.15F, dummyBoot.getStealthSpeedBonus(), 0.001F);
        assertTrue(dummyBoot.hasAnbuStealthSynergy());

        // Accessory regen multiplier
        assertEquals(1.00F, ChakraStaminaLogic.getAccessoryRegenMultiplier(-0.2F), 0.001F);
        assertEquals(1.25F, ChakraStaminaLogic.getAccessoryRegenMultiplier(0.25F), 0.001F);

        // PlayerEvents pure defensive & mobility math helpers
        assertEquals(18.0F, PlayerEvents.applyDeclarativeJutsuDamageReduction(20.0F, 0.10F), 0.001F);
        assertEquals(8.0F, PlayerEvents.applyDeclarativeJutsuDamageReduction(20.0F, 0.90F), 0.001F); // Capped at 60%
        assertEquals(11.5F, PlayerEvents.applyAnbuStealthSynergyDamage(10.0F, true), 0.001F);
        assertEquals(10.0F, PlayerEvents.applyAnbuStealthSynergyDamage(10.0F, false), 0.001F);
        assertEquals(0.80F, PlayerEvents.applyKnockbackResistance(1.0F, 0.20F), 0.001F);
        assertEquals(8.0D, PlayerEvents.applyArmorFallReduction(10.0D, 0.20F), 0.001D);
        assertEquals(0.004D, PlayerEvents.computeArmorMovementAttributeDelta(0.04F, 0.15F, false), 1.0E-6D);
        assertEquals(0.019D, PlayerEvents.computeArmorMovementAttributeDelta(0.04F, 0.15F, true), 1.0E-6D);
    }

    @Test
    @DisplayName("Verify TrainingDummyBlock cooldown gating, melee damage, and projectile defaults")
    void testTrainingDummyPureHelpers() {
        assertTrue(TrainingDummyBlock.isTrainingCooldownReady(100, -15, TrainingDummyBlock.MELEE_EXP_COOLDOWN_TICKS));
        assertFalse(TrainingDummyBlock.isTrainingCooldownReady(110, 100, TrainingDummyBlock.MELEE_EXP_COOLDOWN_TICKS));
        assertTrue(TrainingDummyBlock.isTrainingCooldownReady(115, 100, TrainingDummyBlock.MELEE_EXP_COOLDOWN_TICKS));
        assertTrue(TrainingDummyBlock.isTrainingCooldownReady(5, 100, TrainingDummyBlock.MELEE_EXP_COOLDOWN_TICKS)); // Respawn / tick reset

        assertEquals(1.0F, TrainingDummyBlock.computeMeleeTrainingDamage(0.5D, false), 0.001F);
        assertEquals(7.0F, TrainingDummyBlock.computeMeleeTrainingDamage(7.0D, false), 0.001F);
        assertEquals(10.5F, TrainingDummyBlock.computeMeleeTrainingDamage(7.0D, true), 0.001F);

        assertNull(TrainingDummyBlock.resolveJutsuAbilityId(null));
        assertEquals(4.0F, TrainingDummyBlock.estimateProjectileDamage(null), 0.001F);
    }
}
