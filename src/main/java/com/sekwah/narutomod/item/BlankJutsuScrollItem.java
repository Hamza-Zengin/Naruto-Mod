package com.sekwah.narutomod.item;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Blank Jutsu Scroll (Kara no Jutsu Emakimono)
 * Can be inscribed by experienced shinobi (Mastery Tier 4+ / 350+ Mastery) with any non-clan-restricted jutsu they know,
 * costing 40 Chakra and 3 XP Levels. Once inscribed, behaves as a standard {@link JutsuScrollItem} to teach the jutsu!
 */
public class BlankJutsuScrollItem extends JutsuScrollItem {

    public static final int REQUIRED_MASTERY_TIER = 4;
    public static final int REQUIRED_MASTERY_POINTS = 350;
    public static final float TRANSCRIBE_CHAKRA_COST = 40.0F;
    public static final int TRANSCRIBE_XP_LEVELS = 3;

    public BlankJutsuScrollItem(Properties properties) {
        super(properties, null, null);
    }

    public static boolean isInscribed(ItemStack stack) {
        return stack != null && !stack.isEmpty() && stack.get(NarutoDataComponents.JUTSU_SCROLL_ABILITY.get()) != null;
    }

    public static boolean isTranscribableJutsu(Identifier id, @Nullable Ability ability, @Nullable INinjaData ninjaData) {
        if (id == null || ability == null || ninjaData == null) {
            return false;
        }
        if (JutsuScrollItem.isClanRestrictedJutsu(id)) {
            return false;
        }
        if (ability.defaultCombo() <= 0 || ability.getDefaultChakraCost() <= 0) {
            return false;
        }
        if (!ninjaData.isAbilityUnlocked(id)) {
            return false;
        }
        return Ability.getMasteryTier(ninjaData.getMastery(id)) >= REQUIRED_MASTERY_TIER;
    }

    public static List<Identifier> getEligibleJutsus(INinjaData ninjaData) {
        List<Identifier> result = new ArrayList<>();
        if (ninjaData == null) {
            return result;
        }
        Set<Identifier> unlocked = ninjaData.getUnlockedAbilities();
        if (unlocked == null) {
            return result;
        }
        for (Identifier id : unlocked) {
            Ability ability = NarutoAbilities.getAbility(id);
            if (isTranscribableJutsu(id, ability, ninjaData)) {
                result.add(id);
            }
        }
        result.sort(Comparator.comparing(Identifier::toString));
        return result;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return isInscribed(stack) || super.isFoil(stack);
    }

    @Override
    public Component getName(ItemStack stack) {
        Identifier inscribed = stack.get(NarutoDataComponents.JUTSU_SCROLL_ABILITY.get());
        if (inscribed != null) {
            Ability ability = NarutoAbilities.getAbility(inscribed);
            Component jutsuName = ability != null
                    ? Component.translatable(ability.getTranslationKey())
                    : Component.literal(inscribed.getPath());
            return Component.translatable("item.narutomod.blank_jutsu_scroll.inscribed_name", jutsuName);
        }
        return super.getName(stack);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Once inscribed, act identically to JutsuScrollItem to teach the sealed jutsu
        if (isInscribed(stack)) {
            return super.use(level, player, hand);
        }

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            List<Identifier> eligible = getEligibleJutsus(ninjaData);

            if (eligible.isEmpty()) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.sendOverlayMessage(Component.translatable("item.narutomod.blank_jutsu_scroll.none_ready", REQUIRED_MASTERY_POINTS)
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            }

            Identifier currentSelected = stack.get(NarutoDataComponents.BLANK_SCROLL_SELECTED_JUTSU.get());

            // Crouch + Right-Click cycles eligible Tier 4+ jutsus
            if (player.isCrouching()) {
                int idx = currentSelected != null ? eligible.indexOf(currentSelected) : -1;
                Identifier nextId = eligible.get((idx + 1) % eligible.size());
                stack.set(NarutoDataComponents.BLANK_SCROLL_SELECTED_JUTSU.get(), nextId);

                Ability nextAbility = NarutoAbilities.getAbility(nextId);
                Component jutsuName = nextAbility != null
                        ? Component.translatable(nextAbility.getTranslationKey())
                        : Component.literal(nextId.getPath());

                serverLevel.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.2F);
                player.sendOverlayMessage(Component.translatable("item.narutomod.blank_jutsu_scroll.selected",
                                jutsuName.copy().withStyle(ChatFormatting.YELLOW),
                                ninjaData.getMastery(nextId))
                        .withStyle(ChatFormatting.AQUA));
                return InteractionResult.SUCCESS_SERVER;
            }

            // Normal Right-Click transcribes selected (or first eligible) jutsu
            Identifier targetId = (currentSelected != null && eligible.contains(currentSelected))
                    ? currentSelected
                    : eligible.get(0);

            if (ninjaData.getChakra() < TRANSCRIBE_CHAKRA_COST
                    || (!player.getAbilities().instabuild && player.experienceLevel < TRANSCRIBE_XP_LEVELS)) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.sendOverlayMessage(Component.translatable("item.narutomod.blank_jutsu_scroll.fail_resources",
                                (int) TRANSCRIBE_CHAKRA_COST, TRANSCRIBE_XP_LEVELS)
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            }

            ninjaData.useChakra(TRANSCRIBE_CHAKRA_COST, 40);
            if (!player.getAbilities().instabuild) {
                player.giveExperienceLevels(-TRANSCRIBE_XP_LEVELS);
            }

            if (stack.getCount() > 1) {
                stack.shrink(1);
                ItemStack inscribedStack = new ItemStack(this, 1);
                inscribedStack.set(NarutoDataComponents.JUTSU_SCROLL_ABILITY.get(), targetId);
                if (!player.getInventory().add(inscribedStack)) {
                    player.spawnAtLocation(serverLevel, inscribedStack);
                }
            } else {
                stack.remove(NarutoDataComponents.BLANK_SCROLL_SELECTED_JUTSU.get());
                stack.set(NarutoDataComponents.JUTSU_SCROLL_ABILITY.get(), targetId);
            }

            Ability inscribedAbility = NarutoAbilities.getAbility(targetId);
            Component jutsuName = inscribedAbility != null
                    ? Component.translatable(inscribedAbility.getTranslationKey())
                    : Component.literal(targetId.getPath());

            serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0F, 1.25F);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2F, 1.0F);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.1D, player.getZ(),
                    18, 0.4D, 0.4D, 0.4D, 0.1D,
                    ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
            player.sendSystemMessage(Component.translatable("item.narutomod.blank_jutsu_scroll.inscribed",
                            jutsuName.copy().withStyle(ChatFormatting.YELLOW))
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        if (isInscribed(stack)) {
            super.appendHoverText(stack, context, tooltipDisplay, tooltip, flag);
            return;
        }
        tooltip.accept(Component.translatable("item.narutomod.blank_jutsu_scroll.desc").withStyle(ChatFormatting.GRAY));
        Identifier selected = stack.get(NarutoDataComponents.BLANK_SCROLL_SELECTED_JUTSU.get());
        if (selected != null) {
            Ability ability = NarutoAbilities.getAbility(selected);
            Component jutsuName = ability != null
                    ? Component.translatable(ability.getTranslationKey())
                    : Component.literal(selected.getPath());
            tooltip.accept(Component.translatable("item.narutomod.blank_jutsu_scroll.target",
                    jutsuName.copy().withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.AQUA));
        }
        tooltip.accept(Component.translatable("item.narutomod.blank_jutsu_scroll.instruction",
                REQUIRED_MASTERY_POINTS, (int) TRANSCRIBE_CHAKRA_COST, TRANSCRIBE_XP_LEVELS)
                .withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
