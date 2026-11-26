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
 * 3. SHIKOTSUMYAKU: SAWARABI NO MAI — 3D Calcified Bone Spire Forest.
 */
public class BoneForestModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart ringInner;
    private final ModelPart ringMid;
    private final ModelPart ringOuter;

    public BoneForestModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.ringInner = part.getChild("ring_inner");
        this.ringMid = part.getChild("ring_mid");
        this.ringOuter = part.getChild("ring_outer");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition inner = root.addOrReplaceChild("ring_inner", CubeListBuilder.create(), PartPose.ZERO);
        addBoneRing(inner, "in", 5, 20.0F, 0.28F);

        PartDefinition mid = root.addOrReplaceChild("ring_mid", CubeListBuilder.create(), PartPose.ZERO);
        addBoneRing(mid, "mid", 7, 36.0F, 0.42F);

        PartDefinition outer = root.addOrReplaceChild("ring_outer", CubeListBuilder.create(), PartPose.ZERO);
        addBoneRing(outer, "out", 9, 52.0F, 0.54F);

        return LayerDefinition.create(mesh, 64, 64);
    }

    private static void addBoneRing(PartDefinition parent, String prefix, int count, float radius, float tilt) {
        for (int i = 0; i < count; i++) {
            float angle = (float) (i * Math.PI * 2.0 / count) + (radius * 0.05F);
            float sx = Mth.cos(angle) * radius;
            float sz = Mth.sin(angle) * radius;

            // Smooth octagonal-tapered bone spire
            PartDefinition spike = parent.addOrReplaceChild(prefix + "_spike_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.2F, -14.0F, -2.2F, 4.4F, 16.0F, 4.4F)
                            .texOffs(20, 0).addBox(-1.6F, -27.5F, -1.6F, 3.2F, 14.5F, 3.2F)
                            .texOffs(36, 0).addBox(-0.9F, -40.0F, -0.9F, 1.8F, 13.0F, 1.8F),
                    PartPose.offsetAndRotation(sx, 2.0F, sz, -tilt, -angle - (float) (Math.PI / 2.0), 0.0F));
            spike.addOrReplaceChild(prefix + "_oct_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.1F, -13.6F, -2.1F, 4.2F, 15.5F, 4.2F)
                            .texOffs(20, 0).addBox(-1.5F, -27.0F, -1.5F, 3.0F, 14.0F, 3.0F)
                            .texOffs(36, 0).addBox(-0.85F, -39.5F, -0.85F, 1.7F, 12.5F, 1.7F),
                    PartPose.rotation(0.0F, 0.7854F, 0.0F));
            spike.addOrReplaceChild(prefix + "_barb_l_" + i,
                    CubeListBuilder.create().texOffs(0, 22).addBox(-3.6F, -1.0F, -0.8F, 4.2F, 2.0F, 1.6F),
                    PartPose.offsetAndRotation(-1.0F, -19.0F, 0.0F, 0.0F, 0.0F, 0.42F));
            spike.addOrReplaceChild(prefix + "_barb_r_" + i,
                    CubeListBuilder.create().texOffs(0, 22).addBox(-0.6F, -1.0F, -0.8F, 4.2F, 2.0F, 1.6F),
                    PartPose.offsetAndRotation(1.0F, -19.0F, 0.0F, 0.0F, 0.0F, -0.42F));
        }
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        float inEmerge = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
        float midEmerge = Mth.clamp((age - 7.0F) / 6.0F, 0.0F, 1.0F);
        float outEmerge = Mth.clamp((age - 14.0F) / 6.0F, 0.0F, 1.0F);

        this.ringInner.y = (1.0F - inEmerge) * 38.0F;
        this.ringMid.y = (1.0F - midEmerge) * 42.0F;
        this.ringOuter.y = (1.0F - outEmerge) * 45.0F;
    }
}
