package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Sword of Kusanagi (Kusanagi no Tsurugi)
 * Legendary chakra-conductive katana. Right-click channels Chidori Current (Chidori Nagashi)
 * through the blade, shocking and knocking back nearby enemies.
 */
public class KusanagiSwordItem extends SwordItem {

    public KusanagiSwordItem(Properties properties) {
        super(Tiers.NETHERITE, 5, -2.0F, properties.rarity(Rarity.EPIC));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.getChakra() < 15.0f) {
                    player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                            Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.AQUA)), true);
                    return;
                }
                ninjaData.useChakra(15.0f, 30);
                player.getCooldowns().addCooldown(this, 160); // 8s cooldown

                serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER, SoundSource.PLAYERS, 1.2f, 1.6f);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                        28, 1.5, 0.6, 1.5, 0.12);

                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity nagashi3D =
                        com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.create(
                                serverLevel, player.getX(), player.getY(), player.getZ(),
                                com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity.TYPE_CHIDORI_BURST,
                                20, player.getYRot());
                nagashi3D.setTargetId(player.getId());
                serverLevel.addFreshEntity(nagashi3D);

                AABB area = player.getBoundingBox().inflate(4.5D, 2.0D, 4.5D);
                List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, area,
                        e -> e.isAlive() && e != player);
                for (LivingEntity target : targets) {
                    target.hurt(player.damageSources().magic(), 8.0f);
                    target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    Vec3 push = target.position().subtract(player.position()).normalize().scale(0.85D);
                    target.setDeltaMovement(push.x, 0.35D, push.z);
                    target.hurtMarked = true;
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.narutomod.kusanagi_sword.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.narutomod.kusanagi_sword.ability").withStyle(ChatFormatting.GRAY));
    }
}
