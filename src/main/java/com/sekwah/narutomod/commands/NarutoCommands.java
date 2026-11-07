package com.sekwah.narutomod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.clans.NarutoClans;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Collections;

public class NarutoCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("naruto")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("chakra")
                        .then(Commands.literal("get")
                                .executes(ctx -> getChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> getChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> setChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> addChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> addChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("stamina")
                        .then(Commands.literal("get")
                                .executes(ctx -> getStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> getStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(0))
                                        .executes(ctx -> setStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg())
                                        .executes(ctx -> addStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> addStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount")))))))
                .then(Commands.literal("clan")
                        .then(Commands.literal("list")
                                .executes(ctx -> listClans(ctx.getSource())))
                        .then(Commands.literal("unlock")
                                .executes(ctx -> unlockClanSelection(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> unlockClanSelection(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("get")
                                .executes(ctx -> getClan(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> getClan(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("clanName", StringArgumentType.word())
                                        .executes(ctx -> setClan(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "clanName")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setClan(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "clanName")))))))
                .then(Commands.literal("rank")
                        .then(Commands.literal("list")
                                .executes(ctx -> listRanks(ctx.getSource())))
                        .then(Commands.literal("get")
                                .executes(ctx -> getRank(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> getRank(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("rankName", StringArgumentType.word())
                                        .executes(ctx -> setRank(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "rankName")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setRank(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "rankName")))))))
                .then(Commands.literal("jutsu")
                        .then(Commands.literal("list")
                                .executes(ctx -> listJutsus(ctx.getSource())))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .executes(ctx -> castJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> castJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .executes(ctx -> unlockJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> unlockJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .executes(ctx -> lockJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> lockJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))));
    }

    private static int getChakra(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Chakra: " + (int) data.getChakra() + " / " + (int) data.getMaxChakra()).withStyle(ChatFormatting.AQUA), false);
            });
        }
        return players.size();
    }

    private static int setChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setChakra(amount);
                source.sendSuccess(() -> Component.literal("Set " + player.getScoreboardName() + "'s chakra to " + amount).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int addChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.addChakra(amount);
                source.sendSuccess(() -> Component.literal("Added " + amount + " chakra to " + player.getScoreboardName()).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int getStamina(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Stamina: " + (int) data.getStamina() + " / " + (int) data.getMaxStamina()).withStyle(ChatFormatting.YELLOW), false);
            });
        }
        return players.size();
    }

    private static int setStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setStamina(amount);
                source.sendSuccess(() -> Component.literal("Set " + player.getScoreboardName() + "'s stamina to " + amount).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int addStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.addStamina(amount);
                source.sendSuccess(() -> Component.literal("Added " + amount + " stamina to " + player.getScoreboardName()).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int getClan(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                String clan = data.getClan() != null ? data.getClan().getPath() : "None";
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Clan: " + clan).withStyle(ChatFormatting.GOLD), false);
            });
        }
        return players.size();
    }

    private static int setClan(CommandSourceStack source, Collection<ServerPlayer> players, String clanName) {
        ResourceLocation clanLoc = new ResourceLocation(NarutoMod.MOD_ID, clanName.toLowerCase());
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setClan(clanLoc);
                source.sendSuccess(() -> Component.literal("Set " + player.getScoreboardName() + "'s clan to " + clanName).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int getRank(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Rank: " + data.getRank()).withStyle(ChatFormatting.LIGHT_PURPLE), false);
            });
        }
        return players.size();
    }

    private static int setRank(CommandSourceStack source, Collection<ServerPlayer> players, String rankName) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setRank(rankName.toUpperCase());
                source.sendSuccess(() -> Component.literal("Set " + player.getScoreboardName() + "'s rank to " + rankName.toUpperCase()).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int castJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        ResourceLocation loc = new ResourceLocation(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        Ability ability = NarutoAbilities.getAbility(loc);
        if (ability == null) {
            source.sendFailure(Component.literal("Unknown jutsu: " + jutsuId));
            return 0;
        }

        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                ability.performServer(player, data, 0);
                source.sendSuccess(() -> Component.literal("Cast " + jutsuId + " on " + player.getScoreboardName()).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int listClans(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Klanlar ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (RegistryObject<Clan> entry : NarutoClans.CLANS.getEntries()) {
            source.sendSuccess(() -> Component.literal(" - " + entry.getId().getPath()).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    private static int unlockClanSelection(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setClanLocked(false);
                source.sendSuccess(() -> Component.literal("Unlocked clan selection for " + player.getScoreboardName() + ". They can now re-pick in the 'J' Clan menu.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int listRanks(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Rütbeler ===").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        for (NinjaRank rank : NinjaRank.values()) {
            source.sendSuccess(() -> Component.literal(" - " + rank.name() + " (" + rank.getTurkishName() + ")").withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    private static int listJutsus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Jutsular & Yetenekler ===").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        for (RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            Ability a = entry.get();
            String path = entry.getId().getPath();
            boolean isScroll = NinjaData.isScrollRequiredJutsu(entry.getId());
            String tag = isScroll ? " §e[Parşömen]" : "";
            source.sendSuccess(() -> Component.literal(" - " + path + tag + " §7(Gerekli: " + a.getRequiredRank().getDisplayName() + ")").withStyle(ChatFormatting.WHITE), false);
        }
        return 1;
    }

    private static int unlockJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        if (jutsuId.equalsIgnoreCase("all")) {
            for (ServerPlayer player : players) {
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                    for (RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
                        data.unlockAbility(entry.getId());
                    }
                    source.sendSuccess(() -> Component.literal("Unlocked ALL jutsus for " + player.getScoreboardName()).withStyle(ChatFormatting.GREEN), true);
                });
            }
            return players.size();
        }

        ResourceLocation loc = new ResourceLocation(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        Ability ability = NarutoAbilities.getAbility(loc);
        if (ability == null) {
            source.sendFailure(Component.literal("Unknown jutsu: " + jutsuId));
            return 0;
        }

        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.unlockAbility(loc);
                source.sendSuccess(() -> Component.literal("Unlocked jutsu '" + jutsuId + "' for " + player.getScoreboardName()).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int lockJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        if (jutsuId.equalsIgnoreCase("all")) {
            for (ServerPlayer player : players) {
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                    data.setUnlockedJutsusString("");
                    source.sendSuccess(() -> Component.literal("Locked / cleared all unlocked jutsus for " + player.getScoreboardName()).withStyle(ChatFormatting.YELLOW), true);
                });
            }
            return players.size();
        }

        ResourceLocation loc = new ResourceLocation(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.lockAbility(loc);
                source.sendSuccess(() -> Component.literal("Locked jutsu '" + jutsuId + "' for " + player.getScoreboardName()).withStyle(ChatFormatting.YELLOW), true);
            });
        }
        return players.size();
    }
}
