package com.sekwah.narutomod.client.model.entity;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

import java.util.ArrayList;
import java.util.List;

/**
 * 3D Skeletal Chakra Ribcage of Susanoo enveloping the shinobi.
 * Engineered with 100% mathematical left/right symmetry and balanced front-to-back chest depth.
 */
public class SusanooRibcageModel extends HumanoidModel<HumanoidRenderState> {

    private static final String[] FIRST_PERSON_HIDDEN_PART_NAMES = {
            "rib_1_farch_l", "rib_1_farch_r", "rib_1_fcurve_l", "rib_1_fcurve_r",
            "rib_2_farch_l", "rib_2_farch_r", "rib_2_fcurve_l", "rib_2_fcurve_r",
            "rib_3_farch_l", "rib_3_farch_r"
    };

    private final List<ModelPart> firstPersonHiddenParts;

    public SusanooRibcageModel(ModelPart root) {
        super(root);
        List<ModelPart> hiddenParts = new ArrayList<>();
        for (String partName : FIRST_PERSON_HIDDEN_PART_NAMES) {
            if (this.body.hasChild(partName)) {
                hiddenParts.add(this.body.getChild(partName));
            }
        }
        this.firstPersonHiddenParts = hiddenParts;
    }

    public void setFirstPersonView(boolean firstPerson) {
        for (ModelPart part : this.firstPersonHiddenParts) {
            part.visible = !firstPerson;
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_arm", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("right_leg", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("left_leg", CubeListBuilder.create(), PartPose.ZERO);

        // Attach ribcage structure to the body part so it moves, rotates, and bends with player torso
        PartDefinition bodyPart = root.addOrReplaceChild("body", CubeListBuilder.create(), PartPose.ZERO);

        // Central Dorsal Spine along the back (Z = +6.4F to +8.0F) with beveled vertebral ridges
        PartDefinition spine = bodyPart.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(-1.2F, -2.5F, 6.4F, 2.4F, 16.5F, 1.5F, CubeDeformation.NONE)
                .texOffs(9, 0)
                .addBox(-0.6F, -1.5F, 7.7F, 1.2F, 1.8F, 0.9F, CubeDeformation.NONE)
                .addBox(-0.6F, 2.5F, 7.7F, 1.2F, 1.8F, 0.9F, CubeDeformation.NONE)
                .addBox(-0.6F, 6.5F, 7.7F, 1.2F, 1.8F, 0.9F, CubeDeformation.NONE)
                .addBox(-0.6F, 10.5F, 7.7F, 1.2F, 1.8F, 0.9F, CubeDeformation.NONE),
                PartPose.ZERO);
        spine.addOrReplaceChild("spine_bevel_l", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.7F, -2.2F, 6.3F, 1.0F, 16.0F, 1.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.9F, 0.0F, 0.0F, 0.0F, -0.35F, 0.0F));
        spine.addOrReplaceChild("spine_bevel_r", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-0.3F, -2.2F, 6.3F, 1.0F, 16.0F, 1.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-0.9F, 0.0F, 0.0F, 0.0F, 0.35F, 0.0F));

        // Lateral Vertical Rib Stabilizer Pillars (rounded octagonal flank posts)
        PartDefinition pillarL = bodyPart.addOrReplaceChild("pillar_left", CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(9.2F, -1.0F, -0.75F, 1.1F, 13.8F, 1.5F, CubeDeformation.NONE),
                PartPose.ZERO);
        pillarL.addOrReplaceChild("pillar_left_rot", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-0.65F, -1.0F, -0.65F, 1.3F, 13.6F, 1.3F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(9.75F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        PartDefinition pillarR = bodyPart.addOrReplaceChild("pillar_right", CubeListBuilder.create()
                .texOffs(0, 18)
                .addBox(-10.3F, -1.0F, -0.75F, 1.1F, 13.8F, 1.5F, CubeDeformation.NONE),
                PartPose.ZERO);
        pillarR.addOrReplaceChild("pillar_right_rot", CubeListBuilder.create()
                .texOffs(0, 18).addBox(-0.65F, -1.0F, -0.65F, 1.3F, 13.6F, 1.3F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-9.75F, 0.0F, 0.0F, 0.0F, 0.7854F, 0.0F));

        // Build 4 Symmetric Rib Pairs with smooth 7-segment arches per side
        addSymmetricRibPair(bodyPart, "rib_1", -0.5F, 10.4F, 7.6F, 6.8F, 0);
        addSymmetricRibPair(bodyPart, "rib_2", 3.5F, 10.7F, 8.0F, 7.0F, 8);
        addSymmetricRibPair(bodyPart, "rib_3", 7.5F, 10.4F, 7.6F, 6.8F, 16);
        addSymmetricRibPair(bodyPart, "rib_4", 11.5F, 9.8F, 7.0F, 6.5F, 24);

        // Symmetric 3-Segment Curved Shoulder Horn Crests
        addCurvedCrest(bodyPart, "crest_left", 1.0F);
        addCurvedCrest(bodyPart, "crest_right", -1.0F);

        return LayerDefinition.create(mesh, 64, 32);
    }

    private static void addCurvedCrest(PartDefinition bodyPart, String name, float dirSign) {
        PartDefinition base = bodyPart.addOrReplaceChild(name, CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-0.8F, -2.4F, -1.1F, 1.6F, 2.4F, 2.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(dirSign * 10.0F, -0.2F, 0.0F, 0.0F, 0.0F, dirSign * 0.18F));
        PartDefinition mid = base.addOrReplaceChild(name + "_mid", CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-0.65F, -2.0F, -0.9F, 1.3F, 2.0F, 1.8F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -2.2F, 0.0F, -0.10F, 0.0F, dirSign * 0.22F));
        mid.addOrReplaceChild(name + "_tip", CubeListBuilder.create()
                .texOffs(48, 0)
                .addBox(-0.45F, -1.8F, -0.65F, 0.9F, 1.8F, 1.3F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -1.8F, 0.0F, -0.15F, 0.0F, dirSign * 0.26F));
    }

    /**
     * Creates a 100% mathematically symmetric left (+X) and right (-X) rib arch at height Y,
     * curving smoothly in 7 graded segments per side from the dorsal spine (+backZ) around
     * the lateral flank (±outerX) to the open front chest (-frontZ).
     */
    private static void addSymmetricRibPair(PartDefinition bodyPart, String prefix, float y,
                                            float outerX, float frontZ, float backZ, int texY) {
        float thickness = 1.35F;
        float halfT = thickness * 0.5F;
        float innerX = outerX - thickness;

        // 1. Mid-Lateral Flank Bars (shorter center flank to blend smoothly into curved shoulders)
        bodyPart.addOrReplaceChild(prefix + "_flank_l", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(innerX, y, -2.6F, thickness, thickness, 5.2F, CubeDeformation.NONE),
                PartPose.ZERO);
        bodyPart.addOrReplaceChild(prefix + "_flank_r", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-outerX, y, -2.6F, thickness, thickness, 5.2F, CubeDeformation.NONE),
                PartPose.ZERO);

        // 2. Front-Lateral Curve A (gentle initial turn forward)
        bodyPart.addOrReplaceChild(prefix + "_fcurve_a_l", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.6F, -halfT, -halfT, 3.2F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(outerX - 1.1F, y + halfT, -3.7F, 0.0F, -1.10F, 0.06F));
        bodyPart.addOrReplaceChild(prefix + "_fcurve_a_r", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.6F, -halfT, -halfT, 3.2F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-(outerX - 1.1F), y + halfT, -3.7F, 0.0F, 1.10F, -0.06F));

        // 3. Front-Lateral Curve B (transitions smoothly into chest front)
        bodyPart.addOrReplaceChild(prefix + "_fcurve_l", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.8F, -halfT, -halfT, 3.6F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(outerX - 2.9F, y + halfT, -6.1F, 0.0F, -0.56F, 0.04F));
        bodyPart.addOrReplaceChild(prefix + "_fcurve_r", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.8F, -halfT, -halfT, 3.6F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-(outerX - 2.9F), y + halfT, -6.1F, 0.0F, 0.56F, -0.04F));

        // 4. Front Chest Arch (open sternum framing)
        bodyPart.addOrReplaceChild(prefix + "_farch_l", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-2.2F, -halfT, -halfT, 4.4F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.2F, y + halfT, -frontZ, 0.0F, -0.22F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_farch_r", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-2.2F, -halfT, -halfT, 4.4F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-5.2F, y + halfT, -frontZ, 0.0F, 0.22F, 0.0F));

        // 5. Back-Lateral Curve A (gentle backward sweep)
        bodyPart.addOrReplaceChild(prefix + "_bcurve_a_l", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.6F, -halfT, -halfT, 3.2F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(outerX - 1.1F, y + halfT, 3.5F, 0.0F, 1.05F, -0.05F));
        bodyPart.addOrReplaceChild(prefix + "_bcurve_a_r", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.6F, -halfT, -halfT, 3.2F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-(outerX - 1.1F), y + halfT, 3.5F, 0.0F, -1.05F, 0.05F));

        // 6. Back-Lateral Curve B
        bodyPart.addOrReplaceChild(prefix + "_bcurve_l", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.8F, -halfT, -halfT, 3.6F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(outerX - 2.9F, y + halfT, 5.6F, 0.0F, 0.52F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_bcurve_r", CubeListBuilder.create()
                .texOffs(34, texY)
                .addBox(-1.8F, -halfT, -halfT, 3.6F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-(outerX - 2.9F), y + halfT, 5.6F, 0.0F, -0.52F, 0.0F));

        // 7. Back Dorsal Arch (connects smoothly into central spine at Z = +backZ)
        bodyPart.addOrReplaceChild(prefix + "_barch_l", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-3.4F, -halfT, -halfT, 6.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.2F, y + halfT, backZ, 0.0F, 0.16F, 0.0F));
        bodyPart.addOrReplaceChild(prefix + "_barch_r", CubeListBuilder.create()
                .texOffs(16, texY)
                .addBox(-3.4F, -halfT, -halfT, 6.8F, thickness, thickness, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-4.2F, y + halfT, backZ, 0.0F, -0.16F, 0.0F));
    }
}

