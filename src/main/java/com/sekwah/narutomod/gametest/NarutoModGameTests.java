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
    private static final ResourceKey<Consumer<GameTestHelper>> FN_EXPANSION_INTEGRATION =
            ResourceKey.create(Registries.TEST_FUNCTION, Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "expansion_structures_and_entities"));

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
            helper.register(FN_EXPANSION_INTEGRATION, NarutoModGameTests::testExpansionStructuresAndEntities);
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
        event.registerTest(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "expansion_structures_and_entities"),
                new FunctionGameTestInstance(FN_EXPANSION_INTEGRATION, new TestData<>(env, EMPTY_STRUCTURE, 40, 0, true))
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

    private static void testExpansionStructuresAndEntities(GameTestHelper helper) {
        // Phase 1 abilities registered
        helper.assertTrue(NarutoAbilities.PHOENIX_FLOWER.get() != null, "Phoenix Flower ability must be registered");
        helper.assertTrue(NarutoAbilities.CHIDORI_STREAM.get() != null, "Chidori Stream ability must be registered");
        helper.assertTrue(NarutoAbilities.MYSTICAL_PALM.get() != null, "Mystical Palm ability must be registered");

        // Phase 2 Kubikiribocho live ItemStack blood-repair check
        net.minecraft.world.item.ItemStack blade = new net.minecraft.world.item.ItemStack(
                com.sekwah.narutomod.item.NarutoItems.KUBIKIRIBOCHO.get());
        blade.setDamageValue(50);
        com.sekwah.narutomod.item.weapons.KubikiribochoItem.applyBloodRepair(blade, true);
        helper.assertValueEqual(blade.getDamageValue(), 35, "Kubikiribocho kill repair");

        // Phase 3 & 2nd Revision VillageNinja & ShinobiVillager spawns and merchant offers
        com.sekwah.narutomod.entity.mob.VillageNinjaEntity guard =
                com.sekwah.narutomod.entity.NarutoEntities.VILLAGE_NINJA.get().create(
                        helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        helper.assertTrue(guard != null, "VillageNinjaEntity must instantiate cleanly");
        if (guard != null) {
            guard.setVillageType(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KUMO);
            guard.equipForVillage(com.sekwah.narutomod.entity.mob.VillageNinjaEntity.VillageType.KUMO, helper.getLevel().getRandom());
            helper.assertTrue(
                    guard.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(
                            com.sekwah.narutomod.item.NarutoItems.HEADBAND_CLOUD.get()),
                    "Kumo Village Ninja must equip Cloud Headband");
            helper.assertTrue(
                    guard.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST).is(
                            com.sekwah.narutomod.item.NarutoItems.CLOUD_JONIN_VEST.get()),
                    "Kumo Village Ninja must equip Cloud Jonin Vest");
        }

        for (var entry : java.util.Map.of(
                com.sekwah.narutomod.entity.NarutoEntities.KONOHA_NINJA.get(), com.sekwah.narutomod.item.NarutoItems.HEADBAND_LEAF.get(),
                com.sekwah.narutomod.entity.NarutoEntities.SUNA_NINJA.get(), com.sekwah.narutomod.item.NarutoItems.HEADBAND_SAND.get(),
                com.sekwah.narutomod.entity.NarutoEntities.KIRI_NINJA.get(), com.sekwah.narutomod.item.NarutoItems.HEADBAND_MIST.get(),
                com.sekwah.narutomod.entity.NarutoEntities.KUMO_NINJA.get(), com.sekwah.narutomod.item.NarutoItems.HEADBAND_CLOUD.get(),
                com.sekwah.narutomod.entity.NarutoEntities.IWA_NINJA.get(), com.sekwah.narutomod.item.NarutoItems.HEADBAND_ROCK.get()).entrySet()) {
            var ninja = entry.getKey().create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            helper.assertTrue(ninja != null, "Per-village ninja must instantiate");
            if (ninja != null) {
                ninja.equipForVillage(ninja.getVillageType(), helper.getLevel().getRandom());
                helper.assertTrue(ninja.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).is(entry.getValue()),
                        "Per-village ninja must equip matching headband: " + ninja.getVillageType());
            }
        }

        for (var villagerType : java.util.List.of(
                com.sekwah.narutomod.entity.NarutoEntities.KONOHA_VILLAGER.get(),
                com.sekwah.narutomod.entity.NarutoEntities.SUNA_VILLAGER.get(),
                com.sekwah.narutomod.entity.NarutoEntities.KIRI_VILLAGER.get(),
                com.sekwah.narutomod.entity.NarutoEntities.KUMO_VILLAGER.get(),
                com.sekwah.narutomod.entity.NarutoEntities.IWA_VILLAGER.get())) {
            var villager = villagerType.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            helper.assertTrue(villager != null, "ShinobiVillagerEntity must instantiate");
            if (villager != null) {
                helper.assertValueEqual(villager.getOffers().size(), 10,
                        "ShinobiVillagerEntity offer count for " + villager.getVillageType());
            }
        }

        // Phase 3 structures & template pools loaded cleanly in datapack registries
        var structureRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String structName : new String[]{
                "konoha_village", "suna_village", "kiri_village", "kumo_village", "iwa_village",
                "valley_of_the_end", "chunin_arena", "myobokuzan_shrine", "uchiha_shrine", "akatsuki_hideout"}) {
            helper.assertTrue(
                    structureRegistry.get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, structName)).isPresent(),
                    "Structure must load cleanly: " + structName);
        }
        var poolRegistry = helper.getLevel().registryAccess().lookupOrThrow(Registries.TEMPLATE_POOL);
        for (String poolName : new String[]{
                "konoha_village", "konoha_streets", "konoha_village_addons", "konoha_ichiraku_unique",
                "suna_village", "suna_streets", "suna_village_addons",
                "kiri_village", "kiri_streets", "kiri_village_addons",
                "kumo_village", "kumo_streets", "kumo_village_addons",
                "iwa_village", "iwa_streets", "iwa_village_addons",
                "valley_of_the_end", "valley_of_the_end_addons", "valley_hashirama_pool", "valley_madara_pool", "valley_river_pool",
                "chunin_arena", "chunin_arena_addons", "chunin_kage_pool",
                "myobokuzan_shrine", "myobokuzan_shrine_addons",
                "uchiha_shrine", "uchiha_shrine_addons",
                "akatsuki_hideout", "akatsuki_hideout_addons"}) {
            helper.assertTrue(
                    poolRegistry.get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, poolName)).isPresent(),
                    "Template pool must load cleanly: " + poolName);
        }

        var structMgr = helper.getLevel().getStructureTemplateManager();
        for (String nbtName : new String[]{
                "konoha_plaza", "konoha_main_street", "konoha_crossroad", "konoha_gate", "konoha_training_ground", "konoha_ichiraku_house", "konoha_house", "konoha_dojo",
                "suna_dome", "suna_avenue", "suna_courtyard", "suna_gate", "suna_barracks", "suna_workshop",
                "kiri_tower", "kiri_bridge", "kiri_canal_plaza", "kiri_gate", "kiri_harbor_house", "kiri_armory",
                "kumo_peak_tower", "kumo_sky_bridge", "kumo_cloud_plaza", "kumo_gate", "kumo_thunder_dojo", "kumo_pavilion",
                "iwa_keep", "iwa_stone_bridge", "iwa_pillars", "iwa_gate", "iwa_forge", "iwa_dwelling",
                "valley_waterfall", "valley_hashirama_statue", "valley_madara_statue", "valley_lower_river", "valley_canyon_walls", "valley_shore",
                "chunin_arena_floor", "chunin_stands", "chunin_kage_box",
                "myobokuzan_sanctuary", "myobokuzan_torii",
                "uchiha_shrine", "uchiha_courtyard",
                "akatsuki_hideout", "akatsuki_canyon"}) {
            helper.assertTrue(
                    structMgr.get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, nbtName)).isPresent(),
                    "Structure template NBT must load cleanly in StructureTemplateManager: " + nbtName);
        }

        // Verify Phoenix Flower spawns 4 mini fireballs and Chidori Stream spawns persistent field effect
        net.minecraft.world.entity.player.Player caster = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        net.minecraft.core.BlockPos testOrigin = helper.absolutePos(new net.minecraft.core.BlockPos(1, 2, 1));
        caster.setPos(testOrigin.getX() + 0.5D, testOrigin.getY(), testOrigin.getZ() + 0.5D);
        NinjaData casterData = caster.getData(NinjaCapabilityHandler.NINJA_DATA);
        NarutoAbilities.PHOENIX_FLOWER.get().performServer(caster, casterData, 0);
        var fireballs = helper.getLevel().getEntitiesOfClass(
                com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity.class,
                caster.getBoundingBox().inflate(8.0D),
                fb -> fb.getOwner() == caster && fb.isPhoenixMini());
        helper.assertValueEqual(fireballs.size(), 4, "Phoenix Flower mini fireball spawn count");
        for (var fb : fireballs) {
            fb.tick();
            helper.assertTrue(fb.isAlive(), "Phoenix Flower mini fireball must not self-clash with sibling fireballs");
        }

        NarutoAbilities.CHIDORI_STREAM.get().performServer(caster, casterData, 0);
        var fields = helper.getLevel().getEntitiesOfClass(
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.class,
                caster.getBoundingBox().inflate(6.0D),
                fx -> fx.getVisualType() == com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_CHIDORI_BURST
                        && fx.getTargetId() == caster.getId());
        helper.assertValueEqual(fields.size(), 1, "Chidori Stream persistent electric field entity count");
        helper.assertTrue(fields.get(0).getMaxLife() >= 30, "Chidori Stream field maxLife must be >= 30 ticks (1.5s)");

        var advancements = helper.getLevel().getServer().getAdvancements();
        for (String advPath : new String[]{
                "shinobi/five_great_nations", "shinobi/sage_of_myobokuzan", "shinobi/seven_swordsmen",
                "shinobi/rank_jonin", "shinobi/rank_anbu", "shinobi/clan_uchiha", "shinobi/clan_hyuga",
                "shinobi/clan_uzumaki", "shinobi/clan_senju", "shinobi/clan_kaguya", "shinobi/clan_nara",
                "shinobi/medical_shinobi", "shinobi/eight_gates_master", "shinobi/master_rasenshuriken",
                "shinobi/thunder_god_kirin", "shinobi/wind_demon_fuma", "shinobi/arena_champion",
                "shinobi/akatsuki_hunter", "shinobi/valley_of_the_end"}) {
            helper.assertTrue(
                    advancements.get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, advPath)) != null,
                    "Advancement must load cleanly: " + advPath);
        }

        // Phase 2 registry checks
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.CHAKRA_METAL_ORE.get() != null, "Chakra Metal Ore must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.DEEPSLATE_CHAKRA_METAL_ORE.get() != null, "Deepslate Chakra Metal Ore must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.CHAKRA_METAL_BLOCK.get() != null, "Chakra Metal Block must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.MAKIBISHI.get() != null, "Makibishi must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.WIRE_TRAP.get() != null, "Wire Trap must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.WEAPON_RACK.get() != null, "Weapon Rack must be registered");
        helper.assertTrue(com.sekwah.narutomod.item.NarutoItems.RAW_CHAKRA_METAL.get() != null, "Raw Chakra Metal must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlockEntities.WEAPON_RACK.get() != null, "Weapon Rack BlockEntity must be registered");
        helper.assertTrue(
                helper.getLevel().registryAccess()
                        .lookupOrThrow(ResourceKey.createRegistryKey(Identifier.withDefaultNamespace("worldgen/feature")))
                        .get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ore_chakra_metal")).isPresent(),
                "Chakra Metal ore feature must load cleanly in worldgen/feature registry");
        helper.assertTrue(
                helper.getLevel().registryAccess().lookupOrThrow(Registries.PLACED_FEATURE)
                        .get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "ore_chakra_metal_placed")).isPresent(),
                "Chakra Metal ore placed feature must load cleanly in worldgen/placed_feature registry");

        // Phase 2 live WeaponRackBlock + WeaponRackBlockEntity comparator & slot workflow
        net.minecraft.core.BlockPos rackPos = new net.minecraft.core.BlockPos(2, 1, 2);
        helper.setBlock(rackPos, com.sekwah.narutomod.block.NarutoBlocks.WEAPON_RACK.get().defaultBlockState());
        com.sekwah.narutomod.block.WeaponRackBlockEntity rackEntity =
                helper.getBlockEntity(rackPos, com.sekwah.narutomod.block.WeaponRackBlockEntity.class);
        helper.assertValueEqual(rackEntity.getAnalogOutputSignal(), 0, "Empty weapon rack analog signal");

        net.minecraft.world.item.ItemStack kunaiStack = new net.minecraft.world.item.ItemStack(
                com.sekwah.narutomod.item.NarutoItems.KUNAI.get(), 2);
        int slot1 = rackEntity.placeItem(com.sekwah.narutomod.block.WeaponRackBlockEntity.SLOT_UPPER, kunaiStack);
        helper.assertValueEqual(slot1, com.sekwah.narutomod.block.WeaponRackBlockEntity.SLOT_UPPER, "Weapon rack upper slot placement");
        helper.assertValueEqual(rackEntity.getAnalogOutputSignal(), 7, "1-slot weapon rack analog signal");

        net.minecraft.world.item.ItemStack shurikenStack = new net.minecraft.world.item.ItemStack(
                com.sekwah.narutomod.item.NarutoItems.SHURIKEN.get(), 1);
        int slot2 = rackEntity.placeItem(com.sekwah.narutomod.block.WeaponRackBlockEntity.SLOT_LOWER, shurikenStack);
        helper.assertValueEqual(slot2, com.sekwah.narutomod.block.WeaponRackBlockEntity.SLOT_LOWER, "Weapon rack lower slot placement");
        helper.assertValueEqual(rackEntity.getAnalogOutputSignal(), 15, "Full weapon rack analog signal");

        net.minecraft.world.item.ItemStack extracted = rackEntity.takeItem(com.sekwah.narutomod.block.WeaponRackBlockEntity.SLOT_UPPER);
        helper.assertTrue(extracted.is(com.sekwah.narutomod.item.NarutoItems.KUNAI.get()), "Weapon rack should return Kunai");
        helper.assertValueEqual(rackEntity.getAnalogOutputSignal(), 7, "Weapon rack analog signal after 1 extract");
        rackEntity.takeItem(com.sekwah.narutomod.block.WeaponRackBlockEntity.SLOT_LOWER);
        helper.assertValueEqual(rackEntity.getAnalogOutputSignal(), 0, "Weapon rack analog signal when emptied");

        // Phase 2 live MakibishiBlock state & WireTrapBlock + PaperBombBlock synergy check
        net.minecraft.core.BlockPos trapPos = new net.minecraft.core.BlockPos(3, 1, 2);
        helper.setBlock(trapPos, com.sekwah.narutomod.block.NarutoBlocks.MAKIBISHI.get().defaultBlockState());
        helper.assertValueEqual(
                helper.getBlockState(trapPos).getValue(com.sekwah.narutomod.block.weapons.MakibishiBlock.CHARGES),
                com.sekwah.narutomod.block.weapons.MakibishiBlock.MAX_CHARGES,
                "Default Makibishi charge count");
        helper.assertTrue(
                com.sekwah.narutomod.block.weapons.WireTrapBlock.isPaperBombState(
                        com.sekwah.narutomod.block.NarutoBlocks.PAPER_BOMB.get().defaultBlockState()),
                "WireTrap must recognize PaperBombBlock state");

        // Phase 2 live declarative JutsuUnlockRules check on registered abilities
        helper.assertTrue(NarutoAbilities.RASENGAN.get().requiresScroll(), "Registered Rasengan must require scroll");
        helper.assertTrue("uchiha".equals(NarutoAbilities.SHARINGAN.get().getRequiredClanPath()), "Registered Sharingan must require uchiha clan");

        // Phase 3 live registry, jutsu & item integration checks
        helper.assertTrue(NarutoAbilities.ASH_PILE_BURNING.get() != null, "Ash Pile Burning ability must be registered");
        helper.assertTrue(NarutoAbilities.WATER_ENCAMPMENT_WALL.get() != null, "Water Encampment Wall ability must be registered");
        helper.assertTrue(NarutoAbilities.CHAKRA_SCALPEL.get() != null, "Chakra Scalpel ability must be registered");
        helper.assertTrue(com.sekwah.narutomod.item.NarutoItems.GUNBAI_UCHIWA.get() != null, "Gunbai Uchiwa must be registered");
        helper.assertTrue(com.sekwah.narutomod.item.NarutoItems.MEDICAL_POUCH.get() != null, "Medical Pouch must be registered");

        NarutoAbilities.ASH_PILE_BURNING.get().performServer(caster, casterData, 0);
        var ashClouds = helper.getLevel().getEntitiesOfClass(
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.class,
                caster.getBoundingBox().inflate(14.0D),
                fx -> fx.getVisualType() == com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_ASH_CLOUD);
        helper.assertValueEqual(ashClouds.size(), 1, "Ash Pile Burning cloud entity count");
        ashClouds.get(0).detonateAshCloud(helper.getLevel());
        helper.assertFalse(ashClouds.get(0).isAlive(), "Detonated Ash Cloud must discard cleanly");

        NarutoAbilities.WATER_ENCAMPMENT_WALL.get().performServer(caster, casterData, 0);
        var waterWalls = helper.getLevel().getEntitiesOfClass(
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.class,
                caster.getBoundingBox().inflate(8.0D),
                fx -> fx.getVisualType() == com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_WATER_WALL);
        helper.assertValueEqual(waterWalls.size(), 1, "Water Encampment Wall barrier entity count");

        NarutoAbilities.CHAKRA_SCALPEL.get().performServer(caster, casterData, 0);
        helper.assertTrue("chakra_scalpel".equals(casterData.getActiveHandJutsu()), "Chakra Scalpel must set activeHandJutsu");
        helper.assertTrue(casterData.getActiveHandJutsuTicks() >= 200, "Chakra Scalpel duration must be >= 200 ticks");
        com.sekwah.narutomod.abilities.jutsus.ChakraScalpelAbility.executeStrike(caster, casterData, null);
        helper.assertTrue("chakra_scalpel".equals(casterData.getActiveHandJutsu()), "Chakra Scalpel must remain active after strike");

        caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.POISON, 100, 0));
        caster.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100, 1));
        int cleansed = com.sekwah.narutomod.item.MedicalPouchItem.cleanseEffects(caster);
        helper.assertValueEqual(cleansed, 2, "Medical Pouch cleansed debuff count");
        helper.assertFalse(caster.hasEffect(net.minecraft.world.effect.MobEffects.POISON), "Poison must be cleansed");

        net.minecraft.world.item.ItemStack pouchStack = new net.minecraft.world.item.ItemStack(
                com.sekwah.narutomod.item.NarutoItems.MEDICAL_POUCH.get());
        helper.assertValueEqual(pouchStack.getMaxDamage(), com.sekwah.narutomod.item.MedicalPouchItem.MAX_DURABILITY,
                "Medical Pouch max durability");
        net.minecraft.world.item.ItemStack gunbaiStack = new net.minecraft.world.item.ItemStack(
                com.sekwah.narutomod.item.NarutoItems.GUNBAI_UCHIWA.get());
        helper.assertFalse(gunbaiStack.isEmpty(), "Gunbai Uchiwa ItemStack must instantiate");

        helper.assertTrue(
                helper.getLevel().registryAccess().lookupOrThrow(Registries.STRUCTURE_TYPE)
                        .get(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "anchored_jigsaw")).isPresent(),
                "Custom narutomod:anchored_jigsaw StructureType must be registered");

        // v0.2.0 live registry, Mission Board, Mission v2, Hiraishin & Sage Mode checks
        helper.assertTrue(com.sekwah.narutomod.item.NarutoItems.FLASH_BOMB.get() != null, "Flash Bomb item must be registered");
        helper.assertTrue(com.sekwah.narutomod.item.NarutoItems.HIRAISHIN_KUNAI.get() != null, "Hiraishin Kunai item must be registered");
        helper.assertTrue(com.sekwah.narutomod.item.NarutoItems.SAGE_TRIAL_BOSS_SPAWN_EGG.get() != null, "Sage Trial Boss spawn egg must be registered");
        helper.assertTrue(com.sekwah.narutomod.block.NarutoBlocks.MISSION_BOARD.get() != null, "Mission Board block must be registered");
        helper.assertTrue(com.sekwah.narutomod.entity.NarutoEntities.FLASH_BOMB.get() != null, "Flash Bomb entity must be registered");
        helper.assertTrue(com.sekwah.narutomod.entity.NarutoEntities.HIRAISHIN_KUNAI.get() != null, "Hiraishin Kunai entity must be registered");
        helper.assertTrue(com.sekwah.narutomod.entity.NarutoEntities.SAGE_TRIAL_BOSS.get() != null, "Sage Trial Boss entity must be registered");
        helper.assertTrue(NarutoAbilities.HIRAISHIN.get() != null, "Hiraishin ability must be registered");
        helper.assertTrue(NarutoAbilities.SAGE_MODE.get() != null, "Sage Mode ability must be registered");
        helper.assertTrue(com.sekwah.narutomod.capabilities.MissionDataManager.getAll().size() >= 9,
                "MissionDataManager must expose at least 9 missions");

        net.minecraft.core.BlockPos boardPos = new net.minecraft.core.BlockPos(4, 1, 2);
        helper.setBlock(boardPos, com.sekwah.narutomod.block.NarutoBlocks.MISSION_BOARD.get().defaultBlockState());
        helper.assertTrue(helper.getBlockState(boardPos).is(com.sekwah.narutomod.block.NarutoBlocks.MISSION_BOARD.get()),
                "Mission Board block must place cleanly");

        com.sekwah.narutomod.capabilities.SageModeHelper.activateSageMode(caster, false);
        helper.assertTrue(com.sekwah.narutomod.capabilities.SageModeHelper.isSageModeActive(caster),
                "Sage Mode must activate via SageModeHelper");
        helper.assertTrue(com.sekwah.narutomod.capabilities.SageModeHelper.applySageModeDamageMultiplier(10.0F, caster) > 12.9F,
                "Active Sage Mode must boost damage by +30%");

        helper.succeed();
    }
}
