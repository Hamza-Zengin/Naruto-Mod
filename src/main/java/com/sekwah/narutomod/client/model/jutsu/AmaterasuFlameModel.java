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
 * 8. AMATERASU — 3D Inextinguishable Black-Crimson Flame Shroud &amp; Pillars.
 */
public class AmaterasuFlameModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart innerPillars;
    private final ModelPart outerPillars;

    public AmaterasuFlameModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.innerPillars = part.getChild("inner_pillars");
        this.outerPillars = part.getChild("outer_pillars");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Multi-angle Tapered Inner Black Flame Core
        PartDefinition inner = root.addOrReplaceChild("inner_pillars",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.8F, -14.0F, -2.8F, 5.6F, 14.0F, 5.6F)
                        .texOffs(24, 0).addBox(-1.8F, -25.0F, -1.8F, 3.6F, 12.0F, 3.6F),
                PartPose.ZERO);
        inner.addOrReplaceChild("inner_rot_a",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.6F, -15.0F, -2.6F, 5.2F, 15.0F, 5.2F)
                        .texOffs(24, 0).addBox(-1.5F, -27.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                PartPose.rotation(0.06F, 0.7854F, -0.06F));
        inner.addOrReplaceChild("inner_rot_b",
                CubeListBuilder.create().texOffs(24, 0).addBox(-1.2F, -29.0F, -1.2F, 2.4F, 16.0F, 2.4F),
                PartPose.rotation(-0.08F, 0.5236F, 0.08F));

        // 8 Spiraling 3-Segment Tapered Outer Flame Tongues
        PartDefinition outer = root.addOrReplaceChild("outer_pillars", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 8.0);
            float radius = (i % 2 == 0) ? 7.8F : 6.4F;
            float rx = Mth.cos(angle) * radius;
            float rz = Mth.sin(angle) * radius;
            PartDefinition f0 = outer.addOrReplaceChild("flame_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 36).addBox(-2.0F, -8.5F, -0.9F, 4.0F, 8.5F, 1.8F),
                    PartPose.offsetAndRotation(rx, 0.0F, rz, 0.16F, -angle + (float) (Math.PI / 2.0), 0.14F));
            PartDefinition f1 = f0.addOrReplaceChild("flame_mid_" + i,
                    CubeListBuilder.create().texOffs(0, 36).addBox(-1.4F, -7.5F, -0.7F, 2.8F, 7.5F, 1.4F),
                    PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.14F, 0.18F, 0.12F));
            f1.addOrReplaceChild("flame_tip_" + i,
                    CubeListBuilder.create().texOffs(0, 36).addBox(-0.8F, -6.5F, -0.5F, 1.6F, 6.5F, 1.0F),
                    PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.18F, 0.20F, 0.14F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        this.innerPillars.yRot = age * 0.25F;
        this.outerPillars.yRot = -age * 0.18F;
        this.outerPillars.y = Mth.sin(age * 0.35F) * 1.5F;
    }
}
