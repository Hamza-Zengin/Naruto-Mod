package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Sharingan (Mirror Wheel Eye)
 * Uchiha Clan Dōjutsu: Grants perception, night vision, reflex speed, and slows surrounding foes.
 */
public class SharinganAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
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

        if (ninjaData.getChakra() < 0.25f) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ninjaData.useChakra(0.25f, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Buff caster
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 220, 0, false, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, false));

        if (player.level() instanceof ServerLevel serverLevel) {
            // On initial activation: play crisp, mystical ocular awakening sound and subtle 8-point halo
            if (ticksActive == 1) {
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.ENCHANTMENT_TABLE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 1.7f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.8f, 1.3f);
                serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                        net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE, net.minecraft.sounds.SoundSource.PLAYERS, 0.5f, 1.8f);

                double eyeY = player.getEyeY();
                // 360-degree subtle 8-point crimson chakra halo radiating outward at eye level
                for (int i = 0; i < 8; i++) {
                    double angle = (2 * Math.PI / 8) * i;
                    double vx = Math.cos(angle) * 0.12;
                    double vz = Math.sin(angle) * 0.12;
                    serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                            player.getX() + Math.cos(angle) * 0.35,
                            eyeY,
                            player.getZ() + Math.sin(angle) * 0.35,
                            1, vx, 0.02, vz, 0.02);
                }
            }

            // Gentle crimson eye trails drifting backward from temples (halved particle frequency: 1/sec)
            if (ticksActive % 20 == 0) {
                Vec3 look = player.getLookAngle();
                Vec3 up = new Vec3(0, 1, 0);
                Vec3 right = look.cross(up).normalize().scale(0.26);
                double eyeY = player.getEyeY() - 0.05;
                Vec3 leftEyePos = player.position().add(0, eyeY - player.getY(), 0).subtract(right).subtract(look.scale(0.1));
                Vec3 rightEyePos = player.position().add(0, eyeY - player.getY(), 0).add(right).subtract(look.scale(0.1));

                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        leftEyePos.x, leftEyePos.y, leftEyePos.z,
                        1, -look.x * 0.025, 0.01, -look.z * 0.025, 0.01);
                serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                        rightEyePos.x, rightEyePos.y, rightEyePos.z,
                        1, -look.x * 0.025, 0.01, -look.z * 0.025, 0.01);
            }

            // Reflex field: Enemies within 8 blocks are perceived as slowed
            if (ticksActive % 20 == 0) {
                AABB field = player.getBoundingBox().inflate(8.0);
                List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, field, e -> e != player && e.isAlive());
                for (LivingEntity enemy : enemies) {
                    enemy.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false));
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
    }
}
