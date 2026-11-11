package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Fūton: Rasenshuriken (Wind Style: Rasenshuriken)
 * S-Rank Forbidden/Master Wind Technique: Manifests a 4-bladed spinning wind-chakra shuriken
 * and hurls it forward, creating a cellular wind vortex dome on impact.
 */
public class RasenshurikenAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public com.sekwah.narutomod.abilities.ChakraElement getChakraElement() {
        return com.sekwah.narutomod.abilities.ChakraElement.WIND;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public List<ResourceLocation> getPrerequisites() {
        return List.of(new ResourceLocation(NarutoMod.MOD_ID, "rasengan"));
    }

    @Override
    public int getRequiredMastery() {
        return 100;
    }

    @Override
    public long defaultCombo() {
        return 1122; // C - C - V - V
    }

    @Override
    public float getDefaultChakraCost() {
        return 55.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 20 * 20;
    }

    @Override
    public float getDefaultDamage() {
        return 5.5f;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (checkCooldown(player, ninjaData, this.getTranslationKey())) {
            return false;
        }
        if (!tryConsume(player, ninjaData, getChakraCost(), getStaminaCost(), 60)) {
            return false;
        }
        registerCooldown(ninjaData, this.getTranslationKey());
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        Vec3 look = player.getLookAngle();
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.4f, 1.7f);
        serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX() + look.x, player.getEyeY(), player.getZ() + look.z,
                12, 0.3, 0.2, 0.3, 0.05);

        RasenshurikenEntity projectile = new RasenshurikenEntity(player, look.x, look.y, look.z);
        serverLevel.addFreshEntity(projectile);
    }
}
