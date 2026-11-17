package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 3D Model for Giant 4-Bladed Fūma Shuriken
 */
public class FumaShurikenModel extends Model<EntityRenderState> {

    public FumaShurikenModel(ModelPart modelPart) {
        super(modelPart, RenderTypes::entityCutout);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition hub = root.addOrReplaceChild("hub",
                CubeListBuilder.create()
                        // Chamfered central spindle
                        .texOffs(0, 0).addBox(-2.0F, -0.5F, -2.0F, 4, 1, 4),
                PartPose.ZERO);
        hub.addOrReplaceChild("hub_center_rot",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -0.5F, -2.0F, 4, 1, 4),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));

        // 8-segment circular metallic hub ring
        int ringSegments = 8;
        for (int i = 0; i < ringSegments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / ringSegments);
            float radius = 3.2F;
            float rx = (float) Math.cos(angle) * radius;
            float rz = (float) Math.sin(angle) * radius;
            hub.addOrReplaceChild("hub_ring_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-1.4F, -0.5F, -0.7F, 2.8F, 1.0F, 1.4F),
                    PartPose.offsetAndRotation(rx, 0.0F, rz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
        }

        // 4 Swept Curved & Tapered Shuriken Blades
        for (int b = 0; b < 4; b++) {
            float yaw = (float) (b * Math.PI / 2.0);
            PartDefinition bladeRoot = hub.addOrReplaceChild("blade_" + b,
                    CubeListBuilder.create()
                            .texOffs(0, 8).addBox(-1.9F, -0.45F, 3.0F, 3.8F, 0.9F, 4.0F),
                    PartPose.rotation(0.0F, yaw, 0.0F));
            PartDefinition mid = bladeRoot.addOrReplaceChild("blade_mid_" + b,
                    CubeListBuilder.create()
                            .texOffs(0, 8).addBox(-1.5F, -0.4F, 0.0F, 3.0F, 0.8F, 4.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 6.8F, 0.0F, 0.16F, 0.0F));
            PartDefinition outer = mid.addOrReplaceChild("blade_outer_" + b,
                    CubeListBuilder.create()
                            .texOffs(0, 18).addBox(-1.0F, -0.35F, 0.0F, 2.0F, 0.7F, 3.5F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.8F, 0.0F, 0.20F, 0.0F));
            outer.addOrReplaceChild("blade_tip_" + b,
                    CubeListBuilder.create()
                            .texOffs(26, 18).addBox(-0.55F, -0.3F, 0.0F, 1.1F, 0.6F, 2.8F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.3F, 0.0F, 0.22F, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 32);
    }
}
