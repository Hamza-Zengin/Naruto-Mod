package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.JutsuCombatHelper;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.events.ElementalInteractionHandler;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
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
 * Gunbai Uchiwa (Uchiha War Fan - 軍配団扇)
 * Legendary Uchiha wind-reflecting war fan.
 * - Right-Click (Uchiwa Gaeshi): Reflects incoming projectiles at 1.6x velocity, wind-boosts friendly
 *   Fireball jutsus, and unleashes a Fūton shockwave damaging and knocking back nearby foes.
 * - Melee Hit: Heavy wind-blunt knockback and +4.0F bonus fire damage against burning targets.
 */
public class GunbaiUchiwaItem extends Item {

    public static final float UCHIWA_GAESHI_CHAKRA_COST = 14.0F;
    public static final int UCHIWA_GAESHI_COOLDOWN_TICKS = 100;
    public static final double REFLECT_RADIUS = 4.6D;
    public static final double REFLECT_SPEED_MULTIPLIER = 1.6D;
    public static final float SHOCKWAVE_DAMAGE = 8.0F;
    public static final float IGNITED_BONUS_DAMAGE = 4.0F;

    public GunbaiUchiwaItem(Properties properties) {
        super(properties.sword(ToolMaterial.NETHERITE, 6, -2.5F).rarity(Rarity.EPIC));
    }

    /**
     * Pure reflection velocity calculation shared by combat logic and unit tests.
     */
    public static Vec3 computeReflectVelocity(Vec3 playerLook, Vec3 incomingVelocity) {
        double speed = incomingVelocity != null ? incomingVelocity.length() * REFLECT_SPEED_MULTIPLIER : 1.5D;
        double clampedSpeed = Math.max(1.25D, speed);
        if (playerLook != null && playerLook.lengthSqr() > 1.0E-5D) {
            return playerLook.normalize().scale(clampedSpeed);
        }
        if (incomingVelocity != null && incomingVelocity.lengthSqr() > 1.0E-5D) {
            return incomingVelocity.scale(-REFLECT_SPEED_MULTIPLIER);
        }
        return new Vec3(0.0D, 0.0D, 1.25D);
    }

    public static float computeBonusFireDamage(boolean targetOnFire) {
        return targetOnFire ? IGNITED_BONUS_DAMAGE : 0.0F;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel serverLevel) {
            Vec3 look = attacker.getLookAngle();
            JutsuCombatHelper.applyVelocity(target, new Vec3(look.x * 0.90D, 0.30D, look.z * 0.90D), false);

            float fireBonus = computeBonusFireDamage(target.isOnFire());
            if (fireBonus > 0.0F) {
                target.setInvulnerableTime(0);
                target.hurtServer(serverLevel, serverLevel.damageSources().inFire(), fireBonus);
                serverLevel.sendParticles(ParticleTypes.FLAME,
                        target.getX(), target.getY() + target.getBbHeight() * 0.6D, target.getZ(),
                        12, 0.3D, 0.3D, 0.3D, 0.06D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            }

            serverLevel.playSound(null, target.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 0.9F, 0.75F);
            if (attacker instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ClientCameraShakePacket(0.22F, 3));
                Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target, new ClientJutsuEffectPacket(
                        Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                        hitPos,
                        look,
                        1.0F,
                        fireBonus > 0.0F ? 0xFF5511 : 0xDC2626
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
            if (!player.getAbilities().instabuild && ninjaData.getChakra() < UCHIWA_GAESHI_CHAKRA_COST) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.RED)));
                return InteractionResult.PASS;
            }
            if (!player.getAbilities().instabuild) {
                ninjaData.useChakra(UCHIWA_GAESHI_CHAKRA_COST, 30);
            }
            player.getCooldowns().addCooldown(stack, UCHIWA_GAESHI_COOLDOWN_TICKS);

            Vec3 look = player.getLookAngle().normalize();
            Vec3 frontOrigin = player.getEyePosition().add(look.scale(1.8D)).subtract(0.0D, 0.35D, 0.0D);
            AABB reflectBox = new AABB(
                    frontOrigin.x - REFLECT_RADIUS, frontOrigin.y - 2.2D, frontOrigin.z - REFLECT_RADIUS,
                    frontOrigin.x + REFLECT_RADIUS, frontOrigin.y + 2.2D, frontOrigin.z + REFLECT_RADIUS);

            List<Projectile> projectiles = serverLevel.getEntitiesOfClass(Projectile.class, reflectBox, Projectile::isAlive);
            for (Projectile proj : projectiles) {
                if (proj instanceof FireballJutsuEntity fireball && fireball.getOwner() == player) {
                    ElementalInteractionHandler.boostFireballWithWind(fireball);
                } else if (proj.getOwner() != player) {
                    proj.setOwner(player);
                    proj.setDeltaMovement(computeReflectVelocity(look, proj.getDeltaMovement()));
                    proj.needsSync = true;
                }
            }

            // Spawn 3D Wind Shockwave visual effect
            Vec3 wavePos = player.position().add(look.x * 1.4D, 0.1D, look.z * 1.4D);
            JutsuVisualEffectEntity windWave = JutsuVisualEffectEntity.create(
                    serverLevel, wavePos.x, wavePos.y, wavePos.z,
                    JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE, 22, player.getYRot());
            windWave.setCasterUUID(player.getUUID());
            windWave.setEffectDamage(SHOCKWAVE_DAMAGE);
            serverLevel.addFreshEntity(windWave);

            // Damage and repel nearby hostile targets in front
            List<LivingEntity> foes = JutsuCombatHelper.findHostileTargetsInRadius(
                    serverLevel, player, frontOrigin, REFLECT_RADIUS, 2.2D);
            for (LivingEntity foe : foes) {
                foe.hurtServer(serverLevel, player.damageSources().playerAttack(player), SHOCKWAVE_DAMAGE);
                Vec3 pushDir = foe.position().subtract(player.position());
                double horiz = Math.hypot(pushDir.x, pushDir.z);
                if (horiz > 1.0E-4D) {
                    JutsuCombatHelper.applyVelocity(foe,
                            new Vec3((pushDir.x / horiz) * 1.35D, 0.42D, (pushDir.z / horiz) * 1.35D), true);
                }
            }

            serverLevel.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(), SoundSource.PLAYERS, 1.15F, 0.85F);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.35F, 0.90F);
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    frontOrigin.x, frontOrigin.y, frontOrigin.z,
                    24, 1.1D, 0.6D, 1.1D, 0.12D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    frontOrigin.x, frontOrigin.y, frontOrigin.z,
                    3, 0.6D, 0.25D, 0.6D, 0.02D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            if (player instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer, new ClientCameraShakePacket(0.35F, 5));
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.gunbai_uchiwa.desc").withStyle(ChatFormatting.RED));
        tooltip.accept(Component.translatable("item.narutomod.gunbai_uchiwa.ability").withStyle(ChatFormatting.GRAY));
    }
}
