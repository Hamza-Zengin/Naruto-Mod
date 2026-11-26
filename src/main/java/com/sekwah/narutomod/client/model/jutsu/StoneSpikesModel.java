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
 * 12. DOTON: DORYŪSŌ — 3D Jagged Erupting Stone Spikes Cluster.
 */
public class StoneSpikesModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart spikesCluster;

    public StoneSpikesModel(ModelPart part) {
        super(part, RenderTypes::entityCutout);
        this.spikesCluster = part.getChild("spikes_cluster");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition cluster = root.addOrReplaceChild("spikes_cluster",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-14.0F, -2.5F, -14.0F, 28, 3, 28),
                PartPose.ZERO);

        // Central towering tapered stone spear
        PartDefinition center = cluster.addOrReplaceChild("center_spike",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.2F, -24.0F, -4.2F, 8.4F, 24.0F, 8.4F)
                        .texOffs(34, 0).addBox(-2.8F, -42.0F, -2.8F, 5.6F, 19.0F, 5.6F)
                        .texOffs(34, 26).addBox(-1.4F, -56.0F, -1.4F, 2.8F, 15.0F, 2.8F),
                PartPose.ZERO);
        center.addOrReplaceChild("center_oct",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.0F, -23.0F, -4.0F, 8.0F, 23.0F, 8.0F)
                        .texOffs(34, 0).addBox(-2.6F, -41.0F, -2.6F, 5.2F, 18.0F, 5.2F),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));

        // 6 outward-angled jagged perimeter rock spears
        for (int i = 0; i < 6; i++) {
            float angle = (float) (i * Math.PI * 2.0 / 6.0);
            float radius = (i % 2 == 0) ? 15.5F : 12.0F;
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            PartDefinition spear = cluster.addOrReplaceChild("outer_spike_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 32).addBox(-3.0F, -16.0F, -3.0F, 6.0F, 17.0F, 6.0F)
                            .texOffs(24, 32).addBox(-1.9F, -30.0F, -1.9F, 3.8F, 15.0F, 3.8F)
                            .texOffs(40, 32).addBox(-1.0F, -40.0F, -1.0F, 2.0F, 11.0F, 2.0F),
                    PartPose.offsetAndRotation(px, 1.0F, pz, 0.30F, -angle + (float) (Math.PI / 2.0), 0.0F));
            spear.addOrReplaceChild("outer_oct_" + i,
                    CubeListBuilder.create().texOffs(0, 32).addBox(-2.8F, -15.5F, -2.8F, 5.6F, 16.0F, 5.6F),
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
        float emerge = age < 6.0F ? (1.0F - (age / 6.0F)) * 54.0F : 0.0F;
        float retract = progress > 0.85F ? ((progress - 0.85F) / 0.15F) * 54.0F : 0.0F;
        this.spikesCluster.y = emerge + retract;
    }

    public void setupAnim(float progress) {
        float emerge = progress < 0.12F ? (1.0F - (progress / 0.12F)) * 54.0F : 0.0F;
        float retract = progress > 0.82F ? ((progress - 0.82F) / 0.18F) * 54.0F : 0.0F;
        this.spikesCluster.y = emerge + retract;
    }
}
