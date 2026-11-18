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
 * ANBU Tantō (ANBU Black Ops Short-Blade)
 * Ultra-fast assassin blade. Deals +50% backstab/stealth damage when striking from behind
 * or while crouching, invisible, or fighting targets blinded by smoke bombs.
 * Right-click performs a quick Stamina Dash Slash.
 */
public class AnbuTantoItem extends Item {

    public static final float DASH_STAMINA_COST = 14.0F;
    public static final float BACKSTAB_MULTIPLIER = 1.50F;

    public AnbuTantoItem(Properties properties) {
        super(properties.sword(ToolMaterial.DIAMOND, 2, -1.2F).rarity(Rarity.UNCOMMON));
    }

    /**
     * Pure geometric backstab check shared by combat logic and unit tests.
     * Returns true when the attacker stands behind the target's horizontal facing cone.
     */
    public static boolean isBehindTarget(Vec3 attackerPos, Vec3 targetPos, Vec3 targetLookVec) {
        Vec3 lookHoriz = new Vec3(targetLookVec.x, 0.0D, targetLookVec.z);
        Vec3 toAttackerHoriz = new Vec3(attackerPos.x - targetPos.x, 0.0D, attackerPos.z - targetPos.z);
        if (lookHoriz.lengthSqr() < 1.0E-5D || toAttackerHoriz.lengthSqr() < 1.0E-5D) {
            return false;
        }
        return lookHoriz.normalize().dot(toAttackerHoriz.normalize()) < -0.25D;
    }

    public static boolean isStealthBackstab(LivingEntity attacker, LivingEntity target) {
        if (attacker == null || target == null) {
            return false;
        }
        if (attacker.isCrouching()
                || attacker.isInvisible()
                || attacker.hasEffect(MobEffects.INVISIBILITY)
                || target.hasEffect(MobEffects.BLINDNESS)) {
            return true;
        }
        return isBehindTarget(attacker.position(), target.position(), target.getLookAngle());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (!player.getAbilities().instabuild && ninjaData.getStamina() < DASH_STAMINA_COST) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughstamina",
                        Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.GRAY)));
                return InteractionResult.PASS;
            }
            ninjaData.useStamina(DASH_STAMINA_COST, 20);

            player.getCooldowns().addCooldown(stack, 70); // 3.5s cooldown

            Vec3 look = player.getLookAngle();
            Vec3 dashVel = new Vec3(look.x * 1.25D, 0.16D + Math.max(0.0D, look.y * 0.35D), look.z * 1.25D);
            player.setDeltaMovement(dashVel);
            player.needsSync = true;

            Vec3 slashOrigin = player.getEyePosition().add(look.scale(1.6D)).subtract(0.0D, 0.35D, 0.0D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.0F, 1.7F);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.9F, 1.4F);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, slashOrigin.x, slashOrigin.y, slashOrigin.z,
                    2, 0.35D, 0.2D, 0.35D, 0.02D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 0.5D, player.getZ(),
                    8, 0.3D, 0.3D, 0.3D, 0.05D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            AABB dashHitBox = player.getBoundingBox().move(look.scale(1.6D)).inflate(1.6D, 0.8D, 1.6D);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, dashHitBox,
                    e -> e.isAlive() && e != player);
            for (LivingEntity target : targets) {
                float dmg = isStealthBackstab(player, target) ? 5.5F * BACKSTAB_MULTIPLIER : 5.5F;
                target.hurtServer(serverLevel, player.damageSources().playerAttack(player), dmg);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel serverLevel) {
            boolean stealthHit = isStealthBackstab(attacker, target);
            if (stealthHit) {
                serverLevel.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.65D, target.getZ(),
                        14, 0.3D, 0.3D, 0.3D, 0.15D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 1.0F, 1.35F);
            }
            if (attacker instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer,
                        new ClientCameraShakePacket(stealthHit ? 0.22F : 0.10F, stealthHit ? 3 : 2));
                Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                        new ClientJutsuEffectPacket(
                                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                                hitPos,
                                attacker.getLookAngle(),
                                stealthHit ? 1.1F : 0.8F,
                                stealthHit ? 0xDC2626 : 0xCBD5E1
                        ));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.anbu_tanto.desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.narutomod.anbu_tanto.ability").withStyle(ChatFormatting.DARK_AQUA));
    }
}
