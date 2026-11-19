package com.sekwah.narutomod.client.model.jutsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Collection of High-Detail 3D Jutsu Models bringing every jutsu in the mod
 * to the Rasenshuriken 3D standard and beyond.
 */
public class JutsuVisualModels {

    public record VisualState(float age, float progress) {}

    // =========================================================================
    // 1. RAITON: KIRIN — Colossal 3D Descending Lightning Dragon Model
    // =========================================================================
    public static class KirinDragonModel extends Model<VisualState> {
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

            // Roaring 3D Sculpted Lightning Dragon Head
            PartDefinition head = root.addOrReplaceChild("dragon_head",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-5.2F, -5.2F, -10.0F, 10.4F, 7.8F, 11.5F)
                            .texOffs(0, 21).addBox(-3.8F, -2.8F, -18.5F, 7.6F, 4.4F, 9.0F)
                            .texOffs(0, 21).addBox(-2.8F, -2.0F, -21.5F, 5.6F, 3.5F, 3.5F)
                            .texOffs(38, 21).addBox(-3.2F, 1.4F, -19.0F, 1.6F, 2.6F, 1.6F)
                            .texOffs(38, 21).addBox(1.6F, 1.4F, -19.0F, 1.6F, 2.6F, 1.6F),
                    PartPose.offset(0.0F, -18.0F, 0.0F));

            head.addOrReplaceChild("cranium_bevel",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-4.4F, -5.6F, -8.5F, 8.8F, 2.0F, 9.5F),
                    PartPose.rotation(-0.18F, 0.0F, 0.0F));
            head.addOrReplaceChild("brow_left",
                    CubeListBuilder.create().texOffs(0, 21).addBox(-1.2F, -1.2F, -6.0F, 2.4F, 2.0F, 7.0F),
                    PartPose.offsetAndRotation(4.6F, -4.2F, -4.5F, -0.15F, 0.25F, -0.35F));
            head.addOrReplaceChild("brow_right",
                    CubeListBuilder.create().texOffs(0, 21).addBox(-1.2F, -1.2F, -6.0F, 2.4F, 2.0F, 7.0F),
                    PartPose.offsetAndRotation(-4.6F, -4.2F, -4.5F, -0.15F, -0.25F, 0.35F));

            // Tapered Roaring Lower Jaw
            PartDefinition jaw = head.addOrReplaceChild("lower_jaw",
                    CubeListBuilder.create()
                            .texOffs(0, 36).addBox(-3.6F, 0.0F, -7.5F, 7.2F, 2.6F, 8.0F),
                    PartPose.offsetAndRotation(0.0F, 2.0F, -9.0F, 0.48F, 0.0F, 0.0F));
            jaw.addOrReplaceChild("jaw_tip",
                    CubeListBuilder.create().texOffs(0, 36).addBox(-2.4F, 0.0F, -4.5F, 4.8F, 2.0F, 5.0F),
                    PartPose.offsetAndRotation(0.0F, 0.2F, -7.0F, -0.10F, 0.0F, 0.0F));

            // Articulated 3-Segment Swept-Back Curved Lightning Horns
            addKirinHorn(head, "horn_left", 4.4F, 1.0F);
            addKirinHorn(head, "horn_right", -4.4F, -1.0F);

            // 10 Tapered, Octagonally Beveled Serpentine Lightning Body Segments
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

            // 16-Segment Circular Ground Impact Shockwave Rings
            PartDefinition innerRing = root.addOrReplaceChild("shock_ring_inner", CubeListBuilder.create(), PartPose.ZERO);
            addCircularKirinRing(innerRing, "in", 13.5F, 5.6F, 2.2F, 2.8F, 0.18F);

            PartDefinition outerRing = root.addOrReplaceChild("shock_ring_outer", CubeListBuilder.create(), PartPose.ZERO);
            addCircularKirinRing(outerRing, "out", 21.0F, 8.6F, 1.6F, 3.0F, -0.14F);

            return LayerDefinition.create(mesh, 128, 128);
        }

        private static void addKirinHorn(PartDefinition head, String name, float offsetX, float dirSign) {
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

        private static void addCircularKirinRing(PartDefinition parent, String prefix, float radius,
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
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age(), state.progress());
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
    }

    // =========================================================================
    // 2. MOKUTON: JUKAI KŌTAN — 3D Intertwined Deep Forest Root Cage & Pillars
    // =========================================================================
    public static class WoodPrisonModel extends Model<VisualState> {
        private final ModelPart pillars;
        private final ModelPart vineRings;

        public WoodPrisonModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
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

                // Lower gnarled octagonal trunk + splayed root buttresses
                PartDefinition pillar = pillars.addOrReplaceChild("pillar_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-2.2F, -18.0F, -2.2F, 4.4F, 18.0F, 4.4F),
                        PartPose.offsetAndRotation(px, 0.0F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
                pillar.addOrReplaceChild("pillar_oct_" + i,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-2.1F, -17.6F, -2.1F, 4.2F, 17.6F, 4.2F),
                        PartPose.rotation(0.0F, 0.7854F, 0.0F));
                pillar.addOrReplaceChild("root_flare_l_" + i,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-1.2F, -7.0F, -1.2F, 2.4F, 8.0F, 2.4F),
                        PartPose.offsetAndRotation(1.6F, 0.0F, 0.8F, 0.35F, 0.4F, -0.35F));
                pillar.addOrReplaceChild("root_flare_r_" + i,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-1.2F, -7.0F, -1.2F, 2.4F, 8.0F, 2.4F),
                        PartPose.offsetAndRotation(-1.6F, 0.0F, 0.8F, 0.35F, -0.4F, 0.35F));

                // Middle octagonal trunk curving smoothly inward toward center
                PartDefinition mid = pillar.addOrReplaceChild("mid_" + i,
                        CubeListBuilder.create()
                                .texOffs(20, 0).addBox(-1.75F, -16.0F, -1.75F, 3.5F, 16.0F, 3.5F),
                        PartPose.offsetAndRotation(0.0F, -17.0F, 0.0F, 0.35F, 0.0F, 0.0F));
                mid.addOrReplaceChild("mid_oct_" + i,
                        CubeListBuilder.create().texOffs(20, 0).addBox(-1.65F, -15.6F, -1.65F, 3.3F, 15.6F, 3.3F),
                        PartPose.rotation(0.0F, 0.7854F, 0.0F));

                // Upper canopy arch interlocking overhead + multi-angled chamfered emerald foliage
                PartDefinition top = mid.addOrReplaceChild("top_" + i,
                        CubeListBuilder.create()
                                .texOffs(36, 0).addBox(-1.3F, -14.0F, -1.3F, 2.6F, 14.0F, 2.6F)
                                .texOffs(0, 38).addBox(-4.0F, -12.0F, -4.0F, 8.0F, 5.5F, 8.0F),
                        PartPose.offsetAndRotation(0.0F, -15.0F, 0.0F, 0.55F, 0.0F, 0.0F));
                top.addOrReplaceChild("canopy_rot_" + i,
                        CubeListBuilder.create().texOffs(0, 38).addBox(-3.8F, -11.5F, -3.8F, 7.6F, 5.0F, 7.6F),
                        PartPose.rotation(0.25F, 0.7854F, 0.18F));
            }

            // Two 12-Segment Circular Constricting Root-Vine Rings
            PartDefinition vineRings = root.addOrReplaceChild("vine_rings", CubeListBuilder.create(), PartPose.ZERO);
            addCircularVineRing(vineRings, "low", 18.5F, -11.0F, 0.08F);
            addCircularVineRing(vineRings, "high", 15.0F, -23.0F, -0.08F);

            return LayerDefinition.create(mesh, 128, 64);
        }

        private static void addCircularVineRing(PartDefinition parent, String prefix, float radius, float y, float tilt) {
            int segments = 12;
            float segWidth = (radius * 2.0F * 3.14159F / segments) + 0.8F;
            for (int i = 0; i < segments; i++) {
                float angle = (float) (i * Math.PI * 2.0 / segments);
                float vx = Mth.cos(angle) * radius;
                float vz = Mth.sin(angle) * radius;
                parent.addOrReplaceChild(prefix + "_vine_" + i,
                        CubeListBuilder.create().texOffs(0, 23).addBox(-segWidth * 0.5F, -1.3F, -1.1F, segWidth, 2.6F, 2.2F),
                        PartPose.offsetAndRotation(vx, y, vz, tilt, -angle + (float) (Math.PI / 2.0), 0.05F));
            }
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age(), state.progress());
        }

        public void setupAnim(float age, float progress) {
            float emerge = progress < 0.15F ? (1.0F - (progress / 0.15F)) * 32.0F : 0.0F;
            float sink = progress > 0.85F ? ((progress - 0.85F) / 0.15F) * 32.0F : 0.0F;
            this.pillars.y = emerge + sink;
            this.vineRings.y = emerge + sink;
            this.vineRings.yRot = Mth.sin(age * 0.15F) * 0.12F;
        }
    }

    // =========================================================================
    // 3. SHIKOTSUMYAKU: SAWARABI NO MAI — 3D Calcified Bone Spire Forest
    // =========================================================================
    public static class BoneForestModel extends Model<VisualState> {
        private final ModelPart ringInner;
        private final ModelPart ringMid;
        private final ModelPart ringOuter;

        public BoneForestModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
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

                // Smooth octagonal-tapered bone spire
                PartDefinition spike = parent.addOrReplaceChild(prefix + "_spike_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-2.2F, -14.0F, -2.2F, 4.4F, 16.0F, 4.4F)
                                .texOffs(20, 0).addBox(-1.6F, -27.5F, -1.6F, 3.2F, 14.5F, 3.2F)
                                .texOffs(36, 0).addBox(-0.9F, -40.0F, -0.9F, 1.8F, 13.0F, 1.8F),
                        PartPose.offsetAndRotation(sx, 2.0F, sz, -tilt, -angle - (float) (Math.PI / 2.0), 0.0F));
                spike.addOrReplaceChild(prefix + "_oct_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-2.1F, -13.6F, -2.1F, 4.2F, 15.5F, 4.2F)
                                .texOffs(20, 0).addBox(-1.5F, -27.0F, -1.5F, 3.0F, 14.0F, 3.0F)
                                .texOffs(36, 0).addBox(-0.85F, -39.5F, -0.85F, 1.7F, 12.5F, 1.7F),
                        PartPose.rotation(0.0F, 0.7854F, 0.0F));
                spike.addOrReplaceChild(prefix + "_barb_l_" + i,
                        CubeListBuilder.create().texOffs(0, 22).addBox(-3.6F, -1.0F, -0.8F, 4.2F, 2.0F, 1.6F),
                        PartPose.offsetAndRotation(-1.0F, -19.0F, 0.0F, 0.0F, 0.0F, 0.42F));
                spike.addOrReplaceChild(prefix + "_barb_r_" + i,
                        CubeListBuilder.create().texOffs(0, 22).addBox(-0.6F, -1.0F, -0.8F, 4.2F, 2.0F, 1.6F),
                        PartPose.offsetAndRotation(1.0F, -19.0F, 0.0F, 0.0F, 0.0F, -0.42F));
            }
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age());
        }

        public void setupAnim(float age) {
            float inEmerge = Mth.clamp(age / 6.0F, 0.0F, 1.0F);
            float midEmerge = Mth.clamp((age - 7.0F) / 6.0F, 0.0F, 1.0F);
            float outEmerge = Mth.clamp((age - 14.0F) / 6.0F, 0.0F, 1.0F);

            this.ringInner.y = (1.0F - inEmerge) * 38.0F;
            this.ringMid.y = (1.0F - midEmerge) * 42.0F;
            this.ringOuter.y = (1.0F - outEmerge) * 45.0F;
        }
    }

    // =========================================================================
    // 4. RASENGAN — 3D Multi-Shell Swirling Chakra Sphere & Spiral Ribbons
    // =========================================================================
    public static class RasenganSphereModel extends com.sekwah.narutomod.client.model.jutsu.RasenganSphereModel {
        public RasenganSphereModel(ModelPart part) { super(part); }
        public static LayerDefinition createLayer() {
            return com.sekwah.narutomod.client.model.jutsu.RasenganSphereModel.createLayer();
        }
    }

    // =========================================================================
    // 5. CHIDORI / RAIKIRI — 3D Crackling Lightning Blade & Electric Arcs
    // =========================================================================
    public static class ChidoriBurstModel extends com.sekwah.narutomod.client.model.jutsu.ChidoriBurstModel {
        public ChidoriBurstModel(ModelPart part) { super(part); }
        public static LayerDefinition createLayer() {
            return com.sekwah.narutomod.client.model.jutsu.ChidoriBurstModel.createLayer();
        }
    }

    // =========================================================================
    // 6. SUITON: SUIKŌDAN — 3D Surging Water Shark / Hydro Vortex Torpedo
    // =========================================================================
    public static class WaterVortexModel extends Model<VisualState> {
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
            addCircularZRing(wakeRings, "wake_a", 8.5F, 4.0F, 1.8F, 2.6F, 12, 0.14F, 0, 46);
            addCircularZRing(wakeRings, "wake_b", 11.8F, 11.0F, 1.8F, 2.8F, 12, -0.14F, 0, 46);

            return LayerDefinition.create(mesh, 64, 64);
        }

        private static void addCircularZRing(PartDefinition parent, String prefix, float radius, float zPos,
                                             float thick, float depth, int segments, float tiltPitch, int u, int v) {
            float segLen = (radius * 2.0F * 3.14159F / segments) + 0.7F;
            for (int i = 0; i < segments; i++) {
                float angle = (float) (i * Math.PI * 2.0 / segments);
                float px = Mth.cos(angle) * radius;
                float py = Mth.sin(angle) * radius;
                parent.addOrReplaceChild(prefix + "_seg_" + i,
                        CubeListBuilder.create().texOffs(u, v).addBox(-segLen * 0.5F, -thick * 0.5F, -depth * 0.5F, segLen, thick, depth),
                        PartPose.offsetAndRotation(px, py, zPos, tiltPitch, 0.0F, angle + (float) (Math.PI / 2.0)));
            }
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age());
        }

        public void setupAnim(float age) {
            this.vortexFins.zRot = age * 0.65F;
            this.wakeRings.zRot = -age * 0.45F;
        }
    }

    // =========================================================================
    // 7. FŪTON: DAITOPPA — 3D Sonic Wind Funnel & Expanding Blade Rings
    // =========================================================================
    public static class WindShockwaveModel extends Model<VisualState> {
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
            addWindRing(front, "wf", 13.5F, -12.0F, 2.4F, 2.8F, 16, 0.16F, 0, 0);

            PartDefinition mid = root.addOrReplaceChild("ring_mid", CubeListBuilder.create(), PartPose.ZERO);
            addWindRing(mid, "wm", 9.6F, -2.0F, 2.2F, 3.2F, 16, -0.16F, 0, 31);

            PartDefinition back = root.addOrReplaceChild("ring_back", CubeListBuilder.create(), PartPose.ZERO);
            addWindRing(back, "wb", 5.8F, 8.0F, 2.0F, 3.2F, 12, 0.14F, 0, 31);

            return LayerDefinition.create(mesh, 64, 64);
        }

        private static void addWindRing(PartDefinition parent, String prefix, float radius, float zPos,
                                        float radialThick, float depth, int segments, float tiltPitch, int u, int v) {
            float segLen = (radius * 2.0F * 3.14159F / segments) + 0.6F;
            for (int i = 0; i < segments; i++) {
                float angle = (float) (i * Math.PI * 2.0 / segments);
                float px = Mth.cos(angle) * radius;
                float py = Mth.sin(angle) * radius;
                parent.addOrReplaceChild(prefix + "_seg_" + i,
                        CubeListBuilder.create().texOffs(u, v).addBox(-segLen * 0.5F, -radialThick * 0.5F, -depth * 0.5F, segLen, radialThick, depth),
                        PartPose.offsetAndRotation(px, py, zPos, tiltPitch, 0.0F, angle + (float) (Math.PI / 2.0)));
            }
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age());
        }

        public void setupAnim(float age) {
            this.ringFront.zRot = age * 0.75F;
            this.ringMid.zRot = -age * 0.65F;
            this.ringBack.zRot = age * 0.85F;
        }
    }

    // =========================================================================
    // 8. AMATERASU — 3D Inextinguishable Black-Crimson Flame Shroud & Pillars
    // =========================================================================
    public static class AmaterasuFlameModel extends Model<VisualState> {
        private final ModelPart innerPillars;
        private final ModelPart outerPillars;

        public AmaterasuFlameModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
            this.innerPillars = part.getChild("inner_pillars");
            this.outerPillars = part.getChild("outer_pillars");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Multi-angle Tapered Inner Black Flame Core
            PartDefinition inner = root.addOrReplaceChild("inner_pillars",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.8F, -14.0F, -2.8F, 5.6F, 14.0F, 5.6F)
                            .texOffs(24, 0).addBox(-1.8F, -25.0F, -1.8F, 3.6F, 12.0F, 3.6F),
                    PartPose.ZERO);
            inner.addOrReplaceChild("inner_rot_a",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.6F, -15.0F, -2.6F, 5.2F, 15.0F, 5.2F)
                            .texOffs(24, 0).addBox(-1.5F, -27.0F, -1.5F, 3.0F, 13.0F, 3.0F),
                    PartPose.rotation(0.06F, 0.7854F, -0.06F));
            inner.addOrReplaceChild("inner_rot_b",
                    CubeListBuilder.create().texOffs(24, 0).addBox(-1.2F, -29.0F, -1.2F, 2.4F, 16.0F, 2.4F),
                    PartPose.rotation(-0.08F, 0.5236F, 0.08F));

            // 8 Spiraling 3-Segment Tapered Outer Flame Tongues
            PartDefinition outer = root.addOrReplaceChild("outer_pillars", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 8; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 8.0);
                float radius = (i % 2 == 0) ? 7.8F : 6.4F;
                float rx = Mth.cos(angle) * radius;
                float rz = Mth.sin(angle) * radius;
                PartDefinition f0 = outer.addOrReplaceChild("flame_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 36).addBox(-2.0F, -8.5F, -0.9F, 4.0F, 8.5F, 1.8F),
                        PartPose.offsetAndRotation(rx, 0.0F, rz, 0.16F, -angle + (float) (Math.PI / 2.0), 0.14F));
                PartDefinition f1 = f0.addOrReplaceChild("flame_mid_" + i,
                        CubeListBuilder.create().texOffs(0, 36).addBox(-1.4F, -7.5F, -0.7F, 2.8F, 7.5F, 1.4F),
                        PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.14F, 0.18F, 0.12F));
                f1.addOrReplaceChild("flame_tip_" + i,
                        CubeListBuilder.create().texOffs(0, 36).addBox(-0.8F, -6.5F, -0.5F, 1.6F, 6.5F, 1.0F),
                        PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F, -0.18F, 0.20F, 0.14F));
            }

            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age());
        }

        public void setupAnim(float age) {
            this.innerPillars.yRot = age * 0.25F;
            this.outerPillars.yRot = -age * 0.18F;
            this.outerPillars.y = Mth.sin(age * 0.35F) * 1.5F;
        }
    }

    // =========================================================================
    // 9. KAGE MANE NO JUTSU — 3D Shadow Ribbon & Coiling Shadow Claws
    // =========================================================================
    public static class ShadowBindModel extends Model<VisualState> {
        private final ModelPart claws;

        public ShadowBindModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
            this.claws = part.getChild("claws");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Rounded Multi-Rotated Shadow Pool Disc & Tapered Segmented Shadow Ribbon
            PartDefinition pool = root.addOrReplaceChild("shadow_pool",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-9.0F, -0.8F, -9.0F, 18, 1, 18)
                            .texOffs(0, 25).addBox(-2.2F, -0.6F, 0.0F, 4.4F, 1.0F, 12.0F),
                    PartPose.ZERO);
            pool.addOrReplaceChild("pool_rot_a",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -0.78F, -9.0F, 18, 1, 18),
                    PartPose.rotation(0.0F, 0.5236F, 0.0F));
            pool.addOrReplaceChild("pool_rot_b",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-9.0F, -0.76F, -9.0F, 18, 1, 18),
                    PartPose.rotation(0.0F, 1.0472F, 0.0F));
            pool.addOrReplaceChild("ribbon_mid",
                    CubeListBuilder.create().texOffs(0, 25).addBox(-1.8F, -0.6F, 11.5F, 3.6F, 1.0F, 12.0F),
                    PartPose.rotation(0.0F, 0.08F, 0.0F));
            pool.addOrReplaceChild("ribbon_tip",
                    CubeListBuilder.create().texOffs(0, 25).addBox(-1.3F, -0.6F, 22.5F, 2.6F, 1.0F, 12.5F),
                    PartPose.rotation(0.0F, -0.06F, 0.0F));

            // 6 Articulated 3-Joint Curving Shadow Claws
            PartDefinition claws = root.addOrReplaceChild("claws", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 6.0);
                float cx = Mth.cos(angle) * 9.0F;
                float cz = Mth.sin(angle) * 9.0F;
                PartDefinition clawBase = claws.addOrReplaceChild("claw_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 28).addBox(-1.3F, -8.5F, -1.3F, 2.6F, 8.5F, 2.6F),
                        PartPose.offsetAndRotation(cx, 0.0F, cz, 0.14F, -angle + (float) (Math.PI / 2.0), 0.0F));
                PartDefinition clawMid = clawBase.addOrReplaceChild("claw_mid_" + i,
                        CubeListBuilder.create().texOffs(0, 28).addBox(-1.0F, -8.0F, -1.0F, 2.0F, 8.0F, 2.0F),
                        PartPose.offsetAndRotation(0.0F, -8.0F, 0.0F, 0.28F, 0.0F, 0.0F));
                clawMid.addOrReplaceChild("claw_tip_" + i,
                        CubeListBuilder.create().texOffs(0, 28).addBox(-0.65F, -7.0F, -0.65F, 1.3F, 7.0F, 1.3F),
                        PartPose.offsetAndRotation(0.0F, -7.6F, 0.0F, 0.34F, 0.0F, 0.0F));
            }

            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age(), state.progress());
        }

        public void setupAnim(float age, float progress) {
            float emerge = progress < 0.12F ? (1.0F - (progress / 0.12F)) * 22.0F : 0.0F;
            this.claws.y = emerge;
            this.claws.yRot = Mth.sin(age * 0.18F) * 0.22F;
        }
    }

    // =========================================================================
    // 10. DOTON: DORYŪHEKI — 3D Sculpted Shinobi Earth Bulwark
    // =========================================================================
    public static class EarthWallModel extends Model<VisualState> {
        private final ModelPart wallBody;

        public EarthWallModel(ModelPart part) {
            super(part, RenderTypes::entityCutout);
            this.wallBody = part.getChild("wall_body");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Sculpted 3-block wide stone bulwark with chamfered battlements, side columns, and stone buttresses
            PartDefinition wall = root.addOrReplaceChild("wall_body",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-22.0F, -45.0F, -5.5F, 44, 45, 11)
                            // Stepped stone plinth & top cornice ledge
                            .texOffs(0, 0).addBox(-24.0F, -6.0F, -7.0F, 48, 6, 14)
                            .texOffs(0, 0).addBox(-23.0F, -46.5F, -6.5F, 46, 3, 13),
                    PartPose.ZERO);

            // Beveled octagonal flanking stone pillars
            wall.addOrReplaceChild("pillar_left",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -48.0F, -6.5F, 7, 48, 13),
                    PartPose.offsetAndRotation(-21.5F, 0.0F, 0.0F, 0.0F, 0.22F, 0.0F));
            wall.addOrReplaceChild("pillar_right",
                    CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -48.0F, -6.5F, 7, 48, 13),
                    PartPose.offsetAndRotation(21.5F, 0.0F, 0.0F, 0.0F, -0.22F, 0.0F));

            // Angled Front & Back Stone Buttress Ribs
            for (int i = -1; i <= 1; i++) {
                float bx = i * 11.5F;
                wall.addOrReplaceChild("buttress_f_" + (i + 1),
                        CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -40.0F, -3.0F, 6, 40, 6),
                        PartPose.offsetAndRotation(bx, 0.0F, -5.5F, -0.09F, 0.0F, 0.0F));
                wall.addOrReplaceChild("buttress_b_" + (i + 1),
                        CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -40.0F, -3.0F, 6, 40, 6),
                        PartPose.offsetAndRotation(bx, 0.0F, 5.5F, 0.09F, 0.0F, 0.0F));
            }

            // Sculpted Chamfered Top Battlement Merlons
            for (int i = 0; i < 4; i++) {
                float mx = -15.0F + i * 10.0F;
                PartDefinition merlon = wall.addOrReplaceChild("merlon_" + i,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -5.0F, -5.0F, 7, 5, 10),
                        PartPose.offset(mx, -46.5F, 0.0F));
                merlon.addOrReplaceChild("merlon_cap_" + i,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-2.6F, -2.0F, -4.0F, 5.2F, 2.0F, 8.0F),
                        PartPose.offset(0.0F, -5.0F, 0.0F));
            }

            return LayerDefinition.create(mesh, 128, 64);
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.progress());
        }

        public void setupAnim(float progress) {
            float emerge = progress < 0.08F ? (1.0F - (progress / 0.08F)) * 48.0F : 0.0F;
            this.wallBody.y = emerge;
        }
    }

    // =========================================================================
    // 11. HACHIMON TONKŌ: EIGHT GATES — 3D Horizontal Ground Chakra Vapor Rings
    // =========================================================================
    public static class EightGatesAuraModel extends Model<VisualState> {
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

        @Override
        public void setupAnim(VisualState state) {
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

    // =========================================================================
    // 12. DOTON: DORYŪSŌ — 3D Jagged Erupting Stone Spikes Cluster
    // =========================================================================
    public static class StoneSpikesModel extends Model<VisualState> {
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
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.progress());
        }

        public void setupAnim(float progress) {
            float emerge = progress < 0.12F ? (1.0F - (progress / 0.12F)) * 54.0F : 0.0F;
            float retract = progress > 0.82F ? ((progress - 0.82F) / 0.18F) * 54.0F : 0.0F;
            this.spikesCluster.y = emerge + retract;
        }
    }

    // =========================================================================
    // 13. UZUMAKI KONGŌ FŪSA — 3D Golden Fūinjutsu Ring & Articulated Sealing Chains
    // =========================================================================
    public static class AdamantineChainsModel extends Model<VisualState> {
        private final ModelPart sealRing;
        private final ModelPart innerSealRing;
        private final ModelPart chainsGroup;

        public AdamantineChainsModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
            this.sealRing = part.getChild("seal_ring");
            this.innerSealRing = part.getChild("inner_seal_ring");
            this.chainsGroup = part.getChild("chains_group");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Outer 16-segment golden Fūinjutsu sealing circle
            PartDefinition outer = root.addOrReplaceChild("seal_ring", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 16; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 16.0);
                float radius = 18.0F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                outer.addOrReplaceChild("seal_outer_seg_" + i,
                        CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -0.45F, -1.0F, 7.0F, 0.9F, 2.0F),
                        PartPose.offsetAndRotation(px, -0.4F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
            }

            // Inner counter-rotating 8-segment seal glyph ring
            PartDefinition inner = root.addOrReplaceChild("inner_seal_ring",
                    CubeListBuilder.create().texOffs(0, 38).addBox(-2.5F, -0.5F, -2.5F, 5.0F, 1.0F, 5.0F),
                    PartPose.ZERO);
            for (int i = 0; i < 8; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 8.0);
                float radius = 10.5F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                inner.addOrReplaceChild("seal_inner_seg_" + i,
                        CubeListBuilder.create().texOffs(20, 0).addBox(-2.0F, -0.4F, -0.8F, 4.0F, 0.8F, 1.6F),
                        PartPose.offsetAndRotation(px, -0.5F, pz, 0.0F, -angle + (float) (Math.PI / 2.0), 0.0F));
            }

            // 6 Articulated 3-joint Adamantine Chains with tapered pyramid sealing stakes (kuiki)
            PartDefinition chains = root.addOrReplaceChild("chains_group", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 6.0);
                float radius = 21.0F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                float yaw = -angle + (float) (Math.PI / 2.0);
                float pitchSweep = (i % 2 == 0) ? 0.52F : 0.44F;

                PartDefinition base = chains.addOrReplaceChild("chain_base_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 8).addBox(-1.2F, -9.0F, -0.6F, 2.4F, 9.0F, 1.2F)
                                .texOffs(10, 8).addBox(-0.6F, -9.0F, -1.2F, 1.2F, 9.0F, 2.4F),
                        PartPose.offsetAndRotation(px, 0.0F, pz, pitchSweep, yaw, 0.0F));

                PartDefinition mid = base.addOrReplaceChild("chain_mid_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 8).addBox(-1.1F, -9.0F, -0.6F, 2.2F, 9.0F, 1.2F)
                                .texOffs(10, 8).addBox(-0.6F, -9.0F, -1.1F, 1.2F, 9.0F, 2.2F)
                                .texOffs(20, 8).addBox(-1.6F, -1.5F, -1.6F, 3.2F, 2.2F, 3.2F),
                        PartPose.offsetAndRotation(0.0F, -8.6F, 0.0F, 0.42F, 0.14F, 0.0F));

                PartDefinition tip = mid.addOrReplaceChild("chain_tip_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 8).addBox(-1.0F, -8.0F, -0.55F, 2.0F, 8.0F, 1.1F)
                                .texOffs(10, 8).addBox(-0.55F, -8.0F, -1.0F, 1.1F, 8.0F, 2.0F)
                                .texOffs(0, 24).addBox(-1.5F, -14.5F, -1.5F, 3.0F, 7.0F, 3.0F)
                                .texOffs(16, 24).addBox(-1.1F, -19.0F, -1.1F, 2.2F, 5.0F, 2.2F)
                                .texOffs(28, 24).addBox(-0.6F, -22.5F, -0.6F, 1.2F, 4.0F, 1.2F),
                        PartPose.offsetAndRotation(0.0F, -8.4F, 0.0F, 0.36F, -0.10F, 0.0F));
                tip.addOrReplaceChild("chain_stake_collar_" + i,
                        CubeListBuilder.create().texOffs(20, 8).addBox(-1.8F, -8.5F, -1.8F, 3.6F, 2.0F, 3.6F),
                        PartPose.rotation(0.0F, 0.7854F, 0.0F));
            }

            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age(), state.progress());
        }

        public void setupAnim(float age, float progress) {
            float emerge = progress < 0.12F ? (1.0F - (progress / 0.12F)) * 34.0F : 0.0F;
            float retract = progress > 0.86F ? ((progress - 0.86F) / 0.14F) * 34.0F : 0.0F;
            float pulse = Mth.sin(age * 0.42F) * 0.8F;
            this.sealRing.yRot = age * 0.14F;
            this.innerSealRing.yRot = -age * 0.22F;
            this.chainsGroup.y = emerge + retract + pulse;
            this.chainsGroup.yRot = Mth.sin(age * 0.12F) * 0.08F;
        }
    }

    // =========================================================================
    // 14. KATON: HAISEKISHŌ — 3D Superheated Volcanic Ash Cloud & Ember Vortex
    // =========================================================================
    public static class AshCloudModel extends Model<VisualState> {
        private final ModelPart corePlume;
        private final ModelPart swirlRing;
        private final ModelPart emberCrown;

        public AshCloudModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
            this.corePlume = part.getChild("core_plume");
            this.swirlRing = part.getChild("swirl_ring");
            this.emberCrown = part.getChild("ember_crown");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Multi-lobed octagonal superheated ash dome & incandescent ember core
            PartDefinition core = root.addOrReplaceChild("core_plume",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-11.0F, -14.0F, -11.0F, 22.0F, 13.0F, 22.0F)
                            .texOffs(0, 35).addBox(-8.0F, -22.0F, -8.0F, 16.0F, 9.0F, 16.0F)
                            .texOffs(38, 35).addBox(-4.5F, -13.0F, -4.5F, 9.0F, 9.0F, 9.0F),
                    PartPose.ZERO);
            core.addOrReplaceChild("core_oct",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-10.5F, -13.5F, -10.5F, 21.0F, 12.0F, 21.0F),
                    PartPose.rotation(0.0F, 0.7854F, 0.0F));

            // 8 Billowing Perimeter Volcanic Ash Lobes
            PartDefinition swirl = root.addOrReplaceChild("swirl_ring", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 8; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 8.0);
                float radius = (i % 2 == 0) ? 18.0F : 15.0F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                float py = (i % 2 == 0) ? -7.5F : -11.5F;
                swirl.addOrReplaceChild("ash_lobe_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 0).addBox(-6.5F, -5.5F, -6.5F, 13.0F, 10.0F, 13.0F)
                                .texOffs(38, 35).addBox(-3.0F, -3.5F, -3.0F, 6.0F, 6.0F, 6.0F),
                        PartPose.offsetAndRotation(px, py, pz, 0.12F, -angle, 0.10F));
            }

            // 6 Spiraling Crown Cinder & Flame Plumes
            PartDefinition crown = root.addOrReplaceChild("ember_crown", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 6; i++) {
                float angle = (float) (i * Math.PI * 2.0 / 6.0);
                float radius = 11.5F;
                float px = Mth.cos(angle) * radius;
                float pz = Mth.sin(angle) * radius;
                crown.addOrReplaceChild("ember_col_" + i,
                        CubeListBuilder.create()
                                .texOffs(0, 35).addBox(-3.2F, -12.0F, -3.2F, 6.4F, 12.0F, 6.4F)
                                .texOffs(38, 35).addBox(-1.6F, -18.0F, -1.6F, 3.2F, 7.0F, 3.2F),
                        PartPose.offsetAndRotation(px, -13.0F, pz, 0.22F, -angle + (float) (Math.PI / 2.0), 0.0F));
            }

            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age(), state.progress());
        }

        public void setupAnim(float age, float progress) {
            float rise = Mth.sin(age * 0.24F) * 1.2F;
            float expand = progress < 0.15F ? (1.0F - (progress / 0.15F)) * 10.0F : 0.0F;
            this.corePlume.y = rise + expand;
            this.corePlume.yRot = age * 0.08F;
            this.swirlRing.y = rise * 0.6F + expand;
            this.swirlRing.yRot = age * 0.16F;
            this.emberCrown.y = -rise + expand;
            this.emberCrown.yRot = -age * 0.24F;
        }
    }

    // =========================================================================
    // 15. SUITON: SUIJINHEKI — 3D Surging Encircled Water Encampment Barrier
    // =========================================================================
    public static class WaterWallModel extends Model<VisualState> {
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
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age(), state.progress());
        }

        public void setupAnim(float age, float progress) {
            float emerge = progress < 0.10F ? (1.0F - (progress / 0.10F)) * 46.0F : 0.0F;
            float subsiding = progress > 0.88F ? ((progress - 0.88F) / 0.12F) * 46.0F : 0.0F;
            float surge = Mth.sin(age * 0.45F) * 1.4F;
            this.waveBase.y = emerge + subsiding;
            this.crestSurge.y = emerge + subsiding + surge;
            this.foamRings.y = emerge + subsiding + Mth.cos(age * 0.52F) * 0.8F;
        }
    }

    // =========================================================================
    // 16. CHAKURA NO MESU — 3D Teal Medical Chakra Scalpel Hand Blade
    // =========================================================================
    public static class ChakraScalpelModel extends Model<VisualState> {
        private final ModelPart scalpelCore;
        private final ModelPart edgeAura;

        public ChakraScalpelModel(ModelPart part) {
            super(part, RenderTypes::entityTranslucent);
            this.scalpelCore = part.getChild("scalpel_core");
            this.edgeAura = part.getChild("edge_aura");
        }

        public static LayerDefinition createLayer() {
            MeshDefinition mesh = new MeshDefinition();
            PartDefinition root = mesh.getRoot();

            // Ergonomic gauntlet sheath + tapered dual-bevel glowing teal chakra blade
            PartDefinition core = root.addOrReplaceChild("scalpel_core",
                    CubeListBuilder.create()
                            .texOffs(0, 0).addBox(-2.2F, -1.6F, -4.0F, 4.4F, 3.2F, 8.0F)
                            .texOffs(0, 14).addBox(-1.4F, -1.1F, -13.0F, 2.8F, 2.2F, 10.0F)
                            .texOffs(26, 0).addBox(-0.85F, -0.75F, -20.0F, 1.7F, 1.5F, 8.0F)
                            .texOffs(26, 14).addBox(-0.35F, -0.4F, -24.5F, 0.7F, 0.8F, 5.0F),
                    PartPose.ZERO);
            core.addOrReplaceChild("blade_fin_top",
                    CubeListBuilder.create().texOffs(0, 28).addBox(-0.35F, -3.2F, -16.0F, 0.7F, 2.3F, 13.0F),
                    PartPose.rotation(-0.08F, 0.0F, 0.0F));
            core.addOrReplaceChild("blade_fin_bottom",
                    CubeListBuilder.create().texOffs(0, 28).addBox(-0.35F, 0.9F, -15.0F, 0.7F, 2.0F, 12.0F),
                    PartPose.rotation(0.08F, 0.0F, 0.0F));

            // 4 Crackling Teal Surgical Chakra Ribbons Spiraling Around the Blade
            PartDefinition aura = root.addOrReplaceChild("edge_aura", CubeListBuilder.create(), PartPose.ZERO);
            for (int i = 0; i < 4; i++) {
                float rotZ = (float) (i * Math.PI * 0.5D);
                aura.addOrReplaceChild("scalpel_arc_" + i,
                        CubeListBuilder.create().texOffs(28, 28).addBox(2.4F, -0.45F, -14.5F, 1.1F, 0.9F, 13.0F),
                        PartPose.rotation(0.10F, 0.0F, rotZ));
            }

            return LayerDefinition.create(mesh, 64, 64);
        }

        @Override
        public void setupAnim(VisualState state) {
            super.setupAnim(state);
            setupAnim(state.age());
        }

        public void setupAnim(float age) {
            this.scalpelCore.y = Mth.sin(age * 0.60F) * 0.35F;
            this.edgeAura.zRot = age * 0.55F;
        }
    }
}
