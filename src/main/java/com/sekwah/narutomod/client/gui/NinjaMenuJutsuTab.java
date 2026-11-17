package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * Renders the Jutsu Encyclopedia tab of {@link NinjaMenuScreen}.
 */
final class NinjaMenuJutsuTab {

    private NinjaMenuJutsuTab() {}

    static void render(GuiGraphicsExtractor graphics, Font font, INinjaData ninjaData,
                       List<Ability> allAbilities, int jutsuPage, int jutsusPerPage,
                       int centerX, int boxY, int boxWidth) {
        int boxX = centerX - boxWidth / 2;

        if (allAbilities.isEmpty()) {
            graphics.centeredText(font, "§7Henüz öğrenilmiş bir jutsu yok.", centerX, boxY + 60, 0xFFAAAAAA);
            return;
        }

        int startIndex = jutsuPage * jutsusPerPage;
        int endIndex = Math.min(startIndex + jutsusPerPage, allAbilities.size());
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / jutsusPerPage));

        graphics.centeredText(font, String.format("§eSayfa %d / %d", jutsuPage + 1, maxPages), centerX, boxY + 2, 0xFFFFFF88);

        int cardStartY = boxY + 16;
        int cardH = 36;
        int cardGap = 4;

        for (int i = startIndex; i < endIndex; i++) {
            Ability ability = allAbilities.get(i);
            Identifier id = ability.getRegistryName();
            int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);

            graphics.fill(boxX, cardY, boxX + boxWidth, cardY + cardH, 0xCC112211);
            graphics.outline(boxX, cardY, boxWidth, cardH, 0xFF228822);

            String name = Component.translatable(ability.getTranslationKey()).getString();
            String status = "§a✔ ÖĞRENİLDİ";
            int statusWidth = font.width(status);
            int maxNameWidth = boxWidth - statusWidth - 24;
            String displayName = font.width(name) > maxNameWidth ? font.plainSubstrByWidth(name, maxNameWidth - 8) + "..." : name;
            graphics.text(font, "§f§l" + displayName, boxX + 8, cardY + 4, 0xFFFFFFFF);
            graphics.text(font, status, boxX + boxWidth - statusWidth - 8, cardY + 4, 0xFFFFFFFF);

            String comboText = NinjaMenuRenderUtils.formatCombo(ability.defaultCombo());
            graphics.text(font, "§eMühür: §b" + comboText, boxX + 8, cardY + 15, 0xFFDDDDDD);

            int mastery = ninjaData.getMastery(id);
            String masteryText;
            if (id != null && "sharingan".equals(id.getPath())) {
                SharinganAbility.SharinganStage stage = SharinganAbility.getStage(mastery);
                masteryText = "§eUstalık: §a" + mastery + " §7(" + stage.getDisplayName() + ")";
            } else if (id != null && "eight_gates".equals(id.getPath())) {
                EightGatesAbility.GateStage gstage = EightGatesAbility.getStage(mastery);
                masteryText = "§eUstalık: §a" + mastery + " §7(" + gstage.getDisplayName() + ")";
            } else {
                masteryText = "§eUstalık: §a" + mastery + " §7(Lv." + Ability.getMasteryTier(mastery) + ")";
            }
            int masteryWidth = font.width(masteryText);
            graphics.text(font, masteryText, boxX + boxWidth - masteryWidth - 8, cardY + 15, 0xFFFFFF88);

            graphics.text(font, resolveRequirementBadge(ability, id), boxX + 8, cardY + 25, 0xFFAAAAAA);
        }
    }

    private static String resolveRequirementBadge(Ability ability, Identifier id) {
        if (!ability.getPrerequisites().isEmpty()) {
            StringBuilder sbPrereq = new StringBuilder();
            for (Identifier p : ability.getPrerequisites()) {
                Ability pa = NarutoAbilities.getAbility(p);
                String paName = pa != null ? Component.translatable(pa.getTranslationKey()).getString() : p.getPath();
                if (sbPrereq.length() > 0) sbPrereq.append(", ");
                sbPrereq.append(paName);
            }
            return "§b[Önşart: " + sbPrereq + " (" + ability.getRequiredMastery() + "+ Ustalık)]";
        }
        if (id != null) {
            String path = id.getPath();
            if (path.equals("amaterasu") || path.equals("susanoo_ribcage")) return "§c[Uchiha Parşömeni - Jōnin]";
            if (path.equals("eight_gates_death")) return "§4[Nihai Yasak Parşömen - Jōnin]";
            if (path.equals("eight_gates")) return "§4[Yasak Parşömen - Jōnin]";
            if (path.equals("rasenshuriken")) return "§b[S-Seviye Parşömen - Jōnin]";
            if (path.equals("sharingan")) return "§c[Uchiha Klanı - Genin]";
            if (path.equals("byakugan")) return "§f[Hyūga Klanı - Genin]";
            if (path.equals("wood_prison")) return "§2[Senju Klanı - Genin]";
            if (path.equals("bone_dance")) return "§f[Kaguya Klanı - Genin]";
            if (path.equals("shadow_possession")) return "§8[Nara Klanı - Genin / Parşömen]";
        }
        if (NinjaData.isScrollRequiredJutsu(id)) return "§6[Parşömen Gerekir]";
        if (ability.getRequiredRank() == NinjaRank.ACADEMY_STUDENT) return "§7[Temel Yetenek]";
        return "§a[Rütbe: " + ability.getRequiredRank().getTurkishName() + "]";
    }
}
