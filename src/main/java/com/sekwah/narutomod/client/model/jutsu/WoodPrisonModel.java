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
 * 2. MOKUTON: JUKAI KŌTAN — 3D Intertwined Deep Forest Root Cage &amp; Pillars.
 */
public class WoodPrisonModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart pillars;
    private final ModelPart vineRings;

    public WoodPrisonModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.pillars = part.getChild("pillars");
        this.vineRings = part.getChild("vine_rings");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition pillars = root.addOrReplaceChild("pillars", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 8; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 8.0);
            float radius = 24.0F;
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;

            // Lower gnarled octagonal trunk + splayed root buttresses
            PartDefinition pillar = pillars.addOrReplaceChild("pillar_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.2F, -18.0F, -2.2F, 4.4F, 18.0F, 4.4F),
                    PartPose.offsetAndRotation(px, 0.0F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
            pillar.addOrReplaceChild("pillar_oct_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-2.1F, -17.6F, -2.1F, 4.2F, 17.6F, 4.2F),
                    PartPose.rotation(0.0F, 0.7854F, 0.0F));
            pillar.addOrReplaceChild("root_flare_l_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-1.2F, -7.0F, -1.2F, 2.4F, 8.0F, 2.4F),
                    PartPose.offsetAndRotation(1.6F, 0.0F, 0.8F, 0.35F, 0.4F, -0.35F));
            pillar.addOrReplaceChild("root_flare_r_" + i,
                    CubeListBuilder.create().texOffs(0, 0).addBox(-1.2F, -7.0F, -1.2F, 2.4F, 8.0F, 2.4F),
                    PartPose.offsetAndRotation(-1.6F, 0.0F, 0.8F, 0.35F, -0.4F, 0.35F));

            // Middle octagonal trunk curving smoothly inward toward center
            PartDefinition mid = pillar.addOrReplaceChild("mid_" + i,
                    CubeListBuilder.create()
                            .texOffs(20, 0).addBox(-1.75F, -16.0F, -1.75F, 3.5F, 16.0F, 3.5F),
                    PartPose.offsetAndRotation(0.0F, -17.0F, 0.0F, 0.35F, 0.0F, 0.0F));
            mid.addOrReplaceChild("mid_oct_" + i,
                    CubeListBuilder.create().texOffs(20, 0).addBox(-1.65F, -15.6F, -1.65F, 3.3F, 15.6F, 3.3F),
                    PartPose.rotation(0.0F, 0.7854F, 0.0F));

            // Upper canopy arch interlocking overhead + multi-angled chamfered emerald foliage
            PartDefinition top = mid.addOrReplaceChild("top_" + i,
                    CubeListBuilder.create()
                            .texOffs(36, 0).addBox(-1.3F, -14.0F, -1.3F, 2.6F, 14.0F, 2.6F)
                            .texOffs(0, 38).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 5.5F, 8.0F),
                    PartPose.offsetAndRotation(0.0F, -15.0F, 0.0F, 0.55F, 0.0F, 0.0F));
            top.addOrReplaceChild("canopy_rot_" + i,
                    CubeListBuilder.create().texOffs(0, 38).addBox(-3.8F, -11.5F, -3.8F, 7.6F, 5.0F, 7.6F),
                    PartPose.rotation(0.25F, 0.7854F, 0.18F));
        }

        // Two 12-Segment Circular Constricting Root-Vine Rings
        PartDefinition vineRings = root.addOrReplaceChild("vine_rings", CubeListBuilder.create(), PartPose.ZERO);
        addCircularVineRing(vineRings, "low", 18.5F, -11.0F, 0.08F);
        addCircularVineRing(vineRings, "high", 15.0F, -23.0F, -0.08F);

        return LayerDefinition.create(mesh, 128, 64);
    }

    private static void addCircularVineRing(PartDefinition parent, String prefix, float radius, float y, float tilt) {
        int segments = 12;
        float segWidth = (radius * 2.0F * 3.14159F / segments) + 0.8F;
        JutsuModelGeometryHelper.addCircularXZRing(parent, prefix + "_vine_", segments, radius,
                y, tilt, 0.05F, 0, 23,
                -segWidth * 0.5F, -1.3F, -1.1F, segWidth, 2.6F, 2.2F);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float emerge = age < 8.0F ? (1.0F - (age / 8.0F)) * 32.0F : 0.0F;
        float sink = progress > 0.88F ? ((progress - 0.88F) / 0.12F) * 32.0F : 0.0F;
        this.pillars.y = emerge + sink;
        this.vineRings.y = emerge + sink;
        this.vineRings.yRot = Mth.sin(age * 0.15F) * 0.12F;
    }
}
