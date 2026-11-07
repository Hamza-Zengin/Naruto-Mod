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

    private final ModelPart body;

    public SusanooRibcageModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        // Attach ribcage structure to the body part so it moves, rotates, and bends with player torso
        PartDefinition bodyPart = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        // Spine along the back (slightly raised and thickened)
        bodyPart.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-2.0F, -1.0F, 2.5F, 4.0F, 14.0F, 3.0F, new CubeDeformation(0.2F)),
                PartPose.ZERO);

        // Pair 1: Top Ribs (Upper Chest)
        bodyPart.addOrReplaceChild("rib_top_left", CubeListBuilder.create()
                .texOffs(14, 0)
                .addBox(2.0F, 0.0F, -3.5F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_top_right", CubeListBuilder.create()
                .texOffs(14, 0).mirror()
                .addBox(-6.0F, 0.0F, -3.5F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_top_front", CubeListBuilder.create()
                .texOffs(0, 17)
                .addBox(-4.0F, 0.0F, -4.5F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        // Pair 2: Mid Ribs (Mid Chest)
        bodyPart.addOrReplaceChild("rib_mid_left", CubeListBuilder.create()
                .texOffs(14, 9)
                .addBox(2.2F, 3.5F, -3.8F, 4.0F, 2.0F, 7.5F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_mid_right", CubeListBuilder.create()
                .texOffs(14, 9).mirror()
                .addBox(-6.2F, 3.5F, -3.8F, 4.0F, 2.0F, 7.5F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_mid_front", CubeListBuilder.create()
                .texOffs(0, 21)
                .addBox(-4.5F, 3.5F, -4.8F, 9.0F, 2.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        // Pair 3: Bottom Ribs (Lower Torso)
        bodyPart.addOrReplaceChild("rib_bot_left", CubeListBuilder.create()
                .texOffs(14, 18)
                .addBox(2.0F, 7.0F, -3.5F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_bot_right", CubeListBuilder.create()
                .texOffs(14, 18).mirror()
                .addBox(-6.0F, 7.0F, -3.5F, 4.0F, 2.0F, 7.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_bot_front", CubeListBuilder.create()
                .texOffs(0, 25)
                .addBox(-4.0F, 7.0F, -4.5F, 8.0F, 2.0F, 2.0F, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        // Scale slightly around the torso for a grand ethereal presence
        poseStack.scale(1.22F, 1.15F, 1.25F);
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
