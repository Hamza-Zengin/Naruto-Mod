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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.registries.RegistryObject;

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
        for (RegistryObject<Clan> entry : NarutoClans.CLANS.getEntries()) {
            clanNames.add(entry.getId().getPath());
        }
        return SharedSuggestionProvider.suggest(clanNames, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JUTSUS = (ctx, builder) -> {
        List<String> jutsuNames = new ArrayList<>();
        jutsuNames.add("all");
        for (RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            jutsuNames.add(entry.getId().getPath());
        }
        return SharedSuggestionProvider.suggest(jutsuNames, builder);
    };

    private static final SuggestionProvider<CommandSourceStack> SUGGEST_JUTSU_IDS = (ctx, builder) -> {
        List<ResourceLocation> ids = new ArrayList<>();
        for (RegistryObject<Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            ids.add(entry.getId());
        }
        return SharedSuggestionProvider.suggestResource(ids, builder);
    };

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("naruto")
                .requires(source -> source.hasPermission(2))

                // === CHAKRA ===
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
                                                .executes(ctx -> addChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("max")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(100, 500))
                                        .executes(ctx -> setMaxChakra(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setMaxChakra(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount")))))))

                // === STAMINA ===
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
                                                .executes(ctx -> addStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount"))))))
                        .then(Commands.literal("max")
                                .then(Commands.argument("amount", FloatArgumentType.floatArg(100, 400))
                                        .executes(ctx -> setMaxStamina(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), FloatArgumentType.getFloat(ctx, "amount")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setMaxStamina(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), FloatArgumentType.getFloat(ctx, "amount")))))))

                // === CLAN ===
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
                                        .suggests(SUGGEST_CLANS)
                                        .executes(ctx -> setClan(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "clanName")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setClan(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "clanName")))))))

                // === RANK ===
                .then(Commands.literal("rank")
                        .then(Commands.literal("list")
                                .executes(ctx -> listRanks(ctx.getSource())))
                        .then(Commands.literal("get")
                                .executes(ctx -> getRank(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .executes(ctx -> getRank(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("rankName", StringArgumentType.word())
                                        .suggests(SUGGEST_RANKS)
                                        .executes(ctx -> setRank(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "rankName")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> setRank(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "rankName")))))))

                // === JUTSU ===
                .then(Commands.literal("jutsu")
                        .then(Commands.literal("list")
                                .executes(ctx -> listJutsus(ctx.getSource())))
                        .then(Commands.literal("cast")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .suggests(SUGGEST_JUTSUS)
                                        .executes(ctx -> castJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> castJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))
                        .then(Commands.literal("unlock")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .suggests(SUGGEST_JUTSUS)
                                        .executes(ctx -> unlockJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> unlockJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId"))))))
                        .then(Commands.literal("lock")
                                .then(Commands.argument("jutsuId", StringArgumentType.word())
                                        .suggests(SUGGEST_JUTSUS)
                                        .executes(ctx -> lockJutsu(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException()), StringArgumentType.getString(ctx, "jutsuId")))
                                        .then(Commands.argument("targets", EntityArgument.players())
                                                .executes(ctx -> lockJutsu(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"), StringArgumentType.getString(ctx, "jutsuId")))))))

                // === MASTERY ===
                .then(Commands.literal("mastery")
                        .then(Commands.literal("set")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("jutsu", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                                .suggests(SUGGEST_JUTSU_IDS)
                                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 10000))
                                                        .executes(ctx -> setJutsuMastery(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "jutsu"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount")))))))
                        .then(Commands.literal("add")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("jutsu", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                                .suggests(SUGGEST_JUTSU_IDS)
                                                .then(Commands.argument("amount", com.mojang.brigadier.arguments.IntegerArgumentType.integer(-10000, 10000))
                                                        .executes(ctx -> addJutsuMastery(ctx.getSource(),
                                                                EntityArgument.getPlayers(ctx, "targets"),
                                                                net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "jutsu"),
                                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "amount")))))))
                        .then(Commands.literal("get")
                                .then(Commands.argument("targets", EntityArgument.players())
                                        .then(Commands.argument("jutsu", net.minecraft.commands.arguments.ResourceLocationArgument.id())
                                                .suggests(SUGGEST_JUTSU_IDS)
                                                .executes(ctx -> getJutsuMastery(ctx.getSource(),
                                                        EntityArgument.getPlayers(ctx, "targets"),
                                                        net.minecraft.commands.arguments.ResourceLocationArgument.getId(ctx, "jutsu")))))))

                // === STATS & RESET ===
                .then(Commands.literal("stats")
                        .executes(ctx -> getFullStats(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> getFullStats(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets")))))
                .then(Commands.literal("reset")
                        .executes(ctx -> resetData(ctx.getSource(), Collections.singleton(ctx.getSource().getPlayerOrException())))
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(ctx -> resetData(ctx.getSource(), EntityArgument.getPlayers(ctx, "targets"))))));
    }

    // === CHAKRA HANDLERS ===
    private static int getChakra(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                NinjaRank rank = NinjaRank.fromString(data.getRank());
                float bonus = rank.getChakraBonus();
                String bonusStr = bonus > 0 ? " (Taban: " + (int) data.getBaseMaxChakra() + ", Rütbe Bonusu: +" + (int) bonus + ")" : "";
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Çakra: " + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + bonusStr).withStyle(ChatFormatting.AQUA), false);
            });
        }
        return players.size();
    }

    private static int setChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setChakra(amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun çakrası " + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + " olarak ayarlandı.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int addChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.addChakra(amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncuya " + (int) amount + " çakra eklendi (Mevcut: " + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + ").").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int setMaxChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setBaseMaxChakra(amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun eğitilmiş taban çakrası " + (int) amount + " olarak ayarlandı (Toplam Maksimum: " + (int) data.getMaxChakra() + ").").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    // === STAMINA HANDLERS ===
    private static int getStamina(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                NinjaRank rank = NinjaRank.fromString(data.getRank());
                float bonus = rank.getStaminaBonus();
                String bonusStr = bonus > 0 ? " (Taban: " + (int) data.getBaseMaxStamina() + ", Rütbe Bonusu: +" + (int) bonus + ")" : "";
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Kondisyon: " + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + bonusStr).withStyle(ChatFormatting.YELLOW), false);
            });
        }
        return players.size();
    }

    private static int setStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setStamina(amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun kondisyonu " + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + " olarak ayarlandı.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int addStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.addStamina(amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncuya " + (int) amount + " kondisyon eklendi (Mevcut: " + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + ").").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int setMaxStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setBaseMaxStamina(amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun eğitilmiş taban kondisyonu " + (int) amount + " olarak ayarlandı (Toplam Maksimum: " + (int) data.getMaxStamina() + ").").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    // === CLAN HANDLERS ===
    private static int getClan(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                String clan = data.getClan() != null ? data.getClan().getPath() : "shinobi";
                String locked = data.isClanLocked() ? "§c[Kilitli]" : "§a[Değiştirilebilir]";
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Klanı: " + clan + " " + locked).withStyle(ChatFormatting.GOLD), false);
            });
        }
        return players.size();
    }

    private static int setClan(CommandSourceStack source, Collection<ServerPlayer> players, String clanName) {
        ResourceLocation clanLoc = new ResourceLocation(NarutoMod.MOD_ID, clanName.toLowerCase());
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.forceSetClan(clanLoc);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun klanı '" + clanName + "' olarak ayarlandı ve mühürlendi.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int unlockClanSelection(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setClanLocked(false);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için klan seçimi kilidi açıldı. Oyuncu 'J' menüsünden yeni klan seçebilir.").withStyle(ChatFormatting.GREEN), true);
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

    // === RANK HANDLERS ===
    private static int getRank(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                NinjaRank rank = NinjaRank.fromString(data.getRank());
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Rütbesi: " + rank.getTurkishName() + " (" + rank.getEnglishName() + ")").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            });
        }
        return players.size();
    }

    private static int setRank(CommandSourceStack source, Collection<ServerPlayer> players, String rankName) {
        NinjaRank parsed = NinjaRank.parseExact(rankName);
        if (parsed == null) {
            source.sendFailure(Component.literal("Geçersiz rütbe: '" + rankName + "'. Geçerli rütbeler: academy_student, genin, chunin, jonin, anbu, kage"));
            return 0;
        }

        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setRank(parsed.name());
                data.setJutsusCastCount(0);
                data.setEnemiesDefeated(0);
                data.setChakra(data.getMaxChakra());
                data.setStamina(data.getMaxStamina());
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " rütbesi '" + parsed.getTurkishName() + "' yapıldı. Çakra/kondisyon yenilendi.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int listRanks(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Shinobi Rütbeleri ===").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        for (NinjaRank rank : NinjaRank.values()) {
            source.sendSuccess(() -> Component.literal(" - " + rank.name().toLowerCase() + " (" + rank.getTurkishName() + ") §7[+" + (int) rank.getChakraBonus() + " Çakra, +" + (int) rank.getStaminaBonus() + " Kondisyon]").withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    // === JUTSU HANDLERS ===
    private static int castJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        ResourceLocation loc = new ResourceLocation(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        Ability ability = NarutoAbilities.getAbility(loc);
        if (ability == null) {
            source.sendFailure(Component.literal("Bilinmeyen jutsu: " + jutsuId));
            return 0;
        }

        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                if (ability.activationType() == Ability.ActivationType.TOGGLE) {
                    com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData tracker = data.getToggleAbilityData();
                    if (tracker.hasToggleAbility(ability.getRegistryName())) {
                        tracker.removeAbilityEnded(player, data, ability);
                        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için " + jutsuId + " deaktif edildi.").withStyle(ChatFormatting.YELLOW), true);
                    } else {
                        tracker.addAbilityStarted(player, data, ability);
                        data.addJutsuCast();
                        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için " + jutsuId + " aktif edildi.").withStyle(ChatFormatting.GREEN), true);
                    }
                } else {
                    ability.performServer(player, data, 0);
                    data.addJutsuCast();
                    source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " tarafından " + jutsuId + " uygulandı.").withStyle(ChatFormatting.GREEN), true);
                }
            });
        }
        return players.size();
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
                    source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için TÜM jutsular açıldı.").withStyle(ChatFormatting.GREEN), true);
                });
            }
            return players.size();
        }

        ResourceLocation loc = new ResourceLocation(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        Ability ability = NarutoAbilities.getAbility(loc);
        if (ability == null) {
            source.sendFailure(Component.literal("Bilinmeyen jutsu: " + jutsuId));
            return 0;
        }

        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.unlockAbility(loc);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için '" + jutsuId + "' jutsusu açıldı.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int lockJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        if (jutsuId.equalsIgnoreCase("all")) {
            for (ServerPlayer player : players) {
                player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                    data.setUnlockedJutsusString("");
                    source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için açılan parşömen jutsuları sıfırlandı.").withStyle(ChatFormatting.YELLOW), true);
                });
            }
            return players.size();
        }

        ResourceLocation loc = new ResourceLocation(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.lockAbility(loc);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için '" + jutsuId + "' jutsusu kilitlendi.").withStyle(ChatFormatting.YELLOW), true);
            });
        }
        return players.size();
    }

    // === FULL STATS & RESET ===
    private static int getFullStats(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                NinjaRank rank = NinjaRank.fromString(data.getRank());
                String clan = data.getClan() != null ? data.getClan().getPath() : "shinobi";
                String clanLock = data.isClanLocked() ? "§c[Kilitli]" : "§a[Seçilmedi]";

                float cExpNeeded = 60.0f + (data.getBaseMaxChakra() - 100.0f) * 1.20f;
                float sExpNeeded = 50.0f + (data.getBaseMaxStamina() - 100.0f) * 1.10f;

                source.sendSuccess(() -> Component.literal("=== SHINOBI STATS: " + player.getScoreboardName() + " ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
                source.sendSuccess(() -> Component.literal("• Rütbe: §f" + rank.getTurkishName() + " (" + rank.getEnglishName() + ")").withStyle(ChatFormatting.AQUA), false);
                source.sendSuccess(() -> Component.literal("• Klan: §f" + clan + " " + clanLock).withStyle(ChatFormatting.YELLOW), false);
                source.sendSuccess(() -> Component.literal("• Çakra: §f" + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + " §7(Taban: " + (int) data.getBaseMaxChakra() + ", Rütbe Bonusu: +" + (int) rank.getChakraBonus() + ")").withStyle(ChatFormatting.BLUE), false);
                source.sendSuccess(() -> Component.literal("• Kondisyon: §f" + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + " §7(Taban: " + (int) data.getBaseMaxStamina() + ", Rütbe Bonusu: +" + (int) rank.getStaminaBonus() + ")").withStyle(ChatFormatting.GREEN), false);
                source.sendSuccess(() -> Component.literal("• Çakra Eğitimi: §f" + String.format("%.1f", data.getChakraExp()) + " / " + String.format("%.1f", cExpNeeded) + " EXP").withStyle(ChatFormatting.LIGHT_PURPLE), false);
                source.sendSuccess(() -> Component.literal("• Kondisyon Eğitimi: §f" + String.format("%.1f", data.getStaminaExp()) + " / " + String.format("%.1f", sExpNeeded) + " EXP").withStyle(ChatFormatting.DARK_GREEN), false);
                source.sendSuccess(() -> Component.literal("• Jutsu Pratiği: §f" + data.getJutsusCastCount() + " kez").withStyle(ChatFormatting.GRAY), false);
                source.sendSuccess(() -> Component.literal("• Yenilen Düşman: §f" + data.getEnemiesDefeated()).withStyle(ChatFormatting.RED), false);
                source.sendSuccess(() -> Component.literal("• Öğrenilen Parşömenler: §f" + data.getUnlockedAbilities().size() + " adet").withStyle(ChatFormatting.DARK_AQUA), false);
            });
        }
        return players.size();
    }

    private static int resetData(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.resetData();
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun ninja verileri Akademi Öğrencisi başlangıç seviyesine sıfırlandı.").withStyle(ChatFormatting.GOLD), true);
            });
        }
        return players.size();
    }

    // === MASTERY HANDLERS ===
    private static int setJutsuMastery(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation jutsuId, int amount) {
        if (NarutoAbilities.ABILITY.getEntries().stream().noneMatch(e -> e.getId().equals(jutsuId))) {
            source.sendFailure(Component.literal("Geçersiz jutsu ID'si: " + jutsuId).withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.setMastery(jutsuId, amount);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " - " + jutsuId + " ustalığı " + amount + " olarak ayarlandı.").withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int addJutsuMastery(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation jutsuId, int amount) {
        if (NarutoAbilities.ABILITY.getEntries().stream().noneMatch(e -> e.getId().equals(jutsuId))) {
            source.sendFailure(Component.literal("Geçersiz jutsu ID'si: " + jutsuId).withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                data.addMastery(jutsuId, amount);
                int updated = data.getMastery(jutsuId);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " - " + jutsuId + " ustalığı güncellendi: " + updated).withStyle(ChatFormatting.GREEN), true);
            });
        }
        return players.size();
    }

    private static int getJutsuMastery(CommandSourceStack source, Collection<ServerPlayer> players, ResourceLocation jutsuId) {
        if (NarutoAbilities.ABILITY.getEntries().stream().noneMatch(e -> e.getId().equals(jutsuId))) {
            source.sendFailure(Component.literal("Geçersiz jutsu ID'si: " + jutsuId).withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer player : players) {
            player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(data -> {
                int current = data.getMastery(jutsuId);
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " - " + jutsuId + " ustalığı: " + current).withStyle(ChatFormatting.AQUA), false);
            });
        }
        return players.size();
    }
}
