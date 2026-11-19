package com.sekwah.narutomod.item;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.network.s2c.ClientJutsuEffectPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.function.Consumer;

/**
 * Medical Shinobi Pouch (Iryō-yō Pōchi)
 * Tactical field medical kit (16 uses) that consumes 15 Chakra to rapidly cleanse
 * Poison, Wither, Slowness, Weakness, Blindness, Nausea, and Darkness while restoring health
 * and granting brief Regeneration II.
 */
public class MedicalPouchItem extends Item {

    public static final int MAX_DURABILITY = 16;
    public static final float CHAKRA_COST = 15.0F;
    public static final int COOLDOWN_TICKS = 120;
    public static final float HEAL_AMOUNT = 6.0F;
    public static final int REGEN_TICKS = 40;
    public static final int REGEN_AMPLIFIER = 1; // Regeneration II

    public static final List<Holder<MobEffect>> CURE_EFFECTS = List.of(
            MobEffects.POISON,
            MobEffects.WITHER,
            MobEffects.SLOWNESS,
            MobEffects.WEAKNESS,
            MobEffects.BLINDNESS,
            MobEffects.NAUSEA,
            MobEffects.DARKNESS
    );

    public MedicalPouchItem(Properties properties) {
        super(properties.durability(MAX_DURABILITY).rarity(Rarity.UNCOMMON));
    }

    public static List<Holder<MobEffect>> getCureEffects() {
        return CURE_EFFECTS;
    }

    public static boolean canTreatTarget(float currentHealth, float maxHealth, boolean hasHarmfulEffect) {
        return hasHarmfulEffect || currentHealth < maxHealth;
    }

    public static boolean hasAnyCurableEffect(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        for (Holder<MobEffect> effect : CURE_EFFECTS) {
            if (entity.hasEffect(effect)) {
                return true;
            }
        }
        return false;
    }

    public static int cleanseEffects(LivingEntity entity) {
        if (entity == null) {
            return 0;
        }
        int removed = 0;
        for (Holder<MobEffect> effect : CURE_EFFECTS) {
            if (entity.removeEffect(effect)) {
                removed++;
            }
        }
        return removed;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            boolean hasDebuff = hasAnyCurableEffect(player);
            if (!canTreatTarget(player.getHealth(), player.getMaxHealth(), hasDebuff)) {
                return InteractionResult.PASS;
            }

            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (!player.getAbilities().instabuild && ninjaData.getChakra() < CHAKRA_COST) {
                player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughchakra",
                        Component.translatable(this.getDescriptionId()).withStyle(ChatFormatting.GREEN)));
                return InteractionResult.PASS;
            }

            if (!player.getAbilities().instabuild) {
                ninjaData.useChakra(CHAKRA_COST, 25);
            }
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);

            cleanseEffects(player);
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(HEAL_AMOUNT);
            }
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, REGEN_AMPLIFIER));

            serverLevel.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.9F, 1.45F);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.7F, 1.55F);
            serverLevel.sendParticles(ParticleTypes.HEART,
                    player.getX(), player.getY() + 1.1D, player.getZ(),
                    6, 0.35D, 0.35D, 0.35D, 0.05D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new ClientJutsuEffectPacket(
                    Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mystical_palm"),
                    player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D),
                    new Vec3(0.0D, 1.0D, 0.0D),
                    1.2F,
                    0x22EE88
            ));

            player.sendOverlayMessage(Component.translatable("item.narutomod.medical_pouch.used").withStyle(ChatFormatting.GREEN));

            if (!player.getAbilities().instabuild) {
                EquipmentSlot slot = hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, player, slot);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.narutomod.medical_pouch.desc").withStyle(ChatFormatting.GREEN));
    }
}
