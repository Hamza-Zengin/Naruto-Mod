package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Chidori / Raikiri (One Thousand Birds)
 * Lightning-infused thrust that grants high velocity and piercing lightning damage.
 */
public class ChidoriAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.abilities.ChakraElement getChakraElement() {
        return com.sekwah.narutomod.abilities.ChakraElement.LIGHTNING;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 213;
    }

    @Override
    public float getDefaultChakraCost() {
        return 35.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 6 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 26.0f;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        int holdTicks = 100 + (getMasteryTier(mastery) >= 4 ? 40 : (getMasteryTier(mastery) >= 2 ? 20 : 0));
        int speedAmp = getMasteryTier(mastery) >= 4 ? 3 : 2;

        // Activate hand holding window (scaled with mastery)
        ninjaData.setActiveHandJutsu("chidori", holdTicks);

        // Give high-speed burst effect for the window
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, holdTicks, speedAmp, false, false, true));

        // Sound effect: initial Chidori activation chirp and continuous lightning surge
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 1.2f, 1.0f);
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CHIDORI_LOOP.get(), SoundSource.PLAYERS, 0.9f, 1.1f);
        player.displayClientMessage(Component.translatable("jutsu.chidori.ready").withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD), true);
    }

    public static void executeStrike(Player player, INinjaData ninjaData, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        // High velocity forward thrust with client-side prediction and ground drag avoidance
        com.sekwah.sekclib.util.PlayerUtil.setVelocity(player, look.x * 2.4, 0.22, look.z * 2.4, true);
        player.hurtMarked = true;

        Vec3 strikePos = target != null ? target.position().add(0, target.getBbHeight() * 0.5, 0)
                : player.position().add(look.scale(2.2)).add(0, player.getEyeHeight() * 0.7, 0);

        // Spawn 3D Chidori Burst Entity
        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity chidori3D =
                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                        serverLevel, strikePos.x, player.getY(), strikePos.z,
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_CHIDORI_BURST,
                        20, player.getYRot());

        List<LivingEntity> hitEnemies = new java.util.ArrayList<>();
        if (target != null && target.isAlive()) {
            hitEnemies.add(target);
            chidori3D.setTargetId(target.getId());
        } else {
            AABB box = new AABB(strikePos.x - 1.8, strikePos.y - 1.2, strikePos.z - 1.8,
                    strikePos.x + 1.8, strikePos.y + 2.0, strikePos.z + 1.8);
            hitEnemies.addAll(serverLevel.getEntitiesOfClass(LivingEntity.class, box, e -> e != player && e.isAlive()));
        }
        serverLevel.addFreshEntity(chidori3D);

        float strikeDamage = com.sekwah.narutomod.abilities.NarutoAbilities.CHIDORI.get().getDamage(ninjaData);
        for (LivingEntity enemy : hitEnemies) {
            enemy.hurt(player.damageSources().lightningBolt(), strikeDamage);
            serverLevel.sendParticles(ParticleTypes.FLASH, enemy.getX(), enemy.getY() + 1.0, enemy.getZ(), 1, 0, 0, 0, 0);
        }

        // Minimalist lightning spark discharge
        for (int i = 0; i < 15; i++) {
            double ox = (player.getRandom().nextDouble() - 0.5) * 1.2;
            double oy = player.getRandom().nextDouble() * 1.5;
            double oz = (player.getRandom().nextDouble() - 0.5) * 1.2;
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    strikePos.x + ox, strikePos.y + oy, strikePos.z + oz,
                    1, 0, 0, 0, 0.15);
        }
        serverLevel.playSound(null, player.blockPosition(), NarutoSounds.CHIDORI.get(), SoundSource.PLAYERS, 1.4f, 1.2f);

        player.removeEffect(MobEffects.MOVEMENT_SPEED);
        ninjaData.clearActiveHandJutsu();
    }
}
