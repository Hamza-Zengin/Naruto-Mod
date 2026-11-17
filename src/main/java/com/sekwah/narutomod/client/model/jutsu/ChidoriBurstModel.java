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
 * 5. CHIDORI / RAIKIRI — 3D Crackling Lightning Blade &amp; Electric Arcs.
 */
public class ChidoriBurstModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart core;
    private final ModelPart[] arcs = new ModelPart[14];

    public ChidoriBurstModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.core = part.getChild("core");
        for (int i = 0; i < 14; i++) {
            this.arcs[i] = part.getChild("arc_" + i);
        }
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Multi-angle Chamfered Plasma Orb Core (eliminates sharp cube corners)
        PartDefinition core = root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.0F, -2.0F, -2.0F, 4, 4, 4),
                PartPose.ZERO);
        core.addOrReplaceChild("core_rot_y",
                CubeListBuilder.create().texOffs(0, 0).addBox(-2.0F, -1.6F, -2.0F, 4, 3.2F, 4),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));
        core.addOrReplaceChild("core_rot_x",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.6F, -2.0F, -2.0F, 3.2F, 4, 4),
                PartPose.rotation(0.7854F, 0.0F, 0.0F));
        core.addOrReplaceChild("core_rot_diag",
                CubeListBuilder.create().texOffs(0, 0).addBox(-1.9F, -1.9F, -1.9F, 3.8F, 3.8F, 3.8F),
                PartPose.rotation(0.5236F, 0.7854F, 0.5236F));

        for (int i = 0; i < 14; i++) {
            float baseRx = (i * 0.85F);
            float baseRy = (i * 1.12F);
            float baseRz = (i * 0.63F);
            PartDefinition arc = root.addOrReplaceChild("arc_" + i,
                    CubeListBuilder.create(),
                    PartPose.rotation(baseRx, baseRy, baseRz));

            PartDefinition seg0 = arc.addOrReplaceChild("seg_0",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.32F, -0.32F, 0.0F, 0.64F, 0.64F, 2.8F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 1.3F, 0.22F, 0.28F, 0.0F));
            PartDefinition seg1 = seg0.addOrReplaceChild("seg_1",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.28F, -0.28F, 0.0F, 0.56F, 0.56F, 2.8F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 2.6F, -0.38F, -0.45F, 0.14F));
            seg1.addOrReplaceChild("fork_0",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.18F, -0.18F, 0.0F, 0.36F, 0.36F, 2.0F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 1.4F, 0.55F, 0.50F, 0.0F));
            PartDefinition seg2 = seg1.addOrReplaceChild("seg_2",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.24F, -0.24F, 0.0F, 0.48F, 0.48F, 2.6F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 2.6F, 0.42F, 0.38F, -0.18F));
            PartDefinition seg3 = seg2.addOrReplaceChild("seg_3",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.18F, -0.18F, 0.0F, 0.36F, 0.36F, 2.4F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 2.4F, -0.32F, -0.34F, 0.12F));
            seg3.addOrReplaceChild("seg_4",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.12F, -0.12F, 0.0F, 0.24F, 0.24F, 1.8F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 2.2F, 0.26F, 0.30F, -0.10F));
        }

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        this.core.yRot = age * 1.4F;
        this.core.xRot = age * 1.1F;
        float pulse = 0.92F + 0.16F * (float) Math.sin(age * 3.5F);
        this.core.xScale = pulse;
        this.core.yScale = pulse;
        this.core.zScale = pulse;

        int step = (int) (age * 1.5F);
        for (int i = 0; i < this.arcs.length; i++) {
            float seed = (step * 17.3F + i * 31.7F);
            this.arcs[i].xRot = (i * 0.85F) + (float) Math.sin(seed) * 0.55F;
            this.arcs[i].yRot = (i * 1.12F) + (float) Math.cos(seed * 1.3F) * 0.65F;
            this.arcs[i].zRot = (i * 0.63F) + (float) Math.sin(seed * 0.7F) * 0.45F;
        }
    }
}
