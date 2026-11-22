package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuTargetingHelper;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import com.sekwah.narutomod.sounds.NarutoSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Doton: Stone Spikes (Doryūsō - 土遁·土流槍)
 * High-level Earth Release jutsu that erupts jagged stone spears beneath a targeted area.
 */
public class StoneSpikesAbility extends Ability implements Ability.Cooldown {

    @Override
    public ActivationType activationType() {
        return ActivationType.INSTANT;
    }

    @Override
    public ChakraElement getChakraElement() {
        return ChakraElement.EARTH;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public List<Identifier> getPrerequisites() {
        return List.of(Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "mud_wall"));
    }

    @Override
    public int getRequiredMastery() {
        return 150;
    }

    @Override
    public long defaultCombo() {
        return 2312;
    }

    @Override
    public boolean requiresScroll() {
        return true;
    }

    @Override
    public float getDefaultChakraCost() {
        return 45.0f;
    }

    @Override
    public int getDefaultCooldown() {
        return 200;
    }

    @Override
    public float getDefaultDamage() {
        return 22.0f;
    }

    public static double getScaledRange(int mastery) {
        double progress = Math.min(1.0D, Math.max(0, mastery) / 700.0D);
        return 22.0D + (4.0D * progress);
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;

        int mastery = ninjaData != null ? ninjaData.getMastery(this.getRegistryName()) : 0;
        Vec3 targetPos = JutsuTargetingHelper.resolveTargetOrGroundOrigin(serverLevel, player, getScaledRange(mastery), 2.8D);
        float damage = this.getDamage(ninjaData);

        serverLevel.playSound(null, BlockPos.containing(targetPos), NarutoSounds.MUD_WALL_RISE.get(), SoundSource.PLAYERS, 1.5F, 0.85F);
        serverLevel.playSound(null, BlockPos.containing(targetPos), SoundEvents.STONE_PLACE, SoundSource.PLAYERS, 1.6F, 0.7F);

        JutsuVisualEffectEntity spikes = JutsuVisualEffectEntity.create(
                serverLevel, targetPos.x, targetPos.y, targetPos.z,
                JutsuVisualEffectEntity.TYPE_STONE_SPIKES,
                Ability.getScaledDurationTicks(50, mastery, 0.60D), player.getYRot());
        spikes.setCasterUUID(player.getUUID());
        spikes.setEffectDamage(damage);
        serverLevel.addFreshEntity(spikes);
    }
}
