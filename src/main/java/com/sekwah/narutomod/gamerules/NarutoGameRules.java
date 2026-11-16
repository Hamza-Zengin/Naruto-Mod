package com.sekwah.narutomod.gamerules;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import com.sekwah.narutomod.NarutoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Registers custom Minecraft GameRules for Naruto Mod.
 */
public final class NarutoGameRules {

    public static final DeferredRegister<GameRule<?>> GAME_RULES =
            DeferredRegister.create(Registries.GAME_RULE, NarutoMod.MOD_ID);

    /**
     * Controls whether Naruto Mod jutsus and ninja explosives can break, burn, or transform blocks.
     * Registered as {@code narutomod:naruto_jutsu_griefing} (Minecraft Identifiers require lowercase).
     */
    public static final DeferredHolder<GameRule<?>, GameRule<Boolean>> NARUTO_JUTSU_GRIEFING =
            GAME_RULES.register("naruto_jutsu_griefing", () -> new GameRule<>(
                    GameRuleCategory.MISC,
                    GameRuleType.BOOL,
                    BoolArgumentType.bool(),
                    GameRuleTypeVisitor::visitBoolean,
                    Codec.BOOL,
                    value -> value ? 1 : 0,
                    Boolean.TRUE,
                    FeatureFlagSet.of()
            ));

    private NarutoGameRules() {
    }

    public static void register(IEventBus eventBus) {
        GAME_RULES.register(eventBus);
    }
}
