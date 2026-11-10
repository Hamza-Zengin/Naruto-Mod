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
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.clans.NarutoClans;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class NinjaMenuScreen extends Screen {

    private int activeTab = 0; // 0: Profil, 1: Klan Seçimi, 2: Jutsu Kitabı
    private int clanPage = 0;
    private static final int CLANS_PER_PAGE = 4;
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
        Component profileTitle = Component.translatable("naruto.gui.ninja_menu.tab.profile");
        Component clansTitle = Component.translatable("naruto.gui.ninja_menu.tab.clans");
        Component jutsusTitle = Component.translatable("naruto.gui.ninja_menu.tab.jutsus");

        Component tab0 = this.activeTab == 0 ? Component.literal("§6§l[ ").append(profileTitle).append(" ]") : profileTitle;
        Component tab1 = this.activeTab == 1 ? Component.literal("§6§l[ ").append(clansTitle).append(" ]") : clansTitle;
        Component tab2 = this.activeTab == 2 ? Component.literal("§6§l[ ").append(jutsusTitle).append(" ]") : jutsusTitle;

        this.addRenderableWidget(Button.builder(tab0, b -> switchTab(0))
                .bounds(centerX - 155, topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(tab1, b -> switchTab(1))
                .bounds(centerX - 50, topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(tab2, b -> switchTab(2))
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
        this.clanPage = 0;
        this.jutsuPage = 0;
        this.init();
    }

    private void initProfileTab(int centerX, int boxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
            if (rank.getLevel() < NinjaRank.KAGE.getLevel()) {
                boolean canRank = ninjaData.canRankUp();
                NinjaRank next = rank.next();
                Component btnText;
                if (canRank && next != null) {
                    btnText = Component.translatable("naruto.gui.ninja_menu.btn.rank_up", Component.translatable(next.getTranslationKey()));
                } else {
                    btnText = Component.translatable("naruto.gui.ninja_menu.btn.not_ready");
                }

                int btnY = boxY + BOX_HEIGHT - 24;

                Button rankBtn = Button.builder(btnText, b -> {
                    PacketHandler.sendToServer(new ServerRankUpPacket());
                    ninjaData.rankUp();
                    this.init();
                }).bounds(centerX - 120, btnY, 240, 20).build();

                rankBtn.active = canRank;
                this.addRenderableWidget(rankBtn);
            }
        });
    }

    private List<RegistryObject<Clan>> getSelectableClans() {
        List<RegistryObject<Clan>> list = new ArrayList<>();
        for (RegistryObject<Clan> entry : NarutoClans.CLANS.getEntries()) {
            if (!entry.getId().getPath().equalsIgnoreCase("shinobi")) {
                list.add(entry);
            }
        }
        return list;
    }

    private void initClanTab(int centerX, int boxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean isLocked = ninjaData.isClanLocked();

            List<RegistryObject<Clan>> clans = getSelectableClans();
            int maxPages = Math.max(1, (int) Math.ceil((double) clans.size() / CLANS_PER_PAGE));
            if (this.clanPage >= maxPages) this.clanPage = maxPages - 1;

            int startIndex = this.clanPage * CLANS_PER_PAGE;
            int endIndex = Math.min(startIndex + CLANS_PER_PAGE, clans.size());

            int cardStartY = boxY + 32;
            int cardH = 38;
            int cardGap = 3;
            int boxX = centerX - BOX_WIDTH / 2;

            for (int i = startIndex; i < endIndex; i++) {
                final RegistryObject<Clan> clanEntry = clans.get(i);
                final ResourceLocation clanLoc = clanEntry.getId();
                int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);
                int btnY = cardY + 9;

                boolean isCurrentClan = ninjaData.getClan() != null && ninjaData.getClan().equals(clanLoc);

                Component label;
                if (isCurrentClan) {
                    label = Component.literal("§a").append(Component.translatable("naruto.gui.ninja_menu.clan.current"));
                } else if (isLocked) {
                    label = Component.literal("§8").append(Component.translatable("naruto.gui.ninja_menu.clan.locked"));
                } else {
                    label = Component.literal("§e").append(Component.translatable("naruto.gui.ninja_menu.clan.select"));
                }

                Button clanBtn = Button.builder(
                                label,
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

            int navBtnY = Math.min(boxY + BOX_HEIGHT + 4, this.height - 24);
            if (this.clanPage > 0) {
                this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.ninja_menu.btn.previous"), b -> {
                    this.clanPage--;
                    this.init();
                }).bounds(centerX - 150, navBtnY, 75, 20).build());
            }
            if (this.clanPage < maxPages - 1) {
                this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.ninja_menu.btn.next"), b -> {
                    this.clanPage++;
                    this.init();
                }).bounds(centerX + 75, navBtnY, 75, 20).build());
            }
        });
    }

    private void initJutsuTab(int centerX, int boxY) {
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));
        int btnY = Math.min(boxY + BOX_HEIGHT + 4, this.height - 24);

        if (this.jutsuPage > 0) {
            this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.ninja_menu.btn.previous"), b -> {
                this.jutsuPage--;
                this.init();
            }).bounds(centerX - 150, btnY, 75, 20).build());
        }

        if (this.jutsuPage < maxPages - 1) {
            this.addRenderableWidget(Button.builder(Component.translatable("naruto.gui.ninja_menu.btn.next"), b -> {
                this.jutsuPage++;
                this.init();
            }).bounds(centerX + 75, btnY, 75, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int topY = 10;

        // Header Title
        graphics.drawCenteredString(this.font, Component.translatable("naruto.gui.ninja_menu.header").copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), centerX, topY, 0xFFFFAA00);

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

        // Clan Badge & Lock calculation
        ResourceLocation clanLoc = ninjaData.getClan();
        String clanStatusStr;
        if (clanLoc == null || clanLoc.getPath().equalsIgnoreCase("shinobi")) {
            clanStatusStr = "§eKlan: §7" + Component.translatable("naruto.gui.ninja_menu.clan.unselected").getString();
        } else {
            Clan clan = NarutoClans.getClan(clanLoc);
            String clanDisplay = clan != null ? clan.getName().getString() : (clanLoc.getPath().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + clanLoc.getPath().substring(1) + " Klanı");
            String lockBadge = ninjaData.isClanLocked() ? "§2[Mühürlü]" : "§e[Değiştirilebilir]";
            clanStatusStr = "§eKlan: §f" + clanDisplay + " " + lockBadge;
        }

        int clanStrW = this.font.width(clanStatusStr);
        int maxNameW = contentW - clanStrW - 10;
        String nameStr = "§6§lShinobi: §f" + playerName;
        if (this.font.width(nameStr) > maxNameW) {
            nameStr = this.font.plainSubstrByWidth(nameStr, maxNameW - 6) + "...";
        }
        graphics.drawString(this.font, nameStr, x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, clanStatusStr, boxX + BOX_WIDTH - 12 - clanStrW, y, 0xFFFFFFFF);
        y += 13;

        // Ninja Rank
        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        float cBonus = rank.getChakraBonus();
        float sBonus = rank.getStaminaBonus();
        String bonusNote = (cBonus > 0 || sBonus > 0) ? " §7(+" + (int)cBonus + " Çakra, +" + (int)sBonus + " Kondisyon)" : "";
        graphics.drawString(this.font, Component.literal("§bNinja Rütbesi: §f").append(Component.translatable(rank.getTranslationKey())).append(Component.literal(bonusNote)), x, y, 0xFFFFFFFF);
        y += 14;

        // 1. Stat Bar: Chakra
        float chakra = ninjaData.getChakra();
        float maxChakra = ninjaData.getMaxChakra();
        float baseChakra = ninjaData.getBaseMaxChakra();
        float chakraPct = maxChakra > 0 ? (chakra / maxChakra) * 100f : 0f;
        String chakraValStr = String.format("§f%.0f / %.0f §7(Taban: %.0f)", chakra, maxChakra, baseChakra);
        graphics.drawString(this.font, "§9Çakra", x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, chakraValStr, boxX + BOX_WIDTH - 12 - this.font.width(chakraValStr), y, 0xFFFFFFFF);
        y += 10;
        renderBar(graphics, x, y, contentW, 5, maxChakra > 0 ? chakra / maxChakra : 0f, 0xFF2255FF, 0xFF001144);
        y += 8;

        // 2. Stat Bar: Stamina
        float stamina = ninjaData.getStamina();
        float maxStamina = ninjaData.getMaxStamina();
        float baseStamina = ninjaData.getBaseMaxStamina();
        String staminaValStr = String.format("§f%.0f / %.0f §7(Taban: %.0f)", stamina, maxStamina, baseStamina);
        graphics.drawString(this.font, "§aKondisyon", x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, staminaValStr, boxX + BOX_WIDTH - 12 - this.font.width(staminaValStr), y, 0xFFFFFFFF);
        y += 10;
        renderBar(graphics, x, y, contentW, 5, maxStamina > 0 ? stamina / maxStamina : 0f, 0xFF22DD44, 0xFF003311);
        y += 8;

        // 3 & 4. Training EXP Bars (Side by Side)
        int halfW = (contentW - 10) / 2;
        int rightColX = x + halfW + 10;

        // Chakra EXP (Left)
        float chakraExp = ninjaData.getChakraExp();
        float cNeededExp = baseChakra < 150.0f ? (30.0f + (baseChakra - 100.0f) * 0.50f) : (55.0f + (baseChakra - 150.0f) * 1.10f);
        String cExpVal = baseChakra >= 500.0f ? "§aMAKS (500)" : String.format("§f%.0f / %.0f", chakraExp, cNeededExp);
        graphics.drawString(this.font, "§dÇakra Eğitimi", x, y, 0xFFFFFFFF);
        graphics.drawString(this.font, cExpVal, x + halfW - this.font.width(cExpVal), y, 0xFFFFFFFF);

        // Stamina EXP (Right)
        float staminaExp = ninjaData.getStaminaExp();
        float sNeededExp = 50.0f + (baseStamina - 100.0f) * 1.10f;
        String sExpVal = baseStamina >= 400.0f ? "§aMAKS (400)" : String.format("§f%.0f / %.0f", staminaExp, sNeededExp);
        graphics.drawString(this.font, "§2Kondisyon Eğitimi", rightColX, y, 0xFFFFFFFF);
        graphics.drawString(this.font, sExpVal, rightColX + halfW - this.font.width(sExpVal), y, 0xFFFFFFFF);
        y += 10;

        renderBar(graphics, x, y, halfW, 5, baseChakra >= 500f ? 1f : (cNeededExp > 0 ? chakraExp / cNeededExp : 0f), 0xFFAA22EE, 0xFF330044);
        renderBar(graphics, rightColX, y, halfW, 5, baseStamina >= 400f ? 1f : (sNeededExp > 0 ? staminaExp / sNeededExp : 0f), 0xFF22AA66, 0xFF003318);
        y += 11;

        // Exam & Progression Card
        int examBoxY = y;
        int examBoxH = (rank == NinjaRank.KAGE) ? 68 : 56;

        if (rank == NinjaRank.ACADEMY_STUDENT) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needJ = rank.getRequiredJutsusToAdvance();
            boolean reqC = baseChakra >= needC;
            boolean reqJ = ninjaData.getJutsusCastCount() >= needJ;
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Mezuniyet Sınavı (Akademi ➔ Genin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Taban Çakra: " + (int)Math.min(baseChakra, needC) + " / " + needC;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Jutsu Pratiği: " + Math.min(ninjaData.getJutsusCastCount(), needJ) + " / " + needJ;
            graphics.drawString(this.font, cText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.drawString(this.font, jText, x + 160, examBoxY + 18, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Sınav şartları hazır! Aşağıdan terfi edebilirsiniz." : "§7Meditasyon yaparak çakranızı geliştirin ve jutsuları çalışın.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 36, 0xFFAAAAAA);

        } else if (rank == NinjaRank.GENIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needK = rank.getRequiredEnemiesToAdvance();
            boolean reqC = baseChakra >= needC;
            boolean reqS = baseStamina >= needS;
            boolean reqK = ninjaData.getEnemiesDefeated() >= needK;
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Chūnin Sınavı (Genin ➔ Chūnin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String kText = (reqK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            graphics.drawString(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.drawString(this.font, kText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Chūnin terfisi hazır! Aşağıdaki butona tıklayın." : "§7Koşarak kondisyonunuzu artırın ve düşmanları yenin.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

        } else if (rank == NinjaRank.CHUNIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needK = rank.getRequiredEnemiesToAdvance();
            int needJ = rank.getRequiredJutsusToAdvance();
            boolean reqC = baseChakra >= needC;
            boolean reqS = baseStamina >= needS;
            boolean reqK = ninjaData.getEnemiesDefeated() >= needK;
            int unlocked = ninjaData.getUnlockedAbilities().size();
            boolean reqJ = unlocked >= needJ;
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Jōnin Değerlendirmesi (Chūnin ➔ Jōnin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String kText = (reqK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Parşömen: " + Math.min(unlocked, needJ) + "/" + needJ;
            graphics.drawString(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.drawString(this.font, kText + "   " + jText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Jōnin ataması hazır! Aşağıdan rütbeye terfi edin." : "§7Gelişmiş parşömen tekniklerini (Rasengan/Chidori vb.) öğrenin.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

        } else if (rank == NinjaRank.JONIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needK = rank.getRequiredEnemiesToAdvance();
            int needJ = rank.getRequiredJutsusToAdvance();
            boolean reqC = baseChakra >= needC;
            boolean reqS = baseStamina >= needS;
            boolean reqK = ninjaData.getEnemiesDefeated() >= needK;
            int unlocked = ninjaData.getUnlockedAbilities().size();
            boolean reqJ = unlocked >= needJ;
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ ANBU Özel Kuvvet Sınavı (Jōnin ➔ ANBU)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String kText = (reqK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Parşömen: " + Math.min(unlocked, needJ) + "/" + needJ;
            graphics.drawString(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.drawString(this.font, kText + "   " + jText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Özel kuvvetler daveti onaylandı! Aşağıdan terfi edin." : "§7Köyün seçkin gölge koruyucusu olmak için sınırlarınızı zorlayın.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

        } else if (rank == NinjaRank.ANBU) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needK = rank.getRequiredEnemiesToAdvance();
            int needJ = rank.getRequiredJutsusToAdvance();
            boolean reqC = baseChakra >= needC;
            boolean reqS = baseStamina >= needS;
            boolean reqK = ninjaData.getEnemiesDefeated() >= needK;
            int unlocked = ninjaData.getUnlockedAbilities().size();
            boolean reqJ = unlocked >= needJ;
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.drawString(this.font, "§e★ Kage Seçim Adaylığı (ANBU ➔ Kage)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String kText = (reqK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Parşömen: " + Math.min(unlocked, needJ) + "/" + needJ;
            graphics.drawString(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.drawString(this.font, kText + "   " + jText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Köy Meclisi sizi Kage ilan etti! Liderliği devralın." : "§7Köyün en kudretli şinobisi olma unvanına ulaşmak üzeresiniz.";
            graphics.drawString(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

        } else {
            // KAGE (Max Rank) - Word-wrapped, beautiful celebratory card
            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, 0x33FFAA00);
            graphics.renderOutline(x, examBoxY, contentW, examBoxH, 0xFFFFAA00);

            graphics.drawString(this.font, "§6★ En Yüksek Rütbe – Kage ★", x + 6, examBoxY + 6, 0xFFFFAA00);

            drawWrapped(this.font, graphics,
                    Component.translatable("naruto.gui.ninja_menu.kage_congrats").copy().withStyle(ChatFormatting.GREEN),
                    x + 6, examBoxY + 20, contentW - 12, 0xFFFFFFFF);
        }
    }

    private void renderClanTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        int warnH = 28;
        if (ninjaData.isClanLocked()) {
            graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + warnH, 0xEE441111);
            graphics.renderOutline(boxX, boxY, BOX_WIDTH, warnH, 0xFFFF2222);
            graphics.drawCenteredString(this.font, "§c§lKLANINIZ KALICI OLARAK MÜHÜRLENDİ", centerX, boxY + 4, 0xFFFF5555);
            graphics.drawCenteredString(this.font, "§7Klana dahil olundu; klan üyeliğiniz artık değiştirilemez.", centerX, boxY + 15, 0xFFAAAAAA);
        } else {
            graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + warnH, 0xEE443311);
            graphics.renderOutline(boxX, boxY, BOX_WIDTH, warnH, 0xFFFFAA00);
            graphics.drawCenteredString(this.font, "§e§lDİKKAT: BİR KEZ SEÇİLEN KLAN DEĞİŞTİRİLEMEZ!", centerX, boxY + 4, 0xFFFFAA00);
            graphics.drawCenteredString(this.font, "§fKlanınızı dikkatli seçiniz; seçim kalıcı olarak mühürlenir.", centerX, boxY + 15, 0xFFFFFFFF);
        }

        List<RegistryObject<Clan>> clans = getSelectableClans();
        int maxPages = Math.max(1, (int) Math.ceil((double) clans.size() / CLANS_PER_PAGE));
        int startIndex = this.clanPage * CLANS_PER_PAGE;
        int endIndex = Math.min(startIndex + CLANS_PER_PAGE, clans.size());

        int cardStartY = boxY + 32;
        int cardH = 38;
        int cardGap = 3;
        int maxTextW = BOX_WIDTH - 96;

        for (int i = startIndex; i < endIndex; i++) {
            RegistryObject<Clan> clanEntry = clans.get(i);
            ResourceLocation clanLoc = clanEntry.getId();
            Clan clan = clanEntry.get();
            boolean isCurrent = ninjaData.getClan() != null && ninjaData.getClan().equals(clanLoc);
            int bgCol = isCurrent ? 0xCC113311 : 0xCC111116;
            int borderCol = isCurrent ? 0xFF22AA22 : 0xFF444455;

            int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);

            graphics.fill(boxX, cardY, boxX + BOX_WIDTH, cardY + cardH, bgCol);
            graphics.renderOutline(boxX, cardY, BOX_WIDTH, cardH, borderCol);

            String title = clan != null ? clan.getName().getString() : clanLoc.getPath();
            graphics.drawString(this.font, "§6" + title, boxX + 8, cardY + 4, 0xFFFFFFFF);

            String desc = clan != null ? clan.getDescription().getString() : "";
            int lineY = cardY + 16;
            for (FormattedCharSequence line : this.font.split(Component.literal(desc), maxTextW)) {
                graphics.drawString(this.font, line, boxX + 8, lineY, 0xFFAAAAAA);
                lineY += this.font.lineHeight + 1;
            }
        }

        int navTextY = Math.min(boxY + BOX_HEIGHT + 10, this.height - 18);
        graphics.drawCenteredString(this.font, String.format("§eKlan Sayfası %d / %d", this.clanPage + 1, maxPages), centerX, navTextY, 0xFFFFFF88);
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
            if (unlocked) {
                String comboText = formatCombo(ability.defaultCombo());
                graphics.drawString(this.font, "§eMühür: §b" + comboText, boxX + 8, cardY + 15, 0xFFDDDDDD);
            } else {
                graphics.drawString(this.font, "§eMühür: §7??? (Gizli)", boxX + 8, cardY + 15, 0xFFAAAAAA);
            }

            // Acquisition Type
            String reqType;
            if (id != null && (id.getPath().equals("amaterasu") || id.getPath().equals("susanoo_ribcage"))) {
                reqType = "§c[Uchiha Parşömeni - Jōnin]";
            } else if (id != null && id.getPath().equals("eight_gates")) {
                reqType = "§4[Yasak Parşömen - Jōnin]";
            } else if (id != null && id.getPath().equals("rasenshuriken")) {
                reqType = "§b[S-Seviye Parşömen - Jōnin]";
            } else if (id != null && id.getPath().equals("sharingan")) {
                reqType = "§c[Uchiha Klanı - Genin]";
            } else if (id != null && id.getPath().equals("byakugan")) {
                reqType = "§f[Hyūga Klanı - Genin]";
            } else if (id != null && id.getPath().equals("wood_prison")) {
                reqType = "§2[Senju Klanı - Genin]";
            } else if (id != null && id.getPath().equals("bone_dance")) {
                reqType = "§f[Kaguya Klanı - Genin]";
            } else if (id != null && id.getPath().equals("shadow_possession")) {
                reqType = "§8[Nara Klanı - Genin / Parşömen]";
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

    private static void drawWrapped(Font font, GuiGraphics graphics, Component text, int x, int y, int wrapWidth, int color) {
        for (FormattedCharSequence line : font.split(text, wrapWidth)) {
            graphics.drawString(font, line, x, y, color);
            y += font.lineHeight + 2;
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
