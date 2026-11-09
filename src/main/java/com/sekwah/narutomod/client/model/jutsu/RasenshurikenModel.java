package com.sekwah.narutomod.client.model.jutsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;

/**
 * 3D Model for Fūton: Rasenshuriken
 * Features a dense spherical Rasengan core surrounded by 4 aerodynamic curved wind-chakra blades.
 */
public class RasenshurikenModel extends Model {

    private final ModelPart root;
    private final ModelPart core;
    private final ModelPart blades;

    public RasenshurikenModel(ModelPart modelPart) {
        super(RenderType::entityTranslucent);
        this.root = modelPart;
        this.core = modelPart.getChild("core");
        this.blades = modelPart.getChild("blades");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Central Rasengan Chakra Sphere
        PartDefinition core = root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8, 8, 8)
                        .texOffs(32, 0)
                        .addBox(-5.0F, -3.0F, -3.0F, 10, 6, 6)
                        .texOffs(0, 16)
                        .addBox(-3.0F, -5.0F, -3.0F, 6, 10, 6),
                PartPose.ZERO);

        // 4 Outer Wind-Chakra Shuriken Blades + Inner Ring
        PartDefinition blades = root.addOrReplaceChild("blades",
                CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-6.0F, -1.0F, -6.0F, 12, 2, 12)
                        // North Blade (+Z)
                        .texOffs(0, 46)
                        .addBox(-3.0F, -0.5F, 5.0F, 6, 1, 11)
                        .texOffs(34, 46)
                        .addBox(-1.5F, -0.5F, 16.0F, 3, 1, 5)
                        // South Blade (-Z)
                        .texOffs(0, 46)
                        .addBox(-3.0F, -0.5F, -16.0F, 6, 1, 11)
                        .texOffs(34, 46)
                        .addBox(-1.5F, -0.5F, -21.0F, 3, 1, 5)
                        // East Blade (+X)
                        .texOffs(24, 16)
                        .addBox(5.0F, -0.5F, -3.0F, 11, 1, 6)
                        .texOffs(24, 24)
                        .addBox(16.0F, -0.5F, -1.5F, 5, 1, 3)
                        // West Blade (-X)
                        .texOffs(24, 16)
                        .addBox(-16.0F, -0.5F, -3.0F, 11, 1, 6)
                        .texOffs(24, 24)
                        .addBox(-21.0F, -0.5F, -1.5F, 5, 1, 3),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setSpinAngle(float coreSpin, float bladeSpin) {
        this.core.yRot = coreSpin;
        this.core.xRot = coreSpin * 0.7F;
        this.blades.yRot = bladeSpin;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay,
                               float r, float g, float b, float a) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, r, g, b, a);
    }
}
