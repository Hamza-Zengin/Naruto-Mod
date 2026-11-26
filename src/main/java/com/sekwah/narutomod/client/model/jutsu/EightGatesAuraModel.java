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
 * 11. HACHIMON TONKŌ: EIGHT GATES — 3D Horizontal Ground Chakra Vapor Rings.
 */
public class EightGatesAuraModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart innerRing;
    private final ModelPart outerRing;

    public EightGatesAuraModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.innerRing = part.getChild("inner_ring");
        this.outerRing = part.getChild("outer_ring");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // Circular inner ring composed of 16 smooth radial segments
        PartDefinition inner = root.addOrReplaceChild("inner_ring", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularXZRing(inner, "inner_seg_", 16, 10.0F,
                0.0F, 0.0F, 0.0F, 0, 0,
                -2.25F, -0.25F, -1.0F, 5, 1, 2);

        // Circular outer ring composed of 16 smooth radial segments
        PartDefinition outer = root.addOrReplaceChild("outer_ring", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularXZRing(outer, "outer_seg_", 16, 16.0F,
                0.0F, 0.0F, 0.0F, 0, 26,
                -3.5F, -0.25F, -1.0F, 7, 1, 2);

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        // Strictly rotate horizontally around the vertical Y axis (yRot)
        this.innerRing.yRot = age * 0.12F;
        this.innerRing.xRot = 0.0F;
        this.innerRing.zRot = 0.0F;

        this.outerRing.yRot = -age * 0.08F;
        this.outerRing.xRot = 0.0F;
        this.outerRing.zRot = 0.0F;
    }
}
