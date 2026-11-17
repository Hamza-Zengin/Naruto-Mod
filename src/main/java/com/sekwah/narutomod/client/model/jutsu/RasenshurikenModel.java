package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 3D Model for Fūton: Rasenshuriken
 * Features a dense spherical Rasengan core surrounded by 4 aerodynamic curved wind-chakra blades.
 */
public class RasenshurikenModel extends Model<RasenshurikenModel.SpinState> {

    public record SpinState(float coreSpin, float bladeSpin) {}

    private final ModelPart core;
    private final ModelPart blades;

    public RasenshurikenModel(ModelPart modelPart) {
        super(modelPart, RenderTypes::entityTranslucent);
        this.core = modelPart.getChild("core");
        this.blades = modelPart.getChild("blades");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Central Chamfered Multi-Angle Rasengan Chakra Core
        PartDefinition core = root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -3.5F, -3.5F, 7, 7, 7),
                PartPose.ZERO);
        core.addOrReplaceChild("core_rot_y",
                CubeListBuilder.create().texOffs(32, 0).addBox(-3.5F, -2.5F, -3.5F, 7, 5, 7),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));
        core.addOrReplaceChild("core_rot_x",
                CubeListBuilder.create().texOffs(0, 16).addBox(-2.5F, -3.5F, -3.5F, 5, 7, 7),
                PartPose.rotation(0.7854F, 0.0F, 0.0F));
        core.addOrReplaceChild("core_rot_diag",
                CubeListBuilder.create().texOffs(32, 0).addBox(-3.2F, -3.2F, -3.2F, 6.4F, 6.4F, 6.4F),
                PartPose.rotation(0.5236F, 0.7854F, 0.5236F));

        // 4 Swept Curved Wind-Chakra Shuriken Blades + 12-Segment Circular Ring
        PartDefinition blades = root.addOrReplaceChild("blades", CubeListBuilder.create(), PartPose.ZERO);

        int ringSegments = 12;
        for (int i = 0; i < ringSegments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / ringSegments);
            float radius = 5.6F;
            float rx = (float) Math.cos(angle) * radius;
            float rz = (float) Math.sin(angle) * radius;
            blades.addOrReplaceChild("ring_seg_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 32).addBox(-1.6F, -0.7F, -0.9F, 3.2F, 1.4F, 1.8F),
                    PartPose.offsetAndRotation(rx, 0.0F, rz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
        }

        // 4 Smoothly Curved, Tapered Shuriken Blades (4 articulated segments per blade)
        for (int b = 0; b < 4; b++) {
            float yaw = (float) (b * Math.PI / 2.0);
            PartDefinition bladeRoot = blades.addOrReplaceChild("blade_" + b,
                    CubeListBuilder.create()
                            .texOffs(0, 46).addBox(-2.8F, -0.5F, 5.0F, 5.6F, 1.0F, 4.5F),
                    PartPose.rotation(0.0F, yaw, 0.08F));
            PartDefinition seg1 = bladeRoot.addOrReplaceChild("bseg_1_" + b,
                    CubeListBuilder.create()
                            .texOffs(0, 46).addBox(-2.4F, -0.45F, 0.0F, 4.8F, 0.9F, 4.5F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 9.2F, 0.0F, 0.16F, 0.0F));
            PartDefinition seg2 = seg1.addOrReplaceChild("bseg_2_" + b,
                    CubeListBuilder.create()
                            .texOffs(24, 16).addBox(-1.8F, -0.4F, 0.0F, 3.6F, 0.8F, 4.2F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 4.2F, 0.0F, 0.20F, 0.0F));
            seg2.addOrReplaceChild("bseg_3_" + b,
                    CubeListBuilder.create()
                            .texOffs(34, 46).addBox(-0.9F, -0.35F, 0.0F, 1.8F, 0.7F, 4.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.9F, 0.0F, 0.24F, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(SpinState state) {
        super.setupAnim(state);
        setSpinAngle(state.coreSpin(), state.bladeSpin());
    }

    public void setSpinAngle(float coreSpin, float bladeSpin) {
        this.core.yRot = coreSpin;
        this.core.xRot = coreSpin * 0.7F;
        this.blades.yRot = bladeSpin;
    }
}
