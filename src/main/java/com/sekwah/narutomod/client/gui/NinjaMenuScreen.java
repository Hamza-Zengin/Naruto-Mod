package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.JutsuDataManager;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaData;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.network.c2s.ServerRankUpPacket;
import com.sekwah.narutomod.network.c2s.ServerSelectClanPacket;
import com.sekwah.narutomod.network.c2s.ServerUpdateJutsuLoadoutPacket;
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
    private static final int JUTSUS_PER_PAGE = 3;
    private int editingWheelSlot = -1;
    private int missionScrollOffset = 0;
    private int missionMaxScroll = 0;
    private final List<Button> missionAcceptButtons = new ArrayList<>();

    private static final int BOX_WIDTH = 340;
    private static final int BOX_HEIGHT = 195;

    private final List<Ability> allAbilities = new ArrayList<>();

    public NinjaMenuScreen() {
        this(0);
    }

    public NinjaMenuScreen(int initialTab) {
        super(Component.translatable("naruto.gui.ninja_menu.title"));
        this.activeTab = Math.max(0, Math.min(3, initialTab));
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

        // Cache and sort only unlocked non-utility abilities from simplest to most complex
        this.allAbilities.clear();
        Minecraft mcInit = Minecraft.getInstance();
        if (mcInit.player != null) {
            INinjaData ninjaData = mcInit.player.getData(NinjaCapabilityHandler.NINJA_DATA);
            if (this.editingWheelSlot < 0 || this.editingWheelSlot >= INinjaData.MAX_JUTSU_SLOTS) {
                this.editingWheelSlot = Math.max(0, Math.min(INinjaData.MAX_JUTSU_SLOTS - 1, ninjaData.getSelectedJutsuSlot()));
            }
            NarutoAbilities.ABILITY.getEntries().forEach(entry -> {
                Ability ability = entry.get();
                if (!ability.isUtilityAbility() && ninjaData.isAbilityUnlocked(ability)) {
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
        Minecraft mc = Minecraft.getInstance();
        INinjaData ninjaData = mc.player != null ? mc.player.getData(NinjaCapabilityHandler.NINJA_DATA) : null;
        if (this.editingWheelSlot < 0 || this.editingWheelSlot >= INinjaData.MAX_JUTSU_SLOTS) {
            this.editingWheelSlot = ninjaData != null
                    ? Math.max(0, Math.min(INinjaData.MAX_JUTSU_SLOTS - 1, ninjaData.getSelectedJutsuSlot()))
                    : 0;
        }

        int boxX = centerX - BOX_WIDTH / 2;

        // 8-slot Radial Wheel Loadout Bar
        int slotBtnW = 38;
        int slotGap = 3;
        int totalSlotsW = slotBtnW * INinjaData.MAX_JUTSU_SLOTS + slotGap * (INinjaData.MAX_JUTSU_SLOTS - 1);
        int slotStartX = centerX - totalSlotsW / 2;
        int slotY = boxY + 14;

        for (int slot = 0; slot < INinjaData.MAX_JUTSU_SLOTS; slot++) {
            final int targetSlot = slot;
            Identifier equippedId = ninjaData != null ? ninjaData.getEquippedJutsu(slot) : null;
            Ability equippedAbility = equippedId != null ? NarutoAbilities.getAbility(equippedId) : null;
            boolean isOccupied = equippedId != null;
            boolean isSelected = slot == this.editingWheelSlot;

            Component slotLabel;
            if (isSelected) {
                slotLabel = Component.literal("§6§l*#" + (slot + 1) + (isOccupied ? "•" : ""));
            } else if (isOccupied) {
                slotLabel = Component.literal("§a#" + (slot + 1) + "•");
            } else {
                slotLabel = Component.literal("§7#" + (slot + 1));
            }

            Button slotBtn = Button.builder(slotLabel, b -> {
                this.editingWheelSlot = targetSlot;
                this.init();
            }).bounds(slotStartX + slot * (slotBtnW + slotGap), slotY, slotBtnW, 20).build();

            if (equippedAbility != null) {
                slotBtn.setTooltip(Tooltip.create(
                        Component.literal("#" + (slot + 1) + ": ").withStyle(ChatFormatting.GOLD)
                                .append(Component.translatable(equippedAbility.getTranslationKey()).copy().withStyle(ChatFormatting.WHITE))
                ));
            } else {
                slotBtn.setTooltip(Tooltip.create(
                        Component.literal("#" + (slot + 1) + ": ").withStyle(ChatFormatting.GOLD)
                                .append(Component.translatable("naruto.gui.jutsu_wheel.empty").copy().withStyle(ChatFormatting.GRAY))
                ));
            }
            this.addRenderableWidget(slotBtn);
        }

        // Compact Clear button for the selected wheel slot
        int clearW = 62;
        Button clearBtn = Button.builder(Component.translatable("naruto.gui.ninja_menu.jutsu.clear_slot"), b -> {
            if (ninjaData != null) {
                ClientPacketDistributor.sendToServer(ServerUpdateJutsuLoadoutPacket.assignSlot(this.editingWheelSlot, null));
                ninjaData.setEquippedJutsu(this.editingWheelSlot, null);
                this.init();
            }
        }).bounds(boxX + BOX_WIDTH - clearW - 6, boxY + 37, clearW, 18).build();
        clearBtn.active = ninjaData != null && ninjaData.getEquippedJutsu(this.editingWheelSlot) != null;
        this.addRenderableWidget(clearBtn);

        // Jutsu Cards & Equip Buttons
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));
        if (this.jutsuPage >= maxPages) {
            this.jutsuPage = maxPages - 1;
        }
        int startIndex = this.jutsuPage * JUTSUS_PER_PAGE;
        int endIndex = Math.min(startIndex + JUTSUS_PER_PAGE, allAbilities.size());
        int cardStartY = boxY + 60;
        int cardH = 40;
        int cardGap = 4;

        if (ninjaData != null) {
            for (int i = startIndex; i < endIndex; i++) {
                Ability ability = allAbilities.get(i);
                Identifier id = ability.getRegistryName();
                if (id == null || !ability.isWheelEquippable()) continue;

                int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);
                int equippedSlot = -1;
                for (int s = 0; s < INinjaData.MAX_JUTSU_SLOTS; s++) {
                    if (id.equals(ninjaData.getEquippedJutsu(s))) {
                        equippedSlot = s;
                        break;
                    }
                }

                Component equipLabel = equippedSlot >= 0
                        ? Component.literal("§a").append(Component.translatable("naruto.gui.ninja_menu.jutsu.equipped", equippedSlot + 1))
                        : Component.translatable("naruto.gui.ninja_menu.jutsu.equip", this.editingWheelSlot + 1);

                Button equipBtn = Button.builder(equipLabel, b -> {
                    ClientPacketDistributor.sendToServer(ServerUpdateJutsuLoadoutPacket.assignSlot(this.editingWheelSlot, id));
                    ninjaData.setEquippedJutsu(this.editingWheelSlot, id);

                    int nextSlot = -1;
                    for (int offset = 1; offset <= INinjaData.MAX_JUTSU_SLOTS; offset++) {
                        int candidate = (this.editingWheelSlot + offset) % INinjaData.MAX_JUTSU_SLOTS;
                        if (ninjaData.getEquippedJutsu(candidate) == null) {
                            nextSlot = candidate;
                            break;
                        }
                    }
                    if (nextSlot < 0) {
                        nextSlot = (this.editingWheelSlot + 1) % INinjaData.MAX_JUTSU_SLOTS;
                    }
                    this.editingWheelSlot = nextSlot;
                    this.init();
                }).bounds(boxX + BOX_WIDTH - 78, cardY + 10, 72, 20).build();

                this.addRenderableWidget(equipBtn);
            }
        }

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
        this.missionAcceptButtons.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        INinjaData ninjaData = mc.player.getData(NinjaCapabilityHandler.NINJA_DATA);
        com.sekwah.narutomod.capabilities.MissionDefinition active =
                com.sekwah.narutomod.capabilities.MissionDataManager.get(ninjaData.getActiveMissionId());
        int boxX = centerX - BOX_WIDTH / 2;

        if (active != null) {
            this.missionScrollOffset = 0;
            this.missionMaxScroll = 0;
            boolean completed = active.isCompleted(ninjaData);
            int btnY = boxY + 126;
            if (completed) {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("naruto.gui.ninja_menu.mission.claim"),
                        b -> {
                            ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerMissionActionPacket(
                                    com.sekwah.narutomod.network.c2s.ServerMissionActionPacket.Action.CLAIM_REWARD,
                                    active.id()));
                            this.onClose();
                        }).bounds(centerX - 80, btnY, 160, 22).build());
            } else {
                this.addRenderableWidget(Button.builder(
                        Component.translatable("naruto.gui.ninja_menu.mission.abandon"),
                        b -> {
                            ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerMissionActionPacket(
                                    com.sekwah.narutomod.network.c2s.ServerMissionActionPacket.Action.ABANDON_MISSION,
                                    active.id()));
                            this.onClose();
                        }).bounds(centerX - 80, btnY, 160, 22).build());
            }
        } else {
            NinjaRank playerRank = NinjaRank.fromString(ninjaData.getRank());
            List<com.sekwah.narutomod.capabilities.MissionDefinition> missions =
                    com.sekwah.narutomod.capabilities.MissionDataManager.getAll();
            for (com.sekwah.narutomod.capabilities.MissionDefinition m : missions) {
                Button acceptBtn = Button.builder(
                        Component.translatable("naruto.gui.ninja_menu.mission.accept"),
                        b -> {
                            ClientPacketDistributor.sendToServer(new com.sekwah.narutomod.network.c2s.ServerMissionActionPacket(
                                    com.sekwah.narutomod.network.c2s.ServerMissionActionPacket.Action.ACCEPT_MISSION,
                                    m.id()));
                            this.onClose();
                        }).bounds(boxX + BOX_WIDTH - 92, boxY + 32, 80, 20).build();
                acceptBtn.active = m.canAccept(playerRank);
                this.missionAcceptButtons.add(acceptBtn);
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
            int cardStartY = boxY + 60;
            int cardH = 40;
            int cardGap = 4;

            for (int i = startIndex; i < endIndex; i++) {
                int cardY = cardStartY + (i - startIndex) * (cardH + cardGap);
                if (mouseX >= boxX && mouseX <= boxX + BOX_WIDTH - 82 && mouseY >= cardY && mouseY <= cardY + cardH) {
                    Ability ability = allAbilities.get(i);
                    Identifier id = ability.getRegistryName();
                    JutsuDataManager.JutsuStats syncedStats = id != null ? JutsuDataManager.get(id) : null;

                    int mastery = ninjaData.getMastery(id);
                    int tier = Ability.getMasteryTier(mastery);

                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.translatable(ability.getTranslationKey()).copy().withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

                    // 1. Element Type (ChakraElement color & name)
                    ChakraElement element = ability.getChakraElement();
                    ChatFormatting elementColor = NinjaMenuRenderUtils.getElementColor(element);
                    Component elementName = Component.translatable("naruto.element." + element.getName()).withStyle(elementColor);
                    tooltip.add(Component.translatable("naruto.gui.ninja_menu.jutsu_detail.element", elementName).withStyle(ChatFormatting.GRAY));

                    // 2. Chakra Cost (from JutsuDataManager with fallback)
                    float chakraCost = (syncedStats != null && syncedStats.chakraCost() >= 0f)
                            ? syncedStats.chakraCost()
                            : ability.getDefaultChakraCost();
                    tooltip.add(Component.translatable("naruto.gui.ninja_menu.jutsu_detail.chakra_cost",
                            Component.literal(NinjaMenuRenderUtils.formatStatNumber(chakraCost)).withStyle(ChatFormatting.AQUA)).withStyle(ChatFormatting.GRAY));

                    // 3. Cooldown in seconds (from JutsuDataManager cooldown_ticks)
                    int cooldownTicks = (syncedStats != null && syncedStats.cooldownTicks() >= 0)
                            ? syncedStats.cooldownTicks()
                            : ability.getDefaultCooldown();
                    String cooldownSeconds = String.format(java.util.Locale.ROOT, "%.1fs", cooldownTicks / 20.0f);
                    tooltip.add(Component.translatable("naruto.gui.ninja_menu.jutsu_detail.cooldown",
                            Component.literal(cooldownSeconds).withStyle(ChatFormatting.YELLOW)).withStyle(ChatFormatting.GRAY));

                    // 4. Base Damage (only if > 0)
                    float baseDamage = (syncedStats != null && syncedStats.baseDamage() >= 0f)
                            ? syncedStats.baseDamage()
                            : ability.getDefaultDamage();
                    if (baseDamage > 0f) {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.jutsu_detail.base_damage",
                                Component.literal(NinjaMenuRenderUtils.formatStatNumber(baseDamage)).withStyle(ChatFormatting.RED)).withStyle(ChatFormatting.GRAY));
                    }

                    // 5. Lock Status & Requirement
                    boolean unlocked = ninjaData.isAbilityUnlocked(ability);
                    NinjaRank reqRank = null;
                    if (syncedStats != null && syncedStats.requiredRank() != null && !syncedStats.requiredRank().isBlank()) {
                        reqRank = NinjaRank.parseExact(syncedStats.requiredRank());
                    }
                    if (reqRank == null) {
                        reqRank = ability.getRequiredRank();
                    }
                    Component reqComp = reqRank != null
                            ? Component.translatable(reqRank.getTranslationKey())
                            : Component.literal("-");
                    if (unlocked) {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.jutsu_detail.status_unlocked", reqComp)
                                .withStyle(ChatFormatting.GREEN));
                    } else {
                        tooltip.add(Component.translatable("naruto.gui.ninja_menu.jutsu_detail.status_locked", reqComp)
                                .withStyle(ChatFormatting.RED));
                    }

                    // Mastery breakdown
                    tooltip.add(Component.empty());
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
        NinjaMenuProfileTab.render(graphics, this.font, ninjaData, centerX, boxY, BOX_WIDTH, BOX_HEIGHT);
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
        NinjaMenuJutsuTab.render(graphics, this.font, ninjaData, allAbilities, jutsuPage, JUTSUS_PER_PAGE, editingWheelSlot, centerX, boxY, BOX_WIDTH);
        if (!allAbilities.isEmpty()) {
            int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));
            int navTextY = Math.min(boxY + BOX_HEIGHT + 10, this.height - 18);
            graphics.centeredText(this.font, String.format("§eSayfa %d / %d", this.jutsuPage + 1, maxPages), centerX, navTextY, 0xFFFFFF88);
        }
    }

    private void renderMissionsTab(GuiGraphicsExtractor graphics, INinjaData ninjaData, int centerX, int boxY) {
        int[] scrollState = NinjaMenuMissionsTab.render(
                graphics, this.font, ninjaData, this.missionAcceptButtons,
                this.missionScrollOffset, centerX, boxY, BOX_WIDTH, BOX_HEIGHT);
        this.missionScrollOffset = scrollState[0];
        this.missionMaxScroll = scrollState[1];
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.activeTab == 3 && this.missionMaxScroll > 0) {
            this.missionScrollOffset = Math.max(0, Math.min(this.missionMaxScroll,
                    this.missionScrollOffset - (int) Math.round(scrollY * 18.0)));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
