package com.sekwah.narutomod.client.gui;

import com.sekwah.narutomod.NarutoMod;
import net.minecraft.resources.Identifier;

public class BarDesigns {
    private static final Identifier DEFAULTTEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/defaultbar.png");
    private static final Identifier KATANATEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/katana.png");
    private static final Identifier KUBITEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/kubi.png");
    private static final Identifier KUNAITEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/kunai.png");
    private static final Identifier LAMPSHADETEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/lampshade.png");
    private static final Identifier SCEPTERTEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/scepter.png");
    private static final Identifier SCROLLTEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/scroll.png");
    private static final Identifier UNNAMEDTEXTURE = Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "textures/gui/chakrabars/unnamedbar.png");

    public enum BarInfo {

        DEFAULT(DEFAULTTEXTURE, 86,8),

        KATANA(KATANATEXTURE, 94,1),

        KUBI(KUBITEXTURE, 86,11),

        KUNAI(KUNAITEXTURE, 86,9),

        LAMP(LAMPSHADETEXTURE, 86,8),

        SCEPTER(SCEPTERTEXTURE, 86,9),

        SCROLL(SCROLLTEXTURE, 86,8),

        UNNAMED(UNNAMEDTEXTURE, 74,23),
        ;

        public final Identifier texture;
        public final int width;
        public final int offset;

        BarInfo(Identifier texture, int width, int offset) {

            this.texture = texture;
            this.width = width;
            this.offset = offset;
        }
    }
}
