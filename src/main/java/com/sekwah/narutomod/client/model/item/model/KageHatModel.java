package com.sekwah.narutomod.client.model.item.model;

import com.sekwah.sekclib.util.ModelUtils;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * 3D Kage Hat (Kage-bōshi) Model
 * Wide conical bamboo hat with crimson/gold crown trim and white ceremonial side/back drapes.
 */
public class KageHatModel extends HumanoidModel<HumanoidRenderState> {

    public KageHatModel(ModelPart modelPart) {
        super(modelPart);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = ModelUtils.createBlankHumanoidMesh();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("head",
                CubeListBuilder.create()
                        // Wide circular/square bamboo hat brim (u=0..48, v=0..16)
                        .texOffs(0, 0)
                        .addBox(-7.5F, -8.5F, -7.5F, 15, 1.5F, 15, CubeDeformation.NONE)
                        // Second conical tier (u=0..40, v=18..31)
                        .texOffs(0, 18)
                        .addBox(-5.5F, -10.2F, -5.5F, 11, 2.0F, 11, CubeDeformation.NONE)
                        // Upper hat crown (u=0..28, v=32..42)
                        .texOffs(0, 32)
                        .addBox(-3.5F, -12.0F, -3.5F, 7, 2.0F, 7, CubeDeformation.NONE)
                        // Crimson crown peak & gold Kage crest plaque (u=48..64, v=0..12)
                        .texOffs(48, 0)
                        .addBox(-1.5F, -13.2F, -1.5F, 3, 1.5F, 3, CubeDeformation.NONE)
                        .texOffs(48, 6)
                        .addBox(-2.0F, -11.5F, -4.1F, 4, 3.0F, 1, CubeDeformation.NONE),
                PartPose.ZERO);

        head.addOrReplaceChild("hat", CubeListBuilder.create(), PartPose.ZERO);

        // White ceremonial neck/side veil drapes (u=0..30, v=44..56)
        head.addOrReplaceChild("veil_back",
                CubeListBuilder.create()
                        .texOffs(0, 44)
                        .addBox(-6.0F, -7.2F, 4.6F, 12, 7, 1, CubeDeformation.NONE),
                PartPose.rotation(0.10F, 0.0F, 0.0F));
        head.addOrReplaceChild("veil_left",
                CubeListBuilder.create()
                        .texOffs(28, 44)
                        .addBox(4.6F, -7.2F, -4.5F, 1, 6, 9, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, -0.10F));
        head.addOrReplaceChild("veil_right",
                CubeListBuilder.create()
                        .texOffs(48, 44)
                        .addBox(-5.6F, -7.2F, -4.5F, 1, 6, 9, CubeDeformation.NONE),
                PartPose.rotation(0.0F, 0.0F, 0.10F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(HumanoidRenderState state) {
        super.setupAnim(state);
        if (state instanceof ArmorStandRenderState armorStandState) {
            float degToRad = (float) (Math.PI / 180.0);
            this.head.xRot = armorStandState.headPose.x() * degToRad;
            this.head.yRot = armorStandState.headPose.y() * degToRad;
            this.head.zRot = armorStandState.headPose.z() * degToRad;
            this.head.y = 1.0F;
        }
    }
}
