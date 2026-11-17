package com.sekwah.narutomod.client.model.jutsu;

import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * 3D Model for Katon: Gōkakyū no Jutsu (Great Fireball)
 * Features an incandescent white-gold inner core, a roiling crimson-orange outer sphere,
 * and counter-rotating 3D solar-flare corona rings.
 */
public class FireballJutsuModel extends Model<EntityRenderState> {

    private final ModelPart innerCore;
    private final ModelPart outerSphere;
    private final ModelPart flameCorona;

    public FireballJutsuModel(ModelPart modelPart) {
        super(modelPart, RenderTypes::entityTranslucent);
        this.innerCore = modelPart.getChild("inner_core");
        this.outerSphere = modelPart.getChild("outer_sphere");
        this.flameCorona = modelPart.getChild("flame_corona");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        // 1. Multi-angle Chamfered Incandescent Inner Core
        PartDefinition innerCore = root.addOrReplaceChild("inner_core",
                CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-4.5F, -4.5F, -4.5F, 9, 9, 9),
                PartPose.ZERO);
        innerCore.addOrReplaceChild("inner_rot_y",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.5F, -3.5F, -4.5F, 9, 7, 9),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));
        innerCore.addOrReplaceChild("inner_rot_x",
                CubeListBuilder.create().texOffs(80, 0).addBox(-3.5F, -4.5F, -4.5F, 7, 9, 9),
                PartPose.rotation(0.7854F, 0.0F, 0.0F));
        innerCore.addOrReplaceChild("inner_rot_z",
                CubeListBuilder.create().texOffs(40, 0).addBox(-4.5F, -4.5F, -3.5F, 9, 9, 7),
                PartPose.rotation(0.0F, 0.0F, 0.7854F));

        // 2. Multi-angled Geodesic-Style Roiling Outer Fireball Sphere
        PartDefinition outerSphere = root.addOrReplaceChild("outer_sphere",
                CubeListBuilder.create()
                        .texOffs(0, 20).addBox(-7.5F, -7.5F, -7.5F, 15, 15, 15),
                PartPose.ZERO);
        outerSphere.addOrReplaceChild("outer_rot_y",
                CubeListBuilder.create().texOffs(64, 20).addBox(-7.5F, -6.0F, -7.5F, 15, 12, 15),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));
        outerSphere.addOrReplaceChild("outer_rot_x",
                CubeListBuilder.create().texOffs(0, 52).addBox(-6.0F, -7.5F, -7.5F, 12, 15, 15),
                PartPose.rotation(0.7854F, 0.0F, 0.0F));
        outerSphere.addOrReplaceChild("outer_rot_z",
                CubeListBuilder.create().texOffs(52, 52).addBox(-7.5F, -7.5F, -6.0F, 15, 15, 12),
                PartPose.rotation(0.0F, 0.0F, 0.7854F));
        outerSphere.addOrReplaceChild("outer_diag_a",
                CubeListBuilder.create().texOffs(64, 20).addBox(-7.0F, -7.0F, -7.0F, 14, 14, 14),
                PartPose.rotation(0.5236F, 0.7854F, 0.5236F));
        outerSphere.addOrReplaceChild("outer_diag_b",
                CubeListBuilder.create().texOffs(0, 52).addBox(-7.0F, -7.0F, -7.0F, 14, 14, 14),
                PartPose.rotation(-0.5236F, -0.7854F, 0.5236F));

        // 3. Three Intersecting 12-Segment Circular Solar-Flare Corona Rings
        PartDefinition corona = root.addOrReplaceChild("flame_corona", CubeListBuilder.create(), PartPose.ZERO);
        addCircularCoronaRing(corona, "eq", 10.5F, 0.0F, 0.0F);
        addCircularCoronaRing(corona, "orb_a", 10.2F, 0.65F, 0.35F);
        addCircularCoronaRing(corona, "orb_b", 10.8F, -0.65F, -0.35F);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addCircularCoronaRing(PartDefinition parent, String prefix, float radius, float tiltX, float tiltZ) {
        PartDefinition ring = parent.addOrReplaceChild("ring_" + prefix, CubeListBuilder.create(),
                PartPose.rotation(tiltX, 0.0F, tiltZ));
        int segments = 12;
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segments);
            float px = (float) Math.cos(angle) * radius;
            float pz = (float) Math.sin(angle) * radius;
            ring.addOrReplaceChild(prefix + "_seg_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 84).addBox(-2.8F, -0.9F, -1.2F, 5.6F, 1.8F, 2.4F),
                    PartPose.offsetAndRotation(px, 0.0F, pz, 0.22F, -angle + (float) (Math.PI / 2.0), 0.14F));
        }
    }

    @Override
    public void setupAnim(EntityRenderState state) {
        super.setupAnim(state);
        setAnimationAngles(state.ageInTicks);
    }

    public void setAnimationAngles(float ageInTicks) {
        this.innerCore.yRot = ageInTicks * 0.35F;
        this.innerCore.xRot = ageInTicks * 0.25F;
        this.outerSphere.yRot = -ageInTicks * 0.22F;
        this.outerSphere.zRot = ageInTicks * 0.18F;
        this.flameCorona.yRot = ageInTicks * 0.48F;
        this.flameCorona.xRot = -ageInTicks * 0.32F;
    }
}

