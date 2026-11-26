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
 * 7. FŪTON: DAITOPPA — 3D Sonic Wind Funnel &amp; Expanding Blade Rings.
 */
public class WindShockwaveModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart ringFront;
    private final ModelPart ringMid;
    private final ModelPart ringBack;

    public WindShockwaveModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.ringFront = part.getChild("ring_front");
        this.ringMid = part.getChild("ring_mid");
        this.ringBack = part.getChild("ring_back");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 16-Segment Circular Aerodynamic Wind-Blade Torus Rings
        PartDefinition front = root.addOrReplaceChild("ring_front", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularZRing(front, "wf", 13.5F, -12.0F, 2.4F, 2.8F, 16, 0.6F, 0.16F, 0, 0);

        PartDefinition mid = root.addOrReplaceChild("ring_mid", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularZRing(mid, "wm", 9.6F, -2.0F, 2.2F, 3.2F, 16, 0.6F, -0.16F, 0, 31);

        PartDefinition back = root.addOrReplaceChild("ring_back", CubeListBuilder.create(), PartPose.ZERO);
        JutsuModelGeometryHelper.addCircularZRing(back, "wb", 5.8F, 8.0F, 2.0F, 3.2F, 12, 0.6F, 0.14F, 0, 31);

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age());
    }

    public void setupAnim(float age) {
        this.ringFront.zRot = age * 0.75F;
        this.ringMid.zRot = -age * 0.65F;
        this.ringBack.zRot = age * 0.85F;
    }
}
