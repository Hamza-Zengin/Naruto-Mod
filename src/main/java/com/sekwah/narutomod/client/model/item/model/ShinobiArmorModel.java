package com.sekwah.narutomod.client.model.item.model;

import com.sekwah.sekclib.util.ModelUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * High-Detail 3D Armor Models with 100% Non-Overlapping 64x64 UV Islands:
 * 1. Chūnin Tactical Flak Vest (High collar, upper & lower 3D scroll pouches, 3D shoulder pads, Konoha back emblem)
 * 2. ANBU Black Ops Battle Armor (Raised 3D chestplate, segmented waist tassets, forearm bracers, 3-part 3D Tanto on back)
 * 3. Akatsuki Cloak (High crimson-lined collar, pleated torso & flaring lower coat skirt, wide kimono sleeves)
 */
public class ShinobiArmorModel extends HumanoidModel<HumanoidRenderState> {

    private final ModelPart cloakSkirt;

    public ShinobiArmorModel(ModelPart root) {
        super(root);
        this.cloakSkirt = this.body.hasChild("cloak_skirt") ? this.body.getChild("cloak_skirt") : null;
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (this.cloakSkirt != null) {
            float forwardLegXRot = Math.min(0.0F, Math.min(this.leftLeg.xRot, this.rightLeg.xRot));
            this.cloakSkirt.xRot = -this.body.xRot + forwardLegXRot * 0.85F - (state.isCrouching ? 0.35F : 0.0F);
            this.cloakSkirt.z = state.isCrouching ? -0.8F : 0.0F;
        }
    }

    public static LayerDefinition createChuninVestLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // 1. Main tactical flak vest body (u=0..24, v=0..16)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.42F))
                        // 2. High protective padded neck collar (u=0..28, v=18..26)
                        .texOffs(0, 18)
                        .addBox(-4.5F, -2.4F, -2.5F, 9, 3, 5, new CubeDeformation(0.12F))
                        // 3. Upper 3D Tactical Scroll Pouches on right & left chest
                        .texOffs(0, 28)
                        .addBox(-3.7F, 2.0F, -3.1F, 3, 4, 1, new CubeDeformation(0.05F))
                        .texOffs(10, 28)
                        .addBox(0.7F, 2.0F, -3.1F, 3, 4, 1, new CubeDeformation(0.05F))
                        // 4. Lower 3D Utility Holster Pouches
                        .texOffs(20, 28)
                        .addBox(-3.6F, 7.2F, -2.9F, 3, 3, 1, CubeDeformation.NONE)
                        .texOffs(30, 28)
                        .addBox(0.6F, 7.2F, -2.9F, 3, 3, 1, CubeDeformation.NONE),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // Navy shinobi undersleeve (u=0..16, v=36..49)
                        .texOffs(0, 36)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.22F))
                        // Olive 3D Flak Shoulder Guard (u=32..50, v=0..10)
                        .texOffs(32, 0)
                        .addBox(-3.3F, -2.2F, -2.5F, 4, 5, 5, new CubeDeformation(0.32F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        // Navy shinobi undersleeve (u=18..34, v=36..49)
                        .texOffs(18, 36)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.22F))
                        // Olive 3D Flak Shoulder Guard (u=32..50, v=12..22)
                        .texOffs(32, 12)
                        .addBox(-0.7F, -2.2F, -2.5F, 4, 5, 5, new CubeDeformation(0.32F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createAnbuArmorLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // 1. Dark ANBU mesh bodysuit torso (u=0..24, v=0..16)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.30F))
                        // 2. Raised 3D Titanium-Grey ANBU Chestplate (u=0..26, v=18..30)
                        .texOffs(0, 18)
                        .addBox(-4.0F, 0.5F, -2.5F, 8, 7, 5, new CubeDeformation(0.24F))
                        // 3. 3D Shoulder Armor Straps (u=28..42, v=0..8 & v=10..18)
                        .texOffs(28, 0)
                        .addBox(-4.1F, -0.4F, -2.5F, 2, 3, 5, new CubeDeformation(0.18F))
                        .texOffs(28, 10)
                        .addBox(2.1F, -0.4F, -2.5F, 2, 3, 5, new CubeDeformation(0.18F))
                        // 4. Segmented 3D Waist Tassets / Belt Guard (u=0..26, v=32..41)
                        .texOffs(0, 32)
                        .addBox(-4.0F, 8.0F, -2.5F, 8, 4, 5, new CubeDeformation(0.16F)),
                PartPose.ZERO);

        // 5. Diagonal 3D ANBU Tantō Sword (Sheath + Guard + Wrapped Hilt) mounted on back
        body.addOrReplaceChild("tanto_sheath",
                CubeListBuilder.create()
                        // Lacquered black/red sheath (u=44..50, v=0..12)
                        .texOffs(44, 0)
                        .addBox(-1.0F, -5.0F, 2.7F, 2, 11, 1, new CubeDeformation(0.08F))
                        // Metallic handguard tsuba (u=52..62, v=0..3)
                        .texOffs(52, 0)
                        .addBox(-1.5F, -6.0F, 2.2F, 3, 1, 2, CubeDeformation.NONE)
                        // Wrapped Tantō hilt (u=44..48, v=14..19)
                        .texOffs(44, 14)
                        .addBox(-0.5F, -10.0F, 2.7F, 1, 4, 1, new CubeDeformation(0.08F)),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.55F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // Upper arm band & dark glove (u=0..16, v=44..58)
                        .texOffs(0, 44)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.20F))
                        // 3D Metallic Forearm Bracer Plate (u=34..50, v=44..53)
                        .texOffs(34, 44)
                        .addBox(-3.2F, 3.0F, -2.2F, 4, 5, 4, new CubeDeformation(0.38F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        // Upper arm with crimson ANBU tattoo & glove (u=17..33, v=44..58)
                        .texOffs(17, 44)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.20F))
                        // 3D Metallic Forearm Bracer Plate (u=34..50, v=54..63)
                        .texOffs(34, 54)
                        .addBox(-0.8F, 3.0F, -2.2F, 4, 5, 4, new CubeDeformation(0.38F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createAkatsukiCloakLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // 1. Upper Cloak Torso (u=0..28, v=0..17)
                        .texOffs(0, 0)
                        .addBox(-4.5F, 0.0F, -2.5F, 9, 12, 5, new CubeDeformation(0.45F))
                        // 3. Iconic High Stiff Crimson-Lined Akatsuki Collar (widened to prevent ±90° head clipping)
                        .texOffs(0, 34)
                        .addBox(-5.0F, -2.2F, -3.0F, 10, 4, 6, new CubeDeformation(0.65F, 0.0F, 0.65F)),
                PartPose.ZERO);

        // 2. Lower Flowing Pleated Coat Skirt as animated child part (u=0..32, v=18..33)
        body.addOrReplaceChild("cloak_skirt",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-5.0F, 0.0F, -3.0F, 10, 9, 6, new CubeDeformation(0.38F)),
                PartPose.offset(0.0F, 11.2F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // Wide kimono sleeve right (u=0..20, v=45..61)
                        .texOffs(0, 45)
                        .addBox(-3.5F, -2.0F, -2.5F, 5, 11, 5, new CubeDeformation(0.35F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        // Wide kimono sleeve left (u=22..42, v=45..61)
                        .texOffs(22, 45)
                        .addBox(-1.5F, -2.0F, -2.5F, 5, 11, 5, new CubeDeformation(0.35F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createSageCloakLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // 1. Crimson & Black Flame-Hemmed Sage Cloak Torso (u=0..28, v=0..17)
                        .texOffs(0, 0)
                        .addBox(-4.5F, 0.0F, -2.5F, 9, 12, 5, new CubeDeformation(0.45F))
                        // 2. High Sage Cloak Collar (u=0..32, v=34..44)
                        .texOffs(0, 34)
                        .addBox(-5.0F, -2.2F, -3.0F, 10, 4, 6, new CubeDeformation(0.65F, 0.0F, 0.65F)),
                PartPose.ZERO);

        // 3. Lower Flowing Flame-Hemmed Coat Skirt (u=0..32, v=18..33)
        body.addOrReplaceChild("cloak_skirt",
                CubeListBuilder.create()
                        .texOffs(0, 18)
                        .addBox(-5.0F, 0.0F, -3.0F, 10, 9, 6, new CubeDeformation(0.38F)),
                PartPose.offset(0.0F, 11.2F, 0.0F));

        // 4. Iconic Back-Mounted Myobokuzan Summoning Scroll Cylinder (u=34..62, v=0..8 & caps u=34..46, v=10..20)
        body.addOrReplaceChild("sage_scroll",
                CubeListBuilder.create()
                        .texOffs(34, 0)
                        .addBox(-5.0F, -2.0F, -2.0F, 10, 4, 4, new CubeDeformation(0.10F))
                        .texOffs(34, 10)
                        .addBox(-6.0F, -2.5F, -2.5F, 1, 5, 5, CubeDeformation.NONE)
                        .texOffs(34, 10)
                        .addBox(5.0F, -2.5F, -2.5F, 1, 5, 5, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, 4.2F, 4.6F, 0.0F, 0.0F, -0.26F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(0, 45)
                        .addBox(-3.5F, -2.0F, -2.5F, 5, 11, 5, new CubeDeformation(0.35F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(22, 45)
                        .addBox(-1.5F, -2.0F, -2.5F, 5, 11, 5, new CubeDeformation(0.35F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createJoninVestLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // High-rank dark olive Jōnin tactical flak vest torso (u=0..24, v=0..16)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.45F))
                        // Reinforced padded protective collar (u=0..28, v=18..26)
                        .texOffs(0, 18)
                        .addBox(-4.5F, -2.5F, -2.5F, 9, 3, 5, new CubeDeformation(0.15F))
                        // Dual tactical chest scroll holsters + extra waist kunai pouch
                        .texOffs(0, 28)
                        .addBox(-3.8F, 2.0F, -3.2F, 3, 4, 1, new CubeDeformation(0.08F))
                        .texOffs(10, 28)
                        .addBox(0.8F, 2.0F, -3.2F, 3, 4, 1, new CubeDeformation(0.08F))
                        .texOffs(20, 28)
                        .addBox(-3.7F, 7.0F, -3.0F, 3, 3, 1, CubeDeformation.NONE)
                        .texOffs(30, 28)
                        .addBox(0.7F, 7.0F, -3.0F, 3, 3, 1, CubeDeformation.NONE),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // Dark shinobi undersleeve + reinforced forearm guard (u=0..16, v=36..52)
                        .texOffs(0, 36)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.24F))
                        // Flared Jōnin shoulder armor cap (u=32..50, v=0..10)
                        .texOffs(32, 0)
                        .addBox(-3.4F, -2.3F, -2.5F, 4, 5, 5, new CubeDeformation(0.36F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(18, 36)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.24F))
                        .texOffs(32, 12)
                        .addBox(-0.6F, -2.3F, -2.5F, 4, 5, 5, new CubeDeformation(0.36F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createWarringStatesArmorLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Chainmail/dark robe underlayer (u=0..24, v=0..16)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.32F))
                        // Layered Sengoku crimson lamellar cuirass (u=0..28, v=18..31)
                        .texOffs(0, 18)
                        .addBox(-4.3F, 0.4F, -2.6F, 8.6F, 8.0F, 5.2F, new CubeDeformation(0.28F))
                        // Neck throat guard (Nodowa) (u=30..48, v=0..5)
                        .texOffs(30, 0)
                        .addBox(-3.5F, -1.8F, -2.6F, 7, 2, 5, new CubeDeformation(0.14F)),
                PartPose.ZERO);

        // Kusazuri waist lamellar tassets animated against legs
        body.addOrReplaceChild("cloak_skirt",
                CubeListBuilder.create()
                        .texOffs(0, 33)
                        .addBox(-4.6F, 0.0F, -2.7F, 9.2F, 5.5F, 5.4F, new CubeDeformation(0.22F)),
                PartPose.offset(0.0F, 8.8F, 0.0F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        // Armored kote bracer sleeve (u=0..16, v=45..59)
                        .texOffs(0, 45)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.24F))
                        // Iconic Sengoku layered Sode shoulder pauldron (u=32..52, v=8..20)
                        .texOffs(32, 8)
                        .addBox(-4.2F, -2.5F, -2.8F, 4, 6, 5.6F, new CubeDeformation(0.28F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .texOffs(18, 45)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.24F))
                        .texOffs(32, 22)
                        .addBox(0.2F, -2.5F, -2.8F, 4, 6, 5.6F, new CubeDeformation(0.28F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createShinobiLeggingsLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Shinobi waist belt (u=0..24, v=0..6)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 10.0F, -2.0F, 8, 2, 4, new CubeDeformation(0.35F))
                        // Hip scroll pouch (u=26..34, v=0..5)
                        .texOffs(26, 0)
                        .addBox(-4.4F, 9.5F, -2.5F, 2, 3, 2, CubeDeformation.NONE),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // Navy shinobi trousers (u=0..16, v=8..21)
                        .texOffs(0, 8)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.28F))
                        // 3D Bandage thigh wrap (u=0..16, v=22..29)
                        .texOffs(0, 22)
                        .addBox(-2.2F, 2.5F, -2.2F, 4, 3, 4, new CubeDeformation(0.36F))
                        // 3D Outer thigh kunai holster pouch (u=18..26, v=22..28)
                        .texOffs(18, 22)
                        .addBox(-3.0F, 2.6F, -1.3F, 1, 3, 3, new CubeDeformation(0.05F))
                        // 3D Kunai hilt protruding from holster (u=28..32, v=22..25)
                        .texOffs(28, 22)
                        .addBox(-2.8F, 1.4F, -0.5F, 1, 2, 1, CubeDeformation.NONE)
                        // 3D Olive knee guard (u=0..10, v=30..33)
                        .texOffs(0, 30)
                        .addBox(-2.0F, 5.2F, -2.4F, 4, 2, 1, new CubeDeformation(0.12F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // Navy shinobi trousers (u=18..34, v=8..21)
                        .texOffs(18, 8)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.28F))
                        // 3D Knee strap (u=36..52, v=8..14)
                        .texOffs(36, 8)
                        .addBox(-2.1F, 5.4F, -2.1F, 4, 2, 4, new CubeDeformation(0.32F))
                        // 3D Olive knee guard (u=12..22, v=30..33)
                        .texOffs(12, 30)
                        .addBox(-2.0F, 5.2F, -2.4F, 4, 2, 1, new CubeDeformation(0.12F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createShinobiSandalsLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // Ankle bandage wraps (u=0..16, v=0..7)
                        .texOffs(0, 0)
                        .addBox(-2.0F, 6.5F, -2.0F, 4, 3, 4, new CubeDeformation(0.26F))
                        // Open-toe navy sandal upper & heel guard (u=0..14, v=9..14)
                        .texOffs(0, 9)
                        .addBox(-2.0F, 9.5F, -1.4F, 4, 2, 3, new CubeDeformation(0.30F))
                        // Exposed toes front tip (u=16..24, v=9..11)
                        .texOffs(16, 9)
                        .addBox(-1.6F, 10.4F, -2.3F, 3, 1, 1, new CubeDeformation(0.08F))
                        // Sandal sole (u=0..16, v=16..21)
                        .texOffs(0, 16)
                        .addBox(-2.1F, 11.2F, -2.3F, 4, 1, 4, new CubeDeformation(0.28F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // Ankle bandage wraps (u=18..34, v=0..7)
                        .texOffs(18, 0)
                        .addBox(-2.0F, 6.5F, -2.0F, 4, 3, 4, new CubeDeformation(0.26F))
                        // Open-toe navy sandal upper & heel guard (u=26..40, v=9..14)
                        .texOffs(26, 9)
                        .addBox(-2.0F, 9.5F, -1.4F, 4, 2, 3, new CubeDeformation(0.30F))
                        // Exposed toes front tip (u=42..50, v=9..11)
                        .texOffs(42, 9)
                        .addBox(-1.4F, 10.4F, -2.3F, 3, 1, 1, new CubeDeformation(0.08F))
                        // Sandal sole (u=18..34, v=16..21)
                        .texOffs(18, 16)
                        .addBox(-2.1F, 11.2F, -2.3F, 4, 1, 4, new CubeDeformation(0.28F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createAnbuLeggingsLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Tactical ANBU waist belt (u=0..24, v=0..6)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 10.0F, -2.0F, 8, 2, 4, new CubeDeformation(0.34F))
                        // Rear smoke/scroll pouch (u=26..38, v=0..4)
                        .texOffs(26, 0)
                        .addBox(-2.5F, 9.5F, 2.2F, 5, 3, 1, new CubeDeformation(0.08F)),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // Charcoal ANBU combat trousers (u=0..16, v=8..21)
                        .texOffs(0, 8)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.28F))
                        // Strapped thigh wraps & buckle (u=0..16, v=22..29)
                        .texOffs(0, 22)
                        .addBox(-2.1F, 2.2F, -2.1F, 4, 3, 4, new CubeDeformation(0.36F))
                        // Dual kunai holster pouch (u=18..26, v=22..29)
                        .texOffs(18, 22)
                        .addBox(-3.0F, 2.4F, -1.5F, 1, 4, 3, new CubeDeformation(0.05F))
                        // Titanium armored knee plate (u=0..10, v=31..35)
                        .texOffs(0, 31)
                        .addBox(-2.0F, 5.2F, -2.6F, 4, 3, 1, new CubeDeformation(0.14F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // Charcoal ANBU combat trousers (u=18..34, v=8..21)
                        .texOffs(18, 8)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.28F))
                        // Left thigh strap (u=36..52, v=8..14)
                        .texOffs(36, 8)
                        .addBox(-2.1F, 2.8F, -2.1F, 4, 2, 4, new CubeDeformation(0.34F))
                        // Titanium armored knee plate (u=12..22, v=31..35)
                        .texOffs(12, 31)
                        .addBox(-2.0F, 5.2F, -2.6F, 4, 3, 1, new CubeDeformation(0.14F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createAnbuBootsLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // Tall strapped ANBU combat boot shaft (u=0..16, v=0..10)
                        .texOffs(0, 0)
                        .addBox(-2.0F, 6.0F, -2.0F, 4, 6, 4, new CubeDeformation(0.30F))
                        // Titanium shin greave plate (u=0..10, v=12..17)
                        .texOffs(0, 12)
                        .addBox(-1.8F, 5.6F, -2.5F, 4, 4, 1, new CubeDeformation(0.12F))
                        // Silent stealth padded sole & toe cap (u=0..16, v=19..25)
                        .texOffs(0, 19)
                        .addBox(-2.1F, 10.6F, -2.4F, 4, 2, 4, new CubeDeformation(0.28F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // Tall strapped ANBU combat boot shaft (u=18..34, v=0..10)
                        .texOffs(18, 0)
                        .addBox(-2.0F, 6.0F, -2.0F, 4, 6, 4, new CubeDeformation(0.30F))
                        // Titanium shin greave plate (u=12..22, v=12..17)
                        .texOffs(12, 12)
                        .addBox(-2.2F, 5.6F, -2.5F, 4, 4, 1, new CubeDeformation(0.12F))
                        // Silent stealth padded sole & toe cap (u=18..34, v=19..25)
                        .texOffs(18, 19)
                        .addBox(-2.1F, 10.6F, -2.4F, 4, 2, 4, new CubeDeformation(0.28F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createWarringStatesLeggingsLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Gold-trimmed Sengoku waist sash (u=0..24, v=0..7)
                        .texOffs(0, 0)
                        .addBox(-4.0F, 9.5F, -2.0F, 8, 3, 4, new CubeDeformation(0.36F)),
                PartPose.ZERO);

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // Dark armored hakama base (u=0..16, v=9..22)
                        .texOffs(0, 9)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.28F))
                        // Layered crimson haidate thigh plates (u=0..16, v=24..34)
                        .texOffs(0, 24)
                        .addBox(-2.2F, 1.2F, -2.5F, 4, 6, 4, new CubeDeformation(0.38F))
                        // Steel knee boss (u=0..10, v=36..39)
                        .texOffs(0, 36)
                        .addBox(-2.0F, 5.8F, -2.7F, 4, 2, 1, new CubeDeformation(0.15F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // Dark armored hakama base (u=18..34, v=9..22)
                        .texOffs(18, 9)
                        .addBox(-2.0F, 0.0F, -2.0F, 4, 9, 4, new CubeDeformation(0.28F))
                        // Layered crimson haidate thigh plates (u=18..34, v=24..34)
                        .texOffs(18, 24)
                        .addBox(-2.0F, 1.2F, -2.5F, 4, 6, 4, new CubeDeformation(0.38F))
                        // Steel knee boss (u=12..22, v=36..39)
                        .texOffs(12, 36)
                        .addBox(-2.0F, 5.8F, -2.7F, 4, 2, 1, new CubeDeformation(0.15F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public static LayerDefinition createWarringStatesBootsLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("right_leg",
                CubeListBuilder.create()
                        // Suneate armored shin greave (u=0..16, v=0..10)
                        .texOffs(0, 0)
                        .addBox(-2.0F, 5.6F, -2.0F, 4, 6, 4, new CubeDeformation(0.32F))
                        // Layered front iron-crimson splint (u=0..10, v=12..18)
                        .texOffs(0, 12)
                        .addBox(-1.8F, 5.2F, -2.6F, 4, 5, 1, new CubeDeformation(0.14F))
                        // Heavy iron-capped war sandal (u=0..18, v=20..27)
                        .texOffs(0, 20)
                        .addBox(-2.2F, 10.4F, -2.5F, 4, 2, 5, new CubeDeformation(0.28F)),
                PartPose.offset(-1.9F, 12.0F, 0.0F));

        root.addOrReplaceChild("left_leg",
                CubeListBuilder.create()
                        // Suneate armored shin greave (u=18..34, v=0..10)
                        .texOffs(18, 0)
                        .addBox(-2.0F, 5.6F, -2.0F, 4, 6, 4, new CubeDeformation(0.32F))
                        // Layered front iron-crimson splint (u=12..22, v=12..18)
                        .texOffs(12, 12)
                        .addBox(-2.2F, 5.2F, -2.6F, 4, 5, 1, new CubeDeformation(0.14F))
                        // Heavy iron-capped war sandal (u=20..38, v=20..27)
                        .texOffs(20, 20)
                        .addBox(-2.2F, 10.4F, -2.5F, 4, 2, 5, new CubeDeformation(0.28F)),
                PartPose.offset(1.9F, 12.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }
}


