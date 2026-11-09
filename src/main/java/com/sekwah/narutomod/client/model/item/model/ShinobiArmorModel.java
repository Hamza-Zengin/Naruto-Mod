package com.sekwah.narutomod.client.model.item.model;

import com.sekwah.narutomod.util.ModelUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

/**
 * High-Detail 3D Armor Models with 100% Non-Overlapping 64x64 UV Islands:
 * 1. Chūnin Tactical Flak Vest (High collar, upper & lower 3D scroll pouches, 3D shoulder pads, Konoha back emblem)
 * 2. ANBU Black Ops Battle Armor (Raised 3D chestplate, segmented waist tassets, forearm bracers, 3-part 3D Tanto on back)
 * 3. Akatsuki Cloak (High crimson-lined collar, pleated torso & flaring lower coat skirt, wide kimono sleeves)
 */
public class ShinobiArmorModel<T extends LivingEntity> extends HumanoidModel<T> {

    public ShinobiArmorModel(ModelPart root) {
        super(root);
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

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // 1. Upper Cloak Torso (u=0..28, v=0..17)
                        .texOffs(0, 0)
                        .addBox(-4.5F, 0.0F, -2.5F, 9, 12, 5, new CubeDeformation(0.45F))
                        // 2. Lower Flowing Pleated Coat Skirt (u=0..32, v=18..33)
                        .texOffs(0, 18)
                        .addBox(-5.0F, 11.2F, -3.0F, 10, 9, 6, new CubeDeformation(0.38F))
                        // 3. Iconic High Stiff Crimson-Lined Akatsuki Collar (u=0..32, v=34..44)
                        .texOffs(0, 34)
                        .addBox(-5.0F, -3.2F, -3.0F, 10, 4, 6, new CubeDeformation(0.20F)),
                PartPose.ZERO);

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
}

