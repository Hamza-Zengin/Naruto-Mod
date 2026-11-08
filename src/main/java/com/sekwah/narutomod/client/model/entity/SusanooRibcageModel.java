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

        // Spine along the back (centered, connects rear rib arches)
        bodyPart.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.2F, -2.0F, 5.2F, 2.4F, 16.0F, 1.4F, CubeDeformation.NONE),
                PartPose.ZERO);

        // NO sternum - front center remains open for visibility and freedom of movement

        // Pair 1: Top Ribs (Upper Chest) - y = -0.5F
        bodyPart.addOrReplaceChild("rib_top_left", CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(10.0F, -0.5F, -3.5F, 1.2F, 1.2F, 7.0F, CubeDeformation.NONE) // Lateral flank (clears arm)
                .addBox(8.0F, -0.5F, -5.0F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)  // Front-diagonal
                .addBox(2.5F, -0.5F, -6.0F, 5.5F, 1.2F, 1.2F, CubeDeformation.NONE)  // Front arch (stops at x=2.5)
                .addBox(8.0F, -0.5F, 3.5F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)   // Back-diagonal
                .addBox(1.2F, -0.5F, 5.0F, 6.8F, 1.2F, 1.2F, CubeDeformation.NONE),  // Back arch to spine
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_top_right", CubeListBuilder.create()
                .texOffs(16, 0).mirror()
                .addBox(-11.2F, -0.5F, -3.5F, 1.2F, 1.2F, 7.0F, CubeDeformation.NONE)
                .addBox(-10.0F, -0.5F, -5.0F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(-8.0F, -0.5F, -6.0F, 5.5F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(-10.0F, -0.5F, 3.5F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(-8.0F, -0.5F, 5.0F, 6.8F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 2: Mid-Upper Ribs - y = 3.5F
        bodyPart.addOrReplaceChild("rib_mid_left", CubeListBuilder.create()
                .texOffs(16, 10)
                .addBox(10.2F, 3.5F, -3.5F, 1.2F, 1.2F, 7.0F, CubeDeformation.NONE)
                .addBox(8.0F, 3.5F, -5.2F, 2.2F, 1.2F, 1.7F, CubeDeformation.NONE)
                .addBox(2.5F, 3.5F, -6.2F, 5.5F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(8.0F, 3.5F, 3.5F, 2.2F, 1.2F, 1.7F, CubeDeformation.NONE)
                .addBox(1.2F, 3.5F, 5.0F, 6.8F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_mid_right", CubeListBuilder.create()
                .texOffs(16, 10).mirror()
                .addBox(-11.4F, 3.5F, -3.5F, 1.2F, 1.2F, 7.0F, CubeDeformation.NONE)
                .addBox(-10.2F, 3.5F, -5.2F, 2.2F, 1.2F, 1.7F, CubeDeformation.NONE)
                .addBox(-8.0F, 3.5F, -6.2F, 5.5F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(-10.2F, 3.5F, 3.5F, 2.2F, 1.2F, 1.7F, CubeDeformation.NONE)
                .addBox(-8.0F, 3.5F, 5.0F, 6.8F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 3: Mid-Lower Ribs - y = 7.5F
        bodyPart.addOrReplaceChild("rib_bot_left", CubeListBuilder.create()
                .texOffs(16, 20)
                .addBox(10.0F, 7.5F, -3.5F, 1.2F, 1.2F, 7.0F, CubeDeformation.NONE)
                .addBox(8.0F, 7.5F, -5.0F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(2.5F, 7.5F, -6.0F, 5.5F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(8.0F, 7.5F, 3.5F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(1.2F, 7.5F, 5.0F, 6.8F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_bot_right", CubeListBuilder.create()
                .texOffs(16, 20).mirror()
                .addBox(-11.2F, 7.5F, -3.5F, 1.2F, 1.2F, 7.0F, CubeDeformation.NONE)
                .addBox(-10.0F, 7.5F, -5.0F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(-8.0F, 7.5F, -6.0F, 5.5F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(-10.0F, 7.5F, 3.5F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(-8.0F, 7.5F, 5.0F, 6.8F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Pair 4: Lower Flank Ribs - y = 11.5F
        bodyPart.addOrReplaceChild("rib_low_left", CubeListBuilder.create()
                .texOffs(16, 0)
                .addBox(9.6F, 11.5F, -3.0F, 1.2F, 1.2F, 6.0F, CubeDeformation.NONE)
                .addBox(7.6F, 11.5F, -4.5F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(2.5F, 11.5F, -5.5F, 5.1F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(7.6F, 11.5F, 3.0F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(1.2F, 11.5F, 5.0F, 6.4F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("rib_low_right", CubeListBuilder.create()
                .texOffs(16, 0).mirror()
                .addBox(-10.8F, 11.5F, -3.0F, 1.2F, 1.2F, 6.0F, CubeDeformation.NONE)
                .addBox(-9.6F, 11.5F, -4.5F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(-7.6F, 11.5F, -5.5F, 5.1F, 1.2F, 1.2F, CubeDeformation.NONE)
                .addBox(-9.6F, 11.5F, 3.0F, 2.0F, 1.2F, 1.5F, CubeDeformation.NONE)
                .addBox(-7.6F, 11.5F, 5.0F, 6.4F, 1.2F, 1.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Shoulder horn crests (ethereal Susanoo pauldrons rising above the shoulders)
        bodyPart.addOrReplaceChild("crest_left", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(9.8F, -3.5F, -1.0F, 1.4F, 3.2F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("crest_right", CubeListBuilder.create()
                .texOffs(0, 0).mirror()
                .addBox(-11.2F, -3.5F, -1.0F, 1.4F, 3.2F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 64, 32);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        // The model geometry is naturally defined with exact clearance around the player's arms and torso.
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}
