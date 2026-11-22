package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.ChuninChampionEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import com.sekwah.narutomod.entity.mob.ShinobiVillagerEntity;
import com.sekwah.narutomod.entity.mob.VillageNinjaEntity;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.animal.golem.SnowGolem;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.wanderingtrader.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for mob faction targeting rules across ChuninChampionEntity,
 * RogueNinjaEntity, and AkatsukiNinjaEntity.
 */
public class MobFactionTargetingTest {

    @BeforeAll
    static void bootstrapRegistries() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    @DisplayName("ChuninChampionEntity never targets protected village allies regardless of duel state")
    public void testChuninChampionProtectedFriendlies() {
        Class<?>[] protectedClasses = {
                VillageNinjaEntity.class,
                ShinobiVillagerEntity.class,
                ChuninChampionEntity.class,
                AbstractVillager.class,
                Villager.class,
                WanderingTrader.class,
                AbstractGolem.class,
                IronGolem.class,
                SnowGolem.class
        };

        for (Class<?> protectedClass : protectedClasses) {
            assertTrue(ChuninChampionEntity.isProtectedFriendlyClass(protectedClass),
                    protectedClass.getSimpleName() + " must be recognized as a protected friendly");
            assertFalse(ChuninChampionEntity.canAttackTargetClass(protectedClass, false),
                    "ChuninChampion must not attack " + protectedClass.getSimpleName() + " outside a duel");
            assertFalse(ChuninChampionEntity.canAttackTargetClass(protectedClass, true),
                    "ChuninChampion must not attack " + protectedClass.getSimpleName() + " during a duel");
        }

        assertFalse(ChuninChampionEntity.isProtectedFriendly(null));
        assertFalse(ChuninChampionEntity.isProtectedFriendlyClass(null));
        assertFalse(ChuninChampionEntity.canAttackTargetClass(null, true));
    }

    @Test
    @DisplayName("ChuninChampionEntity gates Player/ShadowClone combat on duelActive while fighting hostiles anytime")
    public void testChuninChampionDuelGatingAndHostileTargeting() {
        // Players and Shadow Clones require an active duel
        assertFalse(ChuninChampionEntity.canAttackTargetClass(Player.class, false),
                "ChuninChampion must not attack players before duel activation");
        assertFalse(ChuninChampionEntity.canAttackTargetClass(ShadowCloneEntity.class, false),
                "ChuninChampion must not attack shadow clones before duel activation");

        assertTrue(ChuninChampionEntity.canAttackTargetClass(Player.class, true),
                "ChuninChampion must attack players once duel is active");
        assertTrue(ChuninChampionEntity.canAttackTargetClass(ShadowCloneEntity.class, true),
                "ChuninChampion may attack shadow clones once duel is active");

        // Hostile mobs are valid targets both outside and during duels
        Class<?>[] hostileClasses = {
                AkatsukiNinjaEntity.class,
                RogueNinjaEntity.class,
                Monster.class,
                Zombie.class,
                Skeleton.class,
                Spider.class
        };
        for (Class<?> hostileClass : hostileClasses) {
            assertTrue(ChuninChampionEntity.canAttackTargetClass(hostileClass, false),
                    "ChuninChampion must fight " + hostileClass.getSimpleName() + " even outside a duel");
            assertTrue(ChuninChampionEntity.canAttackTargetClass(hostileClass, true),
                    "ChuninChampion must also be able to fight " + hostileClass.getSimpleName() + " during a duel");
        }

        assertFalse(ChuninChampionEntity.isValidHostileTarget(null));
        assertFalse(ChuninChampionEntity.shouldClearTargetOutsideDuel(null, false));
        assertFalse(ChuninChampionEntity.shouldClearTargetOutsideDuel(null, true));
    }

    @Test
    @DisplayName("RogueNinjaEntity and AkatsukiNinjaEntity are allies and never attack each other")
    public void testOutlawAllianceImmunity() {
        assertTrue(RogueNinjaEntity.isOutlawAllyClass(RogueNinjaEntity.class));
        assertTrue(RogueNinjaEntity.isOutlawAllyClass(AkatsukiNinjaEntity.class));
        assertTrue(AkatsukiNinjaEntity.isOutlawAllyClass(RogueNinjaEntity.class));
        assertTrue(AkatsukiNinjaEntity.isOutlawAllyClass(AkatsukiNinjaEntity.class));

        assertFalse(RogueNinjaEntity.shouldTargetClass(RogueNinjaEntity.class),
                "RogueNinja must not target other RogueNinjas");
        assertFalse(RogueNinjaEntity.shouldTargetClass(AkatsukiNinjaEntity.class),
                "RogueNinja must not target AkatsukiNinjas");
        assertFalse(AkatsukiNinjaEntity.shouldTargetClass(RogueNinjaEntity.class),
                "AkatsukiNinja must not target RogueNinjas");
        assertFalse(AkatsukiNinjaEntity.shouldTargetClass(AkatsukiNinjaEntity.class),
                "AkatsukiNinja must not target other AkatsukiNinjas");

        assertFalse(RogueNinjaEntity.isOutlawAlly(null));
        assertFalse(AkatsukiNinjaEntity.isOutlawAlly(null));
        assertFalse(RogueNinjaEntity.isValidHostileTarget(null));
        assertFalse(AkatsukiNinjaEntity.isValidHostileTarget(null));
        assertFalse(RogueNinjaEntity.shouldTargetClass(null));
        assertFalse(AkatsukiNinjaEntity.shouldTargetClass(null));
    }

    @Test
    @DisplayName("Outlaw shinobi attack players, mod/vanilla NPCs, golems, shadow clones, and other monsters, but ignore passive animals")
    public void testOutlawHostileAndPassiveRules() {
        Class<?>[] validOutlawTargets = {
                Player.class,
                VillageNinjaEntity.class,
                ShinobiVillagerEntity.class,
                ChuninChampionEntity.class,
                ShadowCloneEntity.class,
                AbstractVillager.class,
                Villager.class,
                WanderingTrader.class,
                AbstractGolem.class,
                IronGolem.class,
                SnowGolem.class,
                Monster.class,
                Zombie.class,
                Skeleton.class,
                Spider.class
        };

        for (Class<?> targetClass : validOutlawTargets) {
            assertTrue(RogueNinjaEntity.shouldTargetClass(targetClass),
                    "RogueNinja must target " + targetClass.getSimpleName());
            assertTrue(AkatsukiNinjaEntity.shouldTargetClass(targetClass),
                    "AkatsukiNinja must target " + targetClass.getSimpleName());
        }

        Class<?>[] passiveAnimals = {
                Animal.class,
                Cow.class,
                Pig.class,
                Chicken.class
        };

        for (Class<?> passiveClass : passiveAnimals) {
            assertFalse(RogueNinjaEntity.shouldTargetClass(passiveClass),
                    "RogueNinja must not target passive animal " + passiveClass.getSimpleName());
            assertFalse(AkatsukiNinjaEntity.shouldTargetClass(passiveClass),
                    "AkatsukiNinja must not target passive animal " + passiveClass.getSimpleName());
        }
    }

    @Test
    @DisplayName("Goal wiring and duel lifecycle contracts are preserved in entity sources")
    public void testEntityGoalAndDuelWiring() throws IOException {
        String championSrc = Files.readString(
                Paths.get("src/main/java/com/sekwah/narutomod/entity/mob/ChuninChampionEntity.java"));
        assertTrue(championSrc.contains("new HurtByTargetGoal(this,"),
                "ChuninChampion must ignore friendly retaliation in HurtByTargetGoal");
        assertTrue(championSrc.contains("NearestAttackableTargetGoal<>(this, AkatsukiNinjaEntity.class"),
                "ChuninChampion must target AkatsukiNinjaEntity");
        assertTrue(championSrc.contains("NearestAttackableTargetGoal<>(this, RogueNinjaEntity.class"),
                "ChuninChampion must target RogueNinjaEntity");
        assertTrue(championSrc.contains("NearestAttackableTargetGoal<>(this, Monster.class"),
                "ChuninChampion must target hostile Monster entities");
        assertTrue(championSrc.contains("this.mobInteract") || championSrc.contains("mobInteract("),
                "ChuninChampion must support mobInteract duel trigger");
        assertTrue(championSrc.contains("hurtServer(") && championSrc.contains("activateDuel(player)"),
                "ChuninChampion must activate duel on player damage or interaction");

        for (String outlawPath : new String[]{
                "src/main/java/com/sekwah/narutomod/entity/mob/RogueNinjaEntity.java",
                "src/main/java/com/sekwah/narutomod/entity/mob/AkatsukiNinjaEntity.java"
        }) {
            String outlawSrc = Files.readString(Paths.get(outlawPath));
            assertTrue(outlawSrc.contains("new HurtByTargetGoal(this, RogueNinjaEntity.class, AkatsukiNinjaEntity.class)"),
                    outlawPath + " must ignore friendly fire from RogueNinjaEntity and AkatsukiNinjaEntity");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, VillageNinjaEntity.class"),
                    outlawPath + " must target VillageNinjaEntity");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, ShinobiVillagerEntity.class"),
                    outlawPath + " must target ShinobiVillagerEntity");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, ChuninChampionEntity.class"),
                    outlawPath + " must target ChuninChampionEntity");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, ShadowCloneEntity.class"),
                    outlawPath + " must target ShadowCloneEntity");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, AbstractVillager.class"),
                    outlawPath + " must target AbstractVillager");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, AbstractGolem.class"),
                    outlawPath + " must target AbstractGolem");
            assertTrue(outlawSrc.contains("NearestAttackableTargetGoal<>(this, Monster.class"),
                    outlawPath + " must target non-allied Monster entities");
            assertTrue(outlawSrc.contains("public boolean canAttack(LivingEntity target)"),
                    outlawPath + " must override canAttack to enforce outlaw alliance");
        }
    }
}
