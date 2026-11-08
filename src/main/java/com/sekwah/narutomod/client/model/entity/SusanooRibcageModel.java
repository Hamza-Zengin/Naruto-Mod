package com.sekwah.narutomod.client.model.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

/**
 * 3D Skeletal Chakra Ribcage of Susanoo enveloping the shinobi.
 */
public class SusanooRibcageModel<T extends LivingEntity> extends HumanoidModel<T> {

    public SusanooRibcageModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // Attach ribcage structure to the body part so it moves, rotates, and bends with player torso
        PartDefinition bodyPart = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        // Spine along the back (slightly raised and thickened)
        bodyPart.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.8F, -1.0F, 2.5F, 3.6F, 15.0F, 2.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Front Sternum (Chest connector)
        bodyPart.addOrReplaceChild("sternum", CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(-1.0F, 0.0F, -4.6F, 2.0F, 13.0F, 1.4F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 1: Top Ribs (Upper Chest)
        bodyPart.addOrReplaceChild("rib_top_left", CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(2.0F, 0.5F, -4.0F, 4.5F, 1.4F, 8.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_top_right", CubeListBuilder.create()
                .texOffs(16, 0).mirror()
                .addBox(-6.5F, 0.5F, -4.0F, 4.5F, 1.4F, 8.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 2: Mid-Upper Ribs
        bodyPart.addOrReplaceChild("rib_mid_left", CubeListBuilder.create()
                .texOffs(16, 10)
                .addBox(2.5F, 3.8F, -4.2F, 4.5F, 1.4F, 8.4F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_mid_right", CubeListBuilder.create()
                .texOffs(16, 10).mirror()
                .addBox(-7.0F, 3.8F, -4.2F, 4.5F, 1.4F, 8.4F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 3: Mid-Lower Ribs
        bodyPart.addOrReplaceChild("rib_bot_left", CubeListBuilder.create()
                .texOffs(16, 20)
                .addBox(2.5F, 7.2F, -4.0F, 4.5F, 1.4F, 8.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_bot_right", CubeListBuilder.create()
                .texOffs(16, 20).mirror()
                .addBox(-7.0F, 7.2F, -4.0F, 4.5F, 1.4F, 8.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 4: Lower Flank Ribs
        bodyPart.addOrReplaceChild("rib_low_left", CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(2.0F, 10.5F, -3.6F, 4.0F, 1.4F, 7.2F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_low_right", CubeListBuilder.create()
                .texOffs(16, 0).mirror()
                .addBox(-6.0F, 10.5F, -3.6F, 4.0F, 1.4F, 7.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        // Scale around torso center so it surrounds the player with clear ethereal gap, without touching armor
        poseStack.translate(0.0F, 0.35F, 0.0F);
        poseStack.scale(1.42F, 1.25F, 1.45F);
        poseStack.translate(0.0F, -0.35F, 0.0F);
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
