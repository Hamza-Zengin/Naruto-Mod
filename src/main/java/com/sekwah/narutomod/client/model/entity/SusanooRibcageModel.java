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
 * Engineered with 100% mathematical left/right symmetry and balanced front-to-back chest depth.
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

        // Central Dorsal Spine along the back (Z = +6.4F to +8.0F) with vertebral ridges
        bodyPart.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.3F, -2.5F, 6.4F, 2.6F, 16.5F, 1.6F, CubeDeformation.NONE)
                // Dorsal vertebral spikes along the spine center
                .texOffs(9, 0)
                .addBox(-0.7F, -1.5F, 7.8F, 1.4F, 2.0F, 1.0F, CubeDeformation.NONE)
                .addBox(-0.7F, 2.5F, 7.8F, 1.4F, 2.0F, 1.0F, CubeDeformation.NONE)
                .addBox(-0.7F, 6.5F, 7.8F, 1.4F, 2.0F, 1.0F, CubeDeformation.NONE)
                .addBox(-0.7F, 10.5F, 7.8F, 1.4F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Lateral Vertical Rib Stabilizer Pillars (connecting the 4 ribs along the back-flank)
        bodyPart.addOrReplaceChild("pillar_left", CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(9.2F, -1.0F, -0.8F, 1.2F, 13.8F, 1.6F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild("pillar_right", CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(-10.4F, -1.0F, -0.8F, 1.2F, 13.8F, 1.6F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Build 4 Symmetric Rib Pairs with balanced front/back depth (Width ~21.2, Depth ~15.8)
        addSymmetricRibPair(bodyPart, "rib_1", -0.5F, 10.4F, 7.6F, 6.8F, 0);
        addSymmetricRibPair(bodyPart, "rib_2", 3.5F, 10.7F, 8.0F, 7.0F, 8);
        addSymmetricRibPair(bodyPart, "rib_3", 7.5F, 10.4F, 7.6F, 6.8F, 16);
        addSymmetricRibPair(bodyPart, "rib_4", 11.5F, 9.8F, 7.0F, 6.5F, 24);

        // Symmetric Shoulder Horn Crests (ethereal Susanoo pauldrons rising above the shoulders)
        bodyPart.addOrReplaceChild("crest_left", CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-0.8F, -3.6F, -1.2F, 1.6F, 3.6F, 2.4F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(10.0F, -0.2F, 0.0F, 0.0F, 0.0F, 0.22F));
        bodyPart.addOrReplaceChild("crest_right", CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-0.8F, -3.6F, -1.2F, 1.6F, 3.6F, 2.4F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-10.0F, -0.2F, 0.0F, 0.0F, 0.0F, -0.22F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * Creates a 100% mathematically symmetric left (+X) and right (-X) rib arch at height Y,
     * curving smoothly from the dorsal spine (+backZ) around the lateral flank (±outerX)
     * to the open front chest (-frontZ).
     */
    private static void addSymmetricRibPair(PartDefinition bodyPart, String prefix, float y,
                                            float outerX, float frontZ, float backZ, int texY) {
        float thickness = 1.4F;
        float halfT = thickness * 0.5F;
        float innerX = outerX - thickness;

        // 1. Lateral Flank Bars (strictly identical on +X and -X)
        bodyPart.addOrReplaceChild(prefix + "_flank_l", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(innerX, y, -3.8F, thickness, thickness, 7.6F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild(prefix + "_flank_r", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-outerX, y, -3.8F, thickness, thickness, 7.6F, CubeDeformation.NONE),
                PartPose.ZERO);

        // 2. Front-Lateral Angled Curve (bridges flank at Z=-3.8F forward to Z=-frontZ+0.8F)
        bodyPart.addOrReplaceChild(prefix + "_fcurve_l", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-2.4F, -halfT, -halfT, 4.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(outerX - 2.0F, y + halfT, -5.4F, 0.0F, -0.68F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_fcurve_r", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-2.4F, -halfT, -halfT, 4.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-(outerX - 2.0F), y + halfT, -5.4F, 0.0F, 0.68F, 0.0F));

        // 3. Front Chest Arch (curves gently to the front sternum gap at X = ±2.4F, Z = -frontZ)
        bodyPart.addOrReplaceChild(prefix + "_farch_l", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-2.8F, -halfT, -halfT, 5.6F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, y + halfT, -frontZ, 0.0F, -0.18F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_farch_r", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-2.8F, -halfT, -halfT, 5.6F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.0F, y + halfT, -frontZ, 0.0F, 0.18F, 0.0F));

        // 4. Back-Lateral Angled Curve (bridges flank at Z=+3.8F backward to Z=+backZ-0.6F)
        bodyPart.addOrReplaceChild(prefix + "_bcurve_l", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-2.4F, -halfT, -halfT, 4.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(outerX - 2.0F, y + halfT, 5.1F, 0.0F, 0.65F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_bcurve_r", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-2.4F, -halfT, -halfT, 4.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-(outerX - 2.0F), y + halfT, 5.1F, 0.0F, -0.65F, 0.0F));

        // 5. Back Dorsal Arch (connects securely into the central spine at Z = +backZ)
        bodyPart.addOrReplaceChild(prefix + "_barch_l", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-3.4F, -halfT, -halfT, 6.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.4F, y + halfT, backZ, 0.0F, 0.15F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_barch_r", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-3.4F, -halfT, -halfT, 6.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-4.4F, y + halfT, backZ, 0.0F, -0.15F, 0.0F));
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        poseStack.pushPose();
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
        poseStack.popPose();
    }
}

