package com.sekwah.narutomod.mixin.common;

import com.sekwah.narutomod.abilities.utility.WaterWalkAbility;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityWaterWalkMixin {

    @Inject(method = "canStandOnFluid", at = @At("HEAD"), cancellable = true)
    private void narutomod$canStandOnWater(FluidState fluid, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player
                && (fluid.is(Fluids.WATER) || fluid.is(Fluids.FLOWING_WATER))
                && WaterWalkAbility.hasActiveWaterWalk(player)) {
            cir.setReturnValue(true);
        }
    }
}
