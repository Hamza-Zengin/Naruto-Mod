package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.ShurikenEntity;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ShurikenItem extends Item {

    public ShurikenItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
        ItemStack usedItem = playerIn.getItemInHand(handIn);

        worldIn.playSound(null, playerIn.getX(), playerIn.getY(), playerIn.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS, 1.0F, 1.0F / (worldIn.getRandom().nextFloat() * 0.4F + 1.2F) + 0.5F);
        if (!worldIn.isClientSide()) {
            AbstractArrow shurikenEntity = createShootingEntity(worldIn, playerIn);

            shurikenEntity.shootFromRotation(playerIn, playerIn.getXRot(), playerIn.getYRot(), 0.0F, 3.0F, 1.0F);
            shurikenEntity.setBaseDamage(2.5);

            worldIn.addFreshEntity(shurikenEntity);

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
        ShurikenEntity entity = new ShurikenEntity(worldIn, playerIn);

        entity.pickup = playerIn.getAbilities().instabuild ?
                AbstractArrow.Pickup.CREATIVE_ONLY : AbstractArrow.Pickup.ALLOWED;

        return entity;
    }
}
