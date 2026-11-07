package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Kirin (Raiton: Kirin - 雷遁·麒麟)
 * S-Rank Lightning Release technique that summons natural lightning in the shape of a divine dragon.
 */
public class KirinAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 211; // V - C - C
    }

    @Override
    public int getCooldown() {
        return 160; // 8 seconds cooldown
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (ninjaData.getChakra() < 70) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }
        if (ninjaData.getStamina() < 30) {
            player.displayClientMessage(Component.translatable("jutsu.fail.notenoughstamina",
                    Component.translatable(this.getTranslationKey()).withStyle(ChatFormatting.YELLOW)), true);
            return false;
        }

        ninjaData.useChakra(70, 40);
        ninjaData.useStamina(30, 40);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        HitResult hit = player.pick(30.0D, 0.0F, false);
        Vec3 targetPos = hit.getLocation();
        BlockPos targetBlock = BlockPos.containing(targetPos);

        // Lightning strike
        LightningBolt lightningBolt = EntityType.LIGHTNING_BOLT.create(serverLevel);
        if (lightningBolt != null) {
            lightningBolt.moveTo(targetPos);
            lightningBolt.setVisualOnly(false);
            serverLevel.addFreshEntity(lightningBolt);
        }

        // Thunderous boom and electric particles
        serverLevel.playSound(null, targetBlock, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 5.0f, 0.8f);
        serverLevel.playSound(null, targetBlock, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 2.0f, 1.2f);

        // Huge vertical electric dragon beam
        for (double y = targetPos.y; y < targetPos.y + 25; y += 0.5) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    targetPos.x, y, targetPos.z,
                    8, 0.6, 0.2, 0.6, 0.1);
            serverLevel.sendParticles(ParticleTypes.FLASH,
                    targetPos.x, y, targetPos.z,
                    1, 0.1, 0.1, 0.1, 0.0);
        }

        // Massive electric shockwave blast
        AABB blastArea = new AABB(targetPos.x - 5.0, targetPos.y - 2.0, targetPos.z - 5.0,
                targetPos.x + 5.0, targetPos.y + 4.0, targetPos.z + 5.0);
        List<LivingEntity> victims = serverLevel.getEntitiesOfClass(LivingEntity.class, blastArea, e -> e != player && e.isAlive());

        for (LivingEntity victim : victims) {
            victim.hurt(serverLevel.damageSources().lightningBolt(), 24.0f);
            Vec3 knockback = victim.position().subtract(targetPos).normalize().scale(1.5);
            victim.push(knockback.x, 0.6, knockback.z);
            victim.setRemainingFireTicks(60);
        }
    }
}
