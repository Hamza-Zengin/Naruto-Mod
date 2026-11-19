package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.jutsus.AdamantineChainsAbility;
import com.sekwah.narutomod.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutomod.abilities.jutsus.ChidoriAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.block.WeaponRackBlock;
import com.sekwah.narutomod.block.WeaponRackBlockEntity;
import com.sekwah.narutomod.block.weapons.MakibishiBlock;
import com.sekwah.narutomod.block.weapons.WireTrapBlock;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.JutsuUnlockRules;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure JUnit tests for Phase 2 Worldgen, Tactical Trap Blocks, Dojo Weapon Rack helpers,
 * and Declarative Ability Unlock Metadata (no registry/Block instantiation).
 */
public class Phase2WorldBlocksTest {

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("Verify declarative Ability.requiresScroll() and Ability.getRequiredClanPath() rules")
    void testDeclarativeAbilityUnlockMetadata() {
        RasenganAbility rasengan = new RasenganAbility();
        ChidoriAbility chidori = new ChidoriAbility();
        SharinganAbility sharingan = new SharinganAbility();
        ByakuganAbility byakugan = new ByakuganAbility();
        AdamantineChainsAbility chains = new AdamantineChainsAbility();

        assertTrue(rasengan.requiresScroll());
        assertNull(rasengan.getRequiredClanPath());
        assertTrue(chidori.requiresScroll());
        assertNull(chidori.getRequiredClanPath());
        assertFalse(sharingan.requiresScroll());
        assertEquals("uchiha", sharingan.getRequiredClanPath());
        assertFalse(byakugan.requiresScroll());
        assertEquals("hyuga", byakugan.getRequiredClanPath());
        assertFalse(chains.requiresScroll());
        assertEquals("uzumaki", chains.getRequiredClanPath());

        // Verify JutsuUnlockRules honors declarative overrides even for unseen ability IDs
        Identifier customId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "custom_declarative_test");
        Ability declarativeClanAndScroll = new Ability() {
            @Override
            public ActivationType activationType() {
                return ActivationType.INSTANT;
            }

            @Override
            public boolean requiresScroll() {
                return true;
            }

            @Override
            public String getRequiredClanPath() {
                return "hyuga";
            }

            @Override
            public void performServer(Player player, INinjaData ninjaData, int ticksActive) {}
        };

        NinjaData uchihaWithScroll = new NinjaData(true);
        uchihaWithScroll.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha"));
        uchihaWithScroll.setRank(NinjaRank.JONIN.name());
        uchihaWithScroll.unlockAbility(customId);
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(uchihaWithScroll, declarativeClanAndScroll, customId),
                "Wrong clan must be blocked even when scroll is unlocked");

        NinjaData hyugaNoScroll = new NinjaData(true);
        hyugaNoScroll.setClan(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hyuga"));
        hyugaNoScroll.setRank(NinjaRank.JONIN.name());
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(hyugaNoScroll, declarativeClanAndScroll, customId),
                "Matching clan without scroll must remain locked when requiresScroll() is true");

        hyugaNoScroll.unlockAbility(customId);
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(hyugaNoScroll, declarativeClanAndScroll, customId),
                "Matching clan + unlocked scroll must unlock declarative ability");
    }

    @Test
    @DisplayName("Verify MakibishiBlock hit cooldown gating and charge decrement helpers")
    void testMakibishiHelpers() {
        assertTrue(MakibishiBlock.isHitCooldownReady(100, -1));
        assertFalse(MakibishiBlock.isHitCooldownReady(110, 100));
        assertFalse(MakibishiBlock.isHitCooldownReady(114, 100));
        assertTrue(MakibishiBlock.isHitCooldownReady(115, 100));
        assertTrue(MakibishiBlock.isHitCooldownReady(50, 200), "World tick reset must allow immediate trigger");

        assertEquals(3, MakibishiBlock.computeRemainingCharges(4));
        assertEquals(2, MakibishiBlock.computeRemainingCharges(3));
        assertEquals(1, MakibishiBlock.computeRemainingCharges(2));
        assertEquals(0, MakibishiBlock.computeRemainingCharges(1));
        assertEquals(0, MakibishiBlock.computeRemainingCharges(0));
    }

    @Test
    @DisplayName("Verify WireTrapBlock guard predicates for null entity and BlockState")
    void testWireTrapNullGuards() {
        assertFalse(WireTrapBlock.shouldTriggerForEntity(null));
        assertFalse(WireTrapBlock.isPaperBombState(null));
    }

    @Test
    @DisplayName("Verify WeaponRackBlock slot resolution and comparator analog signal math")
    void testWeaponRackSlotAndComparatorHelpers() {
        assertEquals(WeaponRackBlockEntity.SLOT_UPPER, WeaponRackBlock.resolveSlotFromHitY(0.90D));
        assertEquals(WeaponRackBlockEntity.SLOT_UPPER, WeaponRackBlock.resolveSlotFromHitY(0.50D));
        assertEquals(WeaponRackBlockEntity.SLOT_LOWER, WeaponRackBlock.resolveSlotFromHitY(0.49D));
        assertEquals(WeaponRackBlockEntity.SLOT_LOWER, WeaponRackBlock.resolveSlotFromHitY(0.10D));

        assertEquals(0, WeaponRackBlockEntity.computeComparatorSignal(0));
        assertEquals(7, WeaponRackBlockEntity.computeComparatorSignal(1));
        assertEquals(15, WeaponRackBlockEntity.computeComparatorSignal(2));
    }
}
