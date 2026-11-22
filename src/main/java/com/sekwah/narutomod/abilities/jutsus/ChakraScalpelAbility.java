package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Chakura no Mesu (Chakra Scalpel - チャクラのメス)
 * Medical Ninjutsu combat form: manifests a razor-sharp teal chakra blade around the hand
 * for 10 seconds, allowing multiple precision strikes that bypass armor (magic damage),
 * sever tendons (Slowness II + Weakness), and temporarily disrupt enemy chakra regen.
 */
public class ChakraScalpelAbility extends Ability implements Ability.Cooldown {

    public static final int BASE_HOLD_TICKS = 200; // 10 seconds
    public static final int STRIKE_INTERVAL_TICKS = 8;
    public static final int CHAKRA_LOCK_TICKS = 80;
    private static final String LAST_STRIKE_TICK_KEY = "narutomod_scalpel_strike_tick";

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
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
        return 331L;
    }

    @Override
    public float getDefaultChakraCost() {
        return 26.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 180;
    }

    @Override
    public float getDefaultDamage() {
        return 9.0f;
    }

    public static int getScaledHoldDuration(int mastery) {
        return Ability.getScaledDurationTicks(BASE_HOLD_TICKS, mastery, 0.60D);
    }

    public static boolean isStrikeCooldownReady(int currentTick, int lastStrikeTick) {
        return lastStrikeTick <= 0 || currentTick < lastStrikeTick || (currentTick - lastStrikeTick) >= STRIKE_INTERVAL_TICKS;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        int holdTicks = getScaledHoldDuration(mastery);

        ninjaData.setActiveHandJutsu("chakra_scalpel", holdTicks);
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.65F);
        serverLevel.sendParticles(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                18, 0.35D, 0.4D, 0.35D, 0.12D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        player.sendOverlayMessage(Component.translatable("jutsu.chakra_scalpel.ready").withStyle(ChatFormatting.DARK_AQUA, ChatFormatting.BOLD));
    }

    public static void executeStrike(Player player, INinjaData ninjaData, LivingEntity target) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int lastStrike = player.getPersistentData().getIntOr(LAST_STRIKE_TICK_KEY, -100);
        if (!isStrikeCooldownReady(player.tickCount, lastStrike)) {
            return;
        }
        player.getPersistentData().putInt(LAST_STRIKE_TICK_KEY, player.tickCount);

        Vec3 look = player.getLookAngle();
        Vec3 strikePos = target != null
                ? target.position().add(0.0D, target.getBbHeight() * 0.55D, 0.0D)
                : player.getEyePosition().add(look.scale(1.9D));

        List<LivingEntity> hitTargets = new java.util.ArrayList<>();
        if (target != null && target.isAlive() && !ShadowCloneEntity.areAllies(player, target)) {
            hitTargets.add(target);
        } else if (target == null) {
            AABB hitBox = new AABB(strikePos.x - 1.3D, strikePos.y - 1.0D, strikePos.z - 1.3D,
                    strikePos.x + 1.3D, strikePos.y + 1.0D, strikePos.z + 1.3D);
            hitTargets.addAll(serverLevel.getEntitiesOfClass(LivingEntity.class, hitBox,
                    e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(player, e)));
        }

        float strikeDamage = NarutoAbilities.CHAKRA_SCALPEL.get().getDamage(ninjaData);
        for (LivingEntity hit : hitTargets) {
            hit.hurtServer(serverLevel, player.damageSources().indirectMagic(player, player), strikeDamage);
            hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            hit.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0));
            if (hit instanceof Player targetPlayer) {
                NinjaData targetData = targetPlayer.getData(NinjaCapabilityHandler.NINJA_DATA);
                targetData.lockChakraRegen(CHAKRA_LOCK_TICKS);
            }
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(hit, new ClientJutsuEffectPacket(
                    Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mystical_palm"),
                    hit.position().add(0.0D, hit.getBbHeight() * 0.55D, 0.0D),
                    look,
                    0.95F,
                    0x11EECC));
        }

        serverLevel.playSound(null, strikePos.x, strikePos.y, strikePos.z,
                SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.65F);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                strikePos.x, strikePos.y, strikePos.z,
                2, 0.15D, 0.15D, 0.15D, 0.02D,
                ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
        // Intentionally keeps activeHandJutsu alive until the 10-second timer expires for multi-strike surgical combat.
    }
}
