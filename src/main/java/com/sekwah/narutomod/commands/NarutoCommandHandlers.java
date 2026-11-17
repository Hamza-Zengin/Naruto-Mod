package com.sekwah.narutomod.commands;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.advancements.NinjaProgressTrigger;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.capabilities.toggleabilitydata.ToggleAbilityData;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.clans.NarutoClans;
import com.sekwah.narutomod.registries.NarutoRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collection;

/**
 * Execution handlers for {@code /naruto} subcommands, extracted from {@link NarutoCommands}.
 */
final class NarutoCommandHandlers {

    private NarutoCommandHandlers() {}

    // === CHAKRA HANDLERS ===
    static int getChakra(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            NinjaRank rank = NinjaRank.fromString(data.getRank());
            float bonus = rank.getChakraBonus();
            String bonusStr = bonus > 0 ? " (Taban: " + (int) data.getBaseMaxChakra() + ", Rütbe Bonusu: +" + (int) bonus + ")" : "";
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Çakra: " + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + bonusStr).withStyle(ChatFormatting.AQUA), false);
        }
        return players.size();
    }

    static int setChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setChakra(amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun çakrası " + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + " olarak ayarlandı.").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int addChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.addChakra(amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncuya " + (int) amount + " çakra eklendi (Mevcut: " + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + ").").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int setMaxChakra(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setBaseMaxChakra(amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun eğitilmiş taban çakrası " + (int) amount + " olarak ayarlandı (Toplam Maksimum: " + (int) data.getMaxChakra() + ").").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    // === STAMINA HANDLERS ===
    static int getStamina(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            NinjaRank rank = NinjaRank.fromString(data.getRank());
            float bonus = rank.getStaminaBonus();
            String bonusStr = bonus > 0 ? " (Taban: " + (int) data.getBaseMaxStamina() + ", Rütbe Bonusu: +" + (int) bonus + ")" : "";
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Kondisyon: " + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + bonusStr).withStyle(ChatFormatting.YELLOW), false);
        }
        return players.size();
    }

    static int setStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setStamina(amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun kondisyonu " + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + " olarak ayarlandı.").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int addStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.addStamina(amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncuya " + (int) amount + " kondisyon eklendi (Mevcut: " + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + ").").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int setMaxStamina(CommandSourceStack source, Collection<ServerPlayer> players, float amount) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setBaseMaxStamina(amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun eğitilmiş taban kondisyonu " + (int) amount + " olarak ayarlandı (Toplam Maksimum: " + (int) data.getMaxStamina() + ").").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    // === CLAN HANDLERS ===
    static int getClan(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            String clan = data.getClan() != null ? data.getClan().getPath() : "shinobi";
            String locked = data.isClanLocked() ? "§c[Kilitli]" : "§a[Değiştirilebilir]";
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Klanı: " + clan + " " + locked).withStyle(ChatFormatting.GOLD), false);
        }
        return players.size();
    }

    static int setClan(CommandSourceStack source, Collection<ServerPlayer> players, String clanName) {
        Identifier clanLoc = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, clanName.toLowerCase());
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.forceSetClan(clanLoc);
            NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                    .trigger(player, NinjaProgressTrigger.Action.SELECT_CLAN, clanLoc.getPath());
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun klanı '" + clanName + "' olarak ayarlandı ve mühürlendi.").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int unlockClanSelection(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setClanLocked(false);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için klan seçimi kilidi açıldı. Oyuncu 'J' menüsünden yeni klan seçebilir.").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int listClans(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Klanlar ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
        for (DeferredHolder<Clan, ? extends Clan> entry : NarutoClans.CLANS.getEntries()) {
            source.sendSuccess(() -> Component.literal(" - " + entry.getId().getPath()).withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }

    // === RANK HANDLERS ===
    static int getRank(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            NinjaRank rank = NinjaRank.fromString(data.getRank());
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " Rütbesi: ")
                    .append(Component.translatable(rank.getTranslationKey()))
                    .append(" (" + rank.getEnglishName() + ")")
                    .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        }
        return players.size();
    }

    static int setRank(CommandSourceStack source, Collection<ServerPlayer> players, String rankName) {
        NinjaRank parsed = NinjaRank.parseExact(rankName);
        if (parsed == null) {
            source.sendFailure(Component.literal("Geçersiz rütbe: '" + rankName + "'. Geçerli rütbeler: academy_student, genin, chunin, jonin, anbu, kage"));
            return 0;
        }

        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setRank(parsed.name());
            data.setJutsusCastCount(0);
            data.setEnemiesDefeated(0);
            data.setChakra(data.getMaxChakra());
            data.setStamina(data.getMaxStamina());
            NarutoRegistries.NINJA_PROGRESS_TRIGGER.get()
                    .trigger(player, NinjaProgressTrigger.Action.RANK_UP, parsed.name());
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " rütbesi '")
                    .append(Component.translatable(parsed.getTranslationKey()))
                    .append("' yapıldı. Çakra/kondisyon yenilendi.")
                    .withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int listRanks(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Shinobi Rütbeleri ===").withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD), false);
        for (NinjaRank rank : NinjaRank.values()) {
            source.sendSuccess(() -> Component.literal(" - " + rank.name().toLowerCase() + " (")
                    .append(Component.translatable(rank.getTranslationKey()))
                    .append(") §7[+" + (int) rank.getChakraBonus() + " Çakra, +" + (int) rank.getStaminaBonus() + " Kondisyon]")
                    .withStyle(ChatFormatting.AQUA), false);
        }
        return 1;
    }

    // === JUTSU HANDLERS ===
    static int castJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        Identifier loc = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        Ability ability = NarutoAbilities.getAbility(loc);
        if (ability == null) {
            source.sendFailure(Component.literal("Bilinmeyen jutsu: " + jutsuId));
            return 0;
        }

        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (ability.activationType() == Ability.ActivationType.TOGGLE) {
                ToggleAbilityData tracker = data.getToggleAbilityData();
                if (tracker.hasToggleAbility(ability.getRegistryName())) {
                    tracker.removeAbilityEnded(player, data, ability);
                    source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için " + jutsuId + " deaktif edildi.").withStyle(ChatFormatting.YELLOW), true);
                } else {
                    tracker.addAbilityStarted(player, data, ability);
                    data.addJutsuCast();
                    source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için " + jutsuId + " aktif edildi.").withStyle(ChatFormatting.GREEN), true);
                }
            } else {
                ability.perform(player, data, 0);
                data.addJutsuCast();
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " tarafından " + jutsuId + " uygulandı.").withStyle(ChatFormatting.GREEN), true);
            }
        }
        return players.size();
    }

    static int listJutsus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal("=== Mevcut Jutsular & Yetenekler ===").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD), false);
        for (DeferredHolder<Ability, ? extends Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
            Ability a = entry.get();
            String path = entry.getId().getPath();
            boolean isScroll = NinjaData.isScrollRequiredJutsu(entry.getId());
            String tag = isScroll ? " §e[Parşömen]" : "";
            source.sendSuccess(() -> Component.literal(" - " + path + tag + " §7(Gerekli: ")
                    .append(Component.translatable(a.getRequiredRank().getTranslationKey()))
                    .append(")")
                    .withStyle(ChatFormatting.WHITE), false);
        }
        return 1;
    }

    static int unlockJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        if (jutsuId.equalsIgnoreCase("all")) {
            for (ServerPlayer player : players) {
                var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
                for (DeferredHolder<Ability, ? extends Ability> entry : NarutoAbilities.ABILITY.getEntries()) {
                    data.unlockAbility(entry.getId());
                }
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için TÜM jutsular açıldı.").withStyle(ChatFormatting.GREEN), true);
            }
            return players.size();
        }

        Identifier loc = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        Ability ability = NarutoAbilities.getAbility(loc);
        if (ability == null) {
            source.sendFailure(Component.literal("Bilinmeyen jutsu: " + jutsuId));
            return 0;
        }

        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.unlockAbility(loc);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için '" + jutsuId + "' jutsusu açıldı.").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int lockJutsu(CommandSourceStack source, Collection<ServerPlayer> players, String jutsuId) {
        if (jutsuId.equalsIgnoreCase("all")) {
            for (ServerPlayer player : players) {
                var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
                data.setUnlockedJutsusString("");
                source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için açılan parşömen jutsuları sıfırlandı.").withStyle(ChatFormatting.YELLOW), true);
            }
            return players.size();
        }

        Identifier loc = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, jutsuId.toLowerCase());
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.lockAbility(loc);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için '" + jutsuId + "' jutsusu kilitlendi.").withStyle(ChatFormatting.YELLOW), true);
        }
        return players.size();
    }

    // === FULL STATS & RESET ===
    static int getFullStats(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            NinjaRank rank = NinjaRank.fromString(data.getRank());
            String clan = data.getClan() != null ? data.getClan().getPath() : "shinobi";
            String clanLock = data.isClanLocked() ? "§c[Kilitli]" : "§a[Seçilmedi]";

            float baseChakra = data.getBaseMaxChakra();
            float cExpNeeded = baseChakra < 150.0f
                    ? 30.0f + (baseChakra - 100.0f) * 0.50f
                    : 55.0f + (baseChakra - 150.0f) * 1.10f;
            float sExpNeeded = 50.0f + (data.getBaseMaxStamina() - 100.0f) * 1.10f;

            source.sendSuccess(() -> Component.literal("=== SHINOBI STATS: " + player.getScoreboardName() + " ===").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            source.sendSuccess(() -> Component.literal("• Rütbe: §f")
                    .append(Component.translatable(rank.getTranslationKey()))
                    .append(" (" + rank.getEnglishName() + ")")
                    .withStyle(ChatFormatting.AQUA), false);
            source.sendSuccess(() -> Component.literal("• Klan: §f" + clan + " " + clanLock).withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal("• Çakra: §f" + (int) data.getChakra() + " / " + (int) data.getMaxChakra() + " §7(Taban: " + (int) data.getBaseMaxChakra() + ", Rütbe Bonusu: +" + (int) rank.getChakraBonus() + ")").withStyle(ChatFormatting.BLUE), false);
            source.sendSuccess(() -> Component.literal("• Kondisyon: §f" + (int) data.getStamina() + " / " + (int) data.getMaxStamina() + " §7(Taban: " + (int) data.getBaseMaxStamina() + ", Rütbe Bonusu: +" + (int) rank.getStaminaBonus() + ")").withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("• Çakra Eğitimi: §f" + String.format("%.1f", data.getChakraExp()) + " / " + String.format("%.1f", cExpNeeded) + " EXP").withStyle(ChatFormatting.LIGHT_PURPLE), false);
            source.sendSuccess(() -> Component.literal("• Kondisyon Eğitimi: §f" + String.format("%.1f", data.getStaminaExp()) + " / " + String.format("%.1f", sExpNeeded) + " EXP").withStyle(ChatFormatting.DARK_GREEN), false);
            source.sendSuccess(() -> Component.literal("• Jutsu Pratiği: §f" + data.getJutsusCastCount() + " kez").withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("• Yenilen Düşman: §f" + data.getEnemiesDefeated()).withStyle(ChatFormatting.RED), false);
            source.sendSuccess(() -> Component.literal("• Öğrenilen Parşömenler: §f" + data.getUnlockedAbilities().size() + " adet").withStyle(ChatFormatting.DARK_AQUA), false);
        }
        return players.size();
    }

    static int resetData(CommandSourceStack source, Collection<ServerPlayer> players) {
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.resetData();
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " adlı oyuncunun ninja verileri Akademi Öğrencisi başlangıç seviyesine sıfırlandı.").withStyle(ChatFormatting.GOLD), true);
        }
        return players.size();
    }

    // === MASTERY HANDLERS ===
    static int setJutsuMastery(CommandSourceStack source, Collection<ServerPlayer> players, Identifier jutsuId, int amount) {
        if (NarutoAbilities.ABILITY.getEntries().stream().noneMatch(e -> e.getId().equals(jutsuId))) {
            source.sendFailure(Component.literal("Geçersiz jutsu ID'si: " + jutsuId).withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.setMastery(jutsuId, amount);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " - " + jutsuId + " ustalığı " + amount + " olarak ayarlandı.").withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int addJutsuMastery(CommandSourceStack source, Collection<ServerPlayer> players, Identifier jutsuId, int amount) {
        if (NarutoAbilities.ABILITY.getEntries().stream().noneMatch(e -> e.getId().equals(jutsuId))) {
            source.sendFailure(Component.literal("Geçersiz jutsu ID'si: " + jutsuId).withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            data.addMastery(jutsuId, amount);
            int updated = data.getMastery(jutsuId);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " - " + jutsuId + " ustalığı güncellendi: " + updated).withStyle(ChatFormatting.GREEN), true);
        }
        return players.size();
    }

    static int getJutsuMastery(CommandSourceStack source, Collection<ServerPlayer> players, Identifier jutsuId) {
        if (NarutoAbilities.ABILITY.getEntries().stream().noneMatch(e -> e.getId().equals(jutsuId))) {
            source.sendFailure(Component.literal("Geçersiz jutsu ID'si: " + jutsuId).withStyle(ChatFormatting.RED));
            return 0;
        }
        for (ServerPlayer player : players) {
            var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            int current = data.getMastery(jutsuId);
            source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " - " + jutsuId + " ustalığı: " + current).withStyle(ChatFormatting.AQUA), false);
        }
        return players.size();
    }

    // === MISSION HANDLERS ===
    static int assignMission(CommandSourceStack source, ServerPlayer player, String missionId) {
        NinjaMission mission = NinjaMission.fromId(missionId);
        if (mission == null) {
            source.sendFailure(Component.literal("Geçersiz görev ID'si: " + missionId).withStyle(ChatFormatting.RED));
            return 0;
        }
        var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        data.acceptMission(mission);
        data.updateServerData(player);
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " için görev atandı: " + mission.getId())
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    static int completeMission(CommandSourceStack source, ServerPlayer player) {
        var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        NinjaMission active = NinjaMission.fromId(data.getActiveMissionId());
        if (active == null) {
            source.sendFailure(Component.literal(player.getScoreboardName() + " için aktif görev yok.").withStyle(ChatFormatting.RED));
            return 0;
        }
        data.setMissionProgress(active.getTargetCount());
        data.updateServerData(player);
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " görevi tamamlandı olarak işaretlendi: " + active.getId())
                .withStyle(ChatFormatting.GOLD), true);
        return 1;
    }

    static int resetMission(CommandSourceStack source, ServerPlayer player) {
        var data = player.getData(NinjaCapabilityHandler.NINJA_DATA);
        data.abandonMission();
        data.updateServerData(player);
        source.sendSuccess(() -> Component.literal(player.getScoreboardName() + " aktif görevi sıfırlandı.")
                .withStyle(ChatFormatting.YELLOW), true);
        return 1;
    }
}
