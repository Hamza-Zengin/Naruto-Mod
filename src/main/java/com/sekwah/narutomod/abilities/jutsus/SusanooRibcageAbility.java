package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Susanoo Ribcage (Susanoo: Abara)
 * Skeletal ribcage of chakra enveloping the user, conferring immense damage resistance and knockback aura.
 */
public class SusanooRibcageAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 1223; // C - V - V - B (cvvb)
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("uchiha")) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                    Component.literal("Uchiha").withStyle(ChatFormatting.RED)), true);
            return false;
        }

        boolean isInitial = !ninjaData.getToggleAbilityData().hasToggleAbility(this.getRegistryName());
        if (isInitial) {
            return tryConsume(player, ninjaData, 40.0f, 0, 30);
        } else {
            // Sustained ethereal skeleton drain: 1.0f per tick (20 chakra/second)
            if (ninjaData.getChakra() < 1.0f) {
                return false;
            }
            ninjaData.useChakra(1.0f, 30);
            return true;
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Godlike Defense: Resistance IV (80% damage reduction) + Fire immunity
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));

        // Activation sound on first tick
        if (ticksActive == 1 && player.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, SoundSource.PLAYERS, 0.85f, 0.75f);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT, SoundSource.PLAYERS, 0.9f, 0.85f);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 0.6f, 0.6f);
        }

        if (ticksActive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
            // Minimal, elegant ethereal chakra ember at base (halved particle presence)
            serverLevel.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                    player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5,
                    player.getY() + 0.15,
                    player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5,
                    1, 0, 0.02, 0, 0.01);

            // Repulsion shockwave aura: violently push back foes and deal 6.0f magic damage
            AABB auraBox = player.getBoundingBox().inflate(3.0);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != player && e.isAlive());
            for (LivingEntity enemy : enemies) {
                Vec3 push = enemy.position().subtract(player.position()).normalize().scale(1.2);
                enemy.push(push.x, 0.45, push.z);
                enemy.hurt(serverLevel.damageSources().playerAttack(player), 6.0f);
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        if (!player.level().isClientSide()) {
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.level().playSound(null, player.blockPosition(), SoundEvents.SHIELD_BREAK, SoundSource.PLAYERS, 1.2f, 0.8f);

            // Mangekyo ocular exhaustion if drained to empty
            if (ninjaData.getChakra() <= 2.0f) {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
                player.displayClientMessage(Component.translatable("jutsu.susanoo.exhaustion").withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
        }
    }
}
