package com.sekwah.narutomod.damagesource;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;

public class NarutoDamageSource {

    public static DamageSource jutsuProjectile(Entity projectile, @Nullable Entity owner) {
        return new DamageSource(projectile.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.MAGIC), projectile, owner);
    }

    public static DamageSource fireball(Entity fireball, @Nullable Entity shooter) {
        return new DamageSource(fireball.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(DamageTypes.FIREBALL), fireball, shooter);
    }

}
