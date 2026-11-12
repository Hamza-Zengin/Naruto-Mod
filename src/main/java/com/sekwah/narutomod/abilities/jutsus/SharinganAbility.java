package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sharingan (Mirror Wheel Eye)
 * Uchiha Clan Dōjutsu: Evolves through battle experience:
 * - 1 Tomoe (0 - 24 Mastery): Basic perception, Night Vision, Speed I, 6m Slowness I aura.
 * - 2 Tomoe (25 - 74 Mastery): Enhanced reflex, Speed I, 8m Slowness I + Weakness I aura.
 * - 3 Tomoe (75 - 149 Mastery): Superior prediction, Speed II, Strength I, 10m Slowness II aura.
 * - Mangekyō (150+ Mastery): Transcendent awakening, Speed II, Strength I, Resistance I, 12m Slowness II + Weakness I aura.
 */
public class SharinganAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    public enum SharinganStage {
        ONE_TOMOE("1_tomoe", 0, 999, "1 Tomoe", new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/sharingan_eyes_1.png")),
        TWO_TOMOE("2_tomoe", 1000, 2499, "2 Tomoe", new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/sharingan_eyes_2.png")),
        THREE_TOMOE("3_tomoe", 2500, 4999, "3 Tomoe", new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/sharingan_eyes_3.png")),
        MANGEKYOU("mangekyou", 5000, Integer.MAX_VALUE, "Mangekyō Sharingan", new ResourceLocation(NarutoMod.MOD_ID, "textures/entity/jutsu/sharingan_eyes_mangekyo.png"));

        private final String id;
        private final int minMastery;
        private final int maxMastery;
        private final String displayName;
        private final ResourceLocation texture;

        SharinganStage(String id, int minMastery, int maxMastery, String displayName, ResourceLocation texture) {
            this.id = id;
            this.minMastery = minMastery;
            this.maxMastery = maxMastery;
            this.displayName = displayName;
            this.texture = texture;
        }

        public String getId() {
            return id;
        }

        public int getMinMastery() {
            return minMastery;
        }

        public int getMaxMastery() {
            return maxMastery;
        }

        public String getDisplayName() {
            return displayName;
        }

        public ResourceLocation getTexture() {
            return texture;
        }

        public String getTranslationKey() {
            return "jutsu.sharingan.stage." + id;
        }
    }

    public static SharinganStage getStage(int mastery) {
        if (mastery >= SharinganStage.MANGEKYOU.getMinMastery()) {
            return SharinganStage.MANGEKYOU;
        } else if (mastery >= SharinganStage.THREE_TOMOE.getMinMastery()) {
            return SharinganStage.THREE_TOMOE;
        } else if (mastery >= SharinganStage.TWO_TOMOE.getMinMastery()) {
            return SharinganStage.TWO_TOMOE;
        } else {
            return SharinganStage.ONE_TOMOE;
        }
    }

    public static SharinganStage getStage(INinjaData ninjaData) {
        if (ninjaData == null) return SharinganStage.ONE_TOMOE;
        int mastery = ninjaData.getMastery(new ResourceLocation(NarutoMod.MOD_ID, "sharingan"));
        return getStage(mastery);
    }

    public static boolean hasMangekyou(INinjaData ninjaData) {
        return getStage(ninjaData) == SharinganStage.MANGEKYOU;
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.GENIN;
    }

    @Override
    public long defaultCombo() {
        return 111;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("uchiha")) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Uchiha").withStyle(ChatFormatting.RED)), true);
            return false;
        }

        float baseCost = 0.25f;
        if (ninjaData != null) {
            float reduction = Math.min(0.40f, (ninjaData.getMastery(this.getRegistryName()) / 1000.0f) * 0.40f);
            baseCost = Math.max(0.05f, baseCost * (1.0f - reduction));
        }

        if (ninjaData.getChakra() < baseCost) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ninjaData.useChakra(baseCost, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        SharinganStage stage = getStage(ninjaData);

        // Passive battle experience / training: grant +1 mastery every 10 seconds of sustained focus (anti-AFK check)
        if (player.tickCount % 200 == 0) {
            boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean inCombat = player.getLastHurtByMob() != null && (player.tickCount - player.getLastHurtByMobTimestamp() < 100);
            if (isMoving || inCombat) {
                ResourceLocation reg = this.getRegistryName();
                if (reg != null) {
                    ninjaData.addMastery(reg, 1);
                }
            }
        }

        // 1. Self Buffs scaling with Sharingan evolution
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));

        int speedAmp = (stage == SharinganStage.THREE_TOMOE || stage == SharinganStage.MANGEKYOU) ? 1 : 0;
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, speedAmp, false, false, false));

        if (stage == SharinganStage.THREE_TOMOE || stage == SharinganStage.MANGEKYOU) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, false));
        }

        if (stage == SharinganStage.MANGEKYOU) {
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, false));
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            // On initial activation: play crisp, mystical ocular awakening sound
            if (ticksActive == 1) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        NarutoSounds.SHARINGAN_ACTIVATE.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.2f, 1.0f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.7f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.3f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);

                if (stage == SharinganStage.MANGEKYOU) {
                    serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.ENDERMAN_SCREAM, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 0.6f);
                }

                double eyeY = player.getEyeY();
                int count = (stage == SharinganStage.MANGEKYOU) ? 4 : 2;
                for (int i = 0; i < count; i++) {
                    double angle = (2 * Math.PI / count) * i;
                    double vx = Math.cos(angle) * 0.03;
                    double vz = Math.sin(angle) * 0.03;
                    serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX() + Math.cos(angle) * 0.25,
                            eyeY,
                            player.getZ() + Math.sin(angle) * 0.25,
                            1, vx, 0.01, vz, 0.01);
                    if (stage == SharinganStage.MANGEKYOU) {
                        serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                                player.getX() + Math.cos(angle) * 0.35,
                                eyeY,
                                player.getZ() + Math.sin(angle) * 0.35,
                                1, vx * 0.5, 0.005, vz * 0.5, 0.005);
                    }
                }
            }

            // Minimalist subtle eye trail drifting backward from temples (once every 4 seconds)
            if (player.tickCount % 80 == 0) {
                Vec3 look = player.getLookAngle();
                Vec3 up = new Vec3(0, 1, 0);
                Vec3 right = look.cross(up).normalize().scale(0.24);
                double eyeY = player.getEyeY() - 0.05;
                Vec3 leftEyePos = player.position().add(0, eyeY - player.getY(), 0).subtract(right).subtract(look.scale(0.1));
                Vec3 rightEyePos = player.position().add(0, eyeY - player.getY(), 0).add(right).subtract(look.scale(0.1));

                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        leftEyePos.x, leftEyePos.y, leftEyePos.z,
                        1, -look.x * 0.01, 0.003, -look.z * 0.01, 0.003);
                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        rightEyePos.x, rightEyePos.y, rightEyePos.z,
                        1, -look.x * 0.01, 0.003, -look.z * 0.01, 0.003);

                if (stage == SharinganStage.MANGEKYOU) {
                    serverLevel.sendParticles(ParticleTypes.SQUID_INK,
                            leftEyePos.x, leftEyePos.y, leftEyePos.z,
                            1, -look.x * 0.005, 0.002, -look.z * 0.005, 0.002);
                }
            }

            // 2. Reflex perception field: Enemies within evolving radius are slowed / weakened
            if (ticksActive % 20 == 0) {
                double radius = switch (stage) {
                    case ONE_TOMOE -> 6.0;
                    case TWO_TOMOE -> 8.0;
                    case THREE_TOMOE -> 10.0;
                    case MANGEKYOU -> 12.0;
                };

                int slowAmp = (stage == SharinganStage.THREE_TOMOE || stage == SharinganStage.MANGEKYOU) ? 1 : 0;
                boolean applyWeakness = (stage == SharinganStage.TWO_TOMOE || stage == SharinganStage.MANGEKYOU);

                AABB field = player.getBoundingBox().inflate(radius);
                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, field, e -> e != player && e.isAlive());
                for (LivingEntity enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, slowAmp, false, false));
                    if (applyWeakness) {
                        enemy.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0, false, false));
                    }
                }
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        player.removeEffect(MobEffects.NIGHT_VISION);
        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        player.removeEffect(MobEffects.DAMAGE_BOOST);
        player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
    }
}
