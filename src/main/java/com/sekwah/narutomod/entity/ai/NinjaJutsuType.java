package com.sekwah.narutomod.entity.ai;

import com.sekwah.narutomod.abilities.ChakraElement;

/**
 * Tactical jutsu types usable by Ninja NPCs via {@link NinjaJutsuAttackGoal}.
 */
public enum NinjaJutsuType {
    FIREBALL(ChakraElement.FIRE, 0xFF5511),
    WATER_BULLET(ChakraElement.WATER, 0x2299FF),
    MUD_WALL(ChakraElement.EARTH, 0xAA7733),
    KAWARIMI(ChakraElement.NONE, 0x77CCFF);

    private final ChakraElement element;
    private final int colorHex;

    NinjaJutsuType(ChakraElement element, int colorHex) {
        this.element = element;
        this.colorHex = colorHex;
    }

    public ChakraElement getElement() {
        return this.element;
    }

    public int getColorHex() {
        return this.colorHex;
    }
}
