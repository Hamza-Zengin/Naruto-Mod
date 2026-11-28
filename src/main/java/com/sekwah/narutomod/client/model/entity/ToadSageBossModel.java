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
 * Features a squatting broad amphibian body with a tucked inflatable throat sac,
 * dark navy haori coat with white trim, upturned collar, and toad-clan crests,
 * furrowed scarred brow with recessed golden horizontal-pupil eyes, left-mouth-corner kiseru pipe,
 * sleeve-tracked splayed forelimbs and webbed hind legs resting at Y = 24,
 * and a giant hip-mounted shinobi tantō.
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

        // Core squatting toad torso (root Y = 14 -> ground is Y_body = +10 -> Y_world = 24)
        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -4.5F, -5.5F, 14.0F, 9.0F, 11.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 14.0F, 0.0F));

        // Rounded warm-cream belly protruding cleanly between the open haori lapels
        body.addOrReplaceChild("belly",
                CubeListBuilder.create()
                        .texOffs(52, 0)
                        .addBox(-4.5F, -1.3F, -6.3F, 9.0F, 5.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Smooth inflatable vocal throat sac tucked cleanly underneath the lower jaw (behind snout line)
        body.addOrReplaceChild("throat_sac",
                CubeListBuilder.create()
                        .texOffs(52, 8)
                        .addBox(-4.5F, -1.0F, -1.6F, 9.0F, 3.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -2.2F, -4.6F));

        // Dark navy samurai haori coat with white trim, upturned collar, and front lapels
        PartDefinition haoriJacket = body.addOrReplaceChild("haori_jacket",
                CubeListBuilder.create()
                        .texOffs(0, 22)
                        .addBox(-8.0F, -5.0F, 0.0F, 16.0F, 9.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        haoriJacket.addOrReplaceChild("haori_right_lapel",
                CubeListBuilder.create()
                        .texOffs(46, 22)
                        .addBox(-7.8F, -4.8F, -6.0F, 3.0F, 8.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        haoriJacket.addOrReplaceChild("haori_left_lapel",
                CubeListBuilder.create()
                        .texOffs(66, 22)
                        .addBox(4.8F, -4.8F, -6.0F, 3.0F, 8.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        haoriJacket.addOrReplaceChild("haori_collar",
                CubeListBuilder.create()
                        .texOffs(86, 22)
                        .addBox(-6.0F, -6.2F, 3.2F, 12.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Broad amphibian head with sculpted brow ridges, scarred left brow, horizontal-pupil eyes, hinged jaw, and kiseru pipe
        PartDefinition head = body.addOrReplaceChild("head",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-6.0F, -5.0F, -6.0F, 12.0F, 5.0F, 10.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -5.0F, -1.0F));

        // Back-hinged wide lower jaw with subtle overbite recess beneath upper snout
        head.addOrReplaceChild("jaw",
                CubeListBuilder.create()
                        .texOffs(46, 40)
                        .addBox(-5.5F, 0.0F, -7.8F, 11.0F, 2.0F, 8.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.2F, 2.0F));

        // Furrowed supraorbital brow ridges cleanly separated across the nasal bridge
        head.addOrReplaceChild("right_brow",
                CubeListBuilder.create()
                        .texOffs(86, 40)
                        .addBox(-2.0F, -1.5F, -2.5F, 4.0F, 2.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-3.6F, -5.1F, -3.65F, 0.06F, 0.0F, 0.14F));

        head.addOrReplaceChild("left_brow",
                CubeListBuilder.create()
                        .texOffs(106, 40)
                        .addBox(-2.0F, -1.5F, -2.5F, 4.0F, 2.0F, 5.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(3.6F, -5.1F, -3.65F, 0.06F, 0.0F, -0.14F));

        // Crisp 4x3 recessed golden-amber eyes with dark horizontal toad pupils
        head.addOrReplaceChild("right_eye",
                CubeListBuilder.create()
                        .texOffs(78, 0)
                        .addBox(-2.0F, -1.5F, -0.5F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(-3.6F, -3.2F, -5.65F));

        head.addOrReplaceChild("left_eye",
                CubeListBuilder.create()
                        .texOffs(90, 0)
                        .addBox(-2.0F, -1.5F, -0.5F, 4.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.offset(3.6F, -3.2F, -5.65F));

        // Traditional wooden & brass kiseru smoking pipe gripped cleanly in the left mouth corner
        PartDefinition pipe = head.addOrReplaceChild("pipe",
                CubeListBuilder.create()
                        .texOffs(0, 56)
                        .addBox(-0.5F, -0.5F, -5.5F, 1.0F, 1.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(5.0F, 0.1F, -5.0F, 0.08F, -0.58F, 0.0F));

        pipe.addOrReplaceChild("pipe_collar",
                CubeListBuilder.create()
                        .texOffs(16, 56)
                        .addBox(-1.0F, -1.0F, -5.8F, 2.0F, 2.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        pipe.addOrReplaceChild("pipe_bowl",
                CubeListBuilder.create()
                        .texOffs(24, 56)
                        .addBox(-1.0F, -1.8F, -7.8F, 2.0F, 2.0F, 2.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        // Muscular forelimbs with attached haori sleeves + splayed 3-fingered hands resting at Y = 24
        PartDefinition rightArm = body.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 65)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-9.5F, -2.25F, -2.0F, -0.15F, 0.0F, 0.06F));
        rightArm.addOrReplaceChild("haori_right_sleeve",
                CubeListBuilder.create()
                        .texOffs(86, 28)
                        .addBox(-2.0F, -1.5F, -3.0F, 4.0F, 5.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        addToadHand(rightArm, "right", 1);

        PartDefinition leftArm = body.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(14, 65)
                        .addBox(-1.5F, 0.0F, -1.5F, 3.0F, 11.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(9.5F, -2.25F, -2.0F, -0.15F, 0.0F, -0.06F));
        leftArm.addOrReplaceChild("haori_left_sleeve",
                CubeListBuilder.create()
                        .texOffs(108, 28)
                        .addBox(-2.0F, -1.5F, -3.0F, 4.0F, 5.0F, 6.0F, CubeDeformation.NONE),
                PartPose.ZERO);
        addToadHand(leftArm, "left", -1);

        // Heavy bent squatting hind legs + wide webbed feet resting at Y = 24
        PartDefinition rightLeg = body.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        .texOffs(0, 81)
                        .addBox(-2.5F, -2.5F, -8.0F, 5.0F, 5.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-6.2F, 2.5F, 1.5F, -0.15F, 0.62F, 0.0F));
        addToadHindLimb(rightLeg, "right");

        PartDefinition leftLeg = body.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        .texOffs(30, 81)
                        .addBox(-2.5F, -2.5F, -8.0F, 5.0F, 5.0F, 9.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(6.2F, 2.5F, 1.5F, -0.15F, -0.62F, 0.0F));
        addToadHindLimb(leftLeg, "left");

        // Giant sheathed shinobi tantō blade mounted diagonally across the left hip/back
        PartDefinition sword = body.addOrReplaceChild("sword",
                CubeListBuilder.create()
                        .texOffs(0, 98)
                        .addBox(-1.0F, -1.0F, -5.5F, 2.0F, 2.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(8.8F, 1.5F, 3.8F, -0.32F, -0.22F, 0.14F));

        sword.addOrReplaceChild("sword_guard",
                CubeListBuilder.create()
                        .texOffs(18, 98)
                        .addBox(-1.5F, -1.5F, 0.5F, 3.0F, 3.0F, 1.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        sword.addOrReplaceChild("sword_blade",
                CubeListBuilder.create()
                        .texOffs(28, 98)
                        .addBox(-0.5F, -1.5F, 1.5F, 1.0F, 3.0F, 14.0F, CubeDeformation.NONE),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addToadHand(PartDefinition arm, String side, int zSign) {
        PartDefinition palm = arm.addOrReplaceChild(side + "_hand",
                CubeListBuilder.create()
                        .texOffs(28, 65)
                        .addBox(-2.5F, 0.0F, -3.5F, 5.0F, 2.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 10.4F, 0.0F, 0.15F, 0.0F, -0.06F * zSign));

        palm.addOrReplaceChild(side + "_finger_outer",
                CubeListBuilder.create().texOffs(48, 65).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-1.8F, 0.02F, -3.3F, 0.0F, 0.36F, 0.0F));
        palm.addOrReplaceChild(side + "_finger_mid",
                CubeListBuilder.create().texOffs(48, 65).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, -3.5F));
        palm.addOrReplaceChild(side + "_finger_inner",
                CubeListBuilder.create().texOffs(48, 65).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(1.8F, 0.02F, -3.3F, 0.0F, -0.36F, 0.0F));
    }

    private static void addToadHindLimb(PartDefinition thigh, String side) {
        PartDefinition calf = thigh.addOrReplaceChild(side + "_calf",
                CubeListBuilder.create()
                        .texOffs(60, 81)
                        .addBox(-2.0F, 0.0F, -2.0F, 4.0F, 7.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 0.0F, -7.0F, 0.15F, 0.0F, 0.0F));

        PartDefinition foot = calf.addOrReplaceChild(side + "_foot",
                CubeListBuilder.create()
                        .texOffs(78, 81)
                        .addBox(-3.0F, 0.0F, -4.5F, 6.0F, 2.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 6.55F, -0.5F));

        foot.addOrReplaceChild(side + "_toe_outer",
                CubeListBuilder.create().texOffs(48, 65).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-2.2F, 0.02F, -4.3F, 0.0F, 0.34F, 0.0F));
        foot.addOrReplaceChild(side + "_toe_mid",
                CubeListBuilder.create().texOffs(48, 65).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, -4.5F));
        foot.addOrReplaceChild(side + "_toe_inner",
                CubeListBuilder.create().texOffs(48, 65).addBox(-1.0F, 0.0F, -3.0F, 2.0F, 2.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(2.2F, 0.02F, -4.3F, 0.0F, -0.34F, 0.0F));
    }

    @Override
    public void setupAnim(ToadSageBossRenderer.ToadBossRenderState state) {
        super.setupAnim(state);
        this.resetPose();

        // 1. Breathing bob, tucked throat-sac pulse, and gentle kiseru pipe sway
        float breathFreq = state.isPhaseTwo ? 0.18F : 0.10F;
        float breath = Mth.sin(state.ageInTicks * breathFreq);
        float sacPulse = 0.5F + 0.5F * Mth.sin(state.ageInTicks * (state.isPhaseTwo ? 0.24F : 0.12F));

        this.body.y += breath * 0.30F;
        this.throatSac.xScale = 1.0F + sacPulse * (state.isPhaseTwo ? 0.12F : 0.08F);
        this.throatSac.yScale = 1.0F + sacPulse * (state.isPhaseTwo ? 0.20F : 0.14F);
        this.throatSac.zScale = 1.0F + sacPulse * (state.isPhaseTwo ? 0.24F : 0.16F);

        this.pipe.xRot += breath * 0.03F;
        this.pipe.zRot += Mth.cos(state.ageInTicks * 0.08F) * 0.02F;

        // 2. Head tracking & hinged mouth breathing
        this.head.yRot += Mth.clamp(state.yRot, -45.0F, 45.0F) * Mth.DEG_TO_RAD * 0.65F;
        this.head.xRot += Mth.clamp(state.xRot, -30.0F, 30.0F) * Mth.DEG_TO_RAD * 0.45F;
        this.jaw.xRot += state.isPhaseTwo ? (0.12F + sacPulse * 0.07F) : (sacPulse * 0.04F);

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
            this.jaw.xRot += leap * 0.22F;
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
            this.body.y += 1.2F;
            this.body.xRot += 0.12F;
            this.rightArm.zRot += 0.16F;
            this.leftArm.zRot -= 0.16F;
            this.rightLeg.yRot -= 0.14F;
            this.leftLeg.yRot += 0.14F;
            this.sword.xRot += 0.24F;
            this.sword.zRot -= 0.18F;
            this.haoriJacket.xRot += 0.06F + Mth.sin(state.ageInTicks * 0.35F) * 0.03F;
        }
    }
}
