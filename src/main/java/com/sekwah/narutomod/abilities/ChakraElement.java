package com.sekwah.narutomod.abilities;

/**
 * Fundamental Chakra Nature Elements in Naruto:
 * Fire (Katon), Wind (Fūton), Lightning (Raiton), Earth (Doton), Water (Suiton).
 *
 * Elemental superiority cycle:
 * Fire > Wind > Lightning > Earth > Water > Fire
 */
public enum ChakraElement {
    FIRE("fire"),
    WIND("wind"),
    LIGHTNING("lightning"),
    EARTH("earth"),
    WATER("water"),
    NONE("none");

    private final String name;

    ChakraElement(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    /**
     * Returns true if this element has superiority over the other element according to the canon 5-element cycle:
     * Fire > Wind > Lightning > Earth > Water > Fire
     */
    public boolean isSuperiorTo(ChakraElement other) {
        if (other == null || other == NONE || this == NONE) {
            return false;
        }
        return switch (this) {
            case FIRE -> other == WIND;
            case WIND -> other == LIGHTNING;
            case LIGHTNING -> other == EARTH;
            case EARTH -> other == WATER;
            case WATER -> other == FIRE;
            default -> false;
        };
    }

    /**
     * Returns true if this element is weak against the other element.
     */
    public boolean isWeakTo(ChakraElement other) {
        return other != null && other.isSuperiorTo(this);
    }
}
