package com.sekwah.narutomod;

import com.sekwah.narutomod.abilities.ChakraElement;
import com.sekwah.narutomod.abilities.jutsus.*;
import com.sekwah.narutomod.abilities.utility.WaterWalkAbility;
import com.sekwah.narutomod.entity.jutsuprojectile.FireballJutsuEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.JutsuProjectile;
import com.sekwah.narutomod.entity.jutsuprojectile.MudWallEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.RasenshurikenEntity;
import com.sekwah.narutomod.entity.jutsuprojectile.WaterBulletEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests verifying Elemental Superiority and Jutsu Clash mechanics:
 * Canon 5-element cycle: Fire > Wind > Lightning > Earth > Water > Fire
 */
public class JutsuClashLogicTest {

    @Test
    @DisplayName("Verify canon 5-element superiority cycle")
    public void testElementalSuperiorityCycle() {
        // Fire > Wind
        assertTrue(ChakraElement.FIRE.isSuperiorTo(ChakraElement.WIND), "Fire must be superior to Wind");
        assertTrue(ChakraElement.WIND.isWeakTo(ChakraElement.FIRE), "Wind must be weak to Fire");

        // Wind > Lightning
        assertTrue(ChakraElement.WIND.isSuperiorTo(ChakraElement.LIGHTNING), "Wind must be superior to Lightning");
        assertTrue(ChakraElement.LIGHTNING.isWeakTo(ChakraElement.WIND), "Lightning must be weak to Wind");

        // Lightning > Earth
        assertTrue(ChakraElement.LIGHTNING.isSuperiorTo(ChakraElement.EARTH), "Lightning must be superior to Earth");
        assertTrue(ChakraElement.EARTH.isWeakTo(ChakraElement.LIGHTNING), "Earth must be weak to Lightning");

        // Earth > Water
        assertTrue(ChakraElement.EARTH.isSuperiorTo(ChakraElement.WATER), "Earth must be superior to Water");
        assertTrue(ChakraElement.WATER.isWeakTo(ChakraElement.EARTH), "Water must be weak to Earth");

        // Water > Fire
        assertTrue(ChakraElement.WATER.isSuperiorTo(ChakraElement.FIRE), "Water must be superior to Fire");
        assertTrue(ChakraElement.FIRE.isWeakTo(ChakraElement.WATER), "Fire must be weak to Water");
    }

    @Test
    @DisplayName("Verify non-superior and neutral elemental relationships")
    public void testNonSuperiorAndNeutralInteractions() {
        // Inverted checks must be false
        assertFalse(ChakraElement.WIND.isSuperiorTo(ChakraElement.FIRE));
        assertFalse(ChakraElement.LIGHTNING.isSuperiorTo(ChakraElement.WIND));
        assertFalse(ChakraElement.EARTH.isSuperiorTo(ChakraElement.LIGHTNING));
        assertFalse(ChakraElement.WATER.isSuperiorTo(ChakraElement.EARTH));
        assertFalse(ChakraElement.FIRE.isSuperiorTo(ChakraElement.WATER));

        // Same element
        assertFalse(ChakraElement.FIRE.isSuperiorTo(ChakraElement.FIRE));
        assertFalse(ChakraElement.WATER.isSuperiorTo(ChakraElement.WATER));

        // NONE interactions
        assertFalse(ChakraElement.NONE.isSuperiorTo(ChakraElement.FIRE));
        assertFalse(ChakraElement.FIRE.isSuperiorTo(ChakraElement.NONE));
        assertFalse(ChakraElement.FIRE.isSuperiorTo(null));
    }

    @Test
    @DisplayName("Verify Ability classes return their canon ChakraElement")
    public void testAbilityElementAssignments() {
        assertEquals(ChakraElement.FIRE, new FireballJutsuAbility().getChakraElement());
        assertEquals(ChakraElement.FIRE, new AmaterasuAbility().getChakraElement());
        assertEquals(ChakraElement.WIND, new RasenshurikenAbility().getChakraElement());
        assertEquals(ChakraElement.WIND, new WindBreakerAbility().getChakraElement());
        assertEquals(ChakraElement.LIGHTNING, new ChidoriAbility().getChakraElement());
        assertEquals(ChakraElement.LIGHTNING, new KirinAbility().getChakraElement());
        assertEquals(ChakraElement.EARTH, new MudWallAbility().getChakraElement());
        assertEquals(ChakraElement.WATER, new WaterBulletAbility().getChakraElement());
        assertEquals(ChakraElement.WATER, new WaterWalkAbility().getChakraElement());
    }

    @Test
    @DisplayName("Verify JutsuProjectile interface implementation on jutsu entity classes")
    public void testEntityJutsuProjectileImplementation() {
        assertTrue(JutsuProjectile.class.isAssignableFrom(FireballJutsuEntity.class));
        assertTrue(JutsuProjectile.class.isAssignableFrom(RasenshurikenEntity.class));
        assertTrue(JutsuProjectile.class.isAssignableFrom(WaterBulletEntity.class));
        assertTrue(JutsuProjectile.class.isAssignableFrom(MudWallEntity.class));
    }

    /**
     * Test mock implementation of JutsuProjectile to verify clash resolution logic.
     */
    private static class MockJutsuProjectile implements JutsuProjectile {
        private final ChakraElement element;
        private int power;
        private boolean exploded = false;

        public MockJutsuProjectile(ChakraElement element, int power) {
            this.element = element;
            this.power = power;
        }

        @Override
        public ChakraElement getElement() {
            return this.element;
        }

        @Override
        public int getJutsuPower() {
            return this.power;
        }

        @Override
        public void weaken(int amount) {
            this.power = Math.max(0, this.power - amount);
            if (this.power <= 0) {
                this.explodeAndDiscard();
            }
        }

        @Override
        public void explodeAndDiscard() {
            this.exploded = true;
        }

        public boolean isExploded() {
            return this.exploded;
        }

        public void resolveMockClash(MockJutsuProjectile other) {
            ChakraElement thisElem = this.getElement();
            ChakraElement otherElem = other.getElement();

            if (thisElem.isSuperiorTo(otherElem)) {
                int otherP = other.getJutsuPower();
                other.explodeAndDiscard();
                this.weaken(Math.max(1, otherP / 2));
            } else if (otherElem.isSuperiorTo(thisElem)) {
                int thisP = this.getJutsuPower();
                this.explodeAndDiscard();
                other.weaken(Math.max(1, thisP / 2));
            } else {
                this.explodeAndDiscard();
                other.explodeAndDiscard();
            }
        }
    }

    @Test
    @DisplayName("Verify superior projectile destroys inferior projectile and weakens")
    public void testSuperiorElementClash() {
        MockJutsuProjectile fire = new MockJutsuProjectile(ChakraElement.FIRE, 60);
        MockJutsuProjectile wind = new MockJutsuProjectile(ChakraElement.WIND, 40);

        fire.resolveMockClash(wind);

        // Fire > Wind: Wind must be destroyed, Fire survives with weakened power
        assertTrue(wind.isExploded(), "Inferior Wind projectile must explode and be discarded");
        assertFalse(fire.isExploded(), "Superior Fire projectile must survive");
        assertEquals(60 - 20, fire.getJutsuPower(), "Fire power should be weakened by half of wind power");
    }

    @Test
    @DisplayName("Verify water projectile extinguishes fire projectile")
    public void testWaterExtinguishesFireClash() {
        MockJutsuProjectile water = new MockJutsuProjectile(ChakraElement.WATER, 50);
        MockJutsuProjectile fire = new MockJutsuProjectile(ChakraElement.FIRE, 60);

        water.resolveMockClash(fire);

        // Water > Fire: Fire must be destroyed, Water survives with weakened power
        assertTrue(fire.isExploded(), "Fire projectile must be extinguished/destroyed by Water");
        assertFalse(water.isExploded(), "Water projectile must survive");
        assertEquals(50 - 30, water.getJutsuPower());
    }

    @Test
    @DisplayName("Verify equal or neutral elemental clash results in mutual destruction")
    public void testEqualAndNeutralClash() {
        MockJutsuProjectile fireA = new MockJutsuProjectile(ChakraElement.FIRE, 50);
        MockJutsuProjectile fireB = new MockJutsuProjectile(ChakraElement.FIRE, 50);

        fireA.resolveMockClash(fireB);

        assertTrue(fireA.isExploded(), "Equal element projectile A must explode");
        assertTrue(fireB.isExploded(), "Equal element projectile B must explode");

        // Neutral interaction: Fire vs Earth
        MockJutsuProjectile fire = new MockJutsuProjectile(ChakraElement.FIRE, 50);
        MockJutsuProjectile earth = new MockJutsuProjectile(ChakraElement.EARTH, 50);

        fire.resolveMockClash(earth);
        assertTrue(fire.isExploded(), "Neutral clash projectile must explode");
        assertTrue(earth.isExploded(), "Neutral clash projectile must explode");
    }
}
