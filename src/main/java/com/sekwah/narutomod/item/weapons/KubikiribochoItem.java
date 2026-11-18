package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.ShadowCloneEntity;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Kubikiribōchō (Decapitating Carving Knife / Executioner's Blade)
 * Iconic Seven Ninja Swordsmen greatsword that absorbs iron from blood to regenerate its own blade.
 * Passive: Repairs +2 durability on every hit; on kill, repairs +15 durability and heals +4.0 HP.
 * Right-Click: Silent Killing Cleave — heavy sweeping arc that breaks shields and weakens foes.
 */
public class KubikiribochoItem extends Item {

    public static final float CLEAVE_STAMINA_COST = 18.0F;
    public static final float CLEAVE_DAMAGE = 9.0F;
    public static final double CLEAVE_RANGE = 4.2D;
    public static final int HIT_DURABILITY_REPAIR = 2;
    public static final int KILL_DURABILITY_REPAIR = 15;
    public static final float KILL_HEAL_AMOUNT = 4.0F;

    public KubikiribochoItem(Properties properties) {
        super(properties.sword(ToolMaterial.NETHERITE, 8, -2.9F).rarity(Rarity.EPIC));
    }

    /**
     * Pure durability repair helper shared by combat hooks and unit tests.
     */
    public static int computeRepairedDamageValue(int currentDamageValue, boolean isKill) {
        int restore = isKill ? KILL_DURABILITY_REPAIR : HIT_DURABILITY_REPAIR;
        return Math.max(0, currentDamageValue - restore);
    }

    public static void applyBloodRepair(ItemStack stack, boolean isKill) {
        if (stack != null && !stack.isEmpty() && stack.isDamageableItem()) {
            stack.setDamageValue(computeRepairedDamageValue(stack.getDamageValue(), isKill));
        }
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel serverLevel) {
            applyBloodRepair(stack, false);
            serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                    target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
                    6, 0.25D, 0.3D, 0.25D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            if (attacker instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ClientCameraShakePacket(0.24F, 4));
                Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new ClientJutsuEffectPacket(
                        Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                        hitPos,
                        attacker.getLookAngle(),
                        1.0F,
                        0xB91C1C
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

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (!player.getAbilities().instabuild && ninjaData.getStamina() < CLEAVE_STAMINA_COST) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughstamina",
                        Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.RED)));
                return InteractionResult.PASS;
            }
            ninjaData.useStamina(CLEAVE_STAMINA_COST, 30);
            player.getCooldowns().addCooldown(stack, 120);

            Vec3 look = player.getLookAngle();
            Vec3 flatLook = new Vec3(look.x, 0.0D, look.z);
            if (flatLook.lengthSqr() > 1.0E-5D) {
                flatLook = flatLook.normalize();
            } else {
                flatLook = new Vec3(0.0D, 0.0D, 1.0D);
            }

            Vec3 cleaveCenter = player.getEyePosition().add(look.scale(1.8D)).subtract(0.0D, 0.35D, 0.0D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.15F, 0.65F);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, cleaveCenter.x, cleaveCenter.y, cleaveCenter.z,
                    3, 0.7D, 0.25D, 0.7D, 0.02D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            AABB box = player.getBoundingBox().inflate(CLEAVE_RANGE, 1.5D, CLEAVE_RANGE);
            List<LivingEntity> candidates = serverLevel.getEntitiesOfClass(LivingEntity.class, box,
                    e -> e.isAlive() && e != player && !ShadowCloneEntity.areAllies(e, player));

            boolean hitAny = false;
            for (LivingEntity target : candidates) {
                Vec3 toTarget = new Vec3(target.getX() - player.getX(), 0.0D, target.getZ() - player.getZ());
                if (toTarget.length() > CLEAVE_RANGE) {
                    continue;
                }
                if (toTarget.lengthSqr() > 1.0E-4D && flatLook.dot(toTarget.normalize()) < 0.20D) {
                    continue;
                }
                hitAny = true;
                if (target.isBlocking()) {
                    ItemStack blockingStack = target.getUseItem();
                    target.stopUsingItem();
                    if (target instanceof Player targetPlayer && !blockingStack.isEmpty()) {
                        targetPlayer.getCooldowns().addCooldown(blockingStack, 100);
                    }
                    serverLevel.playSound(null, target.blockPosition(), SoundEvents.SHIELD_BREAK.value(), SoundSource.PLAYERS, 1.0F, 0.85F);
                }
                target.hurtServer(serverLevel, player.damageSources().playerAttack(player), CLEAVE_DAMAGE);
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0));
                applyBloodRepair(stack, false);
            }

            if (player instanceof ServerPlayer serverPlayer && hitAny) {
                PacketDistributor.sendToPlayer(serverPlayer, new ClientCameraShakePacket(0.32F, 5));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.kubikiribocho.desc").withStyle(ChatFormatting.DARK_RED));
        tooltip.accept(Component.translatable("item.narutomod.kubikiribocho.ability").withStyle(ChatFormatting.GRAY));
    }
}
