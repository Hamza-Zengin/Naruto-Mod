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

        root.addOrReplaceChild("core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-2.5F, -2.5F, -2.5F, 5, 5, 5),
                PartPose.ZERO);

        for (int i = 0; i < 14; i++) {
            float baseRx = (i * 0.85F);
            float baseRy = (i * 1.12F);
            float baseRz = (i * 0.63F);
            PartDefinition arc = root.addOrReplaceChild("arc_" + i,
                    CubeListBuilder.create(),
                    PartPose.rotation(baseRx, baseRy, baseRz));

            PartDefinition seg0 = arc.addOrReplaceChild("seg_0",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.4F, -0.4F, 0.0F, 0.8F, 0.8F, 3.5F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 1.5F, 0.25F, 0.30F, 0.0F));
            PartDefinition seg1 = seg0.addOrReplaceChild("seg_1",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.35F, -0.35F, 0.0F, 0.7F, 0.7F, 3.5F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.3F, -0.45F, -0.55F, 0.15F));
            PartDefinition seg2 = seg1.addOrReplaceChild("seg_2",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.3F, -0.3F, 0.0F, 0.6F, 0.6F, 3.2F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.3F, 0.50F, 0.45F, -0.20F));
            seg2.addOrReplaceChild("seg_3",
                    CubeListBuilder.create().texOffs(0, 16).addBox(-0.2F, -0.2F, 0.0F, 0.4F, 0.4F, 2.8F),
                    PartPose.offsetAndRotation(0.0F, 0.0F, 3.0F, -0.35F, -0.40F, 0.10F));
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
