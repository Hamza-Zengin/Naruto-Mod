package com.sekwah.narutomod.client.model.item.model;

import com.sekwah.narutomod.util.ModelUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.LivingEntity;

/**
 * Custom 3D Armor Models for V10 Shinobi Attire:
 * 1. Chūnin Tactical Flak Vest (High collar, tactical scroll pouches, shoulder guards)
 * 2. ANBU Black Ops Battle Armor (Chest armor plate, arm bracers, diagonal 3D Tanto sword on back)
 * 3. Akatsuki Cloak (High stiff collar, flowing full-length dark red-cloud coat)
 */
public class ShinobiArmorModel<T extends LivingEntity> extends HumanoidModel<T> {

    public ShinobiArmorModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createChuninVestLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Main tactical flak vest body
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.45F))
                        // High protective neck collar
                        .texOffs(0, 0)
                        .addBox(-4.5F, -2.2F, -2.5F, 9, 3, 5, new CubeDeformation(0.15F))
                        // Left & Right scroll pouches on chest
                        .texOffs(0, 8)
                        .addBox(-3.8F, 2.5F, -3.2F, 3, 4, 1)
                        .texOffs(8, 8)
                        .addBox(0.8F, 2.5F, -3.2F, 3, 4, 1),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 6, 4, new CubeDeformation(0.35F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(40, 16)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 6, 4, new CubeDeformation(0.35F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createAnbuArmorLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition body = root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Sleek grey ANBU tactical chestplate over dark bodysuit
                        .texOffs(16, 16)
                        .addBox(-4.0F, 0.0F, -2.0F, 8, 12, 4, new CubeDeformation(0.42F))
                        // Shoulder straps
                        .texOffs(0, 0)
                        .addBox(-4.2F, -0.5F, -2.5F, 2, 4, 5, new CubeDeformation(0.1F))
                        .texOffs(0, 0)
                        .addBox(2.2F, -0.5F, -2.5F, 2, 4, 5, new CubeDeformation(0.1F)),
                PartPose.ZERO);

        // Diagonal 3D Tanto Sword & Sheath mounted on the ANBU operative's back
        body.addOrReplaceChild("tanto_sheath",
                CubeListBuilder.create()
                        .texOffs(0, 16)
                        .addBox(-1.0F, -5.0F, 2.5F, 2, 11, 1)
                        // Tanto Hilt
                        .texOffs(6, 16)
                        .addBox(-0.5F, -9.0F, 2.5F, 1, 4, 1),
                PartPose.offsetAndRotation(0.0F, 6.0F, 0.0F, 0.0F, 0.0F, -0.55F));

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(40, 16)
                        .addBox(-3.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.3F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(40, 16)
                        .addBox(-1.0F, -2.0F, -2.0F, 4, 10, 4, new CubeDeformation(0.3F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }

    public static LayerDefinition createAkatsukiCloakLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create()
                        // Upper Cloak Torso
                        .texOffs(16, 16)
                        .addBox(-4.5F, 0.0F, -2.5F, 9, 12, 5, new CubeDeformation(0.5F))
                        // Lower Flowing Coat Skirt
                        .texOffs(0, 16)
                        .addBox(-5.0F, 11.5F, -2.8F, 10, 8, 6, new CubeDeformation(0.35F))
                        // Iconic High Stiff Akatsuki Collar
                        .texOffs(0, 0)
                        .addBox(-5.0F, -3.2F, -3.0F, 10, 4, 6, new CubeDeformation(0.2F)),
                PartPose.ZERO);

        root.addOrReplaceChild("right_arm",
                CubeListBuilder.create()
                        .texOffs(44, 16)
                        .addBox(-3.5F, -2.0F, -2.5F, 5, 11, 5, new CubeDeformation(0.35F)),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        root.addOrReplaceChild("left_arm",
                CubeListBuilder.create()
                        .mirror(true)
                        .texOffs(44, 16)
                        .addBox(-1.5F, -2.0F, -2.5F, 5, 11, 5, new CubeDeformation(0.35F)),
                PartPose.offset(5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 32);
    }
}
