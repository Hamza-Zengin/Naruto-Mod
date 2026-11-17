package com.sekwah.narutomod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.clans.NarutoClans;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class NarutoCommands {

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_RANKS = (ctx, builder) -> {
        List<String> rankNames = new ArrayList<>();
        for (NinjaRank r : NinjaRank.values()) {
            rankNames.add(r.name().toLowerCase(Locale.ROOT));
        }
        return SharedSuggestionProvider.suggest(rankNames, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_CLANS = (ctx, builder) -> {
        List<String> clanNames = new ArrayList<>();
        for (DeferredHolder<Clan, ? extends Clan> entry : NarutoClans.CLANS.getEntries()) {
            clanNames.add(entry.getId().getPath());
        }
        return SharedSuggestionProvider.suggest(clanNames, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JUTSUS = (ctx, builder) -> {
        List<String> jutsuNames = new ArrayList<>();
        jutsuNames.add("all");
        for (DeferredHolder<Ability, ? extends Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            jutsuNames.add(entry.getId().getPath());
        }
        return SharedSuggestionProvider.suggest(jutsuNames, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JUTSU_IDS = (ctx, builder) -> {
        List<Identifier> ids = new ArrayList<>();
        for (DeferredHolder<Ability, ? extends Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            ids.add(entry.getId());
        }
        return SharedSuggestionProvider.suggestResource(ids, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_MISSIONS = (ctx, builder) -> {
        List<String> missionIds = new ArrayList<>();
        for (com.sekwah.narutomod.capabilities.NinjaMission m : com.sekwah.narutomod.capabilities.NinjaMission.values()) {
            missionIds.add(m.getId());
        }
        return SharedSuggestionProvider.suggest(missionIds, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("naruto")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                // === CHAKRA ===
                .then(Commands.literal("chakra")
                        .then(Commands.literal("get")
                                .executes(ctx -> NarutoCommandHandlers.getChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> NarutoCommandHandlers.getChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> NarutoCommandHandlers.setChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.setChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> NarutoCommandHandlers.addChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.addChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("max")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(100, 1000))
                                        .executes(ctx -> NarutoCommandHandlers.setMaxChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.setMaxChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount")))))))

                // === STAMINA ===
                .then(Commands.literal("stamina")
                        .then(Commands.literal("get")
                                .executes(ctx -> NarutoCommandHandlers.getStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> NarutoCommandHandlers.getStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> NarutoCommandHandlers.setStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.setStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> NarutoCommandHandlers.addStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.addStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("max")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(100, 800))
                                        .executes(ctx -> NarutoCommandHandlers.setMaxStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.setMaxStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount")))))))

                // === CLAN ===
                .then(Commands.literal("clan")
                        .then(Commands.literal("list")
                                .executes(ctx -> NarutoCommandHandlers.listClans(ctx.getSource())))
                        .then(Commands.literal("unlock")
                                .executes(ctx -> NarutoCommandHandlers.unlockClanSelection(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> NarutoCommandHandlers.unlockClanSelection(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("get")
                                .executes(ctx -> NarutoCommandHandlers.getClan(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> NarutoCommandHandlers.getClan(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("clanName", StringArgumentType.word())
                                        .suggests(SUGGEST_CLANS)
                                        .executes(ctx -> NarutoCommandHandlers.setClan(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "clanName")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.setClan(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "clanName")))))))

                // === RANK ===
                .then(Commands.literal("rank")
                        .then(Commands.literal("list")
                                .executes(ctx -> NarutoCommandHandlers.listRanks(ctx.getSource())))
                        .then(Commands.literal("get")
                                .executes(ctx -> NarutoCommandHandlers.getRank(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> NarutoCommandHandlers.getRank(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("rankName", StringArgumentType.word())
                                        .suggests(SUGGEST_RANKS)
                                        .executes(ctx -> NarutoCommandHandlers.setRank(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "rankName")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.setRank(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "rankName")))))))

                // === JUTSU ===
                .then(Commands.literal("jutsu")
                        .then(Commands.literal("list")
                                .executes(ctx -> NarutoCommandHandlers.listJutsus(ctx.getSource())))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .suggests(SUGGEST_JUTSUS)
                                        .executes(ctx -> NarutoCommandHandlers.castJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.castJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .suggests(SUGGEST_JUTSUS)
                                        .executes(ctx -> NarutoCommandHandlers.unlockJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.unlockJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .suggests(SUGGEST_JUTSUS)
                                        .executes(ctx -> NarutoCommandHandlers.lockJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> NarutoCommandHandlers.lockJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId")))))))

                // === MASTERY ===
                .then(Commands.literal("mastery")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("jutsu", IdentifierArgument.id())
                                                .suggests(SUGGEST_JUTSU_IDS)
                                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 10000))
                                                        .executes(ctx -> NarutoCommandHandlers.setJutsuMastery(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                IdentifierArgument.getId(ctx, "jutsu"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount")))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("jutsu", IdentifierArgument.id())
                                                .suggests(SUGGEST_JUTSU_IDS)
                                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-10000, 10000))
                                                        .executes(ctx -> NarutoCommandHandlers.addJutsuMastery(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                IdentifierArgument.getId(ctx, "jutsu"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount")))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("jutsu", IdentifierArgument.id())
                                                .suggests(SUGGEST_JUTSU_IDS)
                                                .executes(ctx -> NarutoCommandHandlers.getJutsuMastery(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        IdentifierArgument.getId(ctx, "jutsu")))))))

                // === STATS & RESET ===
                .then(Commands.literal("stats")
                        .executes(ctx -> NarutoCommandHandlers.getFullStats(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> NarutoCommandHandlers.getFullStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> NarutoCommandHandlers.resetData(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> NarutoCommandHandlers.resetData(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("mission")
                        .then(Commands.literal("assign")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("missionId", StringArgumentType.word()).suggests(SUGGEST_MISSIONS)
                                                .executes(ctx -> NarutoCommandHandlers.assignMission(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        StringArgumentType.getString(ctx, "missionId"))))))
                        .then(Commands.literal("complete")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> NarutoCommandHandlers.completeMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .executes(ctx -> NarutoCommandHandlers.resetMission(ctx.getSource(), EntityArgument.getPlayer(ctx, "target")))))));
    }
}
