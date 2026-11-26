package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * 9. KAGE MANE NO JUTSU — 3D Shadow Ribbon &amp; Coiling Shadow Claws.
 */
public class ShadowBindModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart claws;

    public ShadowBindModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.claws = part.getChild("claws");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Rounded Multi-Rotated Shadow Pool Disc & Tapered Segmented Shadow Ribbon
        PartDefinition pool = root.addOrReplaceChild("shadow_pool",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-9.0F, -0.8F, -9.0F, 18, 1, 18)
                        .texOffs(0, 25).addBox(-2.2F, -0.6F, 0.0F, 4.4F, 1.0F, 12.0F),
                PartPose.ZERO);
        pool.addOrReplaceChild("pool_rot_a",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -0.78F, -9.0F, 18, 1, 18),
                PartPose.rotation(0.0F, 0.5236F, 0.0F));
        pool.addOrReplaceChild("pool_rot_b",
                CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -0.76F, -9.0F, 18, 1, 18),
                PartPose.rotation(0.0F, 1.0472F, 0.0F));
        pool.addOrReplaceChild("ribbon_mid",
                CubeListBuilder.create().texOffs(0, 25).addBox(-1.8F, -0.6F, 11.5F, 3.6F, 1.0F, 12.0F),
                PartPose.rotation(0.0F, 0.08F, 0.0F));
        pool.addOrReplaceChild("ribbon_tip",
                CubeListBuilder.create().texOffs(0, 25).addBox(-1.3F, -0.6F, 22.5F, 2.6F, 1.0F, 12.5F),
                PartPose.rotation(0.0F, -0.06F, 0.0F));

        // 6 Articulated 3-Joint Curving Shadow Claws
        PartDefinition claws = root.addOrReplaceChild("claws", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 6.0);
            float cx = Mth.cos(angle) * 9.0F;
            float cz = Mth.sin(angle) * 9.0F;
            PartDefinition clawBase = claws.addOrReplaceChild("claw_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 28).addBox(-1.3F, -8.5F, -1.3F, 2.6F, 8.5F, 2.6F),
                    PartPose.offsetAndRotation(cx, 0.0F, cz, 0.14F, -angle + (float) (Math.PI / 2.0), 0.0F));
            PartDefinition clawMid = clawBase.addOrReplaceChild("claw_mid_" + i,
                    CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.28F, 0.0F, 0.0F));
            clawMid.addOrReplaceChild("claw_tip_" + i,
                    CubeListBuilder.create().texOffs(0, 28).addBox(-0.65F, -7.0F, -0.65F, 1.3F, 7.0F, 1.3F),
                    PartPose.offsetAndRotation(0.0F, -7.6F, 0.0F, 0.34F, 0.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float emerge = age < 6.0F ? (1.0F - (age / 6.0F)) * 22.0F : 0.0F;
        this.claws.y = emerge;
        this.claws.yRot = Mth.sin(age * 0.18F) * 0.22F;
    }
}
