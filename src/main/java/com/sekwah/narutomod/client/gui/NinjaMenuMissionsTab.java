package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaMission;
import com.sekwah.narutomod.capabilities.NinjaRank;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the Missions tab of {@link NinjaMenuScreen}.
 */
final class NinjaMenuMissionsTab {

    private NinjaMenuMissionsTab() {}

    static int[] render(GuiGraphicsExtractor graphics, Font font, INinjaData ninjaData,
                        List<Button> missionAcceptButtons, int missionScrollOffset,
                        int centerX, int boxY, int boxWidth, int boxHeight) {
        int boxX = centerX - boxWidth / 2;
        graphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0xCC11111A);
        graphics.outline(boxX, boxY, boxWidth, boxHeight, 0xFF555566);

        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        String headerInfo = "§6Rütbe: §f" + rank.getEnglishName()
                + "   §eTamamlanan Görev: §a" + ninjaData.getCompletedMissionsCount();
        graphics.text(font, headerInfo, boxX + 12, boxY + 12, 0xFFFFFFFF);

        NinjaMission active = NinjaMission.fromId(ninjaData.getActiveMissionId());
        if (active != null) {
            int cardY = boxY + 30;
            int cardH = 128;
            boolean completed = active.isCompleted(ninjaData);
            int borderCol = completed ? 0xFF33AA33 : 0xFFCC9922;
            graphics.fill(boxX + 10, cardY, boxX + boxWidth - 10, cardY + cardH, 0xCC181824);
            graphics.outline(boxX + 10, cardY, boxWidth - 20, cardH, borderCol);

            String title = "§6§l[" + active.getRankLabel() + "] §f" + active.getTitleComponent().getString();
            graphics.text(font, title, boxX + 20, cardY + 8, 0xFFFFFFFF);

            List<FormattedCharSequence> descLines = font.split(active.getDescriptionComponent(), boxWidth - 40);
            int descY = cardY + 21;
            for (int l = 0; l < Math.min(2, descLines.size()); l++) {
                graphics.text(font, descLines.get(l), boxX + 20, descY + l * 10, 0xFFBBBBCC);
            }

            int progress = active.getProgress(ninjaData);
            int target = active.getTargetCount();
            float ratio = target > 0 ? (float) progress / target : 1.0f;

            Component objectiveText = active.getObjectiveComponent(progress).copy()
                    .withStyle(completed ? ChatFormatting.GREEN : ChatFormatting.YELLOW);
            graphics.text(font, objectiveText, boxX + 20, cardY + 46, 0xFFFFFFFF);

            String progText = (completed ? "§a✔ Tamamlandı: " : "§7İlerleme: ") + progress + " / " + target
                    + "   §bÖdül: +" + active.getRewardXp() + " XP";
            graphics.text(font, progText, boxX + 20, cardY + 60, 0xFFDDDDDD);

            NinjaMenuRenderUtils.renderBar(graphics, boxX + 20, cardY + 75, boxWidth - 40, 10, ratio,
                    completed ? 0xFF33CC33 : 0xFF3399FF, 0xFF222233);
            return new int[]{missionScrollOffset, 0};
        }

        NinjaMission[] missions = NinjaMission.values();
        int viewX = boxX + 6;
        int viewY = boxY + 28;
        int viewW = boxWidth - 12;
        int viewH = boxHeight - 36;
        int cardW = viewW - 8;
        int textMaxW = cardW - 96;
        int cardGap = 4;

        int[] cardHeights = new int[missions.length];
        List<List<FormattedCharSequence>> wrappedDescs = new ArrayList<>(missions.length);
        List<List<FormattedCharSequence>> wrappedObjs = new ArrayList<>(missions.length);
        int totalContentH = 0;

        for (int i = 0; i < missions.length; i++) {
            NinjaMission m = missions[i];
            List<FormattedCharSequence> descLines = font.split(m.getDescriptionComponent(), textMaxW);
            List<FormattedCharSequence> objLines = font.split(m.getObjectiveComponent(0), textMaxW);
            wrappedDescs.add(descLines);
            wrappedObjs.add(objLines);
            int h = 6 + 11 + descLines.size() * 10 + objLines.size() * 10 + 11 + 5;
            cardHeights[i] = h;
            totalContentH += h + (i < missions.length - 1 ? cardGap : 0);
        }

        int maxScroll = Math.max(0, totalContentH - viewH);
        int clampedScroll = Math.max(0, Math.min(missionScrollOffset, maxScroll));

        graphics.enableScissor(viewX, viewY, viewX + viewW, viewY + viewH);

        int cursorY = viewY - clampedScroll;
        for (int i = 0; i < missions.length; i++) {
            NinjaMission m = missions[i];
            int cardH = cardHeights[i];
            int rowY = cursorY;
            boolean eligible = m.canAccept(rank);

            if (rowY + cardH >= viewY && rowY <= viewY + viewH) {
                graphics.fill(viewX + 2, rowY, viewX + 2 + cardW, rowY + cardH, 0xCC181822);
                graphics.outline(viewX + 2, rowY, cardW, cardH, eligible ? 0xFF447744 : 0xFF553333);

                int lineY = rowY + 5;
                String mName = (eligible ? "§6[" + m.getRankLabel() + "] §f" : "§8[" + m.getRankLabel() + "] ")
                        + m.getTitleComponent().getString();
                graphics.text(font, mName, viewX + 8, lineY, 0xFFFFFFFF);
                lineY += 11;

                for (FormattedCharSequence line : wrappedDescs.get(i)) {
                    graphics.text(font, line, viewX + 8, lineY, 0xFF9999AA);
                    lineY += 10;
                }

                for (FormattedCharSequence line : wrappedObjs.get(i)) {
                    graphics.text(font, line, viewX + 8, lineY, eligible ? 0xFFEEDD77 : 0xFF888899);
                    lineY += 10;
                }

                String mMeta = "§bÖdül: +" + m.getRewardXp() + " XP"
                        + (eligible ? "" : "  §c(Gerekli: " + m.getRequiredRank().getEnglishName() + ")");
                graphics.text(font, mMeta, viewX + 8, lineY + 1, 0xFFAAAAAA);
            }

            if (i < missionAcceptButtons.size()) {
                Button acceptBtn = missionAcceptButtons.get(i);
                int btnY = rowY + (cardH - 20) / 2;
                acceptBtn.setX(viewX + cardW - 84);
                acceptBtn.setY(btnY);
                boolean inViewport = btnY >= viewY && (btnY + 20) <= (viewY + viewH);
                acceptBtn.visible = inViewport;
                acceptBtn.active = eligible && inViewport;
            }

            cursorY += cardH + cardGap;
        }

        graphics.disableScissor();

        if (maxScroll > 0) {
            int barX = viewX + viewW - 4;
            graphics.fill(barX, viewY, barX + 3, viewY + viewH, 0x55333344);
            int thumbH = Math.max(18, (int) ((float) viewH * viewH / totalContentH));
            int thumbY = viewY + (int) ((float) clampedScroll / maxScroll * (viewH - thumbH));
            graphics.fill(barX, thumbY, barX + 3, thumbY + thumbH, 0xFFCC9922);
        }

        return new int[]{clampedScroll, maxScroll};
    }
}
