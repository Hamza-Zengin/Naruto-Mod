package com.sekwah.narutomod.item;

import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import com.sekwah.narutomod.entity.projectile.KunaiEntity;
import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import com.sekwah.narutomod.entity.projectile.SmokeBombEntity;
import com.sekwah.narutomod.item.SealedAmmoContents.AmmoType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Fūinjutsu: Sealed Ammo Scroll (Buki Fūin no Maki)
 * Stores shinobi throwables (Kunai, Shuriken, Senbon, Explosive Kunai, Smoke Bombs) inside dimensional seals:
 * - Crouch + Right-Click: Seals throwables from inventory into the scroll (or cycles active ammo if none in inventory).
 * - Right-Click: Rapidly summons and throws the active sealed weapon.
 * - Passive: Automatically refills low hotbar throwable stacks from sealed reserves every second.
 */
public class SealedAmmoScrollItem extends Item {

    public static final int HOTBAR_REFILL_THRESHOLD = 2;
    public static final int HOTBAR_REFILL_TARGET = 16;

    public SealedAmmoScrollItem(Properties properties) {
        super(properties.stacksTo(1).rarity(Rarity.UNCOMMON));
    }

    public static SealedAmmoContents getContents(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return SealedAmmoContents.EMPTY;
        }
        SealedAmmoContents contents = stack.get(NarutoDataComponents.SEALED_AMMO_CONTENTS.get());
        return contents != null ? contents : SealedAmmoContents.EMPTY;
    }

    public static void setContents(ItemStack stack, SealedAmmoContents contents) {
        if (stack != null && !stack.isEmpty()) {
            stack.set(NarutoDataComponents.SEALED_AMMO_CONTENTS.get(), contents);
        }
    }

    @Nullable
    public static AmmoType matchAmmoType(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        Item item = stack.getItem();
        if (item == NarutoItems.KUNAI.get()) return AmmoType.KUNAI;
        if (item == NarutoItems.SHURIKEN.get()) return AmmoType.SHURIKEN;
        if (item == NarutoItems.SENBON.get()) return AmmoType.SENBON;
        if (item == NarutoItems.EXPLOSIVE_KUNAI.get()) return AmmoType.EXPLOSIVE_KUNAI;
        if (item == NarutoItems.SMOKE_BOMB.get()) return AmmoType.SMOKE_BOMB;
        return null;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getContents(stack).getTotalCount() > 0 || super.isFoil(stack);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (!(entity instanceof Player player) || level.getGameTime() % 20 != 0) {
            return;
        }
        SealedAmmoContents contents = getContents(stack);
        if (contents.getTotalCount() <= 0) {
            return;
        }

        Inventory inv = player.getInventory();
        boolean changed = false;
        for (int i = 0; i < 9; i++) {
            ItemStack hotbarStack = inv.getItem(i);
            AmmoType type = matchAmmoType(hotbarStack);
            if (type == null) {
                continue;
            }
            int available = contents.getCount(type);
            int maxStack = hotbarStack.getMaxStackSize();
            int desiredTarget = Math.min(HOTBAR_REFILL_TARGET, maxStack);
            if (available > 0 && hotbarStack.getCount() <= HOTBAR_REFILL_THRESHOLD && hotbarStack.getCount() < desiredTarget) {
                int transfer = Math.min(available, desiredTarget - hotbarStack.getCount());
                if (transfer > 0) {
                    hotbarStack.grow(transfer);
                    contents = contents.withCount(type, available - transfer);
                    changed = true;
                }
            }
        }
        if (changed) {
            setContents(stack, contents);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide() && level instanceof ServerLevel serverLevel) {
            SealedAmmoContents contents = getContents(stack);

            if (player.isCrouching()) {
                Inventory inv = player.getInventory();
                int sealedTotal = 0;
                AmmoType firstSealedType = null;

                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack invStack = inv.getItem(i);
                    AmmoType type = matchAmmoType(invStack);
                    if (type == null) {
                        continue;
                    }
                    int currentStored = contents.getCount(type);
                    int room = SealedAmmoContents.MAX_PER_TYPE - currentStored;
                    if (room <= 0) {
                        continue;
                    }
                    int take = Math.min(room, invStack.getCount());
                    if (take > 0) {
                        invStack.shrink(take);
                        contents = contents.withCount(type, currentStored + take);
                        sealedTotal += take;
                        if (firstSealedType == null) {
                            firstSealedType = type;
                        }
                    }
                }

                if (sealedTotal > 0) {
                    if (contents.getCount(contents.getSelectedType()) == 0 && firstSealedType != null) {
                        contents = contents.withSelected(firstSealedType);
                    }
                    setContents(stack, contents);
                    serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.9F, 1.35F);
                    serverLevel.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getY() + 1.1D, player.getZ(),
                            14, 0.35D, 0.35D, 0.35D, 0.08D,
                            ClientboundLevelParticlesPacket.RandomizationType.DEFAULT);
                    player.sendOverlayMessage(Component.translatable("item.narutomod.sealed_ammo_scroll.sealed", sealedTotal)
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    contents = contents.cycleSelected();
                    setContents(stack, contents);
                    AmmoType selected = contents.getSelectedType();
                    serverLevel.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.6F, 1.3F);
                    player.sendOverlayMessage(Component.translatable("item.narutomod.sealed_ammo_scroll.selected",
                                    Component.translatable(selected.getTranslationKey()).withStyle(ChatFormatting.YELLOW),
                                    contents.getCount(selected))
                            .withStyle(ChatFormatting.AQUA));
                }
                return InteractionResult.SUCCESS_SERVER;
            }

            if (contents.getTotalCount() <= 0) {
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.VILLAGER_NO, SoundSource.PLAYERS, 0.8F, 1.0F);
                player.sendOverlayMessage(Component.translatable("item.narutomod.sealed_ammo_scroll.empty")
                        .withStyle(ChatFormatting.RED));
                return InteractionResult.PASS;
            }

            AmmoType activeType = contents.getSelectedType();
            if (contents.getCount(activeType) <= 0) {
                for (AmmoType candidate : AmmoType.values()) {
                    if (contents.getCount(candidate) > 0) {
                        activeType = candidate;
                        contents = contents.withSelected(activeType);
                        break;
                    }
                }
            }

            spawnSealedProjectile(serverLevel, player, activeType);

            if (!player.getAbilities().instabuild) {
                contents = contents.withCount(activeType, contents.getCount(activeType) - 1);
            }
            setContents(stack, contents);
            player.getCooldowns().addCooldown(stack, 4);
            return InteractionResult.SUCCESS_SERVER;
        }

        return InteractionResult.SUCCESS;
    }

    private static void spawnSealedProjectile(ServerLevel level, Player player, AmmoType type) {
        switch (type) {
            case KUNAI -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
                KunaiEntity entity = new KunaiEntity(level, player);
                entity.pickup = player.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                entity.setBaseDamage(2.0D);
                level.addFreshEntity(entity);
            }
            case SHURIKEN -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
                ShurikenEntity entity = new ShurikenEntity(level, player);
                entity.pickup = player.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                entity.setBaseDamage(2.5D);
                level.addFreshEntity(entity);
            }
            case SENBON -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
                SenbonEntity entity = new SenbonEntity(level, player);
                entity.pickup = player.getAbilities().instabuild ? AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                entity.setBaseDamage(0.5D);
                level.addFreshEntity(entity);
            }
            case EXPLOSIVE_KUNAI -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                        1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
                ExplosiveKunaiEntity entity = new ExplosiveKunaiEntity(level, player);
                entity.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 3.0F, 1.0F);
                entity.setBaseDamage(2.0D);
                level.addFreshEntity(entity);
            }
            case SMOKE_BOMB -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL,
                        0.5F, 0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
                SmokeBombEntity bomb = new SmokeBombEntity(level, player, new ItemStack(NarutoItems.SMOKE_BOMB.get()));
                bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, 1.4F, 1.0F);
                level.addFreshEntity(bomb);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay tooltipDisplay, Consumer<Component> tooltip, TooltipFlag flag) {
        SealedAmmoContents contents = getContents(stack);
        AmmoType active = contents.getSelectedType();
        tooltip.accept(Component.translatable("item.narutomod.sealed_ammo_scroll.desc").withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("item.narutomod.sealed_ammo_scroll.selected",
                        Component.translatable(active.getTranslationKey()).withStyle(ChatFormatting.YELLOW),
                        contents.getCount(active))
                .withStyle(ChatFormatting.AQUA));
        for (AmmoType type : AmmoType.values()) {
            int count = contents.getCount(type);
            if (count > 0) {
                tooltip.accept(Component.literal(" - ")
                        .append(Component.translatable(type.getTranslationKey()))
                        .append(Component.literal(": " + count + "/" + SealedAmmoContents.MAX_PER_TYPE))
                        .withStyle(type == active ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
            }
        }
        tooltip.accept(Component.translatable("item.narutomod.sealed_ammo_scroll.instruction").withStyle(ChatFormatting.DARK_PURPLE, ChatFormatting.ITALIC));
    }
}
