package com.sekwah.narutomod.item;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Jutsu Scroll (Jutsu Emakimono)
 * Ancient parchment inscribed with secret ninja arts.
 * Right clicking deciphers and unlocks the contained jutsu permanently!
 */
public class JutsuScrollItem extends Item {

    public static final String JUTSU_TAG = "Jutsu";

    public JutsuScrollItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createWithJutsu(ResourceLocation jutsuId) {
        ItemStack stack = new ItemStack(NarutoItems.JUTSU_SCROLL.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(JUTSU_TAG, jutsuId.toString());
        return stack;
    }

    @Nullable
    public static ResourceLocation getJutsu(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains(JUTSU_TAG)) {
            String idStr = stack.getTag().getString(JUTSU_TAG);
            return new ResourceLocation(idStr);
        }
        return null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                ResourceLocation targetJutsu = getJutsu(stack);

                if (targetJutsu == null) {
                    // If scroll doesn't specify a jutsu, unlock a high-level jutsu that player hasn't unlocked yet!
                    List<ResourceLocation> candidates = new ArrayList<>();
                    candidates.add(NarutoAbilities.RASENGAN.getId());
                    candidates.add(NarutoAbilities.CHIDORI.getId());
                    candidates.add(NarutoAbilities.SHADOW_CLONE.getId());
                    candidates.add(NarutoAbilities.EIGHT_GATES.getId());
                    candidates.add(NarutoAbilities.KIRIN.getId());

                    for (ResourceLocation c : candidates) {
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
                    return;
                }

                if (ninjaData.isAbilityUnlocked(targetJutsu)) {
                    player.sendSystemMessage(Component.translatable("jutsu.scroll.already_known",
                            Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.RED));
                    return;
                }

                // Learn and unlock jutsu!
                ninjaData.unlockAbility(targetJutsu);

                player.sendSystemMessage(Component.translatable("jutsu.scroll.unlocked",
                        Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));

                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 1.0f, 1.0f);
                level.playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.2f, 1.0f);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        ResourceLocation jutsuId = getJutsu(stack);
        if (jutsuId != null) {
            Ability ability = NarutoAbilities.getAbility(jutsuId);
            if (ability != null) {
                tooltip.add(Component.translatable("jutsu.scroll.contains",
                        Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("jutsu.scroll.rank",
                        Component.literal(ability.getRequiredRank().getDisplayName()).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            tooltip.add(Component.translatable("jutsu.scroll.desc").withStyle(ChatFormatting.GRAY));
        }
        tooltip.add(Component.translatable("jutsu.scroll.instruction").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
