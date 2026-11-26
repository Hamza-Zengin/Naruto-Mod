package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 6. SUITON: SUIKŌDAN — 3D Surging Water Shark / Hydro Vortex Torpedo.
 */
public class WaterVortexModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart hydroCore;
    private final ModelPart vortexFins;
    private final ModelPart wakeRings;

    public WaterVortexModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.hydroCore = part.getChild("hydro_core");
        this.vortexFins = part.getChild("vortex_fins");
        this.wakeRings = part.getChild("wake_rings");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Chamfered & Tapered Hydro Torpedo Core
        PartDefinition core = root.addOrReplaceChild("hydro_core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.2F, -2.2F, -22.0F, 4.4F, 4.4F, 5.0F)
                        .texOffs(0, 0).addBox(-3.3F, -3.3F, -17.0F, 6.6F, 6.6F, 8.0F)
                        .texOffs(0, 17).addBox(-4.8F, -4.8F, -9.0F, 9.6F, 9.6F, 16.0F)
                        .texOffs(34, 0).addBox(-0.8F, -10.5F, -6.0F, 1.6F, 6.0F, 9.0F),
                PartPose.ZERO);
        core.addOrReplaceChild("hydro_core_rot",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.2F, -3.2F, -16.5F, 6.4F, 6.4F, 7.5F)
                        .texOffs(0, 17).addBox(-4.6F, -4.6F, -8.5F, 9.2F, 9.2F, 15.0F),
                PartPose.rotation(0.0F, 0.0F, 0.7854F));

        // 6 Pitched Helical Hydro-Vortex Turbine Fins
        PartDefinition fins = root.addOrReplaceChild("vortex_fins", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 6; i++) {
            float finAngle = (float) (i * Math.PI * 2.0 / 6.0);
            fins.addOrReplaceChild("fin_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 46).addBox(2.8F, -0.7F, -7.0F, 8.5F, 1.4F, 11.0F),
                    PartPose.rotation(0.26F, 0.0F, finAngle));
        }

        // Two 12-Segment Circular Hydro Wake Rings around travel axis (Z)
        PartDefinition wakeRings = root.addOrReplaceChild("wake_rings", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularZRing(wakeRings, "wake_a", 8.5F, 4.0F, 1.8F, 2.6F, 12, 0.7F, 0.14F, 0, 46);
        JutsuModelGeometryHelper.addCircularZRing(wakeRings, "wake_b", 11.8F, 11.0F, 1.8F, 2.8F, 12, 0.7F, -0.14F, 0, 46);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        this.vortexFins.zRot = age * 0.65F;
        this.wakeRings.zRot = -age * 0.45F;
    }
}
