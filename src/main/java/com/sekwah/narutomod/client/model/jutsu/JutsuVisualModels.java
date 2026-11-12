package com.sekwah.narutomod.client.model.jutsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.util.Mth;

/**
 * Collection of High-Detail 3D Jutsu Models bringing every jutsu in the mod
 * to the Rasenshuriken 3D standard and beyond.
 */
public class JutsuVisualModels {

    // =========================================================================
    // 1. RAITON: KIRIN — Colossal 3D Descending Lightning Dragon Model
    // =========================================================================
    public static class KirinDragonModel extends Model {
        private final ModelPart root;
        private final ModelPart dragonHead;
        private final ModelPart dragonBody;
        private final ModelPart shockRingInner;
        private final ModelPart shockRingOuter;

        public KirinDragonModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.dragonHead = part.getChild("dragon_head");
            this.dragonBody = part.getChild("dragon_body");
            this.shockRingInner = part.getChild("shock_ring_inner");
            this.shockRingOuter = part.getChild("shock_ring_outer");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Roaring 3D Lightning Dragon Head
            PartDefinition head = root.addOrReplaceChild("dragon_head",
                    CubeListBuilder.create()
                            // Main Dragon Cranium
                            .texOffs(0, 0).addBox(-6.0F, -6.0F, -10.0F, 12, 9, 12)
                            // Upper Snout & Fangs
                            .texOffs(0, 21).addBox(-4.5F, -3.0F, -20.0F, 9, 5, 10)
                            .texOffs(38, 21).addBox(-4.0F, 2.0F, -19.0F, 2, 3, 2)
                            .texOffs(38, 21).addBox(2.0F, 2.0F, -19.0F, 2, 3, 2),
                    PartPose.offset(0.0F, -18.0F, 0.0F));

            // Roaring Lower Jaw
            head.addOrReplaceChild("lower_jaw",
                    CubeListBuilder.create()
                            .texOffs(0, 36).addBox(-4.0F, 0.0F, -10.0F, 8, 3, 10),
                    PartPose.offsetAndRotation(0.0F, 2.0F, -9.0F, 0.48F, 0.0F, 0.0F));

            // Forked Swept-Back Lightning Horns
            head.addOrReplaceChild("horn_left",
                    CubeListBuilder.create()
                            .texOffs(36, 0).addBox(-1.5F, -2.0F, 0.0F, 3, 3, 16)
                            .texOffs(36, 0).addBox(-1.0F, 0.5F, 8.0F, 2, 2, 10),
                    PartPose.offsetAndRotation(4.5F, -5.0F, -2.0F, 0.45F, 0.32F, 0.0F));
            head.addOrReplaceChild("horn_right",
                    CubeListBuilder.create()
                            .texOffs(36, 0).addBox(-1.5F, -2.0F, 0.0F, 3, 3, 16)
                            .texOffs(36, 0).addBox(-1.0F, 0.5F, 8.0F, 2, 2, 10),
                    PartPose.offsetAndRotation(-4.5F, -5.0F, -2.0F, 0.45F, -0.32F, 0.0F));

            // Serpentine Coiled Lightning Body Segments ascending to the heavens
            PartDefinition body = root.addOrReplaceChild("dragon_body", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 6; i++) {
                float segY = -28.0F - (i * 15.0F);
                float offsetX = Mth.sin(i * 1.1F) * 5.0F;
                float offsetZ = Mth.cos(i * 1.1F) * 5.0F;
                body.addOrReplaceChild("seg_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 49).addBox(-4.5F, -8.0F, -4.5F, 9, 16, 9)
                                // Dorsal Lightning Fin
                                .texOffs(38, 36).addBox(-1.0F, -7.0F, 4.5F, 2, 14, 5),
                        PartPose.offsetAndRotation(offsetX, segY, offsetZ, 0.18F, i * 0.55F, 0.12F));
            }

            // Ground Impact Lightning Shockwave Rings
            root.addOrReplaceChild("shock_ring_inner",
                    CubeListBuilder.create()
                            .texOffs(0, 74).addBox(-14.0F, -3.0F, -14.0F, 28, 3, 28),
                    PartPose.ZERO);
            root.addOrReplaceChild("shock_ring_outer",
                    CubeListBuilder.create()
                            .texOffs(0, 74).addBox(-22.0F, -2.0F, -22.0F, 44, 2, 44),
                    PartPose.ZERO);

            return LayerDefinition.create(mesh, 128, 128);
        }

        public void setupAnim(float age, float progress) {
            // Plunge from the sky during first 25% of life, then coil & roar
            float plunge = progress < 0.25F ? (1.0F - (progress / 0.25F)) * -48.0F : 0.0F;
            this.dragonHead.y = -14.0F + plunge + Mth.sin(age * 0.4F) * 1.5F;
            this.dragonHead.xRot = 0.35F + Mth.sin(age * 0.3F) * 0.12F;
            this.dragonBody.y = plunge;
            this.dragonBody.yRot = age * 0.28F;
            this.shockRingInner.yRot = age * 0.45F;
            this.shockRingOuter.yRot = -age * 0.32F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 2. MOKUTON: JUKAI KŌTAN — 3D Intertwined Deep Forest Root Cage & Pillars
    // =========================================================================
    public static class WoodPrisonModel extends Model {
        private final ModelPart root;
        private final ModelPart pillars;
        private final ModelPart vineRings;

        public WoodPrisonModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
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

                PartDefinition pillar = pillars.addOrReplaceChild("pillar_" + i,
                        CubeListBuilder.create()
                                // Lower gnarled trunk
                                .texOffs(0, 0).addBox(-2.5F, -18.0F, -2.5F, 5, 18, 5),
                        PartPose.offsetAndRotation(px, 0.0F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));

                // Middle trunk curving inward toward target center
                PartDefinition mid = pillar.addOrReplaceChild("mid_" + i,
                        CubeListBuilder.create()
                                .texOffs(20, 0).addBox(-2.0F, -16.0F, -2.0F, 4, 16, 4),
                        PartPose.offsetAndRotation(0.0F, -17.0F, 0.0F, 0.35F, 0.0F, 0.0F));

                // Upper canopy arch interlocking overhead + emerald leaf cluster
                mid.addOrReplaceChild("top_" + i,
                        CubeListBuilder.create()
                                .texOffs(36, 0).addBox(-1.5F, -14.0F, -1.5F, 3, 14, 3)
                                .texOffs(0, 38).addBox(-4.5F, -12.0F, -4.5F, 9, 6, 9),
                        PartPose.offsetAndRotation(0.0F, -15.0F, 0.0F, 0.55F, 0.0F, 0.0F));
            }

            // Horizontal constricting root-vine bands
            root.addOrReplaceChild("vine_rings",
                    CubeListBuilder.create()
                            .texOffs(0, 23).addBox(-18.0F, -12.0F, -18.0F, 36, 3, 36)
                            .texOffs(0, 23).addBox(-15.0F, -24.0F, -15.0F, 30, 3, 30),
                    PartPose.ZERO);

            return LayerDefinition.create(mesh, 128, 64);
        }

        public void setupAnim(float age, float progress) {
            float emerge = progress < 0.15F ? (1.0F - (progress / 0.15F)) * 32.0F : 0.0F;
            float sink = progress > 0.85F ? ((progress - 0.85F) / 0.15F) * 32.0F : 0.0F;
            this.pillars.y = emerge + sink;
            this.vineRings.y = emerge + sink;
            this.vineRings.yRot = Mth.sin(age * 0.15F) * 0.12F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 3. SHIKOTSUMYAKU: SAWARABI NO MAI — 3D Calcified Bone Spire Forest
    // =========================================================================
    public static class BoneForestModel extends Model {
        private final ModelPart root;
        private final ModelPart ringInner;
        private final ModelPart ringMid;
        private final ModelPart ringOuter;

        public BoneForestModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.ringInner = part.getChild("ring_inner");
            this.ringMid = part.getChild("ring_mid");
            this.ringOuter = part.getChild("ring_outer");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            PartDefinition inner = root.addOrReplaceChild("ring_inner", CubeListBuilder.create(), PartPose.ZERO);
            addBoneRing(inner, "in", 5, 20.0F, 0.28F);

            PartDefinition mid = root.addOrReplaceChild("ring_mid", CubeListBuilder.create(), PartPose.ZERO);
            addBoneRing(mid, "mid", 7, 36.0F, 0.42F);

            PartDefinition outer = root.addOrReplaceChild("ring_outer", CubeListBuilder.create(), PartPose.ZERO);
            addBoneRing(outer, "out", 9, 52.0F, 0.54F);

            return LayerDefinition.create(mesh, 64, 64);
        }

        private static void addBoneRing(PartDefinition parent, String prefix, int count, float radius, float tilt) {
            for (int i = 0; i < count; i++) {
                float angle = (float) (i * Math.PI * 2.0 / count) + (radius * 0.05F);
                float sx = Mth.cos(angle) * radius;
                float sz = Mth.sin(angle) * radius;

                parent.addOrReplaceChild(prefix + "_spike_" + i,
                        CubeListBuilder.create()
                                // Thick calcified base shaft
                                .texOffs(0, 0).addBox(-2.5F, -14.0F, -2.5F, 5, 16, 5)
                                // Mid bone spire
                                .texOffs(20, 0).addBox(-1.8F, -28.0F, -1.8F, 4, 15, 4)
                                // Razor-sharp osteoblast tip
                                .texOffs(36, 0).addBox(-1.0F, -40.0F, -1.0F, 2, 13, 2)
                                // Protruding lateral bone barbs
                                .texOffs(0, 22).addBox(-4.5F, -20.0F, -1.0F, 9, 3, 2),
                        PartPose.offsetAndRotation(sx, 2.0F, sz, -tilt, -angle - (float) (Math.PI / 2.0), 0.0F));
            }
        }

        public void setupAnim(float age) {
            float inEmerge = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
            float midEmerge = Mth.clamp((age - 7.0F) / 6.0F, 0.0F, 1.0F);
            float outEmerge = Mth.clamp((age - 14.0F) / 6.0F, 0.0F, 1.0F);

            this.ringInner.y = (1.0F - inEmerge) * 38.0F;
            this.ringMid.y = (1.0F - midEmerge) * 42.0F;
            this.ringOuter.y = (1.0F - outEmerge) * 45.0F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 4. RASENGAN — 3D Multi-Shell Swirling Chakra Sphere & Spiral Ribbons
    // =========================================================================
    public static class RasenganSphereModel extends Model {
        private final ModelPart root;
        private float age;

        public RasenganSphereModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();
            root.addOrReplaceChild("core", CubeListBuilder.create(), PartPose.ZERO);
            root.addOrReplaceChild("inner_shell", CubeListBuilder.create(), PartPose.ZERO);
            root.addOrReplaceChild("outer_shell", CubeListBuilder.create(), PartPose.ZERO);
            return LayerDefinition.create(mesh, 64, 64);
        }

        public void setupAnim(float age) {
            this.age = age;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            // 1. Inner Core Sphere (r = 0.36F in model scale -> ~0.11F in hand scale)
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.age * 25.0F));
            renderSmoothSphere(poseStack, buffer, 0.36F, 16, 24, light, overlay,
                    0.80F * r, 0.96F * g, 1.0F * b, 0.95F * a);
            poseStack.popPose();

            // 2. Middle Chakra Layer (r = 0.48F in model scale -> ~0.145F in hand scale)
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(35.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(-this.age * 18.0F));
            renderSmoothSphere(poseStack, buffer, 0.48F, 16, 24, light, overlay,
                    0.20F * r, 0.68F * g, 1.0F * b, 0.55F * a);
            poseStack.popPose();

            // 3. Outer Energy Halo (r = 0.58F in model scale -> ~0.175F in hand scale)
            poseStack.pushPose();
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(-35.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(this.age * 12.0F));
            renderSmoothSphere(poseStack, buffer, 0.58F, 16, 24, light, overlay,
                    0.45F * r, 0.85F * g, 1.0F * b, 0.25F * a);
            poseStack.popPose();
        }

        public static void renderSmoothSphere(PoseStack poseStack, VertexConsumer buffer, float radius,
                                              int stacks, int slices, int light, int overlay,
                                              float r, float g, float b, float a) {
            PoseStack.Pose pose = poseStack.last();
            org.joml.Matrix4f mat = pose.pose();
            org.joml.Matrix3f normalMat = pose.normal();

            for (int i = 0; i < stacks; i++) {
                float phi1 = (float) (Math.PI * i / stacks);
                float phi2 = (float) (Math.PI * (i + 1) / stacks);
                float v1 = (float) i / stacks;
                float v2 = (float) (i + 1) / stacks;

                float sinPhi1 = Mth.sin(phi1);
                float cosPhi1 = Mth.cos(phi1);
                float sinPhi2 = Mth.sin(phi2);
                float cosPhi2 = Mth.cos(phi2);

                for (int j = 0; j < slices; j++) {
                    float theta1 = (float) (2.0 * Math.PI * j / slices);
                    float theta2 = (float) (2.0 * Math.PI * (j + 1) / slices);
                    float u1 = (float) j / slices;
                    float u2 = (float) (j + 1) / slices;

                    float cosT1 = Mth.cos(theta1);
                    float sinT1 = Mth.sin(theta1);
                    float cosT2 = Mth.cos(theta2);
                    float sinT2 = Mth.sin(theta2);

                    // 4 vertices of the quad
                    float nx1 = sinPhi1 * cosT1, ny1 = cosPhi1, nz1 = sinPhi1 * sinT1;
                    float nx2 = sinPhi2 * cosT1, ny2 = cosPhi2, nz2 = sinPhi2 * sinT1;
                    float nx3 = sinPhi2 * cosT2, ny3 = cosPhi2, nz3 = sinPhi2 * sinT2;
                    float nx4 = sinPhi1 * cosT2, ny4 = cosPhi1, nz4 = sinPhi1 * sinT2;

                    buffer.vertex(mat, nx1 * radius, ny1 * radius, nz1 * radius)
                            .color(r, g, b, a).uv(u1, v1).overlayCoords(overlay).uv2(light)
                            .normal(normalMat, nx1, ny1, nz1).endVertex();
                    buffer.vertex(mat, nx2 * radius, ny2 * radius, nz2 * radius)
                            .color(r, g, b, a).uv(u1, v2).overlayCoords(overlay).uv2(light)
                            .normal(normalMat, nx2, ny2, nz2).endVertex();
                    buffer.vertex(mat, nx3 * radius, ny3 * radius, nz3 * radius)
                            .color(r, g, b, a).uv(u2, v2).overlayCoords(overlay).uv2(light)
                            .normal(normalMat, nx3, ny3, nz3).endVertex();
                    buffer.vertex(mat, nx4 * radius, ny4 * radius, nz4 * radius)
                            .color(r, g, b, a).uv(u2, v1).overlayCoords(overlay).uv2(light)
                            .normal(normalMat, nx4, ny4, nz4).endVertex();
                }
            }
        }
    }

    // =========================================================================
    // 5. CHIDORI / RAIKIRI — 3D Crackling Lightning Blade & Electric Arcs
    // =========================================================================
    public static class ChidoriBurstModel extends Model {
        private final ModelPart root;
        private final ModelPart core;
        private final ModelPart arcsA;
        private final ModelPart arcsB;

        public ChidoriBurstModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.core = part.getChild("core");
            this.arcsA = part.getChild("arcs_a");
            this.arcsB = part.getChild("arcs_b");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            root.addOrReplaceChild("core",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6),
                    PartPose.ZERO);

            PartDefinition arcsA = root.addOrReplaceChild("arcs_a", CubeListBuilder.create(), PartPose.ZERO);
            PartDefinition arcsB = root.addOrReplaceChild("arcs_b", CubeListBuilder.create(), PartPose.ZERO);

            for (int i = 0; i < 4; i++) {
                float rx = (i * 0.95F);
                float ry = (i * 1.57F);
                float rz = (i * 0.78F);
                arcsA.addOrReplaceChild("bolt_a_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 16).addBox(-0.5F, -0.5F, -10.0F, 1, 1, 20)
                                .texOffs(0, 38).addBox(-6.0F, -0.5F, -0.5F, 12, 1, 1),
                        PartPose.rotation(rx, ry, rz));
                arcsB.addOrReplaceChild("bolt_b_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 16).addBox(-0.5F, -0.5F, -8.0F, 1, 1, 16),
                        PartPose.rotation(-ry, rx, -rz));
            }

            return LayerDefinition.create(mesh, 64, 64);
        }

        public void setupAnim(float age) {
            this.core.yRot = age * 0.9F;
            this.core.xRot = age * 0.7F;
            this.arcsA.yRot = age * 1.15F;
            this.arcsA.xRot = -age * 0.85F;
            this.arcsB.yRot = -age * 1.25F;
            this.arcsB.zRot = age * 0.95F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 6. SUITON: SUIKŌDAN — 3D Surging Water Shark / Hydro Vortex Torpedo
    // =========================================================================
    public static class WaterVortexModel extends Model {
        private final ModelPart root;
        private final ModelPart hydroCore;
        private final ModelPart vortexFins;
        private final ModelPart wakeRings;

        public WaterVortexModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.hydroCore = part.getChild("hydro_core");
            this.vortexFins = part.getChild("vortex_fins");
            this.wakeRings = part.getChild("wake_rings");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            root.addOrReplaceChild("hydro_core",
                    CubeListBuilder.create()
                            // Snout & Torpedo Head
                            .texOffs(0, 0).addBox(-3.5F, -3.5F, -20.0F, 7, 7, 10)
                            // Main Water Body
                            .texOffs(0, 17).addBox(-5.5F, -5.5F, -10.0F, 11, 11, 18)
                            // Dorsal Water Crest Fin
                            .texOffs(34, 0).addBox(-1.0F, -11.0F, -6.0F, 2, 6, 10),
                    PartPose.ZERO);

            root.addOrReplaceChild("vortex_fins",
                    CubeListBuilder.create()
                            .texOffs(0, 46).addBox(-12.0F, -1.5F, -8.0F, 24, 3, 14)
                            .texOffs(0, 46).addBox(-1.5F, -12.0F, -8.0F, 3, 24, 14),
                    PartPose.ZERO);

            root.addOrReplaceChild("wake_rings",
                    CubeListBuilder.create()
                            .texOffs(0, 46).addBox(-9.0F, -9.0F, 4.0F, 18, 18, 3)
                            .texOffs(0, 46).addBox(-12.0F, -12.0F, 11.0F, 24, 24, 3),
                    PartPose.ZERO);

            return LayerDefinition.create(mesh, 64, 64);
        }

        public void setupAnim(float age) {
            this.vortexFins.zRot = age * 0.65F;
            this.wakeRings.zRot = -age * 0.45F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 7. FŪTON: DAITOPPA — 3D Sonic Wind Funnel & Expanding Blade Rings
    // =========================================================================
    public static class WindShockwaveModel extends Model {
        private final ModelPart root;
        private final ModelPart ringFront;
        private final ModelPart ringMid;
        private final ModelPart ringBack;

        public WindShockwaveModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.ringFront = part.getChild("ring_front");
            this.ringMid = part.getChild("ring_mid");
            this.ringBack = part.getChild("ring_back");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            root.addOrReplaceChild("ring_front",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-14.0F, -14.0F, -12.0F, 28, 28, 3),
                    PartPose.ZERO);

            root.addOrReplaceChild("ring_mid",
                    CubeListBuilder.create()
                            .texOffs(0, 31).addBox(-10.0F, -10.0F, -2.0F, 20, 20, 4),
                    PartPose.ZERO);

            root.addOrReplaceChild("ring_back",
                    CubeListBuilder.create()
                            .texOffs(0, 31).addBox(-6.0F, -6.0F, 8.0F, 12, 12, 4),
                    PartPose.ZERO);

            return LayerDefinition.create(mesh, 64, 64);
        }

        public void setupAnim(float age) {
            this.ringFront.zRot = age * 0.75F;
            this.ringMid.zRot = -age * 0.65F;
            this.ringBack.zRot = age * 0.85F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 8. AMATERASU — 3D Inextinguishable Black-Crimson Flame Shroud & Pillars
    // =========================================================================
    public static class AmaterasuFlameModel extends Model {
        private final ModelPart root;
        private final ModelPart innerPillars;
        private final ModelPart outerPillars;

        public AmaterasuFlameModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.innerPillars = part.getChild("inner_pillars");
            this.outerPillars = part.getChild("outer_pillars");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            PartDefinition inner = root.addOrReplaceChild("inner_pillars",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-3.0F, -22.0F, -3.0F, 6, 22, 6)
                            .texOffs(24, 0).addBox(-2.0F, -26.0F, -2.0F, 4, 26, 4),
                    PartPose.ZERO);

            PartDefinition outer = root.addOrReplaceChild("outer_pillars", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 5; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 5.0);
                float rx = Mth.cos(angle) * 8.0F;
                float rz = Mth.sin(angle) * 8.0F;
                outer.addOrReplaceChild("flame_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 36).addBox(-2.5F, -20.0F, -1.0F, 5, 20, 2),
                        PartPose.offsetAndRotation(rx, 0.0F, rz, 0.15F, -angle + (float) (Math.PI / 2.0), 0.10F));
            }

            return LayerDefinition.create(mesh, 64, 64);
        }

        public void setupAnim(float age) {
            this.innerPillars.yRot = age * 0.25F;
            this.outerPillars.yRot = -age * 0.18F;
            this.outerPillars.y = Mth.sin(age * 0.35F) * 1.5F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 9. KAGE MANE NO JUTSU — 3D Shadow Ribbon & Coiling Shadow Claws
    // =========================================================================
    public static class ShadowBindModel extends Model {
        private final ModelPart root;
        private final ModelPart claws;

        public ShadowBindModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.claws = part.getChild("claws");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Ground Shadow Pool & Ribbon
            root.addOrReplaceChild("shadow_pool",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-12.0F, -0.8F, -12.0F, 24, 1, 24)
                            .texOffs(0, 25).addBox(-2.5F, -0.6F, 0.0F, 5, 1, 36),
                    PartPose.ZERO);

            PartDefinition claws = root.addOrReplaceChild("claws", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 6.0);
                float cx = Mth.cos(angle) * 9.0F;
                float cz = Mth.sin(angle) * 9.0F;
                claws.addOrReplaceChild("claw_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 28).addBox(-1.5F, -22.0F, -1.5F, 3, 22, 3),
                        PartPose.offsetAndRotation(cx, 0.0F, cz, 0.25F, -angle + (float) (Math.PI / 2.0), 0.0F));
            }

            return LayerDefinition.create(mesh, 128, 64);
        }

        public void setupAnim(float age, float progress) {
            float emerge = progress < 0.12F ? (1.0F - (progress / 0.12F)) * 22.0F : 0.0F;
            this.claws.y = emerge;
            this.claws.yRot = Mth.sin(age * 0.18F) * 0.22F;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 10. DOTON: DORYŪHEKI — 3D Sculpted Shinobi Earth Bulwark
    // =========================================================================
    public static class EarthWallModel extends Model {
        private final ModelPart root;
        private final ModelPart wallBody;

        public EarthWallModel(ModelPart part) {
            super(RenderType::entityCutoutNoCull);
            this.root = part;
            this.wallBody = part.getChild("wall_body");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Solid 3-block wide, 3-block tall, 12-pixel thick stone bulwark
            root.addOrReplaceChild("wall_body",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-24.0F, -48.0F, -6.0F, 48, 48, 12),
                    PartPose.ZERO);

            return LayerDefinition.create(mesh, 128, 64);
        }

        public void setupAnim(float progress) {
            float emerge = progress < 0.08F ? (1.0F - (progress / 0.08F)) * 48.0F : 0.0F;
            this.wallBody.y = emerge;
        }

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }

    // =========================================================================
    // 11. HACHIMON TONKŌ: EIGHT GATES — 3D Horizontal Ground Chakra Vapor Rings
    // =========================================================================
    public static class EightGatesAuraModel extends Model {
        private final ModelPart root;
        private final ModelPart innerRing;
        private final ModelPart outerRing;

        public EightGatesAuraModel(ModelPart part) {
            super(RenderType::entityTranslucent);
            this.root = part;
            this.innerRing = part.getChild("inner_ring");
            this.outerRing = part.getChild("outer_ring");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Circular inner ring composed of 16 smooth radial segments
            PartDefinition inner = root.addOrReplaceChild("inner_ring", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 16; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 16.0);
                float radius = 10.0F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                inner.addOrReplaceChild("inner_seg_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-2.25F, -0.25F, -1.0F, 5, 1, 2),
                        PartPose.offsetAndRotation(px, 0.0F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
            }

            // Circular outer ring composed of 16 smooth radial segments
            PartDefinition outer = root.addOrReplaceChild("outer_ring", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 16; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 16.0);
                float radius = 16.0F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                outer.addOrReplaceChild("outer_seg_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 26).addBox(-3.5F, -0.25F, -1.0F, 7, 1, 2),
                        PartPose.offsetAndRotation(px, 0.0F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
            }

            return LayerDefinition.create(mesh, 128, 64);
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

        @Override
        public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                   float r, float g, float b, float a) {
            this.root.render(poseStack, buffer, light, overlay, r, g, b, a);
        }
    }
}

