package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.abilities.Ability;
import com.sekwah.narutomod.abilities.NarutoAbilities;
import com.sekwah.narutomod.capabilities.INinjaData;
import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.capabilities.NinjaRank;
import com.sekwah.narutomod.clans.NarutoClans;
import com.sekwah.narutomod.network.PacketHandler;
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

    private int activeTab = 0; // 0: Profile, 1: Clan Selection, 2: Jutsu Guide
    private int jutsuPage = 0;
    private static final int JUTSUS_PER_PAGE = 4;

    private final List<Ability> allAbilities = new ArrayList<>();

    public NinjaMenuScreen() {
        super(Component.translatable("naruto.gui.ninja_menu.title"));
    }

    @Override
    protected void init() {
        super.init();
        this.clearWidgets();

        int centerX = this.width / 2;
        int topY = 30;

        // Tab Navigation Buttons
        this.addRenderableWidget(Button.builder(Component.literal("Profil"), b -> switchTab(0))
                .bounds(centerX - 160, topY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Klan Seçimi"), b -> switchTab(1))
                .bounds(centerX - 50, topY, 100, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Jutsu Kitabı"), b -> switchTab(2))
                .bounds(centerX + 60, topY, 100, 20).build());

        // Cache abilities
        this.allAbilities.clear();
        NarutoAbilities.ABILITY.getEntries().forEach(entry -> {
            Ability ability = entry.get();
            if (ability.defaultCombo() > 0) {
                this.allAbilities.add(ability);
            }
        });

        if (this.activeTab == 1) {
            initClanTab(centerX, topY);
        } else if (this.activeTab == 2) {
            initJutsuTab(centerX, topY);
        }
    }

    private void switchTab(int tab) {
        this.activeTab = tab;
        this.jutsuPage = 0;
        this.init();
    }

    private void initClanTab(int centerX, int topY) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            boolean isLocked = ninjaData.isClanLocked();

            String[] clans = {"uchiha", "hyuga", "uzumaki", "nara"};
            String[] clanNames = {"Uchiha Klanı", "Hyūga Klanı", "Uzumaki Klanı", "Nara Klanı"};

            int startY = topY + 45;
            for (int i = 0; i < clans.length; i++) {
                final String clanKey = clans[i];
                final ResourceLocation clanLoc = new ResourceLocation(NarutoMod.MOD_ID, clanKey);
                int btnY = startY + (i * 38);

                boolean isCurrentClan = ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase(clanKey);

                Button clanBtn = Button.builder(
                                Component.literal(isCurrentClan ? "Mevcut Klan (" + clanNames[i] + ")" : "Seç: " + clanNames[i]),
                                b -> {
                                    if (!ninjaData.isClanLocked()) {
                                        PacketHandler.sendToServer(new ServerSelectClanPacket(clanLoc));
                                        ninjaData.setClan(clanLoc);
                                        ninjaData.setClanLocked(true);
                                        this.init();
                                    }
                                })
                        .bounds(centerX + 30, btnY + 4, 130, 20)
                        .build();

                clanBtn.active = !isLocked && !isCurrentClan;
                this.addRenderableWidget(clanBtn);
            }
        });
    }

    private void initJutsuTab(int centerX, int topY) {
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));

        if (this.jutsuPage > 0) {
            this.addRenderableWidget(Button.builder(Component.literal("< Önceki"), b -> {
                this.jutsuPage--;
                this.init();
            }).bounds(centerX - 150, this.height - 35, 70, 20).build());
        }

        if (this.jutsuPage < maxPages - 1) {
            this.addRenderableWidget(Button.builder(Component.literal("Sonraki >"), b -> {
                this.jutsuPage++;
                this.init();
            }).bounds(centerX + 80, this.height - 35, 70, 20).build());
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        this.renderBackground(graphics);

        int centerX = this.width / 2;
        int topY = 15;

        // Header Title
        graphics.drawCenteredString(this.font, Component.literal("★ SHINOBI GELİŞİM VE REHBER SİSTEMİ ★").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), centerX, topY, 0xFFFFAA00);

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        mc.player.getCapability(NinjaCapabilityHandler.NINJA_DATA).ifPresent(ninjaData -> {
            if (this.activeTab == 0) {
                renderProfileTab(graphics, ninjaData, centerX, topY + 45);
            } else if (this.activeTab == 1) {
                renderClanTab(graphics, ninjaData, centerX, topY + 45);
            } else if (this.activeTab == 2) {
                renderJutsuTab(graphics, ninjaData, centerX, topY + 45);
            }
        });

        super.render(graphics, mouseX, mouseY, partialTicks);
    }

    private void renderProfileTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int startY) {
        int boxW = 320;
        int boxH = 155;
        int boxX = centerX - boxW / 2;
        int boxY = startY + 10;

        // Dark card background
        graphics.fill(boxX, boxY, boxX + boxW, boxY + boxH, 0xCC111116);
        graphics.renderOutline(boxX, boxY, boxW, boxH, 0xFF555577);

        Minecraft mc = Minecraft.getInstance();
        String playerName = mc.player != null ? mc.player.getName().getString() : "Shinobi";

        int y = boxY + 12;
        int x = boxX + 16;

        graphics.drawString(this.font, "§6§lShinobi: §f" + playerName, x, y, 0xFFFFFFFF);
        y += 16;

        String clanPath = ninjaData.getClan() != null ? ninjaData.getClan().getPath() : "shinobi";
        String clanDisplay = clanPath.substring(0, 1).toUpperCase() + clanPath.substring(1) + " Klanı";
        String lockStatus = ninjaData.isClanLocked() ? " §c[KİLİTLİ - Değiştirilemez]" : " §a[Henüz Seçilmedi]";
        graphics.drawString(this.font, "§eKlan Durumu: §f" + clanDisplay + lockStatus, x, y, 0xFFFFFFFF);
        y += 16;

        NinjaRank rank = NinjaRank.fromString(ninjaData.getRank());
        graphics.drawString(this.font, "§bNinja Rütbesi: §f" + rank.getTurkishName() + " (" + rank.getEnglishName() + ")", x, y, 0xFFFFFFFF);
        y += 20;

        // Chakra Bar
        float chakra = ninjaData.getChakra();
        float maxChakra = ninjaData.getMaxChakra();
        graphics.drawString(this.font, String.format("§9Çakra: §f%.0f / %.0f", chakra, maxChakra), x, y, 0xFFFFFFFF);
        renderBar(graphics, x + 120, y, 160, 8, chakra / maxChakra, 0xFF2255FF, 0xFF001144);
        y += 16;

        // Stamina Bar
        float stamina = ninjaData.getStamina();
        float maxStamina = ninjaData.getMaxStamina();
        graphics.drawString(this.font, String.format("§aKondisyon: §f%.0f / %.0f", stamina, maxStamina), x, y, 0xFFFFFFFF);
        renderBar(graphics, x + 120, y, 160, 8, stamina / maxStamina, 0xFF22DD44, 0xFF003311);
        y += 16;

        // Chakra EXP Bar
        float chakraExp = ninjaData.getChakraExp();
        graphics.drawString(this.font, String.format("§dÇakra Gelişimi: §f%.0f / 100", chakraExp), x, y, 0xFFFFFFFF);
        renderBar(graphics, x + 120, y, 160, 8, chakraExp / 100f, 0xFFAA22EE, 0xFF330044);
        y += 20;

        int unlockedCount = ninjaData.getUnlockedAbilities().size();
        graphics.drawString(this.font, "§7Özel Açılan Jutsular: §6" + unlockedCount + " adet jutsu parşömenle öğrenildi.", x, y, 0xFFCCCCCC);
    }

    private void renderClanTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int startY) {
        int boxW = 320;
        int boxX = centerX - boxW / 2;

        if (ninjaData.isClanLocked()) {
            graphics.fill(boxX, startY, boxX + boxW, startY + 24, 0xEE441111);
            graphics.renderOutline(boxX, startY, boxW, 24, 0xFFFF2222);
            graphics.drawCenteredString(this.font, "§c§lKLANINIZ KALICI OLARAK KİLİTLENDİ!", centerX, startY + 4, 0xFFFF5555);
            graphics.drawCenteredString(this.font, "§7Bir kez klana dahil olduktan sonra klan hiçbir şekilde değiştirilemez.", centerX, startY + 14, 0xFFAAAAAA);
        } else {
            graphics.fill(boxX, startY, boxX + boxW, startY + 24, 0xEE443311);
            graphics.renderOutline(boxX, startY, boxW, 24, 0xFFFFAA00);
            graphics.drawCenteredString(this.font, "§e§lDİKKAT: BİR KEZ SEÇİLEN KLAN DEĞİŞTİRİLEMEZ!", centerX, startY + 4, 0xFFFFAA00);
            graphics.drawCenteredString(this.font, "§fSeçiminizi dikkatli yapınız; klanınız kalıcı olarak mühürlenecektir.", centerX, startY + 14, 0xFFFFFFFF);
        }

        String[] clans = {"uchiha", "hyuga", "uzumaki", "nara"};
        String[] titles = {"Uchiha Klanı (Ateş & Sharingan)", "Hyūga Klanı (Nazik Yumruk & Byakugan)", "Uzumaki Klanı (Devasa Çakra Rezervi)", "Nara Klanı (Gölge & Hızlı Kondisyon)"};
        String[] descs = {
                "Ateş jutsularında ustalık, Sharingan ve Susanoo savunması.",
                "360 derece Byakugan görüşü ve çakra darbeleri.",
                "Muazzam çakra kapasitesi ve hızlı doğal çakra yenilenmesi.",
                "Taktiksel gölge teknikleri ve %30 daha hızlı kondisyon dolumu."
        };

        int cardY = startY + 32;
        for (int i = 0; i < clans.length; i++) {
            boolean isCurrent = ninjaData.getClan() != null && ninjaData.getClan().getPath().equalsIgnoreCase(clans[i]);
            int bgCol = isCurrent ? 0xCC113311 : 0xCC111116;
            int borderCol = isCurrent ? 0xFF22AA22 : 0xFF444455;

            graphics.fill(boxX, cardY, boxX + boxW, cardY + 34, bgCol);
            graphics.renderOutline(boxX, cardY, boxW, 34, borderCol);

            graphics.drawString(this.font, "§6" + titles[i], boxX + 8, cardY + 6, 0xFFFFFFFF);
            graphics.drawString(this.font, "§7" + descs[i], boxX + 8, cardY + 18, 0xFFAAAAAA);

            cardY += 38;
        }
    }

    private void renderJutsuTab(GuiGraphics graphics, INinjaData ninjaData, int centerX, int startY) {
        int boxW = 320;
        int boxX = centerX - boxW / 2;

        int startIndex = jutsuPage * JUTSUS_PER_PAGE;
        int endIndex = Math.min(startIndex + JUTSUS_PER_PAGE, allAbilities.size());
        int maxPages = Math.max(1, (int) Math.ceil((double) allAbilities.size() / JUTSUS_PER_PAGE));

        graphics.drawCenteredString(this.font, String.format("§eSayfa %d / %d", jutsuPage + 1, maxPages), centerX, startY + 5, 0xFFFFFF88);

        int cardY = startY + 18;
        for (int i = startIndex; i < endIndex; i++) {
            Ability ability = allAbilities.get(i);
            boolean unlocked = ninjaData.isAbilityUnlocked(ability);

            int bgCol = unlocked ? 0xCC112211 : 0xCC221111;
            int borderCol = unlocked ? 0xFF228822 : 0xFF882222;

            graphics.fill(boxX, cardY, boxX + boxW, cardY + 36, bgCol);
            graphics.renderOutline(boxX, cardY, boxW, 36, borderCol);

            // Ability Name
            String name = Component.translatable(ability.getTranslationKey()).getString();
            graphics.drawString(this.font, "§f§l" + name, boxX + 8, cardY + 5, 0xFFFFFFFF);

            // Combo & Rank
            String comboText = formatCombo(ability.defaultCombo());
            graphics.drawString(this.font, "§eMühür Kombo: §b" + comboText, boxX + 8, cardY + 16, 0xFFDDDDDD);
            graphics.drawString(this.font, "§7Gereken Rütbe: §a" + ability.getRequiredRank().getTurkishName(), boxX + 8, cardY + 25, 0xFFAAAAAA);

            // Status label
            String status = unlocked ? "§a✔ ÖĞRENİLDİ" : "§c✖ KİLİTLİ";
            graphics.drawString(this.font, status, boxX + boxW - 85, cardY + 12, 0xFFFFFFFF);

            cardY += 40;
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
        graphics.renderOutline(x, y, width, height, 0xFF666666);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
