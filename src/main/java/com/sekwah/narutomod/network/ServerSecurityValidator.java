package com.sekwah.narutomod.network;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.jutsus.ByakuganAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.abilities.jutsus.SusanooRibcageAbility;
import com.sekwah.narutomod.capabilities.CooldownTickEvent;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.clans.NarutoClans;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Centralized, minimalist server-side validator for incoming C2S packets.
 * Enforces tick-based rate limiting, player state checks, ability unlock/chakra/cooldown checks,
 * and clan/rank progression security before any packet handler mutates server state.
 */
public final class ServerSecurityValidator {

    public static final int RATE_LIMIT_WINDOW_TICKS = 20;
    public static final int MAX_PACKETS_PER_WINDOW = 20;
    private static final int STALE_ENTRY_TICKS = 1200;
    private static final int MAX_TRACKED_PLAYERS = 500;

    private static final Map<UUID, RateLimitState> RATE_LIMITS = new ConcurrentHashMap<>();

    private ServerSecurityValidator() {
    }

    private static final class RateLimitState {
        long windowStartTick;
        int packetCount;

        RateLimitState(long windowStartTick, int packetCount) {
            this.windowStartTick = windowStartTick;
            this.packetCount = packetCount;
        }
    }

    /**
     * Validates both rate limiting and live/non-spectator player state for any C2S packet.
     */
    public static boolean validateBasePacket(ServerPlayer player, String packetName) {
        if (player == null) {
            return false;
        }
        if (!validateRateLimit(player, packetName)) {
            return false;
        }
        return validatePlayerState(player);
    }

    /**
     * Enforces a per-player tick-based rate limit (max 20 packets per 20-tick window).
     */
    public static boolean validateRateLimit(ServerPlayer player, String packetName) {
        if (player == null) {
            return false;
        }
        long currentTick = (player.level() instanceof ServerLevel serverLevel && serverLevel.getServer() != null)
                ? serverLevel.getServer().getTickCount()
                : player.tickCount;
        return allowPacket(player.getUUID(), currentTick, packetName);
    }

    /**
     * Pure tick-based rate limiter suitable for both live server execution and unit testing.
     */
    public static boolean allowPacket(UUID playerId, long currentTick, String packetName) {
        if (playerId == null) {
            return false;
        }
        long tick = Math.max(0L, currentTick);

        if (RATE_LIMITS.size() > MAX_TRACKED_PLAYERS) {
            RATE_LIMITS.entrySet().removeIf(entry -> (tick - entry.getValue().windowStartTick) > STALE_ENTRY_TICKS);
        }

        final boolean[] allowed = {true};
        final boolean[] shouldWarn = {false};

        RATE_LIMITS.compute(playerId, (id, state) -> {
            if (state == null || tick < state.windowStartTick || (tick - state.windowStartTick) >= RATE_LIMIT_WINDOW_TICKS) {
                return new RateLimitState(tick, 1);
            }
            state.packetCount++;
            if (state.packetCount > MAX_PACKETS_PER_WINDOW) {
                allowed[0] = false;
                if (state.packetCount == MAX_PACKETS_PER_WINDOW + 1) {
                    shouldWarn[0] = true;
                }
            }
            return state;
        });

        if (shouldWarn[0]) {
            NarutoMod.LOGGER.warn(
                    "Rate limit exceeded for player {} on packet {} (>{} packets / {} ticks). Dropping packets silently.",
                    playerId, packetName, MAX_PACKETS_PER_WINDOW, RATE_LIMIT_WINDOW_TICKS
            );
        }
        return allowed[0];
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            RATE_LIMITS.remove(playerId);
        }
    }

    public static void resetRateLimits() {
        RATE_LIMITS.clear();
    }

    /**
     * Verifies that the player exists, is alive, and is not in Spectator mode.
     */
    public static boolean validatePlayerState(ServerPlayer player) {
        return player != null
                && !player.isRemoved()
                && player.isAlive()
                && !player.isDeadOrDying()
                && !player.isSpectator();
    }

    public static boolean isAbilityUnlocked(INinjaData ninjaData, Ability ability) {
        return ninjaData != null && ability != null && ninjaData.isAbilityUnlocked(ability);
    }

    public static boolean isCooldownReady(INinjaData ninjaData, Ability ability) {
        if (ninjaData == null || ability == null) {
            return false;
        }
        if (ability instanceof Ability.Cooldown cooldownAbility && cooldownAbility.getCooldown(ninjaData) > 0) {
            CooldownTickEvent activeCooldown = ninjaData.getCooldownEvents().get(ability.getTranslationKey());
            return activeCooldown == null || activeCooldown.ticks <= 0;
        }
        return true;
    }

    public static boolean hasSufficientChakra(INinjaData ninjaData, Ability ability, boolean isCreative) {
        if (ninjaData == null || ability == null) {
            return false;
        }
        if (isCreative) {
            return true;
        }
        float baseChakraCost = ability.getChakraCost();
        if (baseChakraCost > 0f) {
            return ninjaData.getChakra() >= ability.getScaledChakraCost(ninjaData);
        }
        if (ability instanceof SusanooRibcageAbility) {
            float costMult = Ability.getChakraCostMultiplier(ninjaData.getAbilityMastery(ability));
            float initCost = Math.max(10.0f, 40.0f * costMult);
            return ninjaData.getChakra() >= initCost;
        }
        if (ability instanceof SharinganAbility || ability instanceof ByakuganAbility) {
            return ninjaData.getChakra() > 0f;
        }
        return true;
    }

    /**
     * Validates player state, unlock status, and optionally cooldown & chakra sufficiency before ability execution.
     */
    public static boolean validateAbilityActivation(ServerPlayer player, INinjaData ninjaData, Ability ability, boolean checkCostAndCooldown) {
        if (!validatePlayerState(player) || ninjaData == null || ability == null) {
            return false;
        }
        if (!isAbilityUnlocked(ninjaData, ability)) {
            NarutoMod.LOGGER.warn("Player {} attempted to use locked ability {}",
                    player.getScoreboardName(), ability.getRegistryName());
            return false;
        }
        if (!checkCostAndCooldown) {
            return true;
        }
        if (!isCooldownReady(ninjaData, ability)) {
            if (ability instanceof Ability.Cooldown cooldownAbility) {
                cooldownAbility.checkCooldown(player, ninjaData, ability.getTranslationKey());
            }
            return false;
        }
        if (!hasSufficientChakra(ninjaData, ability, player.isCreative())) {
            player.sendOverlayMessage(Component.translatable("jutsu.fail.notenoughchakra",
                    Component.translatable(ability.getTranslationKey()).withStyle(ChatFormatting.YELLOW)));
            return false;
        }
        return true;
    }

    public static boolean canSelectClan(INinjaData ninjaData, Identifier clanId, boolean clanExists) {
        if (ninjaData == null || ninjaData.isClanLocked() || clanId == null || !clanExists) {
            return false;
        }
        return !"shinobi".equalsIgnoreCase(clanId.getPath());
    }

    /**
     * Ensures players cannot overwrite a locked clan or select an invalid/default ("shinobi") clan.
     */
    public static boolean validateClanSelection(ServerPlayer player, INinjaData ninjaData, Identifier clanId) {
        if (!validatePlayerState(player) || ninjaData == null) {
            return false;
        }
        if (ninjaData.isClanLocked()) {
            NarutoMod.LOGGER.warn("Player {} attempted unauthorized clan change while clan is locked (requested: {})",
                    player.getScoreboardName(), clanId);
            player.sendSystemMessage(Component.translatable("clan.narutomod.already_locked")
                    .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
            return false;
        }
        boolean clanExists = clanId != null && NarutoClans.getClan(clanId) != null;
        if (!canSelectClan(ninjaData, clanId, clanExists)) {
            NarutoMod.LOGGER.warn("Player {} attempted to select invalid clan {}",
                    player.getScoreboardName(), clanId);
            player.sendSystemMessage(Component.translatable("clan.narutomod.invalid").withStyle(ChatFormatting.RED));
            return false;
        }
        return true;
    }

    public static boolean canRankUp(INinjaData ninjaData) {
        if (ninjaData == null) {
            return false;
        }
        return NinjaRank.fromString(ninjaData.getRank()).canRankUp(ninjaData);
    }

    /**
     * Validates server-side rank graduation requirements before promotion.
     */
    public static boolean validateRankUp(ServerPlayer player, INinjaData ninjaData) {
        if (!validatePlayerState(player) || ninjaData == null) {
            return false;
        }
        if (!canRankUp(ninjaData)) {
            NarutoMod.LOGGER.warn("Player {} attempted rank-up from {} without meeting server requirements",
                    player.getScoreboardName(), ninjaData.getRank());
            return false;
        }
        return true;
    }
}
