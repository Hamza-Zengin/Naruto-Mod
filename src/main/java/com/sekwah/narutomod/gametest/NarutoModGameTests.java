package com.sekwah.narutomod.gametest;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.network.ServerSecurityValidator;
import com.sekwah.narutomod.network.c2s.ServerAbilityActivatePacket;
import com.sekwah.narutomod.network.c2s.ServerMissionActionPacket;
import com.sekwah.narutomod.network.c2s.ServerRankUpPacket;
import com.sekwah.narutomod.network.c2s.ServerSelectClanPacket;
import io.netty.buffer.Unpooled;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

import java.util.function.Consumer;

/**
 * NeoForge GameTests for Naruto-Mod server security, jutsu execution, capability state, and packet codecs.
 */
public class NarutoModGameTests {

    private static final Identifier EMPTY_STRUCTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "empty");

    private static final ResourceKey<Consumer<GameTestHelper>> FN_SECURITY_AND_RATE_LIMIT =
            ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "security_and_rate_limit"));
    private static final ResourceKey<Consumer<GameTestHelper>> FN_JUTSU_CHAKRA_AND_COOLDOWN =
            ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "jutsu_chakra_and_cooldown"));
    private static final ResourceKey<Consumer<GameTestHelper>> FN_MISSION_AND_PACKET_CODECS =
            ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mission_and_packet_codecs"));

    public static boolean isGameTestEnabled() {
        return Boolean.getBoolean("neoforge.enableGameTest")
                || !System.getProperty("neoforge.enabledGameTestNamespaces", "").isEmpty();
    }

    public static void register(IEventBus modEventBus) {
        if (!isGameTestEnabled()) {
            return;
        }
        modEventBus.addListener(NarutoModGameTests::registerTestFunctions);
        modEventBus.addListener(NarutoModGameTests::registerGameTests);
    }

    private static void registerTestFunctions(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, helper -> {
            helper.register(FN_SECURITY_AND_RATE_LIMIT, NarutoModGameTests::testSecurityAndRateLimit);
            helper.register(FN_JUTSU_CHAKRA_AND_COOLDOWN, NarutoModGameTests::testJutsuChakraAndCooldown);
            helper.register(FN_MISSION_AND_PACKET_CODECS, NarutoModGameTests::testMissionAndPacketCodecs);
        });
    }

    private static void registerGameTests(RegisterGameTestsEvent event) {
        Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "default"),
                new TestEnvironmentDefinition.AllOf()
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "security_and_rate_limit"),
                new FunctionGameTestInstance(FN_SECURITY_AND_RATE_LIMIT, new TestData<>(env, EMPTY_STRUCTURE, 40, 0, true))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "jutsu_chakra_and_cooldown"),
                new FunctionGameTestInstance(FN_JUTSU_CHAKRA_AND_COOLDOWN, new TestData<>(env, EMPTY_STRUCTURE, 40, 0, true))
        );
        event.registerTest(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mission_and_packet_codecs"),
                new FunctionGameTestInstance(FN_MISSION_AND_PACKET_CODECS, new TestData<>(env, EMPTY_STRUCTURE, 40, 0, true))
        );
    }

    @SuppressWarnings("removal")
    private static void testSecurityAndRateLimit(GameTestHelper helper) {
        ServerSecurityValidator.resetRateLimits();
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);

        helper.assertTrue(ServerSecurityValidator.validatePlayerState(player), "Live mock player must pass player state check");

        for (int i = 0; i < ServerSecurityValidator.MAX_PACKETS_PER_WINDOW; i++) {
            helper.assertTrue(ServerSecurityValidator.validateRateLimit(player, "TestPacket"),
                    "Packet within rate limit window should be allowed");
        }
        helper.assertFalse(ServerSecurityValidator.validateRateLimit(player, "TestPacket"),
                "Packet exceeding MAX_PACKETS_PER_WINDOW must be rejected");

        ServerSecurityValidator.clearPlayer(player.getUUID());
        helper.assertTrue(ServerSecurityValidator.validateRateLimit(player, "TestPacket"),
                "Cleared player should be allowed again");

        Identifier defaultClan = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "shinobi");
        Identifier uchihaClan = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha");
        helper.assertFalse(ServerSecurityValidator.validateClanSelection(player, ninjaData, defaultClan),
                "Default shinobi clan cannot be manually selected");
        helper.assertTrue(ServerSecurityValidator.validateClanSelection(player, ninjaData, uchihaClan),
                "Valid clan should be selectable when unlocked");

        ninjaData.setClan(uchihaClan);
        ninjaData.setClanLocked(true);
        helper.assertFalse(ServerSecurityValidator.validateClanSelection(player, ninjaData, uchihaClan),
                "Locked clan must reject subsequent clan selection");

        ServerSecurityValidator.resetRateLimits();
        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void testJutsuChakraAndCooldown(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        Ability fireball = NarutoAbilities.FIREBALL.get();

        helper.assertFalse(ServerSecurityValidator.validateAbilityActivation(player, ninjaData, fireball, true),
                "Locked fireball ability must fail activation check");

        ninjaData.unlockAbility(fireball.getRegistryName());
        ninjaData.setChakra(100.0f);

        helper.assertTrue(ServerSecurityValidator.validateAbilityActivation(player, ninjaData, fireball, true),
                "Unlocked fireball with sufficient chakra must pass activation check");

        boolean paid = fireball.handleCost(player, ninjaData);
        helper.assertTrue(paid, "handleCost should succeed when chakra >= cost");
        helper.assertTrue(ninjaData.getChakra() < 100.0f, "Chakra must be deducted after handleCost");
        helper.assertTrue(ninjaData.getChakraRegenCooldown() > 0, "Chakra regen cooldown must be set after spending chakra");

        if (fireball instanceof Ability.Cooldown cooldownAbility) {
            cooldownAbility.registerCooldown(ninjaData, fireball.getTranslationKey());
            helper.assertFalse(ServerSecurityValidator.validateAbilityActivation(player, ninjaData, fireball, true),
                    "Ability on cooldown must fail activation check");
        }

        helper.succeed();
    }

    @SuppressWarnings("removal")
    private static void testMissionAndPacketCodecs(GameTestHelper helper) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        NinjaData ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);

        // Mission lifecycle on ServerPlayer capability
        ninjaData.acceptMission(NinjaMission.D_RANK_PATROL);
        helper.assertValueEqual(ninjaData.getActiveMissionId(), NinjaMission.D_RANK_PATROL.getId(), "active mission id");

        for (int i = 0; i < NinjaMission.D_RANK_PATROL.getTargetCount(); i++) {
            ninjaData.incrementMissionProgress(1);
        }
        helper.assertTrue(ninjaData.claimMissionReward(), "Completed mission reward should be claimable");
        helper.assertValueEqual(ninjaData.getCompletedMissionsCount(), 1, "completed missions count");
        helper.assertValueEqual(ninjaData.getRank(), NinjaRank.ACADEMY_STUDENT.name(), "initial rank");

        // Packet codec round-trips
        FriendlyByteBuf abilityBuf = new FriendlyByteBuf(Unpooled.buffer());
        ServerAbilityActivatePacket.encode(new ServerAbilityActivatePacket(NarutoAbilities.FIREBALL.getId()), abilityBuf);
        ServerAbilityActivatePacket decodedAbility = ServerAbilityActivatePacket.decode(abilityBuf);
        helper.assertTrue(decodedAbility.type().equals(ServerAbilityActivatePacket.TYPE), "ability packet type");

        FriendlyByteBuf clanBuf = new FriendlyByteBuf(Unpooled.buffer());
        ServerSelectClanPacket.encode(new ServerSelectClanPacket(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "uchiha")), clanBuf);
        ServerSelectClanPacket decodedClan = ServerSelectClanPacket.decode(clanBuf);
        helper.assertTrue(decodedClan.type().equals(ServerSelectClanPacket.TYPE), "clan packet type");

        FriendlyByteBuf missionBuf = new FriendlyByteBuf(Unpooled.buffer());
        ServerMissionActionPacket.encode(
                new ServerMissionActionPacket(ServerMissionActionPacket.Action.ACCEPT_MISSION, NinjaMission.D_RANK_PATROL.getId()),
                missionBuf);
        ServerMissionActionPacket decodedMission = ServerMissionActionPacket.decode(missionBuf);
        helper.assertTrue(decodedMission.type().equals(ServerMissionActionPacket.TYPE), "mission packet type");

        FriendlyByteBuf rankBuf = new FriendlyByteBuf(Unpooled.buffer());
        ServerRankUpPacket.encode(new ServerRankUpPacket(), rankBuf);
        ServerRankUpPacket decodedRank = ServerRankUpPacket.decode(rankBuf);
        helper.assertTrue(decodedRank.type().equals(ServerRankUpPacket.TYPE), "rank packet type");

        helper.succeed();
    }
}
