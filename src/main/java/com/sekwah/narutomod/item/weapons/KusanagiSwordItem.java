package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
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
 * Legendary chakra-conductive katana.
 * Right-click infuses the blade with Lightning Chakra Flow for 10 seconds (requires 15 Chakra or Chakra Metal).
 * While active, the blade glows, shocks nearby enemies, and deals +6 extra lightning damage on strike.
 */
public class KusanagiSwordItem extends SwordItem {

    public static final String NBT_CHAKRA_ACTIVE = "chakra_aktif";
    public static final String NBT_ACTIVE_UNTIL = "ChakraActiveUntil";
    public static final int ACTIVE_DURATION_TICKS = 200; // 10 seconds

    public KusanagiSwordItem(Properties properties) {
        super(Tiers.NETHERITE, 5, -2.0F, properties.rarity(Rarity.EPIC));
    }

    public static boolean isChakraActive(ItemStack stack, @Nullable Level level) {
        if (stack == null || !stack.hasTag()) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.getBoolean(NBT_CHAKRA_ACTIVE)) {
            return false;
        }
        if (level != null && tag.contains(NBT_ACTIVE_UNTIL)) {
            long until = tag.getLong(NBT_ACTIVE_UNTIL);
            if (level.getGameTime() > until) {
                tag.putBoolean(NBT_CHAKRA_ACTIVE, false);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getBoolean(NBT_CHAKRA_ACTIVE)) {
            return true;
        }
        return super.isFoil(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && stack.hasTag() && stack.getTag().getBoolean(NBT_CHAKRA_ACTIVE)) {
            long until = stack.getTag().getLong(NBT_ACTIVE_UNTIL);
            if (level.getGameTime() >= until) {
                stack.getTag().putBoolean(NBT_CHAKRA_ACTIVE, false);
            }
        }
        super.inventoryTick(stack, level, entity, slotId, isSelected);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(this)) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide && level instanceof ServerLevel serverLevel) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                boolean hasChakra = ninjaData.getChakra() >= 15.0f;
                int metalSlot = -1;
                if (!hasChakra) {
                    for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                        ItemStack invStack = player.getInventory().getItem(i);
                        if (!invStack.isEmpty() && invStack.is(NarutoItems.CHAKRA_METAL.get())) {
                            metalSlot = i;
                            break;
                        }
                    }
                }

                if (!hasChakra && metalSlot == -1) {
                    player.displayClientMessage(Component.translatable("jutsu.fail.notenoughchakra",
                            Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.AQUA)), true);
                    return;
                }

                if (hasChakra) {
                    ninjaData.useChakra(15.0f, 30);
                } else {
                    player.getInventory().getItem(metalSlot).shrink(1);
                }

                // Activate Chakra Flow on blade for 10 seconds
                CompoundTag tag = stack.getOrCreateTag();
                tag.putBoolean(NBT_CHAKRA_ACTIVE, true);
                tag.putLong(NBT_ACTIVE_UNTIL, level.getGameTime() + ACTIVE_DURATION_TICKS);

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
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        boolean result = super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide && isChakraActive(stack, attacker.level())) {
            target.hurt(attacker.damageSources().magic(), 6.0f);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
            if (attacker.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(),
                        15, 0.4, 0.5, 0.4, 0.1);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.4f);
            }
        }
        return result;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.narutomod.kusanagi_sword.desc").withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.translatable("item.narutomod.kusanagi_sword.ability").withStyle(ChatFormatting.GRAY));
    }
}
