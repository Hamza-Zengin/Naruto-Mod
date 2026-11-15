package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NarutoDataComponents;
import com.sekwah.narutomod.item.NarutoItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Sword of Kusanagi (Kusanagi no Tsurugi)
 * Legendary chakra-conductive katana.
 * Right-click infuses the blade with Lightning Chakra Flow for 10 seconds (requires 15 Chakra or Chakra Metal).
 * While active, the blade glows, shocks nearby enemies, and deals +6 extra lightning damage on strike.
 */
public class KusanagiSwordItem extends Item {

    public static final int ACTIVE_DURATION_TICKS = 200; // 10 seconds

    public KusanagiSwordItem(Properties properties) {
        super(properties.sword(ToolMaterial.NETHERITE, 5, -2.0F).rarity(Rarity.EPIC));
    }

    public static boolean isChakraActive(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!Boolean.TRUE.equals(stack.get(NarutoDataComponents.KUSANAGI_CHAKRA_ACTIVE.get()))) {
            return false;
        }
        Long until = stack.get(NarutoDataComponents.KUSANAGI_ACTIVE_UNTIL.get());
        if (level != null && until != null && level.getGameTime() > until) {
            if (!level.isClientSide()) {
                stack.set(NarutoDataComponents.KUSANAGI_CHAKRA_ACTIVE.get(), false);
            }
            return false;
        }
        return true;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (Boolean.TRUE.equals(stack.get(NarutoDataComponents.KUSANAGI_CHAKRA_ACTIVE.get()))) {
            return true;
        }
        return super.isFoil(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (Boolean.TRUE.equals(stack.get(NarutoDataComponents.KUSANAGI_CHAKRA_ACTIVE.get()))) {
            Long until = stack.get(NarutoDataComponents.KUSANAGI_ACTIVE_UNTIL.get());
            if (until != null && level.getGameTime() >= until) {
                stack.set(NarutoDataComponents.KUSANAGI_CHAKRA_ACTIVE.get(), false);
            }
        }
        super.inventoryTick(stack, level, entity, slot);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
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
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.AQUA)));
                return InteractionResult.PASS;
            }

            if (hasChakra) {
                ninjaData.useChakra(15.0f, 30);
            } else {
                player.getInventory().getItem(metalSlot).shrink(1);
            }

            // Activate Chakra Flow on blade for 10 seconds via Data Components
            stack.set(NarutoDataComponents.KUSANAGI_CHAKRA_ACTIVE.get(), true);
            stack.set(NarutoDataComponents.KUSANAGI_ACTIVE_UNTIL.get(), level.getGameTime() + ACTIVE_DURATION_TICKS);

            player.getCooldowns().addCooldown(stack, 160); // 8s cooldown

            serverLevel.playSound(null, player.blockPosition(), SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 1.2f, 1.6f);
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 1.0, player.getZ(),
                    28, 1.5, 0.6, 1.5, 0.12, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

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
                target.hurtServer(serverLevel, player.damageSources().magic(), 8.0f);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                Vec3 push = target.position().subtract(player.position()).normalize().scale(0.85D);
                target.setDeltaMovement(push.x, 0.35D, push.z);
                target.needsSync = true;
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel serverLevel && isChakraActive(stack, attacker.level())) {
            target.hurtServer(serverLevel, attacker.damageSources().magic(), 6.0f);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 1));
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, target.getX(), target.getY() + 1.0, target.getZ(),
                    15, 0.4, 0.5, 0.4, 0.1, ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.playSound(null, target.blockPosition(), SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.PLAYERS, 0.8f, 1.4f);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.kusanagi_sword.desc").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("item.narutomod.kusanagi_sword.ability").withStyle(ChatFormatting.GRAY));
    }
}
