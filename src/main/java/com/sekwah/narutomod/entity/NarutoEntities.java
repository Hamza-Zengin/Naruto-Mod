package com.sekwah.narutomod.entity;

import com.sekwah.narutomod.entity.item.PaperBombEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.EarthBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.VacuumSphereEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WoodPrisonEntity;
import com.sekwah.narutomod.entity.mob.AkatsukiNinjaEntity;
import com.sekwah.narutomod.entity.mob.RogueNinjaEntity;
import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import com.sekwah.narutomod.entity.projectile.FumaShurikenEntity;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import com.sekwah.narutomod.entity.projectile.SmokeBombEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.UnaryOperator;

import static com.sekwah.narutomod.NarutoMod.MOD_ID;

public class NarutoEntities {

    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<KunaiEntity>> KUNAI = register("kunai",
            KunaiEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<SenbonEntity>> SENBON = register("senbon",
            SenbonEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<ExplosiveKunaiEntity>> EXPLOSIVE_KUNAI = register("explosive_kunai",
            ExplosiveKunaiEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<ShurikenEntity>> SHURIKEN = register("shuriken",
            ShurikenEntity::new, MobCategory.MISC, builder -> builder.sized(0.5F, 0.5F).clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<FumaShurikenEntity>> FUMA_SHURIKEN = register("fuma_shuriken",
            FumaShurikenEntity::new, MobCategory.MISC, builder -> builder.sized(0.9F, 0.4F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<PaperBombEntity>> PAPER_BOMB = register("paper_bomb",
            PaperBombEntity::new, MobCategory.MISC, builder -> builder.fireImmune().sized(0.5F, 0.5F).clientTrackingRange(10));

    public static final DeferredHolder<EntityType<?>, EntityType<FireballJutsuEntity>> FIREBALL_JUTSU = register("fireball_jutsu",
            FireballJutsuEntity::new, MobCategory.MISC, builder -> builder.sized(1.5F, 1.5F).clientTrackingRange(4).updateInterval(10));

    public static final DeferredHolder<EntityType<?>, EntityType<RasenshurikenEntity>> RASENSHURIKEN = register("rasenshuriken",
            RasenshurikenEntity::new, MobCategory.MISC, builder -> builder.sized(1.6F, 0.8F).clientTrackingRange(8).updateInterval(5));

    public static final DeferredHolder<EntityType<?>, EntityType<WaterBulletEntity>> WATER_BULLET = register("water_bullet",
            WaterBulletEntity::new, MobCategory.MISC, builder -> builder.sized(1.0F, 1.0F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<EarthBulletEntity>> EARTH_BULLET = register("earth_bullet",
            EarthBulletEntity::new, MobCategory.MISC, builder -> builder.sized(0.95F, 0.95F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<VacuumSphereEntity>> VACUUM_SPHERE = register("vacuum_sphere",
            VacuumSphereEntity::new, MobCategory.MISC, builder -> builder.sized(0.65F, 0.65F).clientTrackingRange(8).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<MudWallEntity>> MUD_WALL = register("mud_wall",
            MudWallEntity::new, MobCategory.MISC, builder -> builder.fireImmune().sized(3.2F, 2.8F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<WoodPrisonEntity>> WOOD_PRISON = register("wood_prison",
            WoodPrisonEntity::new, MobCategory.MISC, builder -> builder.sized(3.0F, 3.0F).clientTrackingRange(10).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<JutsuVisualEffectEntity>> JUTSU_VISUAL_EFFECT = register("jutsu_visual_effect",
            JutsuVisualEffectEntity::new, MobCategory.MISC, builder -> builder.fireImmune().sized(1.5F, 1.5F).clientTrackingRange(12).updateInterval(2));

    public static final DeferredHolder<EntityType<?>, EntityType<SmokeBombEntity>> SMOKE_BOMB = register("smoke_bomb",
            SmokeBombEntity::new, MobCategory.MISC, builder -> builder.sized(0.25F, 0.25F).clientTrackingRange(4).updateInterval(10));

    public static final DeferredHolder<EntityType<?>, EntityType<ShadowCloneEntity>> SHADOW_CLONE = register("shadow_clone",
            ShadowCloneEntity::new, MobCategory.CREATURE, builder -> builder.sized(0.6F, 1.8F).clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<RogueNinjaEntity>> ROGUE_NINJA = register("rogue_ninja",
            RogueNinjaEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(8));

    public static final DeferredHolder<EntityType<?>, EntityType<AkatsukiNinjaEntity>> AKATSUKI_NINJA = register("akatsuki_ninja",
            AkatsukiNinjaEntity::new, MobCategory.MONSTER, builder -> builder.sized(0.6F, 1.95F).clientTrackingRange(10));

    private static <T extends Entity> DeferredHolder<EntityType<?>, EntityType<T>> register(
            String key, EntityType.EntityFactory<T> factory, MobCategory category, UnaryOperator<EntityType.Builder<T>> builder) {
        return ENTITIES.registerEntityType(key, factory, category, builder);
    }

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
        eventBus.addListener(NarutoEntities::entityAttributes);
    }

    public static void entityAttributes(EntityAttributeCreationEvent event) {
        event.put(SHADOW_CLONE.get(), ShadowCloneEntity.createAttributes().build());
        event.put(ROGUE_NINJA.get(), RogueNinjaEntity.createAttributes().build());
        event.put(AKATSUKI_NINJA.get(), AkatsukiNinjaEntity.createAttributes().build());
    }
}
