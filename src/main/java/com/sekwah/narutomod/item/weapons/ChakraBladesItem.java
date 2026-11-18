package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.item.NarutoDataComponents;
import com.sekwah.narutomod.item.NarutoItems;
import com.sekwah.narutomod.network.s2c.ClientCameraShakePacket;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Chakra Blades (Hien / Asuma's Chakra Trench Knives)
 * Fast dual trench knives forged from Chakra Conductive Metal.
 * Right-click consumes 12 Chakra (or 1 Chakra Metal) to activate Wind Chakra Flow for 10 seconds:
 * - Extends melee reach (+1.5 blocks via Attributes.ENTITY_INTERACTION_RANGE)
 * - Adds armor-piercing wind damage (+4.5 magic) and sweeping turquoise wind particles.
 */
public class ChakraBladesItem extends Item {

    public static final int ACTIVE_DURATION_TICKS = 200; // 10 seconds
    public static final double WIND_FLOW_EXTRA_REACH = 1.5D;
    public static final Identifier HIEN_REACH_MODIFIER_ID =
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "chakra_blade_hien_reach");

    public ChakraBladesItem(Properties properties) {
        super(properties.sword(ToolMaterial.DIAMOND, 3, -1.6F).rarity(Rarity.RARE));
    }

    public static boolean isChakraFlowActive(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!Boolean.TRUE.equals(stack.get(NarutoDataComponents.CHAKRA_BLADE_FLOW_ACTIVE.get()))) {
            return false;
        }
        Long until = stack.get(NarutoDataComponents.CHAKRA_BLADE_ACTIVE_UNTIL.get());
        if (level != null && until != null && level.getGameTime() >= until) {
            if (!level.isClientSide()) {
                setChakraFlowActive(stack, false, 0L);
            }
            return false;
        }
        return true;
    }

    public static void setChakraFlowActive(ItemStack stack, boolean active, long activeUntilTick) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        if (active) {
            stack.set(NarutoDataComponents.CHAKRA_BLADE_FLOW_ACTIVE.get(), true);
            stack.set(NarutoDataComponents.CHAKRA_BLADE_ACTIVE_UNTIL.get(), activeUntilTick);

            ItemAttributeModifiers base = stack.getItem().components()
                    .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
            ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
            for (ItemAttributeModifiers.Entry entry : base.modifiers()) {
                if (!entry.modifier().id().equals(HIEN_REACH_MODIFIER_ID)) {
                    builder.add(entry.attribute(), entry.modifier(), entry.slot());
                }
            }
            builder.add(
                    Attributes.ENTITY_INTERACTION_RANGE,
                    new AttributeModifier(HIEN_REACH_MODIFIER_ID, WIND_FLOW_EXTRA_REACH, AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND
            );
            stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
        } else {
            stack.set(NarutoDataComponents.CHAKRA_BLADE_FLOW_ACTIVE.get(), false);
            stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        if (Boolean.TRUE.equals(stack.get(NarutoDataComponents.CHAKRA_BLADE_FLOW_ACTIVE.get()))) {
            return true;
        }
        return super.isFoil(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (Boolean.TRUE.equals(stack.get(NarutoDataComponents.CHAKRA_BLADE_FLOW_ACTIVE.get()))) {
            Long until = stack.get(NarutoDataComponents.CHAKRA_BLADE_ACTIVE_UNTIL.get());
            if (until != null && level.getGameTime() >= until) {
                setChakraFlowActive(stack, false, 0L);
            } else if (entity instanceof LivingEntity living && slot == EquipmentSlot.MAINHAND && level.getGameTime() % 6 == 0) {
                Vec3 pos = living.getEyePosition().add(living.getLookAngle().scale(0.85D)).subtract(0.0D, 0.25D, 0.0D);
                level.sendParticles(ParticleTypes.CLOUD, pos.x, pos.y, pos.z,
                        2, 0.12D, 0.12D, 0.12D, 0.02D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
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
            boolean hasChakra = ninjaData.getChakra() >= 12.0f;
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
                ninjaData.useChakra(12.0f, 25);
            } else {
                player.getInventory().getItem(metalSlot).shrink(1);
            }

            setChakraFlowActive(stack, true, level.getGameTime() + ACTIVE_DURATION_TICKS);
            player.getCooldowns().addCooldown(stack, 140); // 7s cooldown

            Vec3 look = player.getLookAngle();
            Vec3 slashCenter = player.getEyePosition().add(look.scale(1.9D)).subtract(0.0D, 0.3D, 0.0D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PHANTOM_FLAP, SoundSource.PLAYERS, 1.1F, 1.55F);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 1.0F, 1.35F);
            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, slashCenter.x, slashCenter.y, slashCenter.z,
                    3, 0.45D, 0.25D, 0.45D, 0.04D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            serverLevel.sendParticles(ParticleTypes.CLOUD, slashCenter.x, slashCenter.y, slashCenter.z,
                    12, 0.5D, 0.3D, 0.5D, 0.08D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);

            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player,
                    new ClientJutsuEffectPacket(
                            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                            slashCenter,
                            look,
                            1.2f,
                            0x3CE6C8
                    ));

            AABB sweepBox = new AABB(slashCenter.x - 2.0D, slashCenter.y - 1.2D, slashCenter.z - 2.0D,
                    slashCenter.x + 2.0D, slashCenter.y + 1.4D, slashCenter.z + 2.0D);
            List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, sweepBox,
                    e -> e.isAlive() && e != player);
            for (LivingEntity target : targets) {
                target.hurtServer(serverLevel, player.damageSources().magic(), 5.0f);
                Vec3 push = look.scale(0.55D).add(0.0D, 0.15D, 0.0D);
                target.setDeltaMovement(push);
                target.needsSync = true;
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        super.hurtEnemy(stack, target, attacker);
        if (!attacker.level().isClientSide() && attacker.level() instanceof ServerLevel serverLevel) {
            boolean active = isChakraFlowActive(stack, attacker.level());
            if (active) {
                target.setInvulnerableTime(0);
                target.hurtServer(serverLevel, attacker.damageSources().magic(), 4.5f);
                serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK, target.getX(), target.getY() + 1.0D, target.getZ(),
                        2, 0.3D, 0.3D, 0.3D, 0.03D,
                        ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                serverLevel.playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.85F, 1.5F);
            }
            if (attacker instanceof ServerPlayer serverPlayer) {
                PacketDistributor.sendToPlayer(serverPlayer,
                        new ClientCameraShakePacket(active ? 0.18f : 0.10f, active ? 3 : 2));
                Vec3 hitPos = target.position().add(0.0D, target.getBbHeight() * 0.6D, 0.0D);
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(target,
                        new ClientJutsuEffectPacket(
                                Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "hand_seal_spark"),
                                hitPos,
                                attacker.getLookAngle(),
                                active ? 1.1f : 0.8f,
                                active ? 0x3CE6C8 : 0xA8F5E5
                        ));
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.chakra_blades.desc").withStyle(ChatFormatting.AQUA));
        tooltip.accept(Component.translatable("item.narutomod.chakra_blades.ability").withStyle(ChatFormatting.GRAY));
    }
}
