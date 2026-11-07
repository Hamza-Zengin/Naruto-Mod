package com.sekwah.narutomod.item;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Military Rations Pill (Hyōrōgan - 兵糧丸)
 * Nutrient-rich shinobi pill that stimulates chakra pathways and restores physical stamina.
 */
public class MilitaryRationsPillItem extends Item {

    public MilitaryRationsPillItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide()) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                ninjaData.addChakra(50.0f);
                ninjaData.addStamina(50.0f);

                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 20 * 30, 0)); // Speed I for 30 seconds

                level.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EAT, SoundSource.PLAYERS, 1.0f, 1.2f);
                level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_BURP, SoundSource.PLAYERS, 0.8f, 1.1f);

                player.getCooldowns().addCooldown(this, 20 * 20); // 20 second cooldown between pills
                player.displayClientMessage(Component.translatable("item.narutomod.military_rations_pill.used").withStyle(ChatFormatting.GREEN), true);

                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            });
        }

        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.narutomod.military_rations_pill.desc1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.narutomod.military_rations_pill.desc2").withStyle(ChatFormatting.YELLOW));
    }
}
