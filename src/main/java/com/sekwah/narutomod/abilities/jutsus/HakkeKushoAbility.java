package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Hyuga Clan Gentle Fist: Eight Trigrams Vacuum Palm (Hakke Kusho)
 * Fires a high-speed compressed chakra shockwave while Byakugan is active.
 */
public class HakkeKushoAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public long defaultCombo() {
        return 3212;
    }

    @Override
    public float getDefaultChakraCost() {
        return 22.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 6 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 11.0f;
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (ninjaData == null) return false;
        if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase("hyuga")) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notclan",
                        Component.literal("Hyuga").withStyle(ChatFormatting.AQUA)));
            }
            return false;
        }
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        float powerMult = getPowerMultiplier(mastery);
        float damage = this.getDamage(ninjaData);
        boolean byakuganOn = ninjaData != null && ninjaData.getToggleAbilityData() != null
                && ninjaData.getToggleAbilityData().hasToggleAbility(NarutoAbilities.BYAKUGAN.getId());
        if (byakuganOn) {
            damage *= 1.25f;
        }

        Vec3 eyePos = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.9f, 1.6f);

        com.sekwah.narutomod.network.PacketHandler.sendToTrackingAndSelf(
                new com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket(
                        net.minecraft.resources.Identifier.fromNamespaceAndPath("narutomod", "hakke_kusho"),
                        eyePos, look, byakuganOn ? 1.3f : 1.0f, 0xA8E6FF),
                player);
        com.sekwah.narutomod.network.PacketHandler.sendToTrackingAndSelf(
                new com.sekwah.narutomod.network.s2c.ClientCameraShakePacket(0.45f, 8),
                player);

        for (int i = 1; i <= 14; i++) {
            Vec3 pos = eyePos.add(look.scale(i));
            serverLevel.sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0.0, 0.0, 0.0, 0.0,
                    net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            if (i % 2 == 0) {
                AABB box = new AABB(pos.x - 1.3, pos.y - 1.3, pos.z - 1.3, pos.x + 1.3, pos.y + 1.3, pos.z + 1.3);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                        e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(player, e));
                for (LivingEntity target : targets) {
                    target.hurtServer(serverLevel, player.damageSources().playerAttack(player), damage);
                    target.setDeltaMovement(look.scale(2.0 * powerMult).add(0, 0.45, 0));
                    target.needsSync = true;
                }
            }
        }
    }
}
