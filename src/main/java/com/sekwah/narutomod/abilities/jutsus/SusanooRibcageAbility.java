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
    public List<net.minecraft.resources.ResourceLocation> getPrerequisites() {
        return List.of(new net.minecraft.resources.ResourceLocation(com.sekwah.narutomod.NarutoMod.MOD_ID, "sharingan"));
    }

    @Override
    public int getRequiredMastery() {
        return SharinganAbility.SharinganStage.MANGEKYOU.getMinMastery(); // 150
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (ninjaData == null) return false;
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("uchiha")) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("jutsu.fail.notclan",
                        Component.literal("Uchiha").withStyle(ChatFormatting.RED)), true);
            }
            return false;
        }
        if (!SharinganAbility.hasMangekyou(ninjaData)) {
            if (player != null) {
                player.displayClientMessage(Component.translatable("jutsu.susanoo.requires_mangekyou")
                        .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD), true);
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (!canActivate(player, ninjaData)) {
            return false;
        }

        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        float costMult = getChakraCostMultiplier(mastery);

        boolean isInitial = !ninjaData.getToggleAbilityData().hasToggleAbility(this.getRegistryName());
        if (isInitial) {
            float initCost = Math.max(10.0f, 40.0f * costMult);
            return tryConsume(player, ninjaData, initCost, 0, 30);
        } else {
            // Sustained ethereal skeleton drain: scales down with mastery
            float sustainedCost = Math.max(0.4f, 1.0f * costMult);
            if (ninjaData.getChakra() < sustainedCost) {
                return false;
            }
            ninjaData.useChakra(sustainedCost, 30);
            return true;
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Godlike Defense: Resistance IV (80% damage reduction) + Fire immunity
        player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 3, false, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 40, 0, false, false, false));

        int mastery = ninjaData != null ? ninjaData.getAbilityMastery(this) : 0;
        int tier = getMasteryTier(mastery);
        if (tier >= 3 && !player.hasEffect(MobEffects.ABSORPTION)) {
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 100, tier - 3, false, false, false));
        }

        // Anti-AFK periodic mastery training
        if (ninjaData != null && player.tickCount % 200 == 0) {
            boolean isMoving = player.getDeltaMovement().horizontalDistanceSqr() > 0.001;
            boolean inCombat = player.getLastHurtByMob() != null && (player.tickCount - player.getLastHurtByMobTimestamp() < 100);
            if (isMoving || inCombat) {
                net.minecraft.resources.ResourceLocation reg = this.getRegistryName();
                if (reg != null) {
                    ninjaData.addMastery(reg, 1);
                }
            }
        }

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

            // Repulsion shockwave aura: violently push back foes and deal scaled damage
            AABB auraBox = player.getBoundingBox().inflate(3.0);
            List<LivingEntity> enemies = serverLevel.getEntitiesOfClass(LivingEntity.class, auraBox, e -> e != player && e.isAlive());
            float damage = 6.0f * getPowerMultiplier(mastery);
            for (LivingEntity enemy : enemies) {
                Vec3 push = enemy.position().subtract(player.position()).normalize().scale(1.2);
                enemy.push(push.x, 0.45, push.z);
                enemy.hurt(serverLevel.damageSources().playerAttack(player), damage);
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
