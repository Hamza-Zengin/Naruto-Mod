package com.sekwah.narutomod.client.model.entity;

import com.sekwah.narutomod.NarutoMod;
import com.sekwah.narutomod.client.renderer.entity.ToadSageBossRenderer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * 3D articulated Toad Sage Boss (Gamabunta-inspired) model using a 128x128 texture sheet.
 * Features a squatting broad amphibian body with swelling throat sac, dark navy haori coat
 * with white trim and clan crests, furrowed scarred brow with horizontal toad pupils,
 * smoking kiseru pipe, splayed forelimbs and webbed hind legs, and a giant hip-mounted tantō.
 */
public class ToadSageBossModel extends EntityModel<ToadSageBossRenderer.ToadBossRenderState> {

    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            Identifier.fromNamespaceAndPath(NarutoMod.MOD_ID, "toad_sage_boss"), "main");

    private final ModelPart body;
    private final ModelPart throatSac;
    private final ModelPart haoriJacket;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart pipe;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;
    private final ModelPart sword;

    public ToadSageBossModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.throatSac = this.body.getChild("throat_sac");
        this.haoriJacket = this.body.getChild("haori_jacket");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.pipe = this.head.getChild("pipe");
        this.rightArm = this.body.getChild("right_arm");
        this.leftArm = this.body.getChild("left_arm");
        this.rightLeg = this.body.getChild("right_leg");
        this.leftLeg = this.body.getChild("left_leg");
        this.sword = this.body.getChild("sword");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Core squatting toad torso (root Y = 14 -> bottom at 19, hovering low above ground Y = 24)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -5.0F, -6.0F, 14.0F, 10.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 14.0F, 0.0F));

        // Inflatable vocal throat sac on upper chest
        body.addOrReplaceChild("throat_sac",
                CubeListBuilder.create()
                        .texOffs(54, 0)
                        .addBox(-6.0F, -1.5F, -2.5F, 12.0F, 4.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -3.2F, -5.0F));

        // Dark navy haori samurai coat with white trim, crest lapels, and wide sleeves
        PartDefinition haoriJacket = body.addOrReplaceChild("haori_jacket",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-8.0F, -5.5F, -0.5F, 16.0F, 10.0F, 7.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        haoriJacket.addOrReplaceChild("haori_right_lapel",
                CubeListBuilder.create()
                        .texOffs(48, 24)
                        .addBox(-8.0F, -5.2F, -6.3F, 3.0F, 9.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        haoriJacket.addOrReplaceChild("haori_left_lapel",
                CubeListBuilder.create()
                        .texOffs(68, 24)
                        .addBox(5.0F, -5.2F, -6.3F, 3.0F, 9.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        haoriJacket.addOrReplaceChild("haori_right_sleeve",
                CubeListBuilder.create()
                        .texOffs(88, 24)
                        .addBox(-3.5F, -0.5F, -3.5F, 4.0F, 5.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-8.0F, -4.5F, -1.0F, 0.0F, 0.0F, 0.22F));

        haoriJacket.addOrReplaceChild("haori_left_sleeve",
                CubeListBuilder.create()
                        .texOffs(88, 24)
                        .addBox(-0.5F, -0.5F, -3.5F, 4.0F, 5.0F, 7.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(8.0F, -4.5F, -1.0F, 0.0F, 0.0F, -0.22F));

        // Broad amphibian head with brow ridges, recessed yellow horizontal-pupil eyes, lower jaw, and kiseru pipe
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 43)
                        .addBox(-6.0F, -5.0F, -6.0F, 12.0F, 6.0F, 10.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -5.5F, -1.0F));

        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(46, 43)
                        .addBox(-6.0F, -0.5F, -5.8F, 12.0F, 2.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.8F, 0.0F));

        head.addOrReplaceChild("right_brow",
                CubeListBuilder.create()
                        .texOffs(90, 43)
                        .addBox(-2.8F, -2.6F, -2.5F, 6.0F, 3.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-3.5F, -4.6F, -4.4F, 0.08F, 0.0F, 0.14F));

        head.addOrReplaceChild("left_brow",
                CubeListBuilder.create()
                        .texOffs(90, 53)
                        .addBox(-3.2F, -2.6F, -2.5F, 6.0F, 3.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(3.5F, -4.6F, -4.4F, 0.08F, 0.0F, -0.14F));

        head.addOrReplaceChild("right_eye",
                CubeListBuilder.create()
                        .texOffs(88, 0)
                        .addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-3.3F, -3.2F, -5.9F).scaled(0.75F, 0.55F, 1.0F));

        head.addOrReplaceChild("left_eye",
                CubeListBuilder.create()
                        .texOffs(88, 0)
                        .addBox(-1.5F, -1.5F, -0.5F, 3.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(3.3F, -3.2F, -5.9F).scaled(0.75F, 0.55F, 1.0F));

        // Traditional wooden & brass kiseru smoking pipe jutting from the left mouth corner
        PartDefinition pipe = head.addOrReplaceChild("pipe",
                CubeListBuilder.create()
                        .texOffs(0, 61)
                        .addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.2F, 0.0F, -5.4F, -0.22F, -0.45F, -0.18F));

        pipe.addOrReplaceChild("pipe_collar",
                CubeListBuilder.create()
                        .texOffs(16, 61)
                        .addBox(-1.0F, -1.0F, -6.5F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        pipe.addOrReplaceChild("pipe_bowl",
                CubeListBuilder.create()
                        .texOffs(26, 61)
                        .addBox(-1.5F, -3.2F, -8.5F, 3.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Muscular forelimbs + splayed 3-fingered toad hands resting at Y = 24
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 70)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-8.8F, -1.5F, -2.5F, -0.20F, 0.10F, 0.08F));
        addToadHand(rightArm, "right");

        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(14, 70)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 9.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(8.8F, -1.5F, -2.5F, -0.20F, -0.10F, -0.08F));
        addToadHand(leftArm, "left");

        // Heavy bent squatting hind legs + wide webbed feet resting at Y = 24
        PartDefinition rightLeg = body.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 84)
                        .addBox(-2.5F, -2.5F, -8.0F, 5.0F, 5.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-6.5F, 3.5F, 1.5F, -0.18F, 0.65F, 0.0F));
        addToadHindLimb(rightLeg, "right");

        PartDefinition leftLeg = body.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(30, 84)
                        .addBox(-2.5F, -2.5F, -8.0F, 5.0F, 5.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(6.5F, 3.5F, 1.5F, -0.18F, -0.65F, 0.0F));
        addToadHindLimb(leftLeg, "left");

        // Giant sheathed shinobi tantō blade mounted diagonally across the left hip/back
        PartDefinition sword = body.addOrReplaceChild("sword",
                CubeListBuilder.create()
                        .texOffs(0, 100)
                        .addBox(-1.0F, -1.0F, -5.5F, 2.0F, 2.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(7.0F, 2.0F, 1.5F, -0.32F, -0.45F, 0.28F));

        sword.addOrReplaceChild("sword_guard",
                CubeListBuilder.create()
                        .texOffs(18, 100)
                        .addBox(-1.5F, -1.5F, 0.5F, 3.0F, 3.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        sword.addOrReplaceChild("sword_blade",
                CubeListBuilder.create()
                        .texOffs(30, 100)
                        .addBox(-0.5F, -1.5F, 2.5F, 1.0F, 3.0F, 14.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addToadHand(PartDefinition arm, String side) {
        PartDefinition palm = arm.addOrReplaceChild(side + "_hand",
                CubeListBuilder.create()
                        .texOffs(28, 70)
                        .addBox(-2.5F, 0.0F, -3.5F, 5.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 9.4F, -0.4F, 0.20F, 0.0F, 0.0F));

        palm.addOrReplaceChild(side + "_finger_outer",
                CubeListBuilder.create().texOffs(48, 70).addBox(-1.0F, 0.0F, -3.5F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.8F, 0.0F, -3.0F, 0.0F, 0.38F, 0.0F));
        palm.addOrReplaceChild(side + "_finger_mid",
                CubeListBuilder.create().texOffs(48, 70).addBox(-1.0F, 0.0F, -3.5F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, -3.4F));
        palm.addOrReplaceChild(side + "_finger_inner",
                CubeListBuilder.create().texOffs(48, 70).addBox(-1.0F, 0.0F, -3.5F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.8F, 0.0F, -3.0F, 0.0F, -0.38F, 0.0F));
    }

    private static void addToadHindLimb(PartDefinition thigh, String side) {
        PartDefinition calf = thigh.addOrReplaceChild(side + "_calf",
                CubeListBuilder.create()
                        .texOffs(60, 84)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 6.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -7.2F, 0.18F, 0.0F, 0.0F));

        PartDefinition foot = calf.addOrReplaceChild(side + "_foot",
                CubeListBuilder.create()
                        .texOffs(78, 84)
                        .addBox(-3.0F, 0.0F, -4.5F, 6.0F, 2.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 5.8F, 0.0F));

        foot.addOrReplaceChild(side + "_toe_outer",
                CubeListBuilder.create().texOffs(48, 70).addBox(-1.0F, 0.0F, -3.2F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.2F, 0.0F, -4.0F, 0.0F, 0.36F, 0.0F));
        foot.addOrReplaceChild(side + "_toe_mid",
                CubeListBuilder.create().texOffs(48, 70).addBox(-1.0F, 0.0F, -3.5F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, -4.3F));
        foot.addOrReplaceChild(side + "_toe_inner",
                CubeListBuilder.create().texOffs(48, 70).addBox(-1.0F, 0.0F, -3.2F, 2.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.2F, 0.0F, -4.0F, 0.0F, -0.36F, 0.0F));
    }

    @Override
    public void setupAnim(ToadSageBossRenderer.ToadBossRenderState state) {
        super.setupAnim(state);
        this.resetPose();

        // 1. Breathing bob, throat-sac inflation, and subtle kiseru pipe tilt
        float breathFreq = state.isPhaseTwo ? 0.18F : 0.10F;
        float breath = Mth.sin(state.ageInTicks * breathFreq);
        float sacPulse = 0.5F + 0.5F * Mth.sin(state.ageInTicks * (state.isPhaseTwo ? 0.24F : 0.12F));

        this.body.y += breath * 0.35F;
        this.throatSac.xScale = 1.0F + sacPulse * (state.isPhaseTwo ? 0.16F : 0.10F);
        this.throatSac.yScale = 1.0F + sacPulse * (state.isPhaseTwo ? 0.28F : 0.18F);
        this.throatSac.zScale = 1.0F + sacPulse * (state.isPhaseTwo ? 0.35F : 0.22F);

        this.pipe.xRot += breath * 0.05F;
        this.pipe.zRot += Mth.cos(state.ageInTicks * 0.08F) * 0.03F;

        // 2. Head tracking & mouth breathing
        this.head.yRot += Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD * 0.65F;
        this.head.xRot += Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.45F;
        this.jaw.xRot += state.isPhaseTwo ? (0.14F + sacPulse * 0.08F) : (sacPulse * 0.05F);

        // 3. Rhythmic amphibian hopping / stride cycle
        float walkSpeed = Math.min(state.walkAnimationSpeed, 1.0F);
        float hop = Math.abs(Mth.sin(state.walkAnimationPos * 0.65F)) * walkSpeed;
        float stride = Mth.sin(state.walkAnimationPos * 0.65F) * walkSpeed;

        this.body.y -= hop * 2.6F;
        this.body.xRot += hop * 0.14F;
        this.rightLeg.xRot += hop * 0.52F + stride * 0.22F;
        this.leftLeg.xRot += hop * 0.52F - stride * 0.22F;
        this.rightArm.xRot -= hop * 0.38F - stride * 0.28F;
        this.leftArm.xRot -= hop * 0.38F + stride * 0.28F;

        // 4. Heavy Toad Leap Slam pose
        if (state.leapPhase > 0.0F) {
            float leap = Mth.sin(state.leapPhase * Mth.PI);
            this.body.xRot += leap * 0.26F;
            this.rightLeg.xRot += leap * 0.85F;
            this.leftLeg.xRot += leap * 0.85F;
            this.rightArm.xRot -= leap * 0.72F;
            this.leftArm.xRot -= leap * 0.72F;
            this.jaw.xRot += leap * 0.20F;
        }

        // 5. Powerful melee lunge & tantō cleave
        if (state.attackAnim > 0.0F) {
            float swing = Mth.sin(state.attackAnim * Mth.PI);
            this.body.yRot -= swing * 0.40F;
            this.body.xRot += swing * 0.22F;
            this.sword.yRot += swing * 1.30F;
            this.sword.xRot -= swing * 0.90F;
            this.sword.zRot -= swing * 0.42F;
            this.leftArm.xRot -= swing * 0.80F;
            this.leftArm.yRot += swing * 0.40F;
            this.rightArm.xRot += swing * 0.28F;
        }

        // 6. Phase 2 aggressive lower crouch, flared stance & billowing haori
        if (state.isPhaseTwo) {
            this.body.y += 1.3F;
            this.body.xRot += 0.12F;
            this.rightArm.zRot += 0.16F;
            this.leftArm.zRot -= 0.16F;
            this.rightLeg.yRot -= 0.14F;
            this.leftLeg.yRot += 0.14F;
            this.sword.xRot += 0.24F;
            this.sword.zRot -= 0.18F;
            this.haoriJacket.xRot += 0.08F + Mth.sin(state.ageInTicks * 0.35F) * 0.04F;
        }
    }
}
