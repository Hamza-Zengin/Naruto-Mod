package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.SenbonEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.level.Level;

public class SenbonItem extends Item {

    public SenbonItem(Properties properties) {
        super(properties.attributes(ItemAttributeModifiers.builder()
                .add(Attributes.ATTACK_DAMAGE,
                        new AttributeModifier(Item.BASE_ATTACK_DAMAGE_ID, 1.0D, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .add(Attributes.ATTACK_SPEED,
                        new AttributeModifier(Item.BASE_ATTACK_SPEED_ID, -1.8F, AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.MAINHAND)
                .build()));
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack usedItem = playerIn.getItemInHand(handIn);

        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (worldIn.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
        if (!worldIn.isClientSide()) {
            AbstractArrow senbonEntity = createShootingEntity(worldIn, playerIn);

            senbonEntity.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, 3.0F, 1.0F);
            senbonEntity.setBaseDamage(0.5);

            worldIn.addFreshEntity(senbonEntity);

            if (!playerIn.getAbilities().instabuild) {
                usedItem.shrink(1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }

        if (!playerIn.getAbilities().instabuild) {
            usedItem.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    public AbstractArrow createShootingEntity(Level worldIn, Player playerIn) {
        SenbonEntity entity = new SenbonEntity(worldIn, playerIn);

        entity.pickup = playerIn.getAbilities().instabuild ?
                AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;

        return entity;
    }
}
