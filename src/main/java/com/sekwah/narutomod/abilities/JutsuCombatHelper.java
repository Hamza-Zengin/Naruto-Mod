package com.sekwah.narutomod.abilities;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.sekclib.util.PlayerUtil;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Shared ally-safe AOE targeting, damage source resolution, and velocity synchronization helpers for jutsus.
 */
public final class JutsuCombatHelper {

    private JutsuCombatHelper() {}

    /**
     * Queries all living hostile entities inside {@code box}, excluding the caster and allied shadow clones/summons.
     */
    public static List<LivingEntity> findHostileTargetsInBox(ServerLevel level, @Nullable LivingEntity caster, AABB box) {
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive() && (caster == null || (!e.equals(caster) && !ShadowCloneEntity.areAllies(caster, e))));
    }

    /**
     * Queries all living hostile entities within cylindrical/spherical horizontal radius around {@code center}.
     */
    public static List<LivingEntity> findHostileTargetsInRadius(ServerLevel level, @Nullable LivingEntity caster,
                                                                Vec3 center, double radius, double halfHeight) {
        AABB box = new AABB(
                center.x - radius, center.y - halfHeight, center.z - radius,
                center.x + radius, center.y + halfHeight, center.z + radius);
        double radiusSq = radius * radius;
        return level.getEntitiesOfClass(LivingEntity.class, box,
                e -> e.isAlive()
                        && (caster == null || (!e.equals(caster) && !ShadowCloneEntity.areAllies(caster, e)))
                        && horizDistSq(e.position(), center) <= radiusSq);
    }

    private static double horizDistSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    /**
     * Applies velocity reliably to both Players (via SekCLib {@link PlayerUtil#setVelocity}) and non-player entities.
     */
    public static void applyVelocity(LivingEntity target, Vec3 velocity, boolean ignoreGround) {
        if (target == null || velocity == null) {
            return;
        }
        if (target instanceof Player player) {
            PlayerUtil.setVelocity(player, velocity, ignoreGround);
        } else {
            target.setDeltaMovement(velocity);
            target.needsSync = true;
        }
    }

    /**
     * Resolves an appropriate damage source for jutsu effects given an optional caster.
     */
    public static DamageSource playerOrMagicDamage(ServerLevel level, @Nullable LivingEntity caster) {
        if (caster instanceof Player player) {
            return level.damageSources().playerAttack(player);
        }
        if (caster != null) {
            return level.damageSources().mobAttack(caster);
        }
        return level.damageSources().magic();
    }

    /**
     * Resolves a {@link DamageTypes#LIGHTNING_BOLT} damage source with proper direct and causing entity attribution.
     */
    public static DamageSource lightningDamage(ServerLevel level, @Nullable Entity directEntity, @Nullable LivingEntity caster) {
        if (directEntity == null && caster == null) {
            return level.damageSources().lightningBolt();
        }
        Holder<DamageType> type = level.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(DamageTypes.LIGHTNING_BOLT);
        return new DamageSource(type, directEntity != null ? directEntity : caster, caster);
    }

    /**
     * Awards melee combat mastery with a per-entity cooldown and respawn/tick-reset safety.
     */
    public static boolean awardMeleeCombatMastery(INinjaData ninjaData, CompoundTag persistentData, String tickKey,
                                                  Identifier abilityId, int currentTick, int cooldownTicks, int gain) {
        if (ninjaData == null || persistentData == null || tickKey == null || abilityId == null) {
            return false;
        }
        int lastTick = persistentData.getIntOr(tickKey, -cooldownTicks);
        if (currentTick - lastTick >= cooldownTicks || currentTick < lastTick) {
            persistentData.putInt(tickKey, currentTick);
            ninjaData.addMastery(abilityId, gain);
            return true;
        }
        return false;
    }
}
