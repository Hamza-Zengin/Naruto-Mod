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
 * 1. RAITON: KIRIN — Colossal 3D Descending Lightning Dragon Model.
 */
public class KirinDragonModel extends Model<JutsuVisualModels.VisualState> {
    private final ModelPart dragonHead;
    private final ModelPart dragonBody;
    private final ModelPart shockRingInner;
    private final ModelPart shockRingOuter;

    public KirinDragonModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
        this.dragonHead = part.getChild("dragon_head");
        this.dragonBody = part.getChild("dragon_body");
        this.shockRingInner = part.getChild("shock_ring_inner");
        this.shockRingOuter = part.getChild("shock_ring_outer");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        PartDefinition head = root.addOrReplaceChild("dragon_head",
                CubeListBuilder.create()
                        // Tapered chamfered cranium & snout bridge
                        .texOffs(0, 0).addBox(-5.2F, -5.2F, -10.0F, 10.4F, 7.8F, 11.5F)
                        .texOffs(0, 21).addBox(-3.8F, -2.8F, -18.5F, 7.6F, 4.4F, 9.0F)
                        .texOffs(0, 21).addBox(-2.8F, -2.0F, -21.5F, 5.6F, 3.5F, 3.5F)
                        .texOffs(38, 21).addBox(-3.2F, 1.4F, -19.0F, 1.6F, 2.6F, 1.6F)
                        .texOffs(38, 21).addBox(1.6F, 1.4F, -19.0F, 1.6F, 2.6F, 1.6F),
                PartPose.offset(0.0F, -18.0F, 0.0F));

        // Angled brow & cheek plates for organic sculpted dragon face
        head.addOrReplaceChild("cranium_bevel",
                CubeListBuilder.create().texOffs(0, 0).addBox(-4.4F, -5.6F, -8.5F, 8.8F, 2.0F, 9.5F),
                PartPose.rotation(-0.18F, 0.0F, 0.0F));
        head.addOrReplaceChild("brow_left",
                CubeListBuilder.create().texOffs(0, 21).addBox(-1.2F, -1.2F, -6.0F, 2.4F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(4.6F, -4.2F, -4.5F, -0.15F, 0.25F, -0.35F));
        head.addOrReplaceChild("brow_right",
                CubeListBuilder.create().texOffs(0, 21).addBox(-1.2F, -1.2F, -6.0F, 2.4F, 2.0F, 7.0F),
                PartPose.offsetAndRotation(-4.6F, -4.2F, -4.5F, -0.15F, -0.25F, 0.35F));

        // Tapered multi-part Lower Jaw
        PartDefinition jaw = head.addOrReplaceChild("lower_jaw",
                CubeListBuilder.create()
                        .texOffs(0, 36).addBox(-3.6F, 0.0F, -7.5F, 7.2F, 2.6F, 8.0F),
                PartPose.offsetAndRotation(0.0F, 2.0F, -9.0F, 0.48F, 0.0F, 0.0F));
        jaw.addOrReplaceChild("jaw_tip",
                CubeListBuilder.create().texOffs(0, 36).addBox(-2.4F, 0.0F, -4.5F, 4.8F, 2.0F, 5.0F),
                PartPose.offsetAndRotation(0.0F, 0.2F, -7.0F, -0.10F, 0.0F, 0.0F));

        // Articulated 3-segment Swept-Back Curved Lightning Horns
        addCurvedHorn(head, "horn_left", 4.4F, 1.0F);
        addCurvedHorn(head, "horn_right", -4.4F, -1.0F);

        // 10 Tapered, Octagonally Beveled Serpentine Body Segments
        PartDefinition body = root.addOrReplaceChild("dragon_body", CubeListBuilder.create(), PartPose.ZERO);
        for (int i = 0; i < 10; i++) {
            float segY = -24.0F - (i * 9.0F);
            float spiralRadius = 4.6F + Mth.sin(i * 0.45F) * 1.4F;
            float offsetX = Mth.sin(i * 0.72F) * spiralRadius;
            float offsetZ = Mth.cos(i * 0.72F) * spiralRadius;
            float width = Math.max(4.4F, 8.8F - i * 0.42F);
            float halfW = width * 0.5F;
            PartDefinition seg = body.addOrReplaceChild("seg_" + i,
                    CubeListBuilder.create()
                            .texOffs(0, 49).addBox(-halfW, -5.5F, -halfW, width, 11.0F, width)
                            .texOffs(38, 36).addBox(-0.8F, -5.0F, halfW, 1.6F, 10.0F, 4.2F),
                    PartPose.offsetAndRotation(offsetX, segY, offsetZ, 0.14F, i * 0.38F, 0.10F));
            seg.addOrReplaceChild("seg_bevel_" + i,
                    CubeListBuilder.create().texOffs(0, 49).addBox(-halfW * 0.92F, -5.3F, -halfW * 0.92F, width * 0.92F, 10.6F, width * 0.92F),
                    PartPose.rotation(0.0F, 0.7854F, 0.0F));
        }

        // 16-Segment Circular Impact Lightning Shockwave Rings
        PartDefinition innerRing = root.addOrReplaceChild("shock_ring_inner", CubeListBuilder.create(), PartPose.ZERO);
        addCircularShockRing(innerRing, "in", 13.5F, 5.6F, 2.2F, 2.8F, 0.18F);

        PartDefinition outerRing = root.addOrReplaceChild("shock_ring_outer", CubeListBuilder.create(), PartPose.ZERO);
        addCircularShockRing(outerRing, "out", 21.0F, 8.6F, 1.6F, 3.0F, -0.14F);

        return LayerDefinition.create(mesh, 128, 128);
    }

    private static void addCurvedHorn(PartDefinition head, String name, float offsetX, float dirSign) {
        PartDefinition h0 = head.addOrReplaceChild(name,
                CubeListBuilder.create().texOffs(36, 0).addBox(-1.3F, -1.4F, 0.0F, 2.6F, 2.6F, 7.0F),
                PartPose.offsetAndRotation(offsetX, -4.8F, -2.0F, 0.36F, dirSign * 0.28F, 0.0F));
        PartDefinition h1 = h0.addOrReplaceChild(name + "_mid",
                CubeListBuilder.create().texOffs(36, 0).addBox(-1.0F, -1.1F, 0.0F, 2.0F, 2.2F, 6.5F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 6.6F, 0.18F, dirSign * -0.12F, 0.0F));
        h1.addOrReplaceChild(name + "_tip",
                CubeListBuilder.create().texOffs(36, 0).addBox(-0.65F, -0.75F, 0.0F, 1.3F, 1.5F, 6.0F),
                PartPose.offsetAndRotation(0.0F, 0.0F, 6.2F, 0.22F, dirSign * -0.15F, 0.0F));
    }

    private static void addCircularShockRing(PartDefinition parent, String prefix, float radius,
                                             float segLen, float segHeight, float segThick, float tiltX) {
        int segments = 16;
        for (int i = 0; i < segments; i++) {
            float angle = (float) (i * Math.PI * 2.0 / segments);
            float px = Mth.cos(angle) * radius;
            float pz = Mth.sin(angle) * radius;
            parent.addOrReplaceChild(prefix + "_ring_" + i,
                    CubeListBuilder.create().texOffs(0, 74).addBox(-segLen * 0.5F, -segHeight, -segThick * 0.5F, segLen, segHeight, segThick),
                    PartPose.offsetAndRotation(px, 0.0F, pz, tiltX, -angle + (float) (Math.PI / 2.0), 0.0F));
        }
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        setupAnim(state.age(), state.progress());
    }

    public void setupAnim(float age, float progress) {
        float plunge = progress < 0.25F ? (1.0F - (progress / 0.25F)) * -48.0F : 0.0F;
        this.dragonHead.y = -14.0F + plunge + Mth.sin(age * 0.4F) * 1.5F;
        this.dragonHead.xRot = 0.35F + Mth.sin(age * 0.3F) * 0.12F;
        this.dragonBody.y = plunge;
        this.dragonBody.yRot = age * 0.28F;
        this.shockRingInner.yRot = age * 0.45F;
        this.shockRingOuter.yRot = -age * 0.32F;
    }
}
