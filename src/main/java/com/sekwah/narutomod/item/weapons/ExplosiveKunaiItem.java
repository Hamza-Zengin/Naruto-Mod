package com.sekwah.narutomod.item.weapons;

import com.sekwah.narutomod.entity.projectile.ExplosiveKunaiEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ExplosiveKunaiItem extends KunaiItem {

    public ExplosiveKunaiItem(Properties properties) {
        super(properties);
    }

    @Override
    public AbstractArrow createShootingEntity(Level worldIn, Player playerIn) {
        return new ExplosiveKunaiEntity(worldIn, playerIn);
    }

    @Override
    public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (!(attacker instanceof Player player) || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        if (!attacker.level().isClientSide()) {
            ExplosiveKunaiEntity.explodeKunai(attacker);
        }
    }

    @Override
    public InteractionResult use(Level worldIn, Player playerIn, InteractionHand handIn) {
        return super.use(worldIn, playerIn, handIn);
    }
}
