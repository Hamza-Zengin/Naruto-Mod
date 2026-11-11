package com.sekwah.narutomod.entity.jutsuprojectile;

import com.sekwah.narutomod.abilities.ChakraElement;
import net.minecraft.world.entity.Entity;

/**
 * Interface implemented by jutsu projectile entities to enable authentic elemental clash mechanics:
 * Fire (Katon) > Wind (Fūton) > Lightning (Raiton) > Earth (Doton) > Water (Suiton) > Fire (Katon)
 */
public interface JutsuProjectile {

    ChakraElement getElement();

    int getJutsuPower();

    void weaken(int amount);

    void explodeAndDiscard();

    /**
     * Resolves the clash between this projectile and another JutsuProjectile based on elemental superiority:
     * - Superior element destroys the inferior projectile while weakening itself.
     * - Equal or neutral elements cancel each other out in a mutual explosion.
     */
    default void handleClash(JutsuProjectile other) {
        if (!(this instanceof Entity thisEntity) || !(other instanceof Entity otherEntity)) {
            return;
        }
        if (thisEntity.level().isClientSide || !thisEntity.isAlive() || !otherEntity.isAlive()) {
            return;
        }

        ChakraElement thisElem = this.getElement();
        ChakraElement otherElem = other.getElement();

        if (thisElem.isSuperiorTo(otherElem)) {
            // This entity has elemental superiority: destroys other and weakens
            int otherPower = other.getJutsuPower();
            other.explodeAndDiscard();
            this.weaken(Math.max(1, otherPower / 2));
        } else if (otherElem.isSuperiorTo(thisElem)) {
            // Other entity has elemental superiority: destroys this and weakens
            int thisPower = this.getJutsuPower();
            this.explodeAndDiscard();
            other.weaken(Math.max(1, thisPower / 2));
        } else {
            // Equal or neutral elements: both explode and discard
            this.explodeAndDiscard();
            other.explodeAndDiscard();
        }
    }
}
