package com.sekwah.narutomod.client.model.jutsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;

/**
 * 3D Model for Katon: Gōkakyū no Jutsu (Great Fireball)
 * Features an incandescent white-gold inner core, a roiling crimson-orange outer sphere,
 * and counter-rotating 3D solar-flare corona rings.
 */
public class FireballJutsuModel extends Model {

    private final ModelPart root;
    private final ModelPart innerCore;
    private final ModelPart outerSphere;
    private final ModelPart flameCorona;

    public FireballJutsuModel(ModelPart modelPart) {
        super(RenderType::entityTranslucent);
        this.root = modelPart;
        this.innerCore = modelPart.getChild("inner_core");
        this.outerSphere = modelPart.getChild("outer_sphere");
        this.flameCorona = modelPart.getChild("flame_corona");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Incandescent White-Yellow Inner Core
        root.addOrReplaceChild("inner_core",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -5.0F, -5.0F, 10, 10, 10)
                        .texOffs(40, 0)
                        .addBox(-6.0F, -4.0F, -4.0F, 12, 8, 8)
                        .texOffs(80, 0)
                        .addBox(-4.0F, -6.0F, -4.0F, 8, 12, 8),
                PartPose.ZERO);

        // 2. Roiling Orange-Crimson Outer Fireball Sphere
        root.addOrReplaceChild("outer_sphere",
                CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-8.0F, -8.0F, -8.0F, 16, 16, 16)
                        .texOffs(64, 20)
                        .addBox(-9.5F, -6.5F, -6.5F, 19, 13, 13)
                        .texOffs(0, 52)
                        .addBox(-6.5F, -9.5F, -6.5F, 13, 19, 13)
                        .texOffs(52, 52)
                        .addBox(-6.5F, -6.5F, -9.5F, 13, 13, 19),
                PartPose.ZERO);

        // 3. Swirling 3D Flame Corona & Solar Flare Fins
        root.addOrReplaceChild("flame_corona",
                CubeListBuilder.create()
                        .texOffs(0, 84)
                        .addBox(-11.0F, -2.0F, -11.0F, 22, 4, 22)
                        .texOffs(0, 110)
                        .addBox(-2.0F, -11.0F, -11.0F, 4, 22, 22)
                        .texOffs(66, 84)
                        .addBox(-11.0F, -11.0F, -2.0F, 22, 22, 4),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    public void setAnimationAngles(float ageInTicks) {
        this.innerCore.yRot = ageInTicks * 0.35F;
        this.innerCore.xRot = ageInTicks * 0.25F;
        this.outerSphere.yRot = -ageInTicks * 0.22F;
        this.outerSphere.zRot = ageInTicks * 0.18F;
        this.flameCorona.yRot = ageInTicks * 0.48F;
        this.flameCorona.xRot = -ageInTicks * 0.32F;
    }

    @Override
    public void renderToBuffer(PoseStack matrixStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay,
                               float r, float g, float b, float a) {
        matrixStack.pushPose();
        float scale = 1.65F;
        matrixStack.scale(scale, scale, scale);
        this.root.render(matrixStack, vertexConsumer, packedLight, packedOverlay, r, g, b, a);
        matrixStack.popPose();
    }
}

