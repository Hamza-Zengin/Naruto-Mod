package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.jutsus.FireballJutsuAbility;
import com.sekwah.narutomod.abilities.jutsus.RasenganAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.abilities.jutsus.SusanooRibcageAbility;
import com.sekwah.narutomod.abilities.utility.ChakraChargeAbility;
import com.sekwah.narutomod.abilities.utility.LeapAbility;
import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import net.minecraft.resources.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class ServerSecurityValidatorTest {

    private NinjaData ninjaData;

    @BeforeEach
    public void setup() {
        ServerSecurityValidator.resetRateLimits();
        this.ninjaData = new NinjaData(true);
    }

    @Test
    @DisplayName("Verify tick-based rate limit allows 20 packets per 20-tick window and drops excess")
    public void testRateLimitWindow() {
        UUID playerA = UUID.randomUUID();
        UUID playerB = UUID.randomUUID();

        // First 20 packets in window [100..119] must pass
        for (int i = 0; i < ServerSecurityValidator.MAX_PACKETS_PER_WINDOW; i++) {
            assertTrue(ServerSecurityValidator.allowPacket(playerA, 100L + (i % 10), "TestPacket"),
                    "Packet #" + (i + 1) + " within limit should be allowed");
        }

        // 21st and 22nd packet in the same 20-tick window must be silently dropped
        assertFalse(ServerSecurityValidator.allowPacket(playerA, 115L, "TestPacket"));
        assertFalse(ServerSecurityValidator.allowPacket(playerA, 119L, "TestPacket"));

        // Different player must remain unaffected
        assertTrue(ServerSecurityValidator.allowPacket(playerB, 115L, "TestPacket"));

        // Once 20 ticks elapse (tick >= 120), window resets and allows packets again
        assertTrue(ServerSecurityValidator.allowPacket(playerA, 120L, "TestPacket"));

        // Clearing player resets state immediately
        ServerSecurityValidator.clearPlayer(playerA);
        for (int i = 0; i < ServerSecurityValidator.MAX_PACKETS_PER_WINDOW; i++) {
            assertTrue(ServerSecurityValidator.allowPacket(playerA, 121L, "TestPacket"));
        }
    }

    @Test
    @DisplayName("Verify ability unlock, cooldown, and chakra sufficiency checks")
    public void testAbilitySecurityChecks() {
        FireballJutsuAbility fireball = new FireballJutsuAbility();
        fireball.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "fireball"));

        RasenganAbility rasengan = new RasenganAbility();
        rasengan.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "rasengan"));

        ChakraChargeAbility chakraCharge = new ChakraChargeAbility();
        chakraCharge.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chakra_charge"));

        LeapAbility leap = new LeapAbility();
        leap.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "leap"));

        // Academy student cannot use Genin fireball or scroll-locked rasengan
        assertFalse(ServerSecurityValidator.isAbilityUnlocked(ninjaData, fireball));
        assertFalse(ServerSecurityValidator.isAbilityUnlocked(ninjaData, rasengan));
        assertTrue(ServerSecurityValidator.isAbilityUnlocked(ninjaData, chakraCharge));
        assertTrue(ServerSecurityValidator.isAbilityUnlocked(ninjaData, leap));

        // Promote to Genin -> fireball unlocked, rasengan still locked (scroll-only)
        ninjaData.setRank("GENIN");
        assertTrue(ServerSecurityValidator.isAbilityUnlocked(ninjaData, fireball));
        assertFalse(ServerSecurityValidator.isAbilityUnlocked(ninjaData, rasengan));

        // Unlock rasengan explicitly
        ninjaData.unlockAbility(rasengan.getRegistryName());
        assertTrue(ServerSecurityValidator.isAbilityUnlocked(ninjaData, rasengan));

        // Cooldown checks
        assertTrue(ServerSecurityValidator.isCooldownReady(ninjaData, fireball));
        ninjaData.getCooldownEvents().put(fireball.getTranslationKey(), new CooldownTickEvent(40));
        assertFalse(ServerSecurityValidator.isCooldownReady(ninjaData, fireball));
        ninjaData.getCooldownEvents().remove(fireball.getTranslationKey());
        assertTrue(ServerSecurityValidator.isCooldownReady(ninjaData, fireball));

        // Chakra sufficiency checks (0-cost abilities must still work at 0 chakra)
        ninjaData.setChakra(0f);
        assertTrue(ServerSecurityValidator.hasSufficientChakra(ninjaData, chakraCharge, false),
                "Chakra charge must remain usable at 0 chakra");
        assertTrue(ServerSecurityValidator.hasSufficientChakra(ninjaData, leap, false),
                "Stamina-only Leap must pass chakra pre-check");
        assertFalse(ServerSecurityValidator.hasSufficientChakra(ninjaData, fireball, false));
        assertTrue(ServerSecurityValidator.hasSufficientChakra(ninjaData, fireball, true),
                "Creative mode should pass validator chakra check");

        ninjaData.setChakra(fireball.getScaledChakraCost(ninjaData));
        assertTrue(ServerSecurityValidator.hasSufficientChakra(ninjaData, fireball, false));
    }

    @Test
    @DisplayName("Verify toggle ability chakra pre-checks (Sharingan & Susanoo)")
    public void testToggleChakraPreChecks() {
        SharinganAbility sharingan = new SharinganAbility();
        sharingan.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sharingan"));

        SusanooRibcageAbility susanoo = new SusanooRibcageAbility();
        susanoo.setRegistryName(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "susanoo_ribcage"));

        ninjaData.setChakra(0f);
        assertFalse(ServerSecurityValidator.hasSufficientChakra(ninjaData, sharingan, false));
        assertFalse(ServerSecurityValidator.hasSufficientChakra(ninjaData, susanoo, false));

        ninjaData.setChakra(20f);
        assertTrue(ServerSecurityValidator.hasSufficientChakra(ninjaData, sharingan, false));
        assertFalse(ServerSecurityValidator.hasSufficientChakra(ninjaData, susanoo, false));

        ninjaData.setChakra(40f);
        assertTrue(ServerSecurityValidator.hasSufficientChakra(ninjaData, susanoo, false));
    }

    @Test
    @DisplayName("Verify clan selection rejects locked, default 'shinobi', and invalid clans")
    public void testClanSelectionSecurity() {
        Identifier uchiha = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha");
        Identifier hyuga = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hyuga");
        Identifier shinobi = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi");

        // Reject default 'shinobi', null, or non-existent clan
        assertFalse(ServerSecurityValidator.canSelectClan(ninjaData, shinobi, true));
        assertFalse(ServerSecurityValidator.canSelectClan(ninjaData, null, true));
        assertFalse(ServerSecurityValidator.canSelectClan(ninjaData, uchiha, false));

        // Allow valid clan when unlocked
        assertTrue(ServerSecurityValidator.canSelectClan(ninjaData, uchiha, true));

        // Lock clan -> reject any subsequent clan selection
        ninjaData.forceSetClan(uchiha);
        assertTrue(ninjaData.isClanLocked());
        assertFalse(ServerSecurityValidator.canSelectClan(ninjaData, hyuga, true));
        assertFalse(ServerSecurityValidator.canSelectClan(ninjaData, uchiha, true));
    }

    @Test
    @DisplayName("Verify rank-up security enforces server-side progression requirements")
    public void testRankUpSecurity() {
        assertFalse(ServerSecurityValidator.canRankUp(null));
        assertFalse(ServerSecurityValidator.canRankUp(ninjaData),
                "Academy Student without chakra/cast requirements must not rank up");

        ninjaData.setBaseMaxChakra(120.0f);
        ninjaData.setJutsusCastCount(8);
        assertTrue(ServerSecurityValidator.canRankUp(ninjaData));
    }
}
