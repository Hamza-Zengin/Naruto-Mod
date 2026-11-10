package com.sekwah.narutomod.damagesource;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class NarutoDamageSource {

    public static final ResourceKey<DamageType> JUTSU_PROJECTILE = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(NarutoMod.MOD_ID, "jutsu_projectile"));
    public static final ResourceKey<DamageType> FIREBALL = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(NarutoMod.MOD_ID, "fireball"));
    public static final ResourceKey<DamageType> AMATERASU = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(NarutoMod.MOD_ID, "amaterasu"));
    public static final ResourceKey<DamageType> RASENSHURIKEN = ResourceKey.create(Registries.DAMAGE_TYPE, new ResourceLocation(NarutoMod.MOD_ID, "rasenshuriken"));

    public static DamageSource jutsuProjectile(Entity projectile, @Nullable Entity owner) {
        return create(projectile.level(), JUTSU_PROJECTILE, projectile, owner);
    }

    public static DamageSource fireball(Entity fireball, @Nullable Entity shooter) {
        return create(fireball.level(), FIREBALL, fireball, shooter);
    }

    public static DamageSource amaterasu(Level level, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return create(level, AMATERASU, directEntity, causingEntity);
    }

    public static DamageSource rasenshuriken(Entity projectile, @Nullable Entity owner) {
        return create(projectile.level(), RASENSHURIKEN, projectile, owner);
    }

    public static DamageSource create(Level level, ResourceKey<DamageType> damageTypeKey, @Nullable Entity directEntity, @Nullable Entity causingEntity) {
        return new DamageSource(level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(damageTypeKey), directEntity, causingEntity);
    }

}
