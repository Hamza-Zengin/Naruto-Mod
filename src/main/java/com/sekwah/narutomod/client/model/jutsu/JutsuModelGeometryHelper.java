package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * Shared procedural geometry helpers for circular rings in 3D jutsu models.
 */
public final class JutsuModelGeometryHelper {

    private JutsuModelGeometryHelper() {
    }

    /**
     * Adds a horizontal circular ring of box segments in the XZ plane around the Y axis.
     */
    public static void addCircularXZRing(PartDefinition parent, String namePrefix, int segments, float radius,
                                         float y, float xRot, float zRot, int texU, int texV,
                                         float boxX, float boxY, float boxZ,
                                         float boxW, float boxH, float boxD) {
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segments);
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            parent.addOrReplaceChild(namePrefix + i,
                    CubeListBuilder.create().texOffs(texU, texV).addBox(boxX, boxY, boxZ, boxW, boxH, boxD),
                    PartPose.offsetAndRotation(px, y, pz, xRot, -angle + (float) (Math.PI / 2.0), zRot));
        }
    }

    /**
     * Adds a vertical circular ring of box segments in the XY plane around the Z travel axis.
     */
    public static void addCircularZRing(PartDefinition parent, String prefix, float radius, float zPos,
                                        float thick, float depth, int segments, float overlap,
                                        float tiltPitch, int u, int v) {
        float segLen = (radius * 2.0F * 3.14159F / segments) + overlap;
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segments);
            float px = Mth.cos(angle) * radius;
            float py = Mth.sin(angle) * radius;
            parent.addOrReplaceChild(prefix + "_seg_" + i,
                    CubeListBuilder.create().texOffs(u, v).addBox(-segLen * 0.5F, -thick * 0.5F, -depth * 0.5F, segLen, thick, depth),
                    PartPose.offsetAndRotation(px, py, zPos, tiltPitch, 0.0F, angle + (float) (Math.PI / 2.0)));
        }
    }
}
