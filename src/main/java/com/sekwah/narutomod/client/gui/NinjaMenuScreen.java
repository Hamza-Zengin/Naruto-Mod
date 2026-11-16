package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.network.c2s.ServerRankUpPacket;
import com.sekwah.narutomod.network.c2s.ServerSelectClanPacket;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import com.sekwah.narutomod.clans.Clan;
import com.sekwah.narutomod.clans.NarutoClans;
import net.neoforged.neoforge.registries.DeferredHolder;

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

        // Modern Tab Navigation Buttons (4 tabs centered inside 340px)
        int tabBtnW = 80;
        int tabGap = 5;
        int totalTabsW = tabBtnW * 4 + tabGap * 3;
        int tabStartX = centerX - totalTabsW / 2;

        Component profileTitle = Component.translatable("naruto.gui.ninja_menu.tab.profile");
        Component clansTitle = Component.translatable("naruto.gui.ninja_menu.tab.clans");
        Component jutsusTitle = Component.translatable("naruto.gui.ninja_menu.tab.jutsus");
        Component missionsTitle = Component.translatable("naruto.gui.ninja_menu.tab.missions");

        Component tab0 = this.activeTab == 0 ? Component.literal("§6§l[ ").append(profileTitle).append(" ]") : profileTitle;
        Component tab1 = this.activeTab == 1 ? Component.literal("§6§l[ ").append(clansTitle).append(" ]") : clansTitle;
        Component tab2 = this.activeTab == 2 ? Component.literal("§6§l[ ").append(jutsusTitle).append(" ]") : jutsusTitle;
        Component tab3 = this.activeTab == 3 ? Component.literal("§6§l[ ").append(missionsTitle).append(" ]") : missionsTitle;

        this.addRenderableWidget(Button.builder(tab0, b -> switchTab(0))
                .bounds(tabStartX, topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(tab1, b -> switchTab(1))
                .bounds(tabStartX + (tabBtnW + tabGap), topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(tab2, b -> switchTab(2))
                .bounds(tabStartX + (tabBtnW + tabGap) * 2, topY, tabBtnW, 20).build());

        this.addRenderableWidget(Button.builder(tab3, b -> switchTab(3))
                .bounds(tabStartX + (tabBtnW + tabGap) * 3, topY, tabBtnW, 20).build());

        // Cache and sort only unlocked abilities from simplest to most complex
        this.allAbilities.clear();
        Minecraft mcInit = Minecraft.getInstance();
        if (mcInit.player != null) {
            INinjaData ninjaData = mcInit.player.getData(NinjaCapabilityHandler.NINJA_DATA);
            NarutoAbilities.ABILITY.getEntries().forEach(entry -> {
                Ability ability = entry.get();
                if (ability.defaultCombo() > 0 && ninjaData.isAbilityUnlocked(ability)) {
                    this.allAbilities.add(ability);
                }
            });
        }
        this.allAbilities.sort(Ability.COMPLEXITY_COMPARATOR);

        int boxY = topY + 28;

        if (this.activeTab == 0) {
            initProfileTab(centerX, boxY);
        } else if (this.activeTab == 1) {
            initClanTab(centerX, boxY);
        } else if (this.activeTab == 2) {
            initJutsuTab(centerX, boxY);
        } else if (this.activeTab == 3) {
            initMissionsTab(centerX, boxY);
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

        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
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
                if (canRank) {
                    b.active = false;
                    ClientPacketDistributor.sendToServer(new ServerRankUpPacket());
                }
            }).bounds(centerX - 120, btnY, 240, 20).build();

            rankBtn.active = canRank;

            if (canRank) {
                rankBtn.setTooltip(Tooltip.create(Component.translatable("naruto.gui.ninja_menu.btn.rank_up_ready").withStyle(ChatFormatting.GREEN)));
            } else {
                MutableComponent tooltipComp = Component.translatable("naruto.gui.ninja_menu.missing_requirements").withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
                List<Component> missing = rank.getMissingRequirements(ninjaData);
                for (Component req : missing) {
                    tooltipComp.append(Component.literal("\n • ").withStyle(ChatFormatting.GRAY)).append(req.copy().withStyle(ChatFormatting.YELLOW));
                }
                rankBtn.setTooltip(Tooltip.create(tooltipComp));
            }

            this.addRenderableWidget(rankBtn);
        }
    }

    private List<DeferredHolder<Clan, ? extends Clan>> getSelectableClans() {
        List<DeferredHolder<Clan, ? extends Clan>> list = new ArrayList<>();
        for (DeferredHolder<Clan, ? extends Clan> entry : NarutoClans.CLANS.getEntries()) {
            if (!entry.getId().getPath().equalsIgnoreCase("shinobi")) {
                list.add(entry);
            }
        }
        return list;
    }

    private void initClanTab(int centerX, int boxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        boolean isLocked = ninjaData.isClanLocked();

        List<DeferredHolder<Clan, ? extends Clan>> clans = getSelectableClans();
        int maxPages = Math.max(1, (int) Math.ceil((double) clans.size() / CLANS_PER_PAGE));
        if (this.clanPage >= maxPages) this.clanPage = maxPages - 1;

        int startIndex = this.clanPage * CLANS_PER_PAGE;
        int endIndex = Math.min(startIndex + CLANS_PER_PAGE, clans.size());

        int cardStartY = boxY + 32;
        int cardH = 38;
        int cardGap = 3;
        int boxX = centerX - BOX_WIDTH / 2;

        for (int i = startIndex; i < endIndex; i++) {
            final DeferredHolder<Clan, ? extends Clan> clanEntry = clans.get(i);
            final Identifier clanLoc = clanEntry.getId();
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
                                    b.active = false;
                                    ClientPacketDistributor.sendToServer(new ServerSelectClanPacket(clanLoc));
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

    private void initMissionsTab(int centerX, int boxY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        com.sekwah.narutomod.capabilities.NinjaMission active =
                com.sekwah.narutomod.capabilities.NinjaMission.fromId(ninjaData.getActiveMissionId());
        int boxX = centerX - BOX_WIDTH / 2;

        if (active != null) {
            boolean completed = active.isCompleted(ninjaData);
            int btnY = boxY + 125;
            if (completed) {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("naruto.gui.ninja_menu.mission.claim"),
                        b -> {
                            ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerMissionActionPacket(
                                    com.sekwah.narutomod.network.c2s.ServerMissionActionPacket.Action.CLAIM_REWARD,
                                    active.getId()));
                            this.onClose();
                        }).bounds(centerX - 80, btnY, 160, 22).build());
            } else {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("naruto.gui.ninja_menu.mission.abandon"),
                        b -> {
                            ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerMissionActionPacket(
                                    com.sekwah.narutomod.network.c2s.ServerMissionActionPacket.Action.ABANDON_MISSION,
                                    active.getId()));
                            this.onClose();
                        }).bounds(centerX - 80, btnY, 160, 22).build());
            }
        } else {
            NinjaRank playerRank = NinjaRank.fromString(ninjaData.getRank());
            com.sekwah.narutomod.capabilities.NinjaMission[] missions =
                    com.sekwah.narutomod.capabilities.NinjaMission.values();
            int startY = boxY + 36;
            int rowH = 30;
            for (int i = 0; i < missions.length; i++) {
                com.sekwah.narutomod.capabilities.NinjaMission m = missions[i];
                int rowY = startY + i * rowH;
                Button acceptBtn = Button.builder(
                        Component.translatable("naruto.gui.ninja_menu.mission.accept"),
                        b -> {
                            ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerMissionActionPacket(
                                    com.sekwah.narutomod.network.c2s.ServerMissionActionPacket.Action.ACCEPT_MISSION,
                                    m.getId()));
                            this.onClose();
                        }).bounds(boxX + BOX_WIDTH - 88, rowY + 3, 80, 20).build();
                acceptBtn.active = m.canAccept(playerRank);
                this.addRenderableWidget(acceptBtn);
            }
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        int centerX = this.width / 2;
        int topY = 10;

        // Header Title
        graphics.centeredText(this.font, Component.translatable("naruto.gui.ninja_menu.header").copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), centerX, topY, 0xFFFFAA00);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        int boxY = 24 + 28;

        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        if (this.activeTab == 0) {
            renderProfileTab(graphics, ninjaData, centerX, boxY);
        } else if (this.activeTab == 1) {
            renderClanTab(graphics, ninjaData, centerX, boxY);
        } else if (this.activeTab == 2) {
            renderJutsuTab(graphics, ninjaData, centerX, boxY);
        } else if (this.activeTab == 3) {
            renderMissionsTab(graphics, ninjaData, centerX, boxY);
        }

        super.extractRenderState(graphics, mouseX, mouseY, partialTicks);

        if (this.activeTab == 0) {
            NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
            if (rank.getLevel() < NinjaRank.KAGE.getLevel() && !ninjaData.canRankUp()) {
                int btnY = boxY + BOX_HEIGHT - 24;
                int btnX = centerX - 120;
                int btnW = 240;
                int btnH = 20;
                if (mouseX >= btnX && mouseX <= btnX + btnW && mouseY >= btnY && mouseY <= btnY + btnH) {
                    List<Component> missing = rank.getMissingRequirements(ninjaData);
                    List<Component> tooltipLines = new ArrayList<>();
                    tooltipLines.add(Component.translatable("naruto.gui.ninja_menu.missing_requirements").withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    for (Component req : missing) {
                        tooltipLines.add(Component.literal(" • ").withStyle(ChatFormatting.GRAY).append(req.copy().withStyle(ChatFormatting.YELLOW)));
                    }
                    graphics.setComponentTooltipForNextFrame(this.font, tooltipLines, mouseX, mouseY);
                }
            }
        } else if (this.activeTab == 2) {
            int startIndex = jutsuPage * JUTSUS_PER_PAGE;
            int endIndex = Math.min(startIndex + JUTSUS_PER_PAGE, allAbilities.size());
            int boxX = centerX - BOX_WIDTH / 2;
            int cardStartY = boxY + 16;
            int cardH = 36;
            int cardGap = 4;

            for (int i = startIndex; i < endIndex; i++) {
                int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);
                if (mouseX >= boxX && mouseX <= boxX + BOX_WIDTH && mouseY >= cardY && mouseY <= cardY + cardH) {
                    Ability ability = allAbilities.get(i);
                    Identifier id = ability.getRegistryName();
                    int mastery = ninjaData.getMastery(id);
                    int tier = Ability.getMasteryTier(mastery);

                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable(ability.getTranslationKey()).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                    tooltip.add(Component.translatable("naruto.gui.ninja_menu.mastery.level", tier, mastery));

                    int discountPercent = (int) Math.round((1.0f - Ability.getChakraCostMultiplier(mastery)) * 100);
                    int powerBonusPercent = (int) Math.round((Ability.getPowerMultiplier(mastery) - 1.0f) * 100);
                    int cdReductionPercent = (int) Math.round((1.0f - Ability.getCooldownMultiplier(mastery)) * 100);

                    if (discountPercent > 0) {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.mastery.chakra_discount", "§a-%" + discountPercent));
                    } else {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.mastery.chakra_discount", "§7%0"));
                    }

                    if (powerBonusPercent > 0) {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.mastery.power_bonus", "§a+%" + powerBonusPercent));
                    } else {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.mastery.power_bonus", "§7%0"));
                    }

                    if (cdReductionPercent > 0) {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.mastery.cooldown_reduction", "§a-%" + cdReductionPercent));
                    }

                    graphics.setComponentTooltipForNextFrame(this.font, tooltip, mouseX, mouseY);
                    break;
                }
            }
        }
    }

    private void renderProfileTab(GuiGraphicsExtractor graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        // Modern dark translucent card background
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xEE111116);
        graphics.outline(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 0xFF555577);

        Minecraft mc = Minecraft.getInstance();
        String playerName = mc.player != null ? mc.player.getName().getString() : "Shinobi";

        int y = boxY + 8;
        int x = boxX + 12;
        int contentW = BOX_WIDTH - 24;

        // Clan Badge & Lock calculation
        Identifier clanLoc = ninjaData.getClan();
        String clanStatusStr;
        if (clanLoc == null || clanLoc.getPath().equalsIgnoreCase("shinobi")) {
            clanStatusStr = Component.translatable("naruto.gui.ninja_menu.profile.clan_label",
                    Component.translatable("naruto.gui.ninja_menu.clan.unselected").getString()).getString();
        } else {
            Clan clan = NarutoClans.getClan(clanLoc);
            String clanDisplay = clan != null ? clan.getName().getString() : (clanLoc.getPath().substring(0, 1).toUpperCase(java.util.Locale.ROOT) + clanLoc.getPath().substring(1));
            String lockBadge = Component.translatable(ninjaData.isClanLocked()
                    ? "naruto.gui.ninja_menu.profile.sealed"
                    : "naruto.gui.ninja_menu.profile.changeable").getString();
            clanStatusStr = Component.translatable("naruto.gui.ninja_menu.profile.clan_label", clanDisplay + " " + lockBadge).getString();
        }

        int clanStrW = this.font.width(clanStatusStr);
        int maxNameW = contentW - clanStrW - 10;
        String nameStr = "§6§lShinobi: §f" + playerName;
        if (this.font.width(nameStr) > maxNameW) {
            nameStr = this.font.plainSubstrByWidth(nameStr, maxNameW - 6) + "...";
        }
        graphics.text(this.font, nameStr, x, y, 0xFFFFFFFF);
        graphics.text(this.font, clanStatusStr, boxX + BOX_WIDTH - 12 - clanStrW, y, 0xFFFFFFFF);
        y += 13;

        // Ninja Rank
        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        float cBonus = rank.getChakraBonus();
        float sBonus = rank.getStaminaBonus();
        String bonusNote = (cBonus > 0 || sBonus > 0)
                ? " " + Component.translatable("naruto.gui.ninja_menu.profile.rank_bonus", (int) cBonus, (int) sBonus).getString()
                : "";
        graphics.text(this.font, Component.translatable("naruto.gui.ninja_menu.profile.rank_label",
                Component.translatable(rank.getTranslationKey())).append(Component.literal(bonusNote)), x, y, 0xFFFFFFFF);
        y += 14;

        // 1. Stat Bar: Chakra
        float chakra = ninjaData.getChakra();
        float maxChakra = ninjaData.getMaxChakra();
        float baseChakra = ninjaData.getBaseMaxChakra();
        String chakraValStr = Component.translatable("naruto.gui.ninja_menu.profile.stat_value",
                String.format("%.0f", chakra), String.format("%.0f", maxChakra), String.format("%.0f", baseChakra)).getString();
        graphics.text(this.font, Component.translatable("naruto.gui.ninja_menu.profile.chakra"), x, y, 0xFFFFFFFF);
        graphics.text(this.font, chakraValStr, boxX + BOX_WIDTH - 12 - this.font.width(chakraValStr), y, 0xFFFFFFFF);
        y += 10;
        renderBar(graphics, x, y, contentW, 5, maxChakra > 0 ? chakra / maxChakra : 0f, 0xFF2255FF, 0xFF001144);
        y += 8;

        // 2. Stat Bar: Stamina
        float stamina = ninjaData.getStamina();
        float maxStamina = ninjaData.getMaxStamina();
        float baseStamina = ninjaData.getBaseMaxStamina();
        String staminaValStr = Component.translatable("naruto.gui.ninja_menu.profile.stat_value",
                String.format("%.0f", stamina), String.format("%.0f", maxStamina), String.format("%.0f", baseStamina)).getString();
        graphics.text(this.font, Component.translatable("naruto.gui.ninja_menu.profile.stamina"), x, y, 0xFFFFFFFF);
        graphics.text(this.font, staminaValStr, boxX + BOX_WIDTH - 12 - this.font.width(staminaValStr), y, 0xFFFFFFFF);
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
        graphics.text(this.font, "§dÇakra Eğitimi", x, y, 0xFFFFFFFF);
        graphics.text(this.font, cExpVal, x + halfW - this.font.width(cExpVal), y, 0xFFFFFFFF);

        // Stamina EXP (Right)
        float staminaExp = ninjaData.getStaminaExp();
        float sNeededExp = 50.0f + (baseStamina - 100.0f) * 1.10f;
        String sExpVal = baseStamina >= 400.0f ? "§aMAKS (400)" : String.format("§f%.0f / %.0f", staminaExp, sNeededExp);
        graphics.text(this.font, "§2Kondisyon Eğitimi", rightColX, y, 0xFFFFFFFF);
        graphics.text(this.font, sExpVal, rightColX + halfW - this.font.width(sExpVal), y, 0xFFFFFFFF);
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
            graphics.outline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.text(this.font, "§e★ Mezuniyet Sınavı (Akademi ➔ Genin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Taban Çakra: " + (int)Math.min(baseChakra, needC) + " / " + needC;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Jutsu Pratiği: " + Math.min(ninjaData.getJutsusCastCount(), needJ) + " / " + needJ;
            graphics.text(this.font, cText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(this.font, jText, x + 160, examBoxY + 18, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Sınav şartları hazır! Aşağıdan terfi edebilirsiniz." : "§7Meditasyon yaparak çakranızı geliştirin ve jutsuları çalışın.";
            graphics.text(this.font, hint, x + 6, examBoxY + 36, 0xFFAAAAAA);

        } else if (rank == NinjaRank.GENIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needRogue = 10;
            int needMastered = 3;
            boolean reqC = baseChakra >= needC;
            boolean reqS = baseStamina >= needS;
            int rogueKills = ninjaData.getRogueNinjasDefeated();
            boolean reqRogue = rogueKills >= needRogue;
            int masteredCount = NinjaRank.countMasteredJutsus(ninjaData, 50);
            boolean reqMastered = masteredCount >= needMastered;
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.outline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.text(this.font, "§e★ Chūnin Sınavı (Genin ➔ Chūnin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String rText = (reqRogue ? "§a✔ " : "§c✖ ") + "Kaçak Ninja: " + Math.min(rogueKills, needRogue) + "/" + needRogue;
            String mText = (reqMastered ? "§a✔ " : "§c✖ ") + "Usta Jutsu: " + Math.min(masteredCount, needMastered) + "/" + needMastered;
            graphics.text(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(this.font, rText + "   " + mText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Chūnin terfisi hazır! Aşağıdaki butona tıklayın." : "§7Kaçak ninjaları avlayın ve en az 3 jutsuda 50+ ustalığa ulaşın.";
            graphics.text(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

        } else if (rank == NinjaRank.CHUNIN) {
            int needC = (int) rank.getRequiredChakraToAdvance();
            int needS = (int) rank.getRequiredStaminaToAdvance();
            int needAkatsuki = 1;
            boolean reqC = baseChakra >= needC;
            boolean reqS = baseStamina >= needS;
            int akatsukiKills = ninjaData.getAkatsukiNinjasDefeated();
            boolean reqAkatsuki = akatsukiKills >= needAkatsuki;
            boolean reqAdv = NinjaRank.hasMasteredAdvancedJutsu(ninjaData, 50);
            boolean ready = ninjaData.canRankUp();

            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, ready ? 0x3322AA22 : 0x22333344);
            graphics.outline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.text(this.font, "§e★ Jōnin Değerlendirmesi (Chūnin ➔ Jōnin)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String aText = (reqAkatsuki ? "§a✔ " : "§c✖ ") + "Akatsuki: " + Math.min(akatsukiKills, needAkatsuki) + "/" + needAkatsuki;
            String advText = (reqAdv ? "§a✔ " : "§c✖ ") + "İleri Jutsu: " + (reqAdv ? "Hazır" : "Gerekiyor");
            graphics.text(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(this.font, aText + "   " + advText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Jōnin ataması hazır! Aşağıdan rütbeye terfi edin." : "§7Akatsuki üyesini yenin ve ileri seviye bir teknikte (Rasengan/Chidori) ustalaşın.";
            graphics.text(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

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
            graphics.outline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.text(this.font, "§e★ ANBU Özel Kuvvet Sınavı (Jōnin ➔ ANBU)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String kText = (reqK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Parşömen: " + Math.min(unlocked, needJ) + "/" + needJ;
            graphics.text(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(this.font, kText + "   " + jText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Özel kuvvetler daveti onaylandı! Aşağıdan terfi edin." : "§7Köyün seçkin gölge koruyucusu olmak için sınırlarınızı zorlayın.";
            graphics.text(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

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
            graphics.outline(x, examBoxY, contentW, examBoxH, ready ? 0xFF33CC33 : 0xFF555566);

            graphics.text(this.font, "§e★ Kage Seçim Adaylığı (ANBU ➔ Kage)", x + 6, examBoxY + 5, 0xFFFFAA00);

            String cText = (reqC ? "§a✔ " : "§c✖ ") + "Çakra: " + (int)Math.min(baseChakra, needC) + "/" + needC;
            String sText = (reqS ? "§a✔ " : "§c✖ ") + "Kondisyon: " + (int)Math.min(baseStamina, needS) + "/" + needS;
            String kText = (reqK ? "§a✔ " : "§c✖ ") + "Düşman: " + Math.min(ninjaData.getEnemiesDefeated(), needK) + "/" + needK;
            String jText = (reqJ ? "§a✔ " : "§c✖ ") + "Parşömen: " + Math.min(unlocked, needJ) + "/" + needJ;
            graphics.text(this.font, cText + "   " + sText, x + 6, examBoxY + 18, 0xFFFFFFFF);
            graphics.text(this.font, kText + "   " + jText, x + 6, examBoxY + 30, 0xFFFFFFFF);

            String hint = ready ? "§a✔ Köy Meclisi sizi Kage ilan etti! Liderliği devralın." : "§7Köyün en kudretli şinobisi olma unvanına ulaşmak üzeresiniz.";
            graphics.text(this.font, hint, x + 6, examBoxY + 43, 0xFFAAAAAA);

        } else {
            // KAGE (Max Rank) - Word-wrapped, beautiful celebratory card
            graphics.fill(x, examBoxY, x + contentW, examBoxY + examBoxH, 0x33FFAA00);
            graphics.outline(x, examBoxY, contentW, examBoxH, 0xFFFFAA00);

            graphics.text(this.font, "§6★ En Yüksek Rütbe – Kage ★", x + 6, examBoxY + 6, 0xFFFFAA00);

            drawWrapped(this.font, graphics,
                    Component.translatable("naruto.gui.ninja_menu.kage_congrats").copy().withStyle(ChatFormatting.GREEN),
                    x + 6, examBoxY + 20, contentW - 12, 0xFFFFFFFF);
        }
    }

    private void renderClanTab(GuiGraphicsExtractor graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        int warnH = 28;
        if (ninjaData.isClanLocked()) {
            graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + warnH, 0xEE441111);
            graphics.outline(boxX, boxY, BOX_WIDTH, warnH, 0xFFFF2222);
            graphics.centeredText(this.font, "§c§lKLANINIZ KALICI OLARAK MÜHÜRLENDİ", centerX, boxY + 4, 0xFFFF5555);
            graphics.centeredText(this.font, "§7Klana dahil olundu; klan üyeliğiniz artık değiştirilemez.", centerX, boxY + 15, 0xFFAAAAAA);
        } else {
            graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + warnH, 0xEE443311);
            graphics.outline(boxX, boxY, BOX_WIDTH, warnH, 0xFFFFAA00);
            graphics.centeredText(this.font, "§e§lDİKKAT: BİR KEZ SEÇİLEN KLAN DEĞİŞTİRİLEMEZ!", centerX, boxY + 4, 0xFFFFAA00);
            graphics.centeredText(this.font, "§fKlanınızı dikkatli seçiniz; seçim kalıcı olarak mühürlenir.", centerX, boxY + 15, 0xFFFFFFFF);
        }

        List<DeferredHolder<Clan, ? extends Clan>> clans = getSelectableClans();
        int maxPages = Math.max(1, (int) Math.ceil((double) clans.size() / CLANS_PER_PAGE));
        int startIndex = this.clanPage * CLANS_PER_PAGE;
        int endIndex = Math.min(startIndex + CLANS_PER_PAGE, clans.size());

        int cardStartY = boxY + 32;
        int cardH = 38;
        int cardGap = 3;
        int maxTextW = BOX_WIDTH - 96;

        for (int i = startIndex; i < endIndex; i++) {
            DeferredHolder<Clan, ? extends Clan> clanEntry = clans.get(i);
            Identifier clanLoc = clanEntry.getId();
            Clan clan = clanEntry.get();
            boolean isCurrent = ninjaData.getClan() != null && ninjaData.getClan().equals(clanLoc);
            int bgCol = isCurrent ? 0xCC113311 : 0xCC111116;
            int borderCol = isCurrent ? 0xFF22AA22 : 0xFF444455;

            int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);

            graphics.fill(boxX, cardY, boxX + BOX_WIDTH, cardY + cardH, bgCol);
            graphics.outline(boxX, cardY, BOX_WIDTH, cardH, borderCol);

            String title = clan != null ? clan.getName().getString() : clanLoc.getPath();
            graphics.text(this.font, "§6" + title, boxX + 8, cardY + 4, 0xFFFFFFFF);

            String desc = clan != null ? clan.getDescription().getString() : "";
            int lineY = cardY + 16;
            for (FormattedCharSequence line : this.font.split(Component.literal(desc), maxTextW)) {
                graphics.text(this.font, line, boxX + 8, lineY, 0xFFAAAAAA);
                lineY += this.font.lineHeight + 1;
            }
        }

        int navTextY = Math.min(boxY + BOX_HEIGHT + 10, this.height - 18);
        graphics.centeredText(this.font, String.format("§eKlan Sayfası %d / %d", this.clanPage + 1, maxPages), centerX, navTextY, 0xFFFFFF88);
    }

    private void renderJutsuTab(GuiGraphicsExtractor graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;

        if (allAbilities.isEmpty()) {
            graphics.centeredText(this.font, "§7Henüz öğrenilmiş bir jutsu yok.", centerX, boxY + 60, 0xFFAAAAAA);
            return;
        }

        int startIndex = jutsuPage * JUTSUS_PER_PAGE;
        int endIndex = Math.min(startIndex + JUTSUS_PER_PAGE, allAbilities.size());
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));

        graphics.centeredText(this.font, String.format("§eSayfa %d / %d", jutsuPage + 1, maxPages), centerX, boxY + 2, 0xFFFFFF88);

        int cardStartY = boxY + 16;
        int cardH = 36;
        int cardGap = 4;

        for (int i = startIndex; i < endIndex; i++) {
            Ability ability = allAbilities.get(i);
            Identifier id = ability.getRegistryName();

            int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);

            int bgCol = 0xCC112211;
            int borderCol = 0xFF228822;

            graphics.fill(boxX, cardY, boxX + BOX_WIDTH, cardY + cardH, bgCol);
            graphics.outline(boxX, cardY, BOX_WIDTH, cardH, borderCol);

            // Ability Name and Status (Row 1)
            String name = Component.translatable(ability.getTranslationKey()).getString();
            String status = "§a✔ ÖĞRENİLDİ";
            int statusWidth = this.font.width(status);
            int maxNameWidth = BOX_WIDTH - statusWidth - 24;
            String displayName = this.font.width(name) > maxNameWidth ? this.font.plainSubstrByWidth(name, maxNameWidth - 8) + "..." : name;
            graphics.text(this.font, "§f§l" + displayName, boxX + 8, cardY + 4, 0xFFFFFFFF);
            graphics.text(this.font, status, boxX + BOX_WIDTH - statusWidth - 8, cardY + 4, 0xFFFFFFFF);

            // Combo & Mastery (Row 2)
            String comboText = formatCombo(ability.defaultCombo());
            graphics.text(this.font, "§eMühür: §b" + comboText, boxX + 8, cardY + 15, 0xFFDDDDDD);

            int mastery = ninjaData.getMastery(id);
            String masteryText;
            if (id != null && "sharingan".equals(id.getPath())) {
                com.sekwah.narutomod.abilities.jutsus.SharinganAbility.SharinganStage stage =
                        com.sekwah.narutomod.abilities.jutsus.SharinganAbility.getStage(mastery);
                masteryText = "§eUstalık: §a" + mastery + " §7(" + stage.getDisplayName() + ")";
            } else if (id != null && "eight_gates".equals(id.getPath())) {
                com.sekwah.narutomod.abilities.jutsus.EightGatesAbility.GateStage gstage =
                        com.sekwah.narutomod.abilities.jutsus.EightGatesAbility.getStage(mastery);
                masteryText = "§eUstalık: §a" + mastery + " §7(" + gstage.getDisplayName() + ")";
            } else {
                masteryText = "§eUstalık: §a" + mastery + " §7(Lv." + Ability.getMasteryTier(mastery) + ")";
            }
            int masteryWidth = this.font.width(masteryText);
            graphics.text(this.font, masteryText, boxX + BOX_WIDTH - masteryWidth - 8, cardY + 15, 0xFFFFFF88);

            // Acquisition Type (Row 3)
            String reqType;
            if (!ability.getPrerequisites().isEmpty()) {
                StringBuilder sbPrereq = new StringBuilder();
                for (Identifier p : ability.getPrerequisites()) {
                    Ability pa = NarutoAbilities.getAbility(p);
                    String paName = pa != null ? Component.translatable(pa.getTranslationKey()).getString() : p.getPath();
                    if (sbPrereq.length() > 0) sbPrereq.append(", ");
                    sbPrereq.append(paName);
                }
                reqType = "§b[Önşart: " + sbPrereq + " (" + ability.getRequiredMastery() + "+ Ustalık)]";
            } else if (id != null && (id.getPath().equals("amaterasu") || id.getPath().equals("susanoo_ribcage"))) {
                reqType = "§c[Uchiha Parşömeni - Jōnin]";
            } else if (id != null && id.getPath().equals("eight_gates_death")) {
                reqType = "§4[Nihai Yasak Parşömen - Jōnin]";
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
            graphics.text(this.font, reqType, boxX + 8, cardY + 25, 0xFFAAAAAA);
        }
    }

    private static void drawWrapped(Font font, GuiGraphicsExtractor graphics, Component text, int x, int y, int wrapWidth, int color) {
        for (FormattedCharSequence line : font.split(text, wrapWidth)) {
            graphics.text(font, line, x, y, color);
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

    private void renderBar(GuiGraphicsExtractor graphics, int x, int y, int width, int height, float ratio, int fillCol, int bgCol) {
        ratio = Math.max(0f, Math.min(1f, ratio));
        graphics.fill(x, y, x + width, y + height, bgCol);
        graphics.fill(x, y, x + (int) (width * ratio), y + height, fillCol);
        graphics.outline(x, y, width, height, 0xFF555566);
    }

    private void renderMissionsTab(GuiGraphicsExtractor graphics, INinjaData ninjaData, int centerX, int boxY) {
        int boxX = centerX - BOX_WIDTH / 2;
        graphics.fill(boxX, boxY, boxX + BOX_WIDTH, boxY + BOX_HEIGHT, 0xCC11111A);
        graphics.outline(boxX, boxY, BOX_WIDTH, BOX_HEIGHT, 0xFF555566);

        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        String headerInfo = "§6Rütbe: §f" + rank.getEnglishName()
                + "   §eTamamlanan Görev: §a" + ninjaData.getCompletedMissionsCount();
        graphics.text(this.font, headerInfo, boxX + 12, boxY + 12, 0xFFFFFFFF);

        com.sekwah.narutomod.capabilities.NinjaMission active =
                com.sekwah.narutomod.capabilities.NinjaMission.fromId(ninjaData.getActiveMissionId());

        if (active != null) {
            int cardY = boxY + 34;
            boolean completed = active.isCompleted(ninjaData);
            int borderCol = completed ? 0xFF33AA33 : 0xFFCC9922;
            graphics.fill(boxX + 10, cardY, boxX + BOX_WIDTH - 10, cardY + 106, 0xCC181824);
            graphics.outline(boxX + 10, cardY, BOX_WIDTH - 20, 106, borderCol);

            String title = "§6§l[" + active.getRankLabel() + "] §f"
                    + active.getTitleComponent().getString();
            graphics.text(this.font, title, boxX + 20, cardY + 8, 0xFFFFFFFF);

            java.util.List<net.minecraft.util.FormattedCharSequence> descLines =
                    this.font.split(active.getDescriptionComponent(), BOX_WIDTH - 40);
            int descY = cardY + 22;
            for (int l = 0; l < Math.min(2, descLines.size()); l++) {
                graphics.text(this.font, descLines.get(l), boxX + 20, descY + l * 10, 0xFFBBBBCC);
            }

            int progress = active.getProgress(ninjaData);
            int target = active.getTargetCount();
            float ratio = target > 0 ? (float) progress / target : 1.0f;
            String progText = (completed ? "§a✔ Tamamlandı: " : "§eİlerleme: ") + progress + " / " + target
                    + "   §bÖdül: +" + active.getRewardXp() + " XP";
            graphics.text(this.font, progText, boxX + 20, cardY + 52, 0xFFDDDDDD);

            renderBar(graphics, boxX + 20, cardY + 68, BOX_WIDTH - 40, 10, ratio,
                    completed ? 0xFF33CC33 : 0xFF3399FF, 0xFF222233);
        } else {
            com.sekwah.narutomod.capabilities.NinjaMission[] missions =
                    com.sekwah.narutomod.capabilities.NinjaMission.values();
            int startY = boxY + 32;
            int rowH = 34;
            for (int i = 0; i < missions.length; i++) {
                com.sekwah.narutomod.capabilities.NinjaMission m = missions[i];
                int rowY = startY + i * rowH;
                boolean eligible = m.canAccept(rank);
                graphics.fill(boxX + 8, rowY, boxX + BOX_WIDTH - 96, rowY + 31, 0xCC181822);
                graphics.outline(boxX + 8, rowY, BOX_WIDTH - 104, 31, eligible ? 0xFF447744 : 0xFF553333);

                String mName = (eligible ? "§6[" + m.getRankLabel() + "] §f" : "§8[" + m.getRankLabel() + "] ")
                        + m.getTitleComponent().getString();
                graphics.text(this.font, mName, boxX + 14, rowY + 3, 0xFFFFFFFF);

                java.util.List<net.minecraft.util.FormattedCharSequence> descLines =
                        this.font.split(m.getDescriptionComponent(), BOX_WIDTH - 116);
                if (!descLines.isEmpty()) {
                    graphics.text(this.font, descLines.get(0), boxX + 14, rowY + 13, 0xFF9999AA);
                }

                String mMeta = "§7Hedef: " + m.getTargetCount() + " | Ödül: +" + m.getRewardXp() + " XP"
                        + (eligible ? "" : " §c(" + m.getRequiredRank().getEnglishName() + ")");
                graphics.text(this.font, mMeta, boxX + 14, rowY + 22, 0xFFAAAAAA);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
