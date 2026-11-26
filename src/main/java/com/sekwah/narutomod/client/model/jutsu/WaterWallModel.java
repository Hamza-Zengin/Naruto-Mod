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
 * 15. SUITON: SUIJINHEKI — 3D Surging Encircled Water Encampment Barrier.
 */
public class WaterWallModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart waveBase;
    private final ModelPart crestSurge;
    private final ModelPart foamRings;

    public WaterWallModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.waveBase = part.getChild("wave_base");
        this.crestSurge = part.getChild("crest_surge");
        this.foamRings = part.getChild("foam_rings");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Curved 3-panel hydro wall curving forward to shield the caster
        PartDefinition base = root.addOrReplaceChild("wave_base",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-20.0F, -44.0F, -4.5F, 40.0F, 44.0F, 9.0F)
                        .texOffs(0, 42).addBox(-22.0F, -7.0F, -6.5F, 44.0F, 7.0F, 13.0F),
                PartPose.ZERO);
        base.addOrReplaceChild("wing_left",
                CubeListBuilder.create().texOffs(0, 0).addBox(-14.0F, -42.0F, -4.0F, 14.0F, 42.0F, 8.0F),
                PartPose.offsetAndRotation(-19.0F, 0.0F, -0.5F, 0.0F, 0.48F, 0.0F));
        base.addOrReplaceChild("wing_right",
                CubeListBuilder.create().texOffs(0, 0).addBox(0.0F, -42.0F, -4.0F, 14.0F, 42.0F, 8.0F),
                PartPose.offsetAndRotation(19.0F, 0.0F, -0.5F, 0.0F, -0.48F, 0.0F));

        // 7 Tumbling White-Foam Breaking Wave Crests along the top
        PartDefinition crests = root.addOrReplaceChild("crest_surge", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 7; i++) {
            float cx = -18.0F + i * 6.0F;
            float cz = (i == 0 || i == 6) ? 3.5F : ((i == 1 || i == 5) ? 1.5F : 0.0F);
            PartDefinition crest = crests.addOrReplaceChild("crest_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 32).addBox(-3.4F, -6.5F, -4.0F, 6.8F, 7.0F, 8.0F),
                    PartPose.offsetAndRotation(cx, -43.5F, cz, 0.35F, 0.0F, 0.0F));
            crest.addOrReplaceChild("crest_curl_" + i,
                    CubeListBuilder.create().texOffs(24, 32).addBox(-2.8F, -4.0F, -5.5F, 5.6F, 4.0F, 6.0F),
                    PartPose.offsetAndRotation(0.0F, -5.5F, 1.5F, 0.55F, 0.0F, 0.0F));
        }

        // 12-Segment Turbulent Base Foam Collar
        PartDefinition foam = root.addOrReplaceChild("foam_rings", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 12; i++) {
            float fx = -22.0F + i * 4.0F;
            float tilt = (i % 2 == 0) ? 0.16F : -0.16F;
            foam.addOrReplaceChild("foam_seg_" + i,
                    CubeListBuilder.create().texOffs(24, 32).addBox(-2.4F, -3.5F, -3.5F, 4.8F, 4.0F, 7.0F),
                    PartPose.offsetAndRotation(fx, -1.0F, 5.5F, tilt, 0.0F, 0.0F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float emerge = age < 6.0F ? (1.0F - (age / 6.0F)) * 46.0F : 0.0F;
        float subsiding = progress > 0.90F ? ((progress - 0.90F) / 0.10F) * 46.0F : 0.0F;
        float surge = Mth.sin(age * 0.45F) * 1.4F;
        this.waveBase.y = emerge + subsiding;
        this.crestSurge.y = emerge + subsiding + surge;
        this.foamRings.y = emerge + subsiding + Mth.cos(age * 0.52F) * 0.8F;
    }
}
