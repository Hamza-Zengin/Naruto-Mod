package com.sekwah.narutomod.entity.jutsuprojectile.visual;

import com.sekwah.narutomod.entity.jutsuprojectile.JutsuVisualEffectEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Strategy interface and lookup registry for server-side tick behaviors of {@link JutsuVisualEffectEntity}.
 */
public interface JutsuVisualBehavior {

    JutsuVisualBehavior NO_OP = (entity, serverLevel, tracked) -> {};

    Map<Integer, JutsuVisualBehavior> REGISTRY = createRegistry();

    void tickServer(JutsuVisualEffectEntity entity, ServerLevel serverLevel, @Nullable Entity tracked);

    static JutsuVisualBehavior forType(int visualType) {
        return REGISTRY.getOrDefault(visualType, NO_OP);
    }

    private static Map<Integer, JutsuVisualBehavior> createRegistry() {
        BarrierVisualBehavior barrier = new BarrierVisualBehavior();
        AshCloudVisualBehavior ashCloud = new AshCloudVisualBehavior();
        AreaEffectVisualBehavior areaEffect = new AreaEffectVisualBehavior();
        BindAndAuraVisualBehavior bindAndAura = new BindAndAuraVisualBehavior();

        return Map.ofEntries(
                Map.entry(JutsuVisualEffectEntity.TYPE_WATER_WALL, barrier),
                Map.entry(JutsuVisualEffectEntity.TYPE_EARTH_WALL, barrier),
                Map.entry(JutsuVisualEffectEntity.TYPE_ASH_CLOUD, ashCloud),
                Map.entry(JutsuVisualEffectEntity.TYPE_KIRIN_DRAGON, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_WOOD_PRISON, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_BONE_FOREST, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_WATER_VORTEX, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_WIND_SHOCKWAVE, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_AMATERASU_PILLAR, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_STONE_SPIKES, areaEffect),
                Map.entry(JutsuVisualEffectEntity.TYPE_RASENGAN_SPHERE, bindAndAura),
                Map.entry(JutsuVisualEffectEntity.TYPE_CHIDORI_BURST, bindAndAura),
                Map.entry(JutsuVisualEffectEntity.TYPE_SHADOW_BIND, bindAndAura),
                Map.entry(JutsuVisualEffectEntity.TYPE_ADAMANTINE_CHAINS, bindAndAura)
        );
    }
}
