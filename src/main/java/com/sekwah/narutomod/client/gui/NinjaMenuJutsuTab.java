package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.abilities.jutsus.EightGatesAbility;
import com.sekwah.narutomod.abilities.jutsus.SharinganAbility;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;
import java.util.Locale;

/**
 * Renders the Jutsu Loadout &amp; Encyclopedia tab of {@link NinjaMenuScreen}.
 */
final class NinjaMenuJutsuTab {

    private NinjaMenuJutsuTab() {}

    static void render(GuiGraphicsExtractor graphics, Font font, INinjaData ninjaData,
                       List<Ability> allAbilities, int jutsuPage, int jutsusPerPage,
                       int editingWheelSlot, int centerX, int boxY, int boxWidth) {
        int boxX = centerX - boxWidth / 2;

        // Top 8-slot Radial Wheel Loadout Bar container
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + 56, 0xCC111118);
        graphics.outline(boxX, boxY, boxWidth, 56, 0xFF444466);

        String hintStr = Component.translatable("naruto.gui.ninja_menu.jutsu.loadout_hint").getString();
        if (font.width(hintStr) > boxWidth - 8) {
            hintStr = font.plainSubstrByWidth(hintStr, boxWidth - 16) + "...";
        }
        graphics.centeredText(font, "§e" + hintStr, centerX, boxY + 2, 0xFFEEDD88);

        int safeSlot = Math.max(0, Math.min(INinjaData.MAX_JUTSU_SLOTS - 1, editingWheelSlot));
        Identifier slotAbilityId = ninjaData.getEquippedJutsu(safeSlot);
        Ability slotAbility = slotAbilityId != null ? NarutoAbilities.getAbility(slotAbilityId) : null;
        String slotPreview;
        if (slotAbility != null) {
            ChatFormatting slotElemColor = NinjaMenuRenderUtils.getElementColor(slotAbility.getChakraElement());
            String slotName = Component.translatable(slotAbility.getTranslationKey()).getString();
            slotPreview = "§6#" + (safeSlot + 1) + ": " + slotElemColor + slotName;
        } else {
            slotPreview = "§6#" + (safeSlot + 1) + ": §7" + Component.translatable("naruto.gui.jutsu_wheel.empty").getString();
        }
        int maxPreviewWidth = boxWidth - 80;
        if (font.width(slotPreview) > maxPreviewWidth) {
            slotPreview = font.plainSubstrByWidth(slotPreview, maxPreviewWidth - 8) + "...";
        }
        graphics.text(font, slotPreview, boxX + 8, boxY + 42, 0xFFFFFFFF);

        if (allAbilities.isEmpty()) {
            graphics.centeredText(font, "§7Henüz öğrenilmiş bir jutsu yok.", centerX, boxY + 95, 0xFFAAAAAA);
            return;
        }

        int startIndex = jutsuPage * jutsusPerPage;
        int endIndex = Math.min(startIndex + jutsusPerPage, allAbilities.size());

        int cardStartY = boxY + 60;
        int cardH = 40;
        int cardGap = 4;
        int maxContentWidth = boxWidth - 92; // Leave space on the right for the 72px Equip button

        for (int i = startIndex; i < endIndex; i++) {
            Ability ability = allAbilities.get(i);
            Identifier id = ability.getRegistryName();
            int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);

            int equippedSlot = -1;
            if (id != null) {
                for (int s = 0; s < INinjaData.MAX_JUTSU_SLOTS; s++) {
                    if (id.equals(ninjaData.getEquippedJutsu(s))) {
                        equippedSlot = s;
                        break;
                    }
                }
            }

            int bgColor = equippedSlot == safeSlot ? 0xCC162D16 : (equippedSlot >= 0 ? 0xCC122814 : 0xCC112211);
            int borderColor = equippedSlot == safeSlot ? 0xFFFFAA00 : (equippedSlot >= 0 ? 0xFF33AA33 : 0xFF228822);
            graphics.fill(boxX, cardY, boxX + boxWidth, cardY + cardH, bgColor);
            graphics.outline(boxX, cardY, boxWidth, cardH, borderColor);

            // Row 1: Jutsu name + ChakraElement color tag
            ChakraElement element = ability.getChakraElement();
            ChatFormatting elemColor = NinjaMenuRenderUtils.getElementColor(element);
            String elemTag = elemColor + "[" + Component.translatable("naruto.element." + element.getName()).getString() + "]";
            int elemTagWidth = font.width(elemTag);
            String name = Component.translatable(ability.getTranslationKey()).getString();
            int maxNameWidth = Math.max(40, maxContentWidth - elemTagWidth - 6);
            String displayName = font.width(name) > maxNameWidth ? font.plainSubstrByWidth(name, maxNameWidth - 8) + "..." : name;
            String titleText = "§f§l" + displayName;
            graphics.text(font, titleText, boxX + 8, cardY + 4, 0xFFFFFFFF);
            graphics.text(font, elemTag, boxX + 8 + font.width(titleText) + 5, cardY + 4, 0xFFFFFFFF);

            // Row 2: Chakra cost + Cooldown summary + Mastery level (Lv.X)
            String costStr = NinjaMenuRenderUtils.formatStatNumber(ability.getChakraCost());
            String cdStr = String.format(Locale.ROOT, "%.1fs", ability.getCooldown() / 20.0f);
            String summaryText = "§bChakra: §f" + costStr + "  §eCD: §f" + cdStr;

            int mastery = ninjaData.getMastery(id);
            String masteryText;
            if (id != null && "sharingan".equals(id.getPath())) {
                SharinganAbility.SharinganStage stage = SharinganAbility.getStage(mastery);
                masteryText = "§aLv." + Ability.getMasteryTier(mastery) + " §7(" + Component.translatable(stage.getTranslationKey()).getString() + ")";
            } else if (id != null && "eight_gates".equals(id.getPath())) {
                EightGatesAbility.GateStage gstage = EightGatesAbility.getStage(mastery);
                masteryText = "§aLv." + Ability.getMasteryTier(mastery) + " §7(" + Component.translatable(gstage.getTranslationKey()).getString() + ")";
            } else {
                masteryText = "§aLv." + Ability.getMasteryTier(mastery) + " §7(" + mastery + ")";
            }
            int maxMasteryWidth = Math.max(36, maxContentWidth - font.width(summaryText) - 8);
            if (font.width(masteryText) > maxMasteryWidth) {
                masteryText = "§aLv." + Ability.getMasteryTier(mastery);
            }
            int masteryWidth = font.width(masteryText);
            graphics.text(font, summaryText, boxX + 8, cardY + 16, 0xFFDDDDDD);
            graphics.text(font, masteryText, boxX + boxWidth - 84 - masteryWidth, cardY + 16, 0xFFFFFF88);

            // Row 3: Requirement badge
            String reqBadge = resolveRequirementBadge(ability, id);
            if (font.width(reqBadge) > maxContentWidth) {
                reqBadge = font.plainSubstrByWidth(reqBadge, maxContentWidth - 8) + "...";
            }
            graphics.text(font, reqBadge, boxX + 8, cardY + 28, 0xFFAAAAAA);
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
            if (path.equals("amaterasu") || path.equals("susanoo_ribcage")) return "§c[Uchiha Par\u015f\u00f6meni - J\u014dnin]";
            if (path.equals("eight_gates_death")) return "§4[Nihai Yasak Par\u015f\u00f6men - J\u014dnin]";
            if (path.equals("eight_gates")) return "§4[Yasak Par\u015f\u00f6men - J\u014dnin]";
            if (path.equals("rasenshuriken")) return "§b[S-Seviye Par\u015f\u00f6men - J\u014dnin]";
            if (path.equals("sharingan")) return "§c[Uchiha Klan\u0131 - Genin]";
            if (path.equals("byakugan")) return "§f[Hy\u016bga Klan\u0131 - Genin]";
            if (path.equals("wood_prison")) return "§2[Senju Klan\u0131 - Genin]";
            if (path.equals("bone_dance")) return "§f[Kaguya Klan\u0131 - Genin]";
            if (path.equals("shadow_possession")) return "§8[Nara Klan\u0131 - Genin / Par\u015f\u00f6men]";
            if (path.equals("adamantine_chains")) return "§c[Uzumaki Klan\u0131 - Ch\u016bnin]";
        }
        if (NinjaData.isScrollRequiredJutsu(id)) return "§6[Par\u015f\u00f6men Gerekir]";
        if (ability.getRequiredRank() == NinjaRank.ACADEMY_STUDENT) return "§7[Temel Yetenek]";
        return "§a[R\u00fctbe: " + Component.translatable(ability.getRequiredRank().getTranslationKey()).getString() + "]";
    }
}
