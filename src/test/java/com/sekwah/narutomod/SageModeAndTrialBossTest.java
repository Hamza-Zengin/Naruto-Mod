package com.sekwah.narutomod;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.JutsuDataManager;
import com.sekwah.narutomod.abilities.jutsus.SageModeAbility;
import com.sekwah.narutomod.capabilities.JutsuUnlockRules;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.capabilities.SageModeHelper;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.entity.ai.NinjaJutsuProfile;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.ChuninChampionEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import com.sekwah.narutomod.entity.mob.SageTrialBossEntity;
import com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity;
import com.sekwah.narutomod.entity.mob.VillageNinjaEntity;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Feature B2 (SageTrialBossEntity) and Feature J1 (SageModeHelper + SageModeAbility).
 */
public class SageModeAndTrialBossTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("SageModeHelper handles 3-stage mastery scaling, NBT clamping, Cloak bonuses, and stationary threshold")
    void testSageModeHelperMathAndNbt() {
        assertEquals(1, SageModeHelper.getSageStage(0));
        assertEquals(1, SageModeHelper.getSageStage(499));
        assertEquals(2, SageModeHelper.getSageStage(500));
        assertEquals(2, SageModeHelper.getSageStage(999));
        assertEquals(3, SageModeHelper.getSageStage(1000));
        assertEquals(3, SageModeHelper.getSageStage(1500));

        assertEquals(1.20F, SageModeHelper.getSageDamageMultiplier(1), 0.0001F);
        assertEquals(1.32F, SageModeHelper.getSageDamageMultiplier(2), 0.0001F);
        assertEquals(1.45F, SageModeHelper.getSageDamageMultiplier(3), 0.0001F);
        assertEquals(1.35F, SageModeHelper.SAGE_CLOAK_CHARGE_MULTIPLIER, 0.0001F);
        assertEquals(1.25F, SageModeHelper.SAGE_CLOAK_DURATION_MULTIPLIER, 0.0001F);
        assertEquals(0.0025D, SageModeHelper.STATIONARY_SPEED_SQR_THRESHOLD, 0.00001D);

        assertTrue(SageModeHelper.isStationary(0.0D));
        assertTrue(SageModeHelper.isStationary(0.0025D));
        assertFalse(SageModeHelper.isStationary(0.0026D));

        assertEquals(1.25F, SageModeHelper.computeNatureEnergyGainPerTick(1, false), 0.0001F);
        assertEquals(2.2F, SageModeHelper.computeNatureEnergyGainPerTick(2, false), 0.0001F);
        assertEquals(3.8F, SageModeHelper.computeNatureEnergyGainPerTick(3, false), 0.0001F);
        assertEquals(1.25F * 1.35F, SageModeHelper.computeNatureEnergyGainPerTick(1, true), 0.0001F);

        assertEquals(400, SageModeHelper.computeSageModeDurationTicks(1, false));
        assertEquals(700, SageModeHelper.computeSageModeDurationTicks(2, false));
        assertEquals(1100, SageModeHelper.computeSageModeDurationTicks(3, false));
        assertEquals(1375, SageModeHelper.computeSageModeDurationTicks(3, true));

        assertEquals(16.0D, SageModeHelper.getSageSensoryRadius(1), 0.0001D);
        assertEquals(26.0D, SageModeHelper.getSageSensoryRadius(2), 0.0001D);
        assertEquals(36.0D, SageModeHelper.getSageSensoryRadius(3), 0.0001D);

        assertEquals(10.0F, SageModeHelper.applySageModeDamageMultiplier(10.0F, false), 0.0001F);
        assertEquals(12.0F, SageModeHelper.applySageModeDamageMultiplier(10.0F, 1), 0.0001F);
        assertEquals(13.2F, SageModeHelper.applySageModeDamageMultiplier(10.0F, 2), 0.0001F);
        assertEquals(14.5F, SageModeHelper.applySageModeDamageMultiplier(10.0F, 3), 0.0001F);
        assertEquals(10.0F, SageModeHelper.applySageModeDamageMultiplier(10.0F, (Player) null), 0.0001F);

        CompoundTag tag = new CompoundTag();
        assertFalse(SageModeHelper.isSageModeActive(tag));
        SageModeHelper.setNatureEnergy(tag, 150.0F);
        assertEquals(100.0F, SageModeHelper.getNatureEnergy(tag), 0.0001F);
        SageModeHelper.setNatureEnergy(tag, -10.0F);
        assertEquals(0.0F, SageModeHelper.getNatureEnergy(tag), 0.0001F);

        SageModeHelper.setSageModeTicks(tag, 2000);
        assertEquals(1375, SageModeHelper.getSageModeTicks(tag));
        assertTrue(SageModeHelper.isSageModeActive(tag));
        SageModeHelper.setSageModeTicks(tag, -5);
        assertEquals(0, SageModeHelper.getSageModeTicks(tag));
        assertFalse(SageModeHelper.isSageModeActive(tag));
    }

    @Test
    @DisplayName("SageModeAbility enforces CHANNELED JONIN metadata and explicit Sage Trial unlock")
    void testSageModeAbilityUnlockAndMetadata() {
        Identifier sageModeId = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "sage_mode");
        SageModeAbility ability = new SageModeAbility();
        ability.setRegistryName(sageModeId);

        assertEquals(Ability.ActivationType.CHANNELED, ability.activationType());
        assertEquals(NinjaRank.JONIN, ability.getRequiredRank());
        assertTrue(ability.requiresScroll());
        assertFalse(ability.canActivateBelowMinCharge());
        assertEquals(0.0F, ability.getDefaultChakraCost(), 0.0001F);
        assertEquals(0.0F, ability.getDefaultStaminaCost(), 0.0001F);
        assertEquals(200, ability.getDefaultCooldown());
        assertEquals(0.0F, ability.getDefaultDamage(), 0.0001F);

        // Even a Jonin/Kage cannot activate Sage Mode without defeating SageTrialBossEntity
        NinjaData joninLocked = new NinjaData(true);
        joninLocked.setRank(NinjaRank.JONIN.name());
        assertFalse(JutsuUnlockRules.checkAbilityUnlocked(joninLocked, ability, sageModeId));
        assertFalse(ability.canActivate(null, joninLocked));

        // Explicitly unlocking narutomod:sage_mode enables activation
        joninLocked.unlockAbility(sageModeId);
        assertTrue(JutsuUnlockRules.checkAbilityUnlocked(joninLocked, ability, sageModeId));
        assertTrue(ability.canActivate(null, joninLocked));
    }

    @Test
    @DisplayName("SageTrialBossEntity uses Water/Wind profile and enforces faction & duel targeting")
    void testSageTrialBossProfileAndTargeting() {
        NinjaJutsuProfile profile = SageTrialBossEntity.createSageTrialProfile();
        assertTrue(profile.isWaterBulletEnabled());
        assertTrue(profile.isVacuumSpheresEnabled());
        assertTrue(profile.isKawarimiEnabled());
        assertFalse(profile.isFireballEnabled());
        assertEquals(36, profile.getGlobalCooldownTicks());
        assertEquals(8.0F, profile.getWaterBulletDamage(), 0.0001F);
        assertEquals(85, profile.getWaterBulletCooldownTicks());
        assertEquals(4.5F, profile.getVacuumSpheresDamage(), 0.0001F);
        assertEquals(90, profile.getVacuumSpheresCooldownTicks());
        assertEquals(220, profile.getKawarimiCooldownTicks());

        AttributeSupplier bossAttributes = SageTrialBossEntity.createAttributes().build();
        assertEquals(180.0D, bossAttributes.getBaseValue(Attributes.MAX_HEALTH), 0.0001D);
        assertEquals(8.0D, bossAttributes.getBaseValue(Attributes.ARMOR), 0.0001D);
        assertEquals(8.0D, bossAttributes.getBaseValue(Attributes.ATTACK_DAMAGE), 0.0001D);
        assertEquals(0.32D, bossAttributes.getBaseValue(Attributes.MOVEMENT_SPEED), 0.0001D);
        assertEquals(0.45D, bossAttributes.getBaseValue(Attributes.KNOCKBACK_RESISTANCE), 0.0001D);

        assertTrue(SageTrialBossEntity.isProtectedFriendlyClass(VillageNinjaEntity.class));
        assertTrue(SageTrialBossEntity.isProtectedFriendlyClass(ShinobiVillagerEntity.class));
        assertTrue(SageTrialBossEntity.isProtectedFriendlyClass(ChuninChampionEntity.class));
        assertTrue(SageTrialBossEntity.isProtectedFriendlyClass(SageTrialBossEntity.class));
        assertTrue(SageTrialBossEntity.isProtectedFriendlyClass(AbstractVillager.class));
        assertTrue(SageTrialBossEntity.isProtectedFriendlyClass(AbstractGolem.class));

        assertFalse(SageTrialBossEntity.canAttackTargetClass(Player.class, false));
        assertFalse(SageTrialBossEntity.canAttackTargetClass(ShadowCloneEntity.class, false));
        assertTrue(SageTrialBossEntity.canAttackTargetClass(Player.class, true));
        assertTrue(SageTrialBossEntity.canAttackTargetClass(ShadowCloneEntity.class, true));
        assertTrue(SageTrialBossEntity.canAttackTargetClass(RogueNinjaEntity.class, false));
        assertTrue(SageTrialBossEntity.canAttackTargetClass(AkatsukiNinjaEntity.class, false));
        assertTrue(SageTrialBossEntity.canAttackTargetClass(Monster.class, false));
    }

    @Test
    @DisplayName("Sage Mode jutsu JSON, boss loot table, spawn egg, eye/pigment textures, and 128x128 toad boss texture are valid")
    void testSageAssetsAndDataFiles() throws IOException {
        Path jutsuJson = Paths.get("src/main/resources/data/narutomod/jutsus/sage_mode.json");
        assertTrue(Files.isRegularFile(jutsuJson));
        JsonElement jutsuElem = JsonParser.parseString(Files.readString(jutsuJson));
        DataResult<JutsuDataManager.JutsuStats> statsRes = JutsuDataManager.JutsuStats.CODEC.parse(JsonOps.INSTANCE, jutsuElem);
        assertTrue(statsRes.result().isPresent(), "sage_mode.json must decode cleanly");
        assertEquals(200, statsRes.result().get().cooldownTicks());
        assertEquals("JONIN", statsRes.result().get().requiredRank());

        Path lootJson = Paths.get("src/main/resources/data/narutomod/loot_table/entities/sage_trial_boss.json");
        assertTrue(Files.isRegularFile(lootJson));

        Path clientItem = Paths.get("src/main/resources/assets/narutomod/items/sage_trial_boss_spawn_egg.json");
        Path itemModel = Paths.get("src/main/resources/assets/narutomod/models/item/sage_trial_boss_spawn_egg.json");
        assertTrue(Files.isRegularFile(clientItem));
        assertTrue(Files.isRegularFile(itemModel));

        for (int stage = 1; stage <= 3; stage++) {
            Path eyeTex = Paths.get("src/main/resources/assets/narutomod/textures/entity/eyes/sage_eyes_" + stage + ".png");
            assertTrue(Files.isRegularFile(eyeTex));
        }
        assertTrue(Files.isRegularFile(Paths.get("src/main/resources/assets/narutomod/textures/entity/eyes/sage_pigment.png")));

        Path bossTex = Paths.get("src/main/resources/assets/narutomod/textures/entity/mob/toad_sage_boss.png");
        assertTrue(Files.isRegularFile(bossTex));
        BufferedImage img = ImageIO.read(bossTex.toFile());
        assertNotNull(img);
        assertEquals(128, img.getWidth());
        assertEquals(128, img.getHeight());
    }
}
