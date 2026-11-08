package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.network.PacketHandler;
import com.sekwah.narutomod.network.c2s.ServerRankUpPacket;
import com.sekwah.narutomod.network.c2s.ServerSelectClanPacket;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public class NinjaMenuScreen extends Screen {

    private int activeTab = 0; // 0: Profil, 1: Klan Seçimi, 2: Jutsu Kitabı
    private int jutsuPage = 0;
    private static final int JUTSUS_PER_PAGE = 4;

    private static final int BOX_WIDTH = 340;
    private static final int BOX_HEIGHT = 195;

    private final List<Ability> allAbilities = new ArrayList<>();

    public NinjaMenuScreen() {
        super(Component.translatable("naruto.gui.ninja_menu.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;
        int topY = 24;

        // Modern Tab Navigation Buttons
        int tabBtnW = 100;
        this.addRenderableWidget(Button.builder(Component.literal(this.activeTab == 0 ? "§6§l[ Profil ]" : "Profil"), b -> switchTab(0))
                .bounds(centerX - 155, topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(this.activeTab == 1 ? "§6§l[ Klan Seçimi ]" : "Klan Seçimi"), b -> switchTab(1))
                .bounds(centerX - 50, topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal(this.activeTab == 2 ? "§6§l[ Jutsu Kitabı ]" : "Jutsu Kitabı"), b -> switchTab(2))
                .bounds(centerX + 55, topY, tabBtnW, 20).build());

        // Cache abilities
        this.allAbilities.clear();
        NarutoAbilities.ABILITY.getEntries().forEach(entry -> {
            Ability ability = entry.get();
            if (ability.defaultCombo() > 0) {
                this.allAbilities.add(ability);
            }
        });

        int boxY = topY + 28;

        if (this.activeTab == 0) {
            initProfileTab(centerX, boxY);
        } else if (this.activeTab == 1) {
            initClanTab(centerX, boxY);
        } else if (this.activeTab == 2) {
            initJutsuTab(centerX, boxY);
        }
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        this.jutsuPage = 0;
        this.init();
    }

    private void initProfileTab(int centerX, int boxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
            if (rank.getLevel() < NinjaRank.JONIN.getLevel()) {
                boolean canRank = ninjaData.canRankUp();
                String btnText;
                if (canRank) {
                    NinjaRank next = rank == NinjaRank.ACADEMY_STUDENT ? NinjaRank.GENIN : (rank == NinjaRank.GENIN ? NinjaRank.CHUNIN : NinjaRank.JONIN);
                    btnText = "★ " + next.getTurkishName() + " Rütbesine Terfi Et! ★";
                } else {
                    btnText = "Sınav Şartları Henüz Karşılanmadı";
                }

                int btnY = boxY + BOX_HEIGHT - 24;

                Button rankBtn = Button.builder(Component.literal(btnText), b -> {
                    PacketHandler.sendToServer(new ServerRankUpPacket());
                    ninjaData.rankUp();
                    this.init();
                }).bounds(centerX - 120, btnY, 240, 20).build();

                rankBtn.active = canRank;
                this.addRenderableWidget(rankBtn);
            }
        });
    }

    private void initClanTab(int centerX, int boxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean isLocked = ninjaData.isClanLocked();

            String[] clans = {"uchiha", "hyuga", "uzumaki", "nara"};
            int cardStartY = boxY + 36;
            int cardH = 34;
            int cardGap = 4;
            int boxX = centerX - BOX_WIDTH / 2;

            for (int i = 0; i < clans.length; i++) {
                final String clanKey = clans[i];
                final ResourceLocation clanLoc = new ResourceLocation(NarutoMod.MOD_ID, clanKey);
                int cardY = cardStartY + i * (cardH + cardGap);
                int btnY = cardY + 7;

                boolean isCurrentClan = ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase(clanKey);

                String label;
                if (isCurrentClan) {
                    label = "§a✔ Klanınız";
                } else if (isLocked) {
                    label = "§8Kilitli";
                } else {
                    label = "§eKlanı Seç";
                }

                Button clanBtn = Button.builder(
                                Component.literal(label),
                                b -> {
                                    if (!ninjaData.isClanLocked()) {
                                        PacketHandler.sendToServer(new ServerSelectClanPacket(clanLoc));
                                        ninjaData.setClan(clanLoc);
                                        ninjaData.setClanLocked(true);
                                        this.init();
                                    }
                                })
                        .bounds(boxX + BOX_WIDTH - 82, btnY, 75, 20)
                        .build();

                clanBtn.active = !isLocked && !isCurrentClan;
                this.addRenderableWidget(clanBtn);
            }
        });
    }

    private void initJutsuTab(int centerX, int boxY) {
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));

        if (this.jutsuPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("< Önceki"), b -> {
                this.jutsuPage--;
                this.init();
            }).bounds(centerX - 150, this.height - 35, 75, 20).build());
        }

        if (this.jutsuPage < maxPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("Sonraki >"), b -> {
                this.jutsuPage++;
                this.init();
            }).bounds(centerX + 75, this.height - 35, 75, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int topY = 10;

        // Header Title
        graphics.drawCenteredString(this.font, Component.literal("★ SHINOBI GELİŞİM VE REHBER SİSTEMİ ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), centerX, topY, 0xFFFFAA00);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int boxY = 24 + 28;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (this.activeTab == 0) {
                renderProfileTab(graphics, ninjaData, centerX, boxY);
            } else if (this.activeTab == 1) {
                renderClanTab(graphics, ninjaData, centerX, boxY);
            } else if (this.activeTab == 2) {
                renderJutsuTab(graphics, ninjaData, centerX, boxY);
            }
        });

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderProfileTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        // Modern dark translucent card background
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xEE111116);
        graphics.renderOutline(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 0xFF555577);

        Minecraft mc = Minecraft.getInstance();
        String playerName = mc.player != null ? mc.player.getName().getString() : "Shinobi";

        int y = boxY + 8;
        int x = boxX + 12;
        int contentW = BOX_WIDTH - 24;

        // Header: Shinobi Name (Left) and Clan Lock Status (Right)
        graphics.drawString(this.font, "§6§lShinobi: §f" + playerName, x, y, 0xFFFFFFFF);

        String clanPath = ninjaData.getClan() != null ? ninjaData.getClan().getPath() : "shinobi";
        String clanDisplay = clanPath.substring(0, 1).toUpperCase() + clanPath.substring(1) + " Klanı";
        String lockBadge = ninjaData.isClanLocked() ? "§c[Kilitli]" : "§a[Seçilmedi]";
        String clanStatusStr = "§eKlan: §f" + clanDisplay + " " + lockBadge;
        graphics.drawString(this.font, clanStatusStr, boxX + BOX_WIDTH - 12 - this.font.width(clanStatusStr), y, 0xFFFFFFFF);
        y += 13;

        // Ninja Rank
        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        graphics.drawString(this.font, "§bNinja Rütbesi: §f" + rank.getTurkishName() + " (" + rank.getEnglishName() + ")", x, y, 0xFFFFFFFF);
        y += 14;

        // 1. Stat Bar: Chakra
        float chakra = ninjaData.getChakra();
        float maxChakra = ninjaData.getMaxChakra();
        float chakraPct = maxChakra > 0 ? (chakra / maxChakra) * 100f : 0f;
        String chakraValStr = String.format("§f%.0f / %.0f §7(%%%.0f)", chakra, maxChakra, chakraPct);
        graphics.drawString(this.font, "§9Çakra", x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, chakraValStr, boxX + BOX_WIDTH - 12 - this.font.width(chakraValStr), y, 0xFFFFFFFF);
        y += 10;
        renderBar(graphics, x, y, contentW, 5, chakra / maxChakra, 0xFF2255FF, 0xFF001144);
        y += 8;

        // 2. Stat Bar: Stamina
        float stamina = ninjaData.getStamina();
        float maxStamina = ninjaData.getMaxStamina();
        float staminaPct = maxStamina > 0 ? (stamina / maxStamina) * 100f : 0f;
        String staminaValStr = String.format("§f%.0f / %.0f §7(%%%.0f)", stamina, maxStamina, staminaPct);
        graphics.drawString(this.font, "§aKondisyon", x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, staminaValStr, boxX + BOX_WIDTH - 12 - this.font.width(staminaValStr), y, 0xFFFFFFFF);
        y += 10;
        renderBar(graphics, x, y, contentW, 5, stamina / maxStamina, 0xFF22DD44, 0xFF003311);
        y += 8;

        // 3. Stat Bar: Chakra EXP
        float chakraExp = ninjaData.getChakraExp();
        float neededExp = 100f + (maxChakra - 100f) * 3.5f;
        float expPct = neededExp > 0 ? Math.min(100f, (chakraExp / neededExp) * 100f) : 0f;
        String expValStr = String.format("§f%.0f / %.0f §7(%%%.0f)", chakraExp, neededExp, expPct);
        graphics.drawString(this.font, "§dÇakra Eğitimi", x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, expValStr, boxX + BOX_WIDTH - 12 - this.font.width(expValStr), y, 0xFFFFFFFF);
        y += 10;
        renderBar(graphics, x, y, contentW, 5, chakraExp / neededExp, 0xFFAA22EE, 0xFF330044);
        y += 11;

        // Exam & Progression Card
        int examBoxY = y;
        int examBoxH = 46;

        if (rank == NinjaRank.ACADEMY_STUDENT) {
            boolean reqC = maxChakra >= 100;
            boolean reqJ = ninjaData.getJutsusCastCount() >= 10;
            boolean ready = reqC && reqJ;

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Mezuniyet Sınavı Şartları (Akademi ➔ Genin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = reqC ? "§a✔ Çakra: 100 / 100" : "§c✖ Çakra: " + (int)Math.min(maxChakra, 100) + " / 100";
            String jText = reqJ ? "§a✔ Jutsu Pratiği: 10 / 10" : "§c✖ Jutsu Pratiği: " + Math.min(ninjaData.getJutsusCastCount(), 10) + " / 10";
            graphics.drawString(this.font, cText + "     " + jText, x + 6, examBoxY + 18, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Sınav şartları hazır! Aşağıdan terfi edebilirsiniz." : "§7Temel jutsuları çalışarak çakranızı geliştirin.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 31, 0xFFAAAAAA);

        } else if (rank == NinjaRank.GENIN) {
            boolean reqC = maxChakra >= 200;
            boolean reqK = ninjaData.getEnemiesDefeated() >= 25;
            boolean ready = reqC && reqK;

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Chūnin Sınavı Şartları (Genin ➔ Chūnin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = reqC ? "§a✔ Çakra: 200 / 200" : "§c✖ Çakra: " + (int)Math.min(maxChakra, 200) + " / 200";
            String kText = reqK ? "§a✔ Düşman: 25 / 25" : "§c✖ Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), 25) + " / 25";
            graphics.drawString(this.font, cText + "     " + kText, x + 6, examBoxY + 18, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Sınav şartları hazır! Aşağıdan terfi edebilirsiniz." : "§7Düşmanları yenin ve çakra kapasitenizi yükseltin.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 31, 0xFFAAAAAA);

        } else if (rank == NinjaRank.CHUNIN) {
            boolean reqC = maxChakra >= 350;
            boolean reqK = ninjaData.getEnemiesDefeated() >= 75;
            int unlocked = ninjaData.getUnlockedAbilities().size();
            boolean reqS = unlocked >= 4;
            boolean ready = reqC && reqK && reqS;

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Jōnin Atama Şartları (Chūnin ➔ Jōnin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = reqC ? "§a✔ Çakra: 350/350" : "§c✖ Çakra: " + (int)Math.min(maxChakra, 350) + "/350";
            String kText = reqK ? "§a✔ Düşman: 75/75" : "§c✖ Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), 75) + "/75";
            String sText = reqS ? "§a✔ Parşömen: 4/4" : "§c✖ Parşömen: " + Math.min(unlocked, 4) + "/4";
            graphics.drawString(this.font, cText + "  |  " + kText + "  |  " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Konsey ataması hazır! Aşağıdan Jōnin rütbesine terfi edin." : "§7Gizli parşömenleri öğrenip elit seviyeye ulaşın.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 31, 0xFFAAAAAA);

        } else {
            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, 0x33FFAA00);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, 0xFFFFAA00);
            graphics.drawString(this.font, "§6★ Usta Shinobi - Köyün Koruyucusu (Jōnin Rütbesi) ★", x + 6, examBoxY + 10, 0xFFFFAA00);
            graphics.drawString(this.font, "§aEn yüksek ninja rütbesine ulaştınız. Tekniklerinizde ustalaşmaya devam edin!", x + 6, examBoxY + 26, 0xFF55FF55);
        }
    }

    private void renderClanTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        // Warning banner (exact box width, clean centered text)
        int warnH = 28;
        if (ninjaData.isClanLocked()) {
            graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + warnH, 0xEE441111);
            graphics.renderOutline(boxX, boxY, BOX_WIDTH, warnH, 0xFFFF2222);
            graphics.drawCenteredString(this.font, "§c§lKLANINIZ KALICI OLARAK KİLİTLENDİ", centerX, boxY + 4, 0xFFFF5555);
            graphics.drawCenteredString(this.font, "§7Klana dahil olundu; klan üyeliğiniz artık değiştirilemez.", centerX, boxY + 15, 0xFFAAAAAA);
        } else {
            graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + warnH, 0xEE443311);
            graphics.renderOutline(boxX, boxY, BOX_WIDTH, warnH, 0xFFFFAA00);
            graphics.drawCenteredString(this.font, "§e§lDİKKAT: BİR KEZ SEÇİLEN KLAN DEĞİŞTİRİLEMEZ!", centerX, boxY + 4, 0xFFFFAA00);
            graphics.drawCenteredString(this.font, "§fKlanınızı dikkatli seçiniz; seçim kalıcı olarak mühürlenir.", centerX, boxY + 15, 0xFFFFFFFF);
        }

        String[] clans = {"uchiha", "hyuga", "uzumaki", "nara"};
        String[] titles = {
                "Uchiha Klanı (Ateş & Sharingan)",
                "Hyūga Klanı (Nazik Yumruk & Byakugan)",
                "Uzumaki Klanı (Devasa Çakra Rezervi)",
                "Nara Klanı (Gölge & Hızlı Kondisyon)"
        };
        String[] descs = {
                "Ateş teknikleri, Sharingan ve Susanoo kalkanı.",
                "360° Byakugan görüşü ve çakra darbe sanatı.",
                "Büyük çakra havuzu ve yüksek doğal yenilenme.",
                "Gölge teknikleri ve %30 daha hızlı kondisyon."
        };

        int cardStartY = boxY + 36;
        int cardH = 34;
        int cardGap = 4;
        int maxTextW = BOX_WIDTH - 96; // Space before the [Klanı Seç] button

        for (int i = 0; i < clans.length; i++) {
            boolean isCurrent = ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase(clans[i]);
            int bgCol = isCurrent ? 0xCC113311 : 0xCC111116;
            int borderCol = isCurrent ? 0xFF22AA22 : 0xFF444455;

            int cardY = cardStartY + i * (cardH + cardGap);

            graphics.fill(boxX, cardY, boxX + BOX_WIDTH, cardY + cardH, bgCol);
            graphics.renderOutline(boxX, cardY, BOX_WIDTH, cardH, borderCol);

            graphics.drawString(this.font, "§6" + titles[i], boxX + 8, cardY + 5, 0xFFFFFFFF);

            String desc = descs[i];
            if (this.font.width(desc) > maxTextW) {
                desc = this.font.plainSubstrByWidth(desc, maxTextW - 8) + "...";
            }
            graphics.drawString(this.font, "§7" + desc, boxX + 8, cardY + 18, 0xFFAAAAAA);
        }
    }

    private void renderJutsuTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        int startIndex = jutsuPage * JUTSUS_PER_PAGE;
        int endIndex = Math.min(startIndex + JUTSUS_PER_PAGE, allAbilities.size());
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));

        graphics.drawCenteredString(this.font, String.format("§eSayfa %d / %d", jutsuPage + 1, maxPages), centerX, boxY + 2, 0xFFFFFF88);

        int cardStartY = boxY + 16;
        int cardH = 36;
        int cardGap = 4;

        for (int i = startIndex; i < endIndex; i++) {
            Ability ability = allAbilities.get(i);
            boolean unlocked = ninjaData.isAbilityUnlocked(ability);
            ResourceLocation id = ability.getRegistryName();

            int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);

            int bgCol = unlocked ? 0xCC112211 : 0xCC221111;
            int borderCol = unlocked ? 0xFF228822 : 0xFF882222;

            graphics.fill(boxX, cardY, boxX + BOX_WIDTH, cardY + cardH, bgCol);
            graphics.renderOutline(boxX, cardY, BOX_WIDTH, cardH, borderCol);

            // Ability Name
            String name = Component.translatable(ability.getTranslationKey()).getString();
            graphics.drawString(this.font, "§f§l" + name, boxX + 8, cardY + 4, 0xFFFFFFFF);

            // Combo & Type Info
            String comboText = formatCombo(ability.defaultCombo());
            graphics.drawString(this.font, "§eMühür: §b" + comboText, boxX + 8, cardY + 15, 0xFFDDDDDD);

            // Acquisition Type
            String reqType;
            if (id != null && (id.getPath().equals("amaterasu") || id.getPath().equals("susanoo_ribcage"))) {
                reqType = "§c[Uchiha Parşömeni - Jōnin]";
            } else if (id != null && id.getPath().equals("eight_gates")) {
                reqType = "§4[Yasak Parşömen - Jōnin]";
            } else if (id != null && id.getPath().equals("sharingan")) {
                reqType = "§c[Uchiha Doğuştan]";
            } else if (id != null && id.getPath().equals("byakugan")) {
                reqType = "§f[Hyūga Doğuştan]";
            } else if (NinjaData.isScrollRequiredJutsu(id)) {
                reqType = "§6[Parşömen Gerekir]";
            } else if (ability.getRequiredRank() == NinjaRank.ACADEMY_STUDENT) {
                reqType = "§7[Temel Yetenek]";
            } else {
                reqType = "§a[Rütbe: " + ability.getRequiredRank().getTurkishName() + "]";
            }
            graphics.drawString(this.font, reqType, boxX + 8, cardY + 25, 0xFFAAAAAA);

            // Status label (aligned right)
            String status = unlocked ? "§a✔ ÖĞRENİLDİ" : "§c✖ KİLİTLİ";
            graphics.drawString(this.font, status, boxX + BOX_WIDTH - this.font.width(status) - 10, cardY + 13, 0xFFFFFFFF);
        }
    }

    private String formatCombo(long combo) {
        String s = String.valueOf(combo);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '1') sb.append("C");
            else if (c == '2') sb.append("V");
            else if (c == '3') sb.append("B");
            else sb.append(c);

            if (i < s.length() - 1) {
                sb.append(" - ");
            }
        }
        return sb.toString();
    }

    private void renderBar(GuiGraphics graphics, int x, int y, int width, int height, float ratio, int fillCol, int bgCol) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        graphics.fill(x, y, x + width, y + height, bgCol);
        graphics.fill(x, y, x + (int) (width * ratio), y + height, fillCol);
        graphics.renderOutline(x, y, width, height, 0xFF555566);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
