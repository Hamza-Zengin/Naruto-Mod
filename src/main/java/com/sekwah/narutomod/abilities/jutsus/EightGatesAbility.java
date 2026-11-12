package com.sekwah.narutomod.abilities.jutsus;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * Eight Gates (Hachimon Tonkō - 八門遁甲: Gates 1 through 7)
 * Progressive physical release of the body's internal chakra limiters.
 * Unlocks higher gate stages based on user's mastery:
 * Gate 1 (Kaimon) -> Gate 2 (Kyūmon) -> Gate 3 (Seimon) -> Gate 4 (Shōmon)
 * -> Gate 5 (Tomon) -> Gate 6 (Keimon) -> Gate 7 (Kyōmon - Wonder).
 * 
 * Can be safely toggled off at any time. When stamina is exhausted,
 * the technique deactivates automatically with temporary muscular fatigue (no death penalty).
 */
public class EightGatesAbility extends Ability implements Ability.Toggled, Ability.HandleEnded {

    public enum GateStage {
        GATE_1("kaimon", 0, 39, "1. Kapı: Açılış Kapısı (Kaimon)", 0.05f),
        GATE_2("kyumon", 40, 119, "2. Kapı: Dinlenme Kapısı (Kyūmon)", 0.03f),
        GATE_3("seimon", 120, 249, "3. Kapı: Yaşam Kapısı (Seimon)", 0.08f),
        GATE_4("shomon", 250, 449, "4. Kapı: Acı Kapısı (Shōmon)", 0.12f),
        GATE_5("tomon", 450, 699, "5. Kapı: Kapanış Kapısı (Tomon)", 0.16f),
        GATE_6("keimon", 700, 999, "6. Kapı: Neşe Kapısı (Keimon)", 0.20f),
        GATE_7("kyumon_wonder", 1000, Integer.MAX_VALUE, "7. Kapı: Şok Kapısı (Kyōmon)", 0.25f);

        private final String id;
        private final int minMastery;
        private final int maxMastery;
        private final String displayName;
        private final float staminaCost;

        GateStage(String id, int minMastery, int maxMastery, String displayName, float staminaCost) {
            this.id = id;
            this.minMastery = minMastery;
            this.maxMastery = maxMastery;
            this.displayName = displayName;
            this.staminaCost = staminaCost;
        }

        public String getId() { return id; }
        public int getMinMastery() { return minMastery; }
        public int getMaxMastery() { return maxMastery; }
        public String getDisplayName() { return displayName; }
        public float getStaminaCost() { return staminaCost; }
        public String getTranslationKey() { return "jutsu.eight_gates.stage." + id; }
    }

    public static GateStage getStage(int mastery) {
        if (mastery >= GateStage.GATE_7.getMinMastery()) return GateStage.GATE_7;
        if (mastery >= GateStage.GATE_6.getMinMastery()) return GateStage.GATE_6;
        if (mastery >= GateStage.GATE_5.getMinMastery()) return GateStage.GATE_5;
        if (mastery >= GateStage.GATE_4.getMinMastery()) return GateStage.GATE_4;
        if (mastery >= GateStage.GATE_3.getMinMastery()) return GateStage.GATE_3;
        if (mastery >= GateStage.GATE_2.getMinMastery()) return GateStage.GATE_2;
        return GateStage.GATE_1;
    }

    public static GateStage getStage(INinjaData ninjaData) {
        if (ninjaData == null) return GateStage.GATE_1;
        int mastery = ninjaData.getMastery(new ResourceLocation(NarutoMod.MOD_ID, "eight_gates"));
        return getStage(mastery);
    }

    @Override
    public ActivationType activationType() {
        return ActivationType.TOGGLE;
    }

    @Override
    public NinjaRank getRequiredRank() {
        return NinjaRank.JONIN;
    }

    @Override
    public long defaultCombo() {
        return 2331; // V - B - B - C
    }

    @Override
    public boolean canActivate(Player player, INinjaData ninjaData) {
        if (player != null && player.isSpectator()) {
            return false;
        }
        return true;
    }

    @Override
    public boolean handleCost(Player player, INinjaData ninjaData, int chargeAmount) {
        if (player.isSpectator()) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }

        GateStage stage = getStage(ninjaData);
        float cost = stage.getStaminaCost();

        if (ninjaData.getStamina() < cost) {
            // Stamina exhausted: deactivate technique safely without death penalty
            player.displayClientMessage(Component.translatable("jutsu.eight_gates.stamina_exhausted")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
            return false;
        }

        ninjaData.useStamina(cost, 30);
        return true;
    }

    @Override
    public void performServer(Player player, INinjaData ninjaData, int ticksActive) {
        if (player.isSpectator()) {
            return;
        }

        GateStage stage = getStage(ninjaData);

        // Fast periodic mastery gain while active
        ResourceLocation reg = this.getRegistryName();
        if (reg != null) {
            int mastery = ninjaData.getMastery(reg);
            int interval;
            int gain;
            if (mastery < 250) {
                interval = 40; // Every 2 seconds
                gain = 3;
            } else if (mastery < 600) {
                interval = 50; // Every 2.5 seconds
                gain = 2;
            } else {
                interval = 60; // Every 3 seconds
                gain = 1;
            }
            if (player.tickCount % interval == 0) {
                ninjaData.addMastery(reg, gain);
            }
        }

        // Apply progressive buffs according to gate stage
        switch (stage) {
            case GATE_1 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 0, false, false, false));
            }
            case GATE_2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 0, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, false));
            }
            case GATE_3 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 0, false, false, false));
            }
            case GATE_4 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 0, false, false, false));
            }
            case GATE_5 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 3, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 1, false, false, false));
            }
            case GATE_6 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 4, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 1, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 1, false, false, false));
            }
            case GATE_7 -> {
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 40, 3, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 40, 5, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 40, 2, false, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 40, 2, false, false, false));
            }
        }

        if (player.level() instanceof ServerLevel serverLevel) {
            // Initial opening notification and audio feedback
            if (ticksActive == 1) {
                player.displayClientMessage(Component.translatable("jutsu.eight_gates.opened",
                        Component.translatable(stage.getTranslationKey())).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), true);
                serverLevel.playSound(null, player.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.7f, 1.4f);
            }

            // Stage-specific visual auras
            if (ticksActive % 20 == 0) {
                if (stage == GateStage.GATE_6) {
                    // Green vapor aura
                    serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                            player.getX(), player.getY() + 0.9, player.getZ(),
                            2, 0.25, 0.3, 0.25, 0.01);
                } else if (stage == GateStage.GATE_7) {
                    // Intense blue vapor steam
                    serverLevel.sendParticles(ParticleTypes.SPLASH,
                            player.getX(), player.getY() + 0.9, player.getZ(),
                            4, 0.3, 0.4, 0.3, 0.05);
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            player.getX(), player.getY() + 0.7, player.getZ(),
                            1, 0.2, 0.2, 0.2, 0.01);
                }
            }
        }
    }

    @Override
    public void performToggleClient(Player player, INinjaData ninjaData) {
    }

    @Override
    public void handleAbilityEnded(Player player, INinjaData ninjaData, int ticksActive) {
        if (!player.level().isClientSide()) {
            player.removeEffect(MobEffects.MOVEMENT_SPEED);
            player.removeEffect(MobEffects.DAMAGE_BOOST);
            player.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            player.removeEffect(MobEffects.JUMP);
            player.removeEffect(MobEffects.DIG_SPEED);

            // Safe muscular fatigue (no death penalty)
            if (!player.isCreative() && !player.isSpectator()) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 1)); // 10s Weakness II
                player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 1)); // 10s Slowness II
                player.displayClientMessage(Component.translatable("jutsu.eight_gates.closed")
                        .withStyle(ChatFormatting.YELLOW), true);
                player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8f, 1.0f);
            }
        }
    }

    @net.minecraftforge.fml.common.Mod.EventBusSubscriber(modid = NarutoMod.MOD_ID)
    public static class EightGatesCombatEvents {
        private static final String LAST_COMBAT_MASTERY_TICK = "narutomod_eight_gates_last_hit_tick";
        private static final int COMBAT_MASTERY_COOLDOWN_TICKS = 20;
        private static final int COMBAT_MASTERY_GAIN = 2;

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onLivingHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
            if (event.getEntity().level().isClientSide()) return;
            if (!(event.getSource().getEntity() instanceof Player attacker)) return;
            if (event.getSource().isIndirect()) return; // Melee only

            attacker.getCapability(com.sekwah.narutomod.capabilities.NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
                if (ninjaData.isEightGatesActive()) {
                    int lastTick = attacker.getPersistentData().getInt(LAST_COMBAT_MASTERY_TICK);
                    if (attacker.tickCount - lastTick >= COMBAT_MASTERY_COOLDOWN_TICKS) {
                        attacker.getPersistentData().putInt(LAST_COMBAT_MASTERY_TICK, attacker.tickCount);
                        ninjaData.addMastery(new ResourceLocation(NarutoMod.MOD_ID, "eight_gates"), COMBAT_MASTERY_GAIN);
                    }
                }
            });
        }
    }
}
