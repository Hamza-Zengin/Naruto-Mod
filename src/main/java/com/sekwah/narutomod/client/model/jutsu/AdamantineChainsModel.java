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
 * 13. UZUMAKI KONGŌ FŪSA — 3D Golden Fūinjutsu Ring &amp; Articulated Sealing Chains.
 */
public class AdamantineChainsModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart sealRing;
    private final ModelPart innerSealRing;
    private final ModelPart chainsGroup;

    public AdamantineChainsModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.sealRing = part.getChild("seal_ring");
        this.innerSealRing = part.getChild("inner_seal_ring");
        this.chainsGroup = part.getChild("chains_group");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Outer 16-segment golden Fūinjutsu sealing circle
        PartDefinition outer = root.addOrReplaceChild("seal_ring", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularXZRing(outer, "seal_outer_seg_", 16, 18.0F,
                -0.4F, 0.0F, 0.0F, 0, 0,
                -3.5F, -0.45F, -1.0F, 7.0F, 0.9F, 2.0F);

        // Inner counter-rotating 8-segment seal glyph ring
        PartDefinition inner = root.addOrReplaceChild("inner_seal_ring",
                CubeListBuilder.create().texOffs(0, 38).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 1.0F, 5.0F),
                PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularXZRing(inner, "seal_inner_seg_", 8, 10.5F,
                -0.5F, 0.0F, 0.0F, 20, 0,
                -2.0F, -0.4F, -0.8F, 4.0F, 0.8F, 1.6F);

        // 6 Articulated 3-joint Adamantine Chains with tapered pyramid sealing stakes (kuiki)
        PartDefinition chains = root.addOrReplaceChild("chains_group", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 6.0);
            float radius = 21.0F;
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            float yaw = -angle + (float) (Math.PI / 2.0);
            float pitchSweep = (i % 2 == 0) ? 0.52F : 0.44F;

            PartDefinition base = chains.addOrReplaceChild("chain_base_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 8).addBox(-1.2F, -9.0F, -0.6F, 2.4F, 9.0F, 1.2F)
                            .texOffs(10, 8).addBox(-0.6F, -9.0F, -1.2F, 1.2F, 9.0F, 2.4F),
                    PartPose.offsetAndRotation(px, 0.0F, pz, pitchSweep, yaw, 0.0F));

            PartDefinition mid = base.addOrReplaceChild("chain_mid_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 8).addBox(-1.1F, -9.0F, -0.6F, 2.2F, 9.0F, 1.2F)
                            .texOffs(10, 8).addBox(-0.6F, -9.0F, -1.1F, 1.2F, 9.0F, 2.2F)
                            .texOffs(20, 8).addBox(-1.6F, -1.5F, -1.6F, 3.2F, 2.2F, 3.2F),
                    PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, 0.42F, 0.14F, 0.0F));

            PartDefinition tip = mid.addOrReplaceChild("chain_tip_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 8).addBox(-1.0F, -8.0F, -0.55F, 2.0F, 8.0F, 1.1F)
                            .texOffs(10, 8).addBox(-0.55F, -8.0F, -1.0F, 1.1F, 8.0F, 2.0F)
                            .texOffs(0, 24).addBox(-1.5F, -14.5F, -1.5F, 3.0F, 7.0F, 3.0F)
                            .texOffs(16, 24).addBox(-1.1F, -19.0F, -1.1F, 2.2F, 5.0F, 2.2F)
                            .texOffs(28, 24).addBox(-0.6F, -22.5F, -0.6F, 1.2F, 4.0F, 1.2F),
                    PartPose.offsetAndRotation(0.0F, -8.4F, 0.0F, 0.36F, -0.10F, 0.0F));
            tip.addOrReplaceChild("chain_stake_collar_" + i,
                    CubeListBuilder.create().texOffs(20, 8).addBox(-1.8F, -8.5F, -1.8F, 3.6F, 2.0F, 3.6F),
                    PartPose.rotation(0.0F, 0.7854F, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float emerge = age < 6.0F ? (1.0F - (age / 6.0F)) * 34.0F : 0.0F;
        float retract = progress > 0.88F ? ((progress - 0.88F) / 0.12F) * 34.0F : 0.0F;
        float pulse = Mth.sin(age * 0.42F) * 0.8F;
        this.sealRing.yRot = age * 0.14F;
        this.innerSealRing.yRot = -age * 0.22F;
        this.chainsGroup.y = emerge + retract + pulse;
        this.chainsGroup.yRot = Mth.sin(age * 0.12F) * 0.08F;
    }
}
