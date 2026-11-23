package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.capabilities.NinjaCapabilityHandler;
import com.sekwah.narutomod.config.NarutoConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ChakraAndStaminaGUI {

    public static final BarDesigns.BarInfo[] barTypes = BarDesigns.BarInfo.values();

    private static final float MIN_ALPHA = 0.15f;
    private static final float MAX_ALPHA = 1.0f;
    private static final float BAR_SMOOTH_SPEED = 20.0f;

    private static final int CHAKRA_RGB = 0xFF0000;
    private static final int CHAKRA_DARKER_RGB = 0x3F0000;
    private static final int STAMINA_RGB = 0x00FF00;
    private static final int STAMINA_DARKER_RGB = 0x003F00;

    private final Minecraft minecraft;
    private int screenWidth;
    private int screenHeight;

    private boolean shouldRender;

    private float chakra;
    private float stamina;

    private float maxChakra;
    private float maxStamina;

    private float displayChakra = -1.0f;
    private float displayStamina = -1.0f;
    private long lastRenderTimeNanos = 0L;

    private int lastChakraInt = Integer.MIN_VALUE;
    private int lastMaxChakraInt = Integer.MIN_VALUE;
    private String cachedChakraText = "0/0";

    private int lastStaminaInt = Integer.MIN_VALUE;
    private int lastMaxStaminaInt = Integer.MIN_VALUE;
    private String cachedStaminaText = "0/0";

    private float hudAlpha = MAX_ALPHA;
    private int fadeDelayTimer = 0;
    private float prevChakra = -1.0f;
    private float prevStamina = -1.0f;
    private float prevHealth = -1.0f;

    public ChakraAndStaminaGUI(Minecraft mc) {
        this.minecraft = mc;
    }

    private Minecraft getMinecraft() {
        return this.minecraft != null ? this.minecraft : Minecraft.getInstance();
    }

    public void render(GuiGraphicsExtractor guiGraphics) {
        Minecraft mc = this.getMinecraft();
        if(!shouldRender || mc == null) {
            return;
        }
        this.screenWidth = mc.getWindow().getGuiScaledWidth();
        this.screenHeight = mc.getWindow().getGuiScaledHeight();
        int barDesign = Math.min(Math.max(NarutoConfig.chakraBarDesign, 0), barTypes.length - 1);

        long now = System.nanoTime();
        float dt = this.lastRenderTimeNanos == 0L
                ? (1.0f / 60.0f)
                : Mth.clamp((now - this.lastRenderTimeNanos) * 1.0e-9f, 0.0f, 0.1f);
        this.lastRenderTimeNanos = now;

        if (this.displayChakra < 0.0f) {
            this.displayChakra = this.chakra;
        } else if (Math.abs(this.chakra - this.displayChakra) < 0.01f) {
            this.displayChakra = this.chakra;
        } else {
            float t = 1.0f - (float) Math.exp(-BAR_SMOOTH_SPEED * dt);
            this.displayChakra = Mth.lerp(t, this.displayChakra, this.chakra);
        }

        if (this.displayStamina < 0.0f) {
            this.displayStamina = this.stamina;
        } else if (Math.abs(this.stamina - this.displayStamina) < 0.01f) {
            this.displayStamina = this.stamina;
        } else {
            float t = 1.0f - (float) Math.exp(-BAR_SMOOTH_SPEED * dt);
            this.displayStamina = Mth.lerp(t, this.displayStamina, this.stamina);
        }

        float currentChakraPercent = maxChakra > 0 ? Mth.clamp(this.displayChakra / maxChakra, 0.0f, 1.0f) : 0.0f;
        float currentStaminaPercent = maxStamina > 0 ? Mth.clamp(this.displayStamina / maxStamina, 0.0f, 1.0f) : 0.0f;

        int width = 100;
        int offset = 128;

        Identifier barTexture = barTypes[barDesign].texture;
        int barWidth = barTypes[barDesign].width;
        int xOffset = barTypes[barDesign].offset;

        int valuesOffset = 128 + (barWidth / 2);
        int valuesHeight = 26;

        int screenMid = this.screenWidth / 2;

        int alphaByte = Mth.clamp((int) (this.hudAlpha * 255.0f), 0, 255);
        int underlayColor = ARGB.color(alphaByte, 255, 255, 255);

        int intStaminaColor = ARGB.color(alphaByte, STAMINA_RGB);
        int intChakraColor = ARGB.color(alphaByte, CHAKRA_RGB);
        int intStaminaColorDarker = ARGB.color(alphaByte, STAMINA_DARKER_RGB);
        int intChakraColorDarker = ARGB.color(alphaByte, CHAKRA_DARKER_RGB);

        // Chakra Bar underlay
        int chakraWidth = (int) (barWidth * currentChakraPercent);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid - width - offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                width, 44, underlayColor);

        // Stamina Bar underlay
        int staminaWidth = (int) (barWidth * currentStaminaPercent);
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid + offset, this.screenHeight - 22,
                0, 22,
                width, 22,
                width, 44, underlayColor);

        // Chakra Bar color
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid - chakraWidth - offset - (width - xOffset - barWidth), this.screenHeight - 22,
                xOffset + (barWidth - chakraWidth), 0,
                chakraWidth, 22,
                width, 44, intChakraColor);

        // Stamina Bar color
        guiGraphics.blit(RenderPipelines.GUI_TEXTURED, barTexture, screenMid + offset + (100 - barWidth - xOffset), this.screenHeight - 22,
                xOffset, 0,
                staminaWidth, 22,
                width, 44, intStaminaColor);

        int chakraInt = (int) chakra;
        int maxChakraInt = (int) maxChakra;
        if (chakraInt != this.lastChakraInt || maxChakraInt != this.lastMaxChakraInt) {
            this.lastChakraInt = chakraInt;
            this.lastMaxChakraInt = maxChakraInt;
            this.cachedChakraText = chakraInt + "/" + maxChakraInt;
        }
        this.centeredTextOutlined(guiGraphics,
                this.cachedChakraText,
                screenMid - valuesOffset,
                this.screenHeight - valuesHeight,
                intChakraColor,
                intChakraColorDarker);

        int staminaInt = (int) stamina;
        int maxStaminaInt = (int) maxStamina;
        if (staminaInt != this.lastStaminaInt || maxStaminaInt != this.lastMaxStaminaInt) {
            this.lastStaminaInt = staminaInt;
            this.lastMaxStaminaInt = maxStaminaInt;
            this.cachedStaminaText = staminaInt + "/" + maxStaminaInt;
        }
        this.centeredTextOutlined(guiGraphics,
                this.cachedStaminaText,
                screenMid + valuesOffset,
                this.screenHeight - valuesHeight,
                intStaminaColor,
                intStaminaColorDarker);

    }

    private void centeredTextOutlined(GuiGraphicsExtractor guiGraphics, String text, int x, int y, int color, int backgroundColor) {
        int width = this.getFont().width(text) / 2;
        guiGraphics.text(this.getFont(), text, x + 1 - width, y, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - 1 - width, y, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - width, y + 1, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - width, y - 1, backgroundColor, false);
        guiGraphics.text(this.getFont(), text, x - width, y, color, false);
    }

    public void tick() {
        Minecraft mc = this.getMinecraft();
        if(mc != null && mc.getCameraEntity() instanceof Player player) {
            boolean wasShouldRender = this.shouldRender;
            shouldRender = true;
            var ninjaData = player.getData(NinjaCapabilityHandler.NINJA_DATA);
            chakra = ninjaData.getChakra();
            stamina = ninjaData.getStamina();
            maxChakra = ninjaData.getMaxChakra();
            maxStamina = ninjaData.getMaxStamina();

            if (!wasShouldRender || this.displayChakra < 0.0f
                    || Math.abs(this.chakra - this.displayChakra) > Math.max(50.0f, this.maxChakra * 0.5f)) {
                this.displayChakra = this.chakra;
            }
            if (!wasShouldRender || this.displayStamina < 0.0f
                    || Math.abs(this.stamina - this.displayStamina) > Math.max(50.0f, this.maxStamina * 0.5f)) {
                this.displayStamina = this.stamina;
            }

            if (!NarutoConfig.autoHideHud) {
                this.hudAlpha = MAX_ALPHA;
                this.fadeDelayTimer = 0;
            } else {
                boolean chakraChanged = this.prevChakra >= 0 && Math.abs(chakra - this.prevChakra) > 0.001f;
                boolean staminaChanged = this.prevStamina >= 0 && Math.abs(stamina - this.prevStamina) > 0.001f;
                boolean tookDamage = player.hurtTime > 0 || (this.prevHealth >= 0 && player.getHealth() < this.prevHealth);
                boolean isWheelOpen = JutsuWheelOverlay.isOpen();
                boolean resourcesNotFull = chakra < maxChakra || stamina < maxStamina;

                if (chakraChanged || staminaChanged || tookDamage || isWheelOpen || resourcesNotFull) {
                    this.hudAlpha = MAX_ALPHA;
                    this.fadeDelayTimer = NarutoConfig.hudFadeDelayTicks;
                } else if (this.fadeDelayTimer > 0) {
                    this.fadeDelayTimer--;
                } else if (this.hudAlpha > MIN_ALPHA) {
                    this.hudAlpha = Mth.lerp(0.1f, this.hudAlpha, MIN_ALPHA);
                    if (this.hudAlpha - MIN_ALPHA < 0.005f) {
                        this.hudAlpha = MIN_ALPHA;
                    }
                }
            }

            this.prevChakra = chakra;
            this.prevStamina = stamina;
            this.prevHealth = player.getHealth();
        } else {
            shouldRender = false;
            this.displayChakra = -1.0f;
            this.displayStamina = -1.0f;
            this.lastRenderTimeNanos = 0L;
        }
    }

    private Font getFont() {
        Minecraft mc = this.getMinecraft();
        return mc != null ? mc.font : null;
    }
}
