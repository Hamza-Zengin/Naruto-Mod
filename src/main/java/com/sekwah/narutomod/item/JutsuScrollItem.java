package com.sekwah.narutomod.item;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Jutsu Scroll (Jutsu Emakimono)
 * Ancient parchment inscribed with secret ninja arts.
 * Right clicking deciphers and unlocks the contained jutsu permanently!
 * Supports individual fixed jutsu scrolls and strict clan heritage restrictions!
 */
public class JutsuScrollItem extends Item {

    private final Identifier fixedJutsuId;
    private final String requiredClan;

    public JutsuScrollItem(Properties properties, @Nullable Identifier fixedJutsuId, @Nullable String requiredClan) {
        super(properties);
        this.fixedJutsuId = fixedJutsuId;
        this.requiredClan = requiredClan;
    }

    public JutsuScrollItem(Properties properties, @Nullable Identifier fixedJutsuId) {
        this(properties, fixedJutsuId, null);
    }

    public JutsuScrollItem(Properties properties) {
        this(properties, null, null);
    }

    public static boolean isClanRestrictedJutsu(@Nullable Identifier id) {
        return com.sekwah.narutomod.capabilities.NinjaData.isClanRestrictedJutsu(id);
    }

    @Nullable
    public Identifier getTargetJutsu(ItemStack stack) {
        if (this.fixedJutsuId != null) {
            return this.fixedJutsuId;
        }
        return stack.get(NarutoDataComponents.JUTSU_SCROLL_ABILITY.get());
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            Identifier targetJutsu = getTargetJutsu(stack);

            if (targetJutsu == null) {
                List<Identifier> candidates = new ArrayList<>();
                candidates.add(NarutoAbilities.RASENGAN.getId());
                candidates.add(NarutoAbilities.CHIDORI.getId());
                candidates.add(NarutoAbilities.SHADOW_CLONE.getId());
                candidates.add(NarutoAbilities.KIRIN.getId());

                for (Identifier c : candidates) {
                    if (!ninjaData.isAbilityUnlocked(c)) {
                        targetJutsu = c;
                        break;
                    }
                }
                if (targetJutsu == null) {
                    targetJutsu = NarutoAbilities.RASENGAN.getId();
                }
            }

            Ability ability = NarutoAbilities.getAbility(targetJutsu);
            if (ability == null) {
                player.sendSystemMessage(Component.translatable("jutsu.scroll.invalid").withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            }

            // Clan-restricted jutsus can only be awakened via clan progression, never via scrolls
            if (isClanRestrictedJutsu(targetJutsu)) {
                player.sendSystemMessage(Component.translatable("jutsu.scroll.invalid").withStyle(ChatFormatting.RED));
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                return InteractionResult.PASS;
            }

            // Strict Clan Heritage check: Non-clan members suffer severe backlash!
            if (this.requiredClan != null) {
                if (ninjaData.getClan() == null || !ninjaData.getClan().getPath().equalsIgnoreCase(this.requiredClan)) {
                    String clanName = this.requiredClan.substring(0, 1).toUpperCase() + this.requiredClan.substring(1);
                    player.sendSystemMessage(Component.translatable("jutsu.scroll.fail_clan",
                            Component.literal(clanName).withStyle(ChatFormatting.RED)
                    ).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));

                    level.playSound(null, player.blockPosition(), SoundEvents.WITHER_SHOOT, SoundSource.PLAYERS, 1.0f, 0.5f);
                    player.hurtServer(serverLevel, level.damageSources().magic(), 6.0f);
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 120, 0));
                    return InteractionResult.PASS;
                }
            }

            // Strict Ninja Rank check: Must meet or exceed ability's required rank!
            NinjaRank playerRank = NinjaRank.fromString(ninjaData.getRank());
            if (playerRank.getLevel() < ability.getRequiredRank().getLevel()) {
                player.sendSystemMessage(Component.translatable("jutsu.scroll.fail_rank",
                        Component.translatable(ability.getRequiredRank().getTranslationKey())
                                .withStyle(ChatFormatting.GOLD)
                ).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                return InteractionResult.PASS;
            }

            // Strict Prerequisite & Mastery check:
            if (!ability.meetsPrerequisites(ninjaData)) {
                List<Identifier> prereqs = ability.getPrerequisites();
                StringBuilder prereqNames = new StringBuilder();
                for (int i = 0; i < prereqs.size(); i++) {
                    Identifier p = prereqs.get(i);
                    Ability pa = NarutoAbilities.getAbility(p);
                    String paName = pa != null ? Component.translatable(pa.getTranslationKey()).getString() : p.getPath();
                    if (i > 0) prereqNames.append(", ");
                    prereqNames.append(paName);
                }
                player.sendSystemMessage(Component.translatable("jutsu.scroll.fail_prerequisite",
                        Component.literal(prereqNames.toString()).withStyle(ChatFormatting.YELLOW),
                        Component.literal(String.valueOf(ability.getRequiredMastery())).withStyle(ChatFormatting.GOLD)
                ).withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                level.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 1.0f, 1.0f);
                return InteractionResult.PASS;
            }

            if (ninjaData.isAbilityUnlocked(targetJutsu)) {
                player.sendSystemMessage(Component.translatable("jutsu.scroll.already_known",
                        Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            }

            // Learn and unlock jutsu permanently!
            ninjaData.unlockAbility(targetJutsu);

            player.sendSystemMessage(Component.translatable("jutsu.scroll.unlocked",
                    Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

            level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 1.0f);

            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        Identifier jutsuId = getTargetJutsu(stack);
        if (jutsuId != null) {
            Ability ability = NarutoAbilities.getAbility(jutsuId);
            if (ability != null) {
                tooltip.accept(Component.translatable("jutsu.scroll.contains",
                        Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
                tooltip.accept(Component.translatable("jutsu.scroll.requires_rank",
                        Component.translatable(ability.getRequiredRank().getTranslationKey()).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.DARK_GRAY));
                if (!ability.getPrerequisites().isEmpty()) {
                    List<Identifier> prereqs = ability.getPrerequisites();
                    StringBuilder prereqNames = new StringBuilder();
                    for (int i = 0; i < prereqs.size(); i++) {
                        Identifier p = prereqs.get(i);
                        Ability pa = NarutoAbilities.getAbility(p);
                        String paName = pa != null ? Component.translatable(pa.getTranslationKey()).getString() : p.getPath();
                        if (i > 0) prereqNames.append(", ");
                        prereqNames.append(paName);
                    }
                    tooltip.accept(Component.translatable("jutsu.scroll.requires_prerequisite",
                            Component.literal(prereqNames.toString()).withStyle(ChatFormatting.YELLOW),
                            Component.literal(String.valueOf(ability.getRequiredMastery())).withStyle(ChatFormatting.GOLD)
                    ).withStyle(ChatFormatting.GOLD));
                }
            }
        } else {
            tooltip.accept(Component.translatable("jutsu.scroll.desc").withStyle(ChatFormatting.GRAY));
        }

        if (this.requiredClan != null) {
            String clanName = this.requiredClan.substring(0, 1).toUpperCase() + this.requiredClan.substring(1);
            tooltip.accept(Component.translatable("jutsu.scroll.requires_clan",
                    Component.literal(clanName).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)).withStyle(ChatFormatting.DARK_RED));
        }

        tooltip.accept(Component.translatable("jutsu.scroll.instruction").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
