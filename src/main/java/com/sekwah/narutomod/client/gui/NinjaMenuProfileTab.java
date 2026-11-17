package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.clans.NarutoClans;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.Locale;

/**
 * Renders the Profile &amp; Rank Progression tab of {@link NinjaMenuScreen}.
 */
final class NinjaMenuProfileTab {

    private NinjaMenuProfileTab() {}

    static void render(GuiGraphicsExtractor graphics, Font font, INinjaData ninjaData,
                       int centerX, int boxY, int boxWidth, int boxHeight) {
        int boxX = centerX - boxWidth / 2;

        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xEE111116);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, 0xFF555577);

        Minecraft mc = Minecraft.getInstance();
        String playerName = mc.player != null ? mc.player.getName().getString() : "Shinobi";

        int y = boxY + 8;
        int x = boxX + 12;
        int contentW = boxWidth - 24;

        Identifier clanLoc = ninjaData.getClan();
        String clanStatusStr;
        if (clanLoc == null || clanLoc.getPath().equalsIgnoreCase("shinobi")) {
            clanStatusStr = Component.translatable("naruto.gui.ninja_menu.profile.clan_label",
                    Component.translatable("naruto.gui.ninja_menu.clan.unselected").getString()).getString();
        } else {
            Clan clan = NarutoClans.getClan(clanLoc);
            String clanDisplay = clan != null ? clan.getName().getString()
                    : (clanLoc.getPath().substring(0, 1).toUpperCase(Locale.ROOT) + clanLoc.getPath().substring(1));
            String lockBadge = Component.translatable(ninjaData.isClanLocked()
                    ? "naruto.gui.ninja_menu.profile.sealed"
                    : "naruto.gui.ninja_menu.profile.changeable").getString();
            clanStatusStr = Component.translatable("naruto.gui.ninja_menu.profile.clan_label",
                    clanDisplay + " " + lockBadge).getString();
        }

        int clanStrW = font.width(clanStatusStr);
        int maxNameW = contentW - clanStrW - 10;
        String nameStr = "§6§lShinobi: §f" + playerName;
        if (font.width(nameStr) > maxNameW) {
            nameStr = font.plainSubstrByWidth(nameStr, maxNameW - 6) + "...";
        }
        graphics.text(font, nameStr, x, y, 0xFFFFFFFF);
        graphics.text(font, clanStatusStr, boxX + boxWidth - 12 - clanStrW, y, 0xFFFFFFFF);
        y += 13;

        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        float cBonus = rank.getChakraBonus();
        float sBonus = rank.getStaminaBonus();
        String bonusNote = (cBonus > 0 || sBonus > 0)
                ? " " + Component.translatable("naruto.gui.ninja_menu.profile.rank_bonus", (int) cBonus, (int) sBonus).getString()
                : "";
        graphics.text(font, Component.translatable("naruto.gui.ninja_menu.profile.rank_label",
                Component.translatable(rank.getTranslationKey())).append(Component.literal(bonusNote)), x, y, 0xFFFFFFFF);
        y += 14;

        float chakra = ninjaData.getChakra();
        float maxChakra = ninjaData.getMaxChakra();
        float baseChakra = ninjaData.getBaseMaxChakra();
        String chakraValStr = Component.translatable("naruto.gui.ninja_menu.profile.stat_value",
                String.format("%.0f", chakra), String.format("%.0f", maxChakra), String.format("%.0f", baseChakra)).getString();
        graphics.text(font, Component.translatable("naruto.gui.ninja_menu.profile.chakra"), x, y, 0xFFFFFFFF);
        graphics.text(font, chakraValStr, boxX + boxWidth - 12 - font.width(chakraValStr), y, 0xFFFFFFFF);
        y += 10;
        NinjaMenuRenderUtils.renderBar(graphics, x, y, contentW, 5, maxChakra > 0 ? chakra / maxChakra : 0f, 0xFF2255FF, 0xFF001144);
        y += 8;

        float stamina = ninjaData.getStamina();
        float maxStamina = ninjaData.getMaxStamina();
        float baseStamina = ninjaData.getBaseMaxStamina();
        String staminaValStr = Component.translatable("naruto.gui.ninja_menu.profile.stat_value",
                String.format("%.0f", stamina), String.format("%.0f", maxStamina), String.format("%.0f", baseStamina)).getString();
        graphics.text(font, Component.translatable("naruto.gui.ninja_menu.profile.stamina"), x, y, 0xFFFFFFFF);
        graphics.text(font, staminaValStr, boxX + boxWidth - 12 - font.width(staminaValStr), y, 0xFFFFFFFF);
        y += 10;
        NinjaMenuRenderUtils.renderBar(graphics, x, y, contentW, 5, maxStamina > 0 ? stamina / maxStamina : 0f, 0xFF22DD44, 0xFF003311);
        y += 8;

        int halfW = (contentW - 10) / 2;
        int rightColX = x + halfW + 10;

        float chakraExp = ninjaData.getChakraExp();
        float cNeededExp = baseChakra < 150.0f ? (30.0f + (baseChakra - 100.0f) * 0.50f) : (55.0f + (baseChakra - 150.0f) * 1.10f);
        String cExpVal = baseChakra >= 1000.0f ? "§aMAKS (1000)" : String.format("§f%.0f / %.0f", chakraExp, cNeededExp);
        graphics.text(font, "§dÇakra Eğitimi", x, y, 0xFFFFFFFF);
        graphics.text(font, cExpVal, x + halfW - font.width(cExpVal), y, 0xFFFFFFFF);

        float staminaExp = ninjaData.getStaminaExp();
        float sNeededExp = 50.0f + (baseStamina - 100.0f) * 1.10f;
        String sExpVal = baseStamina >= 800.0f ? "§aMAKS (800)" : String.format("§f%.0f / %.0f", staminaExp, sNeededExp);
        graphics.text(font, "§2Kondisyon Eğitimi", rightColX, y, 0xFFFFFFFF);
        graphics.text(font, sExpVal, rightColX + halfW - font.width(sExpVal), y, 0xFFFFFFFF);
        y += 10;

        NinjaMenuRenderUtils.renderBar(graphics, x, y, halfW, 5,
                baseChakra >= 1000f ? 1f : (cNeededExp > 0 ? chakraExp / cNeededExp : 0f), 0xFFAA22EE, 0xFF330044);
        NinjaMenuRenderUtils.renderBar(graphics, rightColX, y, halfW, 5,
                baseStamina >= 800f ? 1f : (sNeededExp > 0 ? staminaExp / sNeededExp : 0f), 0xFF22AA66, 0xFF003318);
        y += 11;

        renderExamCard(graphics, font, ninjaData, rank, baseChakra, baseStamina, x, y, contentW);
    }

    private static void renderExamCard(GuiGraphicsExtractor graphics, Font font, INinjaData ninjaData,
                                       NinjaRank rank, float baseChakra, float baseStamina,
                                       int x, int examBoxY, int contentW) {
        int examBoxH = (rank == NinjaRank.KAGE) ? 68 : 56;
        boolean ready = ninjaData.canRankUp();

        if (rank == NinjaRank.KAGE) {
            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, 0x33FFAA00);
            graphics.outline(x, examBoxY, contentW, examBoxH, 0xFFFFAA00);
            graphics.text(font, "§6★ En Yüksek Rütbe – Kage ★", x + 6, examBoxY + 6, 0xFFFFAA00);
            NinjaMenuRenderUtils.drawWrapped(font, graphics,
                    Component.translatable("naruto.gui.ninja_menu.kage_congrats").copy().withStyle(ChatFormatting.GREEN),
                    x + 6, examBoxY + 20, contentW - 12, 0xFFFFFFFF);
            return;
        }

        graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
        graphics.outline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

        if (rank == NinjaRank.ACADEMY_STUDENT) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needJ = rank.getRequiredJutsusToAdvance();
            boolean reqC = baseChakra >= needC;
            boolean reqJ = ninjaData.getJutsusCastCount() >= needJ;

            graphics.text(font, "§e★ Mezuniyet Sınavı (Akademi ➔ Genin)", x + 6, examBoxY + 5, 0xFFFFAA00);
            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Taban Çakra: " + (int) Math.min(baseChakra, needC) + " / " + needC;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Jutsu Pratiği: " + Math.min(ninjaData.getJutsusCastCount(), needJ) + " / " + needJ;
            graphics.text(font, cText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(font, jText, x + 160, examBoxY + 18, 0xFFFFFFFF);
            String hint = ready ? "§a✔ Sınav şartları hazır! Aşağıdan terfi edebilirsiniz."
                    : "§7Meditasyon yaparak çakranızı geliştirin ve jutsuları çalışın.";
            graphics.text(font, hint, x + 6, examBoxY + 36, 0xFFAAAAAA);
        } else if (rank == NinjaRank.GENIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int rogueKills = ninjaData.getRogueNinjasDefeated();
            int masteredCount = NinjaRank.countMasteredJutsus(ninjaData, 50);

            graphics.text(font, "§e★ Chūnin Sınavı (Genin ➔ Chūnin)", x + 6, examBoxY + 5, 0xFFFFAA00);
            String cText = (baseChakra >= needC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int) Math.min(baseChakra, needC) + "/" + needC;
            String sText = (baseStamina >= needS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int) Math.min(baseStamina, needS) + "/" + needS;
            String rText = (rogueKills >= 10 ? "§a✔ " : "§c✖ ") + "Kaçak Ninja: " + Math.min(rogueKills, 10) + "/10";
            String mText = (masteredCount >= 3 ? "§a✔ " : "§c✖ ") + "Usta Jutsu: " + Math.min(masteredCount, 3) + "/3";
            graphics.text(font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(font, rText + "   " + mText, x + 6, examBoxY + 30, 0xFFFFFFFF);
            String hint = ready ? "§a✔ Chūnin terfisi hazır! Aşağıdaki butona tıklayın."
                    : "§7Kaçak ninjaları avlayın ve en az 3 jutsuda 50+ ustalığa ulaşın.";
            graphics.text(font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);
        } else if (rank == NinjaRank.CHUNIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int akatsukiKills = ninjaData.getAkatsukiNinjasDefeated();
            boolean reqAdv = NinjaRank.hasMasteredAdvancedJutsu(ninjaData, 50);

            graphics.text(font, "§e★ Jōnin Değerlendirmesi (Chūnin ➔ Jōnin)", x + 6, examBoxY + 5, 0xFFFFAA00);
            String cText = (baseChakra >= needC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int) Math.min(baseChakra, needC) + "/" + needC;
            String sText = (baseStamina >= needS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int) Math.min(baseStamina, needS) + "/" + needS;
            String aText = (akatsukiKills >= 1 ? "§a✔ " : "§c✖ ") + "Akatsuki: " + Math.min(akatsukiKills, 1) + "/1";
            String advText = (reqAdv ? "§a✔ " : "§c✖ ") + "İleri Jutsu: " + (reqAdv ? "Hazır" : "Gerekiyor");
            graphics.text(font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(font, aText + "   " + advText, x + 6, examBoxY + 30, 0xFFFFFFFF);
            String hint = ready ? "§a✔ Jōnin ataması hazır! Aşağıdan rütbeye terfi edin."
                    : "§7Akatsuki üyesini yenin ve ileri seviye bir teknikte (Rasengan/Chidori) ustalaşın.";
            graphics.text(font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);
        } else {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needK = rank.getRequiredEnemiesToAdvance();
            int needJ = rank.getRequiredJutsusToAdvance();
            int unlocked = ninjaData.getUnlockedAbilities().size();
            String title = rank == NinjaRank.JONIN
                    ? "§e★ ANBU Özel Kuvvet Sınavı (Jōnin ➔ ANBU)"
                    : "§e★ Kage Seçim Adaylığı (ANBU ➔ Kage)";
            String readyHint = rank == NinjaRank.JONIN
                    ? "§a✔ Özel kuvvetler daveti onaylandı! Aşağıdan terfi edin."
                    : "§a✔ Köy Meclisi sizi Kage ilan etti! Liderliği devralın.";
            String pendingHint = rank == NinjaRank.JONIN
                    ? "§7Köyün seçkin gölge koruyucusu olmak için sınırlarınızı zorlayın."
                    : "§7Köyün en kudretli şinobisi olma unvanına ulaşmak üzeresiniz.";

            graphics.text(font, title, x + 6, examBoxY + 5, 0xFFFFAA00);
            String cText = (baseChakra >= needC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int) Math.min(baseChakra, needC) + "/" + needC;
            String sText = (baseStamina >= needS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int) Math.min(baseStamina, needS) + "/" + needS;
            String kText = (ninjaData.getEnemiesDefeated() >= needK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            String jText = (unlocked >= needJ ? "§a✔ " : "§c✖ ") + "Parşömen: " + Math.min(unlocked, needJ) + "/" + needJ;
            graphics.text(font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(font, kText + "   " + jText, x + 6, examBoxY + 30, 0xFFFFFFFF);
            graphics.text(font, ready ? readyHint : pendingHint, x + 6, examBoxY + 43, 0xFFAAAAAA);
        }
    }
}
