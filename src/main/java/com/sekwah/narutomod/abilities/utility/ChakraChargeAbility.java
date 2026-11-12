package com.sekwah.narutomod.abilities.utility;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import net.minecraft.world.entity.player.Player;

/**
 * More of a slight speed boost than an actual dash
 */
public class ChakraChargeAbility extends Ability implements Ability.Channeled {

    @Override
    public ActivationType activationType() {
        return ActivationType.CHANNELED;
    }

    @Override
    public com.sekwah.narutomod.capabilities.NinjaRank getRequiredRank() {
        return com.sekwah.narutomod.capabilities.NinjaRank.ACADEMY_STUDENT;
    }

    @Override
    public long defaultCombo() {
        return 1;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        // This jutsu can always be cast
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        // Add particle effects n stuff
    }


    @Override
    public void handleChannelling(Player player, INinjaData ninjaData, int ticksChanneled) {
       boolean wasBelowMax = ninjaData.getChakra() < ninjaData.getMaxChakra();
       int mastery = ninjaData.getAbilityMastery(this);
       float rateMult = switch (getMasteryTier(mastery)) {
           case 2 -> 1.10f;
           case 3 -> 1.20f;
           case 4 -> 1.35f;
           case 5 -> 1.50f;
           default -> 1.00f;
       };
       if(player.isSprinting() || !player.onGround()) {
           ninjaData.addChakra(0.2f * rateMult);
       } else {
           ninjaData.addChakra(1f * rateMult);
           // Meditation: If crouching quietly, focus chakra pathways and train chakra control
           if (player.isCrouching()) {
               ninjaData.addChakraExp(0.10f);
               if (ticksChanneled % 10 == 0 && player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                   serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
                           player.getX(), player.getY() + 0.5, player.getZ(),
                           3, 0.3, 0.3, 0.3, 0.05);
               }
           }
       }

       // Periodic mastery gain when actively restoring chakra
       if (wasBelowMax && ticksChanneled > 0 && ticksChanneled % 60 == 0) {
           net.minecraft.resources.ResourceLocation reg = this.getRegistryName();
           if (reg != null) {
               ninjaData.addMastery(reg, 1);
           }
       }
    }

    @Override
    public boolean canActivateBelowMinCharge() {
        return false;
    }
}
