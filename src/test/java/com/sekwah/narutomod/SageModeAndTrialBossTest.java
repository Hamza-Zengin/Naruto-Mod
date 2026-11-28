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
    @DisplayName("SageModeHelper handles NBT clamping, Cloak bonuses, stationary threshold, and damage multiplier")
    void testSageModeHelperMathAndNbt() {
        assertEquals(1.30F, SageModeHelper.SAGE_DAMAGE_MULTIPLIER, 0.0001F);
        assertEquals(1.35F, SageModeHelper.SAGE_CLOAK_CHARGE_MULTIPLIER, 0.0001F);
        assertEquals(1.25F, SageModeHelper.SAGE_CLOAK_DURATION_MULTIPLIER, 0.0001F);
        assertEquals(0.0025D, SageModeHelper.STATIONARY_SPEED_SQR_THRESHOLD, 0.00001D);

        assertTrue(SageModeHelper.isStationary(0.0D));
        assertTrue(SageModeHelper.isStationary(0.0025D));
        assertFalse(SageModeHelper.isStationary(0.0026D));

        assertEquals(5.0F, SageModeHelper.computeNatureEnergyGainPerTick(false), 0.0001F);
        assertEquals(6.75F, SageModeHelper.computeNatureEnergyGainPerTick(true), 0.0001F);

        assertEquals(600, SageModeHelper.computeSageModeDurationTicks(false));
        assertEquals(750, SageModeHelper.computeSageModeDurationTicks(true));

        assertEquals(10.0F, SageModeHelper.applySageModeDamageMultiplier(10.0F, false), 0.0001F);
        assertEquals(13.0F, SageModeHelper.applySageModeDamageMultiplier(10.0F, true), 0.0001F);
        assertEquals(10.0F, SageModeHelper.applySageModeDamageMultiplier(10.0F, (Player) null), 0.0001F);

        CompoundTag tag = new CompoundTag();
        assertFalse(SageModeHelper.isSageModeActive(tag));
        SageModeHelper.setNatureEnergy(tag, 150.0F);
        assertEquals(100.0F, SageModeHelper.getNatureEnergy(tag), 0.0001F);
        SageModeHelper.setNatureEnergy(tag, -10.0F);
        assertEquals(0.0F, SageModeHelper.getNatureEnergy(tag), 0.0001F);

        SageModeHelper.setSageModeTicks(tag, 900);
        assertEquals(750, SageModeHelper.getSageModeTicks(tag));
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
    @DisplayName("Sage Mode jutsu JSON, boss loot table, spawn egg JSONs, and 64x64 boss texture are valid")
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

        Path bossTex = Paths.get("src/main/resources/assets/narutomod/textures/entity/mob/sage_trial_boss.png");
        assertTrue(Files.isRegularFile(bossTex));
        BufferedImage img = ImageIO.read(bossTex.toFile());
        assertNotNull(img);
        assertEquals(64, img.getWidth());
        assertEquals(64, img.getHeight());
    }
}
