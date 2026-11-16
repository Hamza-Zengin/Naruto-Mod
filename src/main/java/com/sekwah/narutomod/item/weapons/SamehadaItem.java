package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.function.Consumer;

/**
 * Samehada (Shark Skin Greatsword)
 * Sentient Seven Ninja Swordsmen blade that shreds and devours enemy chakra on every strike,
 * restoring the wielder's Chakra (+8) and Health (+2).
 */
public class SamehadaItem extends Item {

    public SamehadaItem(Properties properties) {
        super(properties.sword(ToolMaterial.NETHERITE, 7, -2.8F).rarity(Rarity.EPIC));
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker instanceof Player player) {
            var attackerData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (target instanceof Player targetPlayer) {
                var targetData = targetPlayer.getData(NinjaCapabilityHandler.NINJA_DATA);
                float targetChakra = targetData.getChakra();
                if (targetChakra > 0) {
                    float drain = Math.min(targetChakra, 20.0f);
                    targetData.useChakra(drain, 30);
                    attackerData.addChakra(drain);
                } else {
                    player.heal(3.0f);
                }
            } else {
                attackerData.addChakra(8.0f);
                player.heal(2.0f);
            }
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + 1.0, target.getZ(),
                        10, 0.3, 0.4, 0.3, 0.08, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_BITE, SoundSource.PLAYERS, 0.9f, 0.8f);
            }
            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ClientCameraShakePacket(0.20f, 3));
                Vec3 hitPos = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new ClientJutsuEffectPacket(
                        Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                        hitPos,
                        player.getLookAngle(),
                        1.0f,
                        0x2299FF
                ));
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ninjaData.getChakra() >= 12.0f) {
                ninjaData.useChakra(12.0f, 25);
                player.getCooldowns().addCooldown(stack, 200);
                player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
                player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 100, 0));
                level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.2f, 0.7f);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.samehada.desc").withStyle(ChatFormatting.BLUE));
        tooltip.accept(Component.translatable("item.narutomod.samehada.ability").withStyle(ChatFormatting.GRAY));
    }
}
