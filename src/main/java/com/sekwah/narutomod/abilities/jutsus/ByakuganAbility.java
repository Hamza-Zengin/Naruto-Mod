package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Byakugan (All Seeing White Eye)
 * Hyūga Clan Dōjutsu:
 * - 360-degree vision, grants glowing detection to all entities through walls within 30 blocks.
 * - Jūken (Gentle Fist) Passive: Melee attacks while Byakugan is active strike the opponent's Tenketsu,
 *   locking their chakra regeneration for 5 seconds (100 ticks), while inflicting weakness and slow.
 */
public class ByakuganAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    public static final int CHAKRA_LOCK_TICKS = 100; // 5 seconds (20 ticks/sec * 5)

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public boolean isDojutsu() {
        return true;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.GENIN;
    }

    @Override
    public String getRequiredClanPath() {
        return "hyuga";
    }

    public float getTickChakraCost(INinjaData ninjaData) {
        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        return getExtraParam("drain_per_tick", 0.25f) * getChakraCostMultiplier(mastery);
    }

    @Override
    public float getActivationChakraRequirement(@javax.annotation.Nullable Player player, INinjaData ninjaData) {
        return getTickChakraCost(ninjaData);
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData == null) {
            return false;
        }
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("hyuga")) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notclan",
                        Component.literal("Hyūga").withStyle(ChatFormatting.AQUA)));
            }
            return false;
        }

        float drain = getTickChakraCost(ninjaData);

        if (ninjaData.getChakra() < drain) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)));
            }
            return false;
        }

        ninjaData.useChakra(drain, 20);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Passive battle experience / training while active (preserving ticksActive > 0)
        if (ticksActive > 0 && ninjaData != null) {
            Identifier reg = this.getRegistryName();
            if (reg != null) {
                int mastery = ninjaData.getMastery(reg);
                int interval;
                int gain;
                if (mastery < 100) {
                    interval = 40;
                    gain = 3;
                } else if (mastery < 500) {
                    interval = 50;
                    gain = 2;
                } else {
                    interval = 60;
                    gain = 1;
                }
                if (ticksActive % interval == 0) {
                    ninjaData.addMastery(reg, gain);
                }
            }
        }

        // Ping all non-ally entities in mastery-scaled radius (30 - 48 blocks) with GLOWING effect every second
        if (ticksActive % 20 == 0 && player.level() instanceof ServerLevel serverLevel) {
            int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
            double range = 30.0 * getPowerMultiplier(mastery);
            AABB field = player.getBoundingBox().inflate(range);
            List<LivingEntity> entities = serverLevel.getEntitiesOfClass(LivingEntity.class, field,
                    e -> e != player && e.isAlive() && !com.sekwah.narutomod.entity.ShadowCloneEntity.areAllies(player, e));
            for (LivingEntity entity : entities) {
                entity.addEffect(new MobEffectInstance(MobEffects.GLOWING, 30, 0, false, false, false));
            }
        }
    }

    /**
     * Applies Gentle Fist (Jūken) Tenketsu chakra-blocking strike to the target.
     * Disrupts enemy chakra network, blocking chakra regen for 5 seconds (100 ticks).
     */
    public static void applyChakraLockStrike(Player attacker, LivingEntity target) {
        if (attacker == null || target == null || target.level().isClientSide()) return;

        // Visual and auditory impact of Jūken internal strike
        if (target.level() instanceof ServerLevel serverLevel) {
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.9f, 1.4f);
            serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.PLAYERS, 0.8f, 1.8f);

            // Subtle ethereal chakra injection burst at target heart/center
            serverLevel.sendParticles(ParticleTypes.ENCHANTED_HIT,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    8, 0.2, 0.2, 0.2, 0.1, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(),
                    6, 0.2, 0.2, 0.2, 0.05, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        }

        // Apply Gentle Fist tenketsu disruption: Weakness & Slowness
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, false, false));
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, false));

        // If target is a Player, seal their chakra regeneration for 5 seconds (100 ticks)
        if (target instanceof Player targetPlayer) {
            var targetData = targetPlayer.getData(NinjaCapabilityHandler.NINJA_DATA);
            targetData.lockChakraRegen(CHAKRA_LOCK_TICKS);
            targetPlayer.sendOverlayMessage(
                    Component.translatable("jutsu.byakugan.chakra_locked").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
        }

        // Grant combat mastery to the attacker
        attacker.getData(NinjaCapabilityHandler.NINJA_DATA)
                .addMastery(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "byakugan"), 1);
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
    }
}
