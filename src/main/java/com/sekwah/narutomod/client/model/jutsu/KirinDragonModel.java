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
 * 1. RAITON: KIRIN — Colossal 3D Descending Lightning Dragon Model.
 */
public class KirinDragonModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart dragonHead;
    private final ModelPart dragonBody;
    private final ModelPart shockRingInner;
    private final ModelPart shockRingOuter;

    public KirinDragonModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.dragonHead = part.getChild("dragon_head");
        this.dragonBody = part.getChild("dragon_body");
        this.shockRingInner = part.getChild("shock_ring_inner");
        this.shockRingOuter = part.getChild("shock_ring_outer");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("dragon_head",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-6.0F, -6.0F, -10.0F, 12, 9, 12)
                        .texOffs(0, 21).addBox(-4.5F, -3.0F, -20.0F, 9, 5, 10)
                        .texOffs(38, 21).addBox(-4.0F, 2.0F, -19.0F, 2, 3, 2)
                        .texOffs(38, 21).addBox(2.0F, 2.0F, -19.0F, 2, 3, 2),
                PartPose.offset(0.0F, -18.0F, 0.0F));

        head.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create()
                        .texOffs(0, 36).addBox(-4.0F, 0.0F, -10.0F, 8, 3, 10),
                PartPose.offsetAndRotation(0.0F, 2.0F, -9.0F, 0.48F, 0.0F, 0.0F));

        head.addOrReplaceChild("horn_left",
                CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-1.5F, -2.0F, 0.0F, 3, 3, 16)
                        .texOffs(36, 0).addBox(-1.0F, 0.5F, 8.0F, 2, 2, 10),
                PartPose.offsetAndRotation(4.5F, -5.0F, -2.0F, 0.45F, 0.32F, 0.0F));
        head.addOrReplaceChild("horn_right",
                CubeListBuilder.create()
                        .texOffs(36, 0).addBox(-1.5F, -2.0F, 0.0F, 3, 3, 16)
                        .texOffs(36, 0).addBox(-1.0F, 0.5F, 8.0F, 2, 2, 10),
                PartPose.offsetAndRotation(-4.5F, -5.0F, -2.0F, 0.45F, -0.32F, 0.0F));

        PartDefinition body = root.addOrReplaceChild("dragon_body", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float segY = -28.0F - (i * 15.0F);
            float offsetX = Mth.sin(i * 1.1F) * 5.0F;
            float offsetZ = Mth.cos(i * 1.1F) * 5.0F;
            body.addOrReplaceChild("seg_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 49).addBox(-4.5F, -8.0F, -4.5F, 9, 16, 9)
                            .texOffs(38, 36).addBox(-1.0F, -7.0F, 4.5F, 2, 14, 5),
                    PartPose.offsetAndRotation(offsetX, segY, offsetZ, 0.18F, i * 0.55F, 0.12F));
        }

        root.addOrReplaceChild("shock_ring_inner",
                CubeListBuilder.create()
                        .texOffs(0, 74).addBox(-14.0F, -3.0F, -14.0F, 28, 3, 28),
                PartPose.ZERO);
        root.addOrReplaceChild("shock_ring_outer",
                CubeListBuilder.create()
                        .texOffs(0, 74).addBox(-22.0F, -2.0F, -22.0F, 44, 2, 44),
                PartPose.ZERO);

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float plunge = progress < 0.25F ? (1.0F - (progress / 0.25F)) * -48.0F : 0.0F;
        this.dragonHead.y = -14.0F + plunge + Mth.sin(age * 0.4F) * 1.5F;
        this.dragonHead.xRot = 0.35F + Mth.sin(age * 0.3F) * 0.12F;
        this.dragonBody.y = plunge;
        this.dragonBody.yRot = age * 0.28F;
        this.shockRingInner.yRot = age * 0.45F;
        this.shockRingOuter.yRot = -age * 0.32F;
    }
}
