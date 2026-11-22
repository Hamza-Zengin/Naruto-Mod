package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Iryō: Shōsen Jutsu (Mystical Palm Technique)
 * Channeled medical ninjutsu that continuously heals the caster or a targeted ally
 * while cleansing Poison, Wither, and Slowness effects.
 */
public class MysticalPalmAbility extends Ability implements Ability.Channeled, Ability.Cooldown {

    private static final double HEAL_RANGE = 4.5D;
    private static final int PULSE_INTERVAL_TICKS = 10;

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.NONE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.CHUNIN;
    }

    @Override
    public long defaultCombo() {
        return 332L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 18.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 80;
    }

    @Override
    public float getDefaultDamage() {
        return 3.0f;
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (chargeAmount % PULSE_INTERVAL_TICKS != 0) {
            return true;
        }
        float finalCost = getFinalChakraCost(player, ninjaData);
        float pulseCost = finalCost > 0.0f ? Math.max(1.0f, finalCost * 0.5f) : 0.0f;
        return tryConsume(player, ninjaData, pulseCost, 0.0f, 30);
    }

    public static double getScaledHealRange(int mastery) {
        return HEAL_RANGE * (1.0D + 0.60D * Ability.getMasteryProgress(mastery));
    }

    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
        if (ticksChanneled % PULSE_INTERVAL_TICKS != 0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        LivingEntity target = findHealTarget(player, serverLevel, getScaledHealRange(mastery));
        float healAmount = this.getDamage(ninjaData);
        if (target.getHealth() < target.getMaxHealth()) {
            target.heal(healAmount);
        }
        target.removeEffect(MobEffects.POISON);
        target.removeEffect(MobEffects.WITHER);
        target.removeEffect(MobEffects.SLOWNESS);

        Vec3 fxPos = target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D);
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new ClientJutsuEffectPacket(
                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mystical_palm"),
                fxPos,
                new Vec3(0.0D, 1.0D, 0.0D),
                1.2f,
                0x22EE88));

        serverLevel.playSound(null, target.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.35f, 1.55f);

        if (ticksChanneled > 0 && ticksChanneled % 20 == 0) {
            Identifier reg = this.getRegistryName();
            if (reg != null) {
                ninjaData.addMastery(reg, 1);
            }
        }
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        this.registerCooldown(ninjaData, this.getTranslationKey());
    }

    private static LivingEntity findHealTarget(Player caster, ServerLevel level, double healRange) {
        Vec3 eye = caster.getEyePosition();
        Vec3 look = caster.getLookAngle().normalize();
        Vec3 end = eye.add(look.scale(healRange));
        AABB searchBox = new AABB(eye, end).inflate(1.25D);

        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e.isAlive() && e != caster && isHealableAlly(caster, e));

        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 toTarget = candidate.getEyePosition().subtract(eye);
            double dist = toTarget.length();
            if (dist > healRange || dist < 1.0E-4D) {
                continue;
            }
            double dot = look.dot(toTarget.scale(1.0D / dist));
            if (dot < 0.72D) {
                continue;
            }
            double score = dist * (1.5D - dot);
            if (score < bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best != null ? best : caster;
    }

    private static boolean isHealableAlly(Player caster, LivingEntity entity) {
        if (ShadowCloneEntity.areAllies(caster, entity)) {
            return true;
        }
        if (entity instanceof Player other) {
            return caster.getTeam() == null || caster.isAlliedTo(other);
        }
        if (entity instanceof TamableAnimal pet) {
            return pet.isOwnedBy(caster);
        }
        return false;
    }
}
