package com.sekwah.narutomod.client.model.jutsu;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 4. RASENGAN — Procedural 3D Multi-Layered Swirling Chakra Sphere.
 */
public class RasenganSphereModel extends Model<JutsuVisualModels.VisualState> {
    private float age;

    public RasenganSphereModel(ModelPart part) {
        super(part, RenderTypes::entityTranslucent);
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition core = root.addOrReplaceChild("core",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6),
                PartPose.ZERO);
        core.addOrReplaceChild("core_rot_y",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6),
                PartPose.rotation(0.0F, 0.7854F, 0.0F));
        core.addOrReplaceChild("core_rot_x",
                CubeListBuilder.create().texOffs(0, 0).addBox(-3.0F, -3.0F, -3.0F, 6, 6, 6),
                PartPose.rotation(0.7854F, 0.0F, 0.7854F));

        PartDefinition innerShell = root.addOrReplaceChild("inner_shell",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -4.5F, -4.5F, 9, 9, 9),
                PartPose.ZERO);
        innerShell.addOrReplaceChild("inner_rot_a",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -4.5F, -4.5F, 9, 9, 9),
                PartPose.rotation(0.5236F, 0.7854F, 0.0F));
        innerShell.addOrReplaceChild("inner_rot_b",
                CubeListBuilder.create().texOffs(0, 16).addBox(-4.5F, -4.5F, -4.5F, 9, 9, 9),
                PartPose.rotation(-0.5236F, 0.0F, 0.7854F));

        PartDefinition outerShell = root.addOrReplaceChild("outer_shell",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5.5F, -5.5F, -5.5F, 11, 11, 11),
                PartPose.ZERO);
        outerShell.addOrReplaceChild("outer_rot_a",
                CubeListBuilder.create().texOffs(0, 34).addBox(-5.5F, -5.5F, -5.5F, 11, 11, 11),
                PartPose.rotation(0.7854F, 0.5236F, 0.5236F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(JutsuVisualModels.VisualState state) {
        super.setupAnim(state);
        this.age = state.age();
    }

    public void setupAnim(float age) {
        this.age = age;
    }

    public void renderProcedural(PoseStack.Pose basePose, VertexConsumer buffer, int light, int overlay,
                                 float r, float g, float b, float a) {
        renderProcedural(basePose, buffer, this.age, light, overlay, r, g, b, a);
    }

    public void renderProcedural(PoseStack.Pose basePose, VertexConsumer buffer, float age, int light, int overlay,
                                 float r, float g, float b, float a) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().set(basePose);
        renderProcedural(poseStack, buffer, age, light, overlay, r, g, b, a);
    }

    public void renderProcedural(PoseStack poseStack, VertexConsumer buffer, int light, int overlay,
                                 float r, float g, float b, float a) {
        renderProcedural(poseStack, buffer, this.age, light, overlay, r, g, b, a);
    }

    public void renderProcedural(PoseStack poseStack, VertexConsumer buffer, float age, int light, int overlay,
                                 float r, float g, float b, float a) {
        // Dense incandescent core sphere
        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, age * 26.0F);
        renderSmoothSphere(poseStack, buffer, 0.34F, 24, 36, light, overlay,
                0.85F * r, 0.97F * g, 1.0F * b, 0.95F * a);
        poseStack.popPose();

        // Mid counter-rotating tilted swirl shell
        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.ZP, 35.0F);
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, -age * 19.0F);
        renderSmoothSphere(poseStack, buffer, 0.45F, 24, 36, light, overlay,
                0.24F * r, 0.72F * g, 1.0F * b, 0.58F * a);
        poseStack.popPose();

        // Outer cross-tilted chakra shell
        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.XP, -35.0F);
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, age * 14.0F);
        renderSmoothSphere(poseStack, buffer, 0.54F, 24, 36, light, overlay,
                0.42F * r, 0.85F * g, 1.0F * b, 0.30F * a);
        poseStack.popPose();

        // 4th wispy outer corona shell for spherical silhouette continuity
        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.ZP, -42.0F);
        poseStack.rotateDegrees(com.mojang.math.Axis.XP, 28.0F);
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, -age * 10.0F);
        renderSmoothSphere(poseStack, buffer, 0.62F, 20, 32, light, overlay,
                0.55F * r, 0.90F * g, 1.0F * b, 0.16F * a);
        poseStack.popPose();
    }

    public static final class UnitSphereGrid {
        public static final UnitSphereGrid EMPTY = new UnitSphereGrid(0, 0, new float[0], new float[0], new float[0], new float[0], new float[0]);

        public final int stacks;
        public final int slices;
        public final int vertexCount;
        public final float[] nx;
        public final float[] ny;
        public final float[] nz;
        public final float[] u;
        public final float[] v;

        private UnitSphereGrid(int stacks, int slices, float[] nx, float[] ny, float[] nz, float[] u, float[] v) {
            this.stacks = stacks;
            this.slices = slices;
            this.vertexCount = nx.length;
            this.nx = nx;
            this.ny = ny;
            this.nz = nz;
            this.u = u;
            this.v = v;
        }
    }

    private static final ConcurrentHashMap<Long, UnitSphereGrid> SPHERE_GRID_CACHE = new ConcurrentHashMap<>();

    public static UnitSphereGrid getUnitSphereGrid(int stacks, int slices) {
        if (stacks <= 0 || slices <= 0) {
            return UnitSphereGrid.EMPTY;
        }
        long key = (((long) stacks) << 32) | (slices & 0xFFFFFFFFL);
        UnitSphereGrid grid = SPHERE_GRID_CACHE.get(key);
        if (grid == null) {
            grid = buildUnitSphereGrid(stacks, slices);
            UnitSphereGrid existing = SPHERE_GRID_CACHE.putIfAbsent(key, grid);
            if (existing != null) {
                grid = existing;
            }
        }
        return grid;
    }

    private static UnitSphereGrid buildUnitSphereGrid(int stacks, int slices) {
        int vertexCount = stacks * slices * 4;
        float[] nx = new float[vertexCount];
        float[] ny = new float[vertexCount];
        float[] nz = new float[vertexCount];
        float[] u = new float[vertexCount];
        float[] v = new float[vertexCount];

        int idx = 0;
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

                nx[idx] = sinPhi1 * cosT1;
                ny[idx] = cosPhi1;
                nz[idx] = sinPhi1 * sinT1;
                u[idx] = u1;
                v[idx] = v1;
                idx++;

                nx[idx] = sinPhi2 * cosT1;
                ny[idx] = cosPhi2;
                nz[idx] = sinPhi2 * sinT1;
                u[idx] = u1;
                v[idx] = v2;
                idx++;

                nx[idx] = sinPhi2 * cosT2;
                ny[idx] = cosPhi2;
                nz[idx] = sinPhi2 * sinT2;
                u[idx] = u2;
                v[idx] = v2;
                idx++;

                nx[idx] = sinPhi1 * cosT2;
                ny[idx] = cosPhi1;
                nz[idx] = sinPhi1 * sinT2;
                u[idx] = u2;
                v[idx] = v1;
                idx++;
            }
        }
        return new UnitSphereGrid(stacks, slices, nx, ny, nz, u, v);
    }

    public static void renderSmoothSphere(PoseStack poseStack, VertexConsumer buffer, float radius,
                                          int stacks, int slices, int light, int overlay,
                                          float r, float g, float b, float a) {
        renderSmoothSphere(poseStack.last(), buffer, radius, stacks, slices, light, overlay, r, g, b, a);
    }

    public static void renderSmoothSphere(PoseStack.Pose pose, VertexConsumer buffer, float radius,
                                          int stacks, int slices, int light, int overlay,
                                          float r, float g, float b, float a) {
        if (stacks <= 0 || slices <= 0) {
            return;
        }
        UnitSphereGrid grid = getUnitSphereGrid(stacks, slices);
        org.joml.Matrix4f mat = pose.pose();
        int count = grid.vertexCount;
        float[] nx = grid.nx;
        float[] ny = grid.ny;
        float[] nz = grid.nz;
        float[] u = grid.u;
        float[] v = grid.v;

        for (int k = 0; k < count; k++) {
            float vx = nx[k];
            float vy = ny[k];
            float vz = nz[k];
            buffer.addVertex(mat, vx * radius, vy * radius, vz * radius)
                    .setColor(r, g, b, a).setUv(u[k], v[k]).setOverlay(overlay).setLight(light)
                    .setNormal(pose, vx, vy, vz);
        }
    }

    public static void renderSmoothSphere(PoseStack.Pose pose, VertexConsumer buffer, float radius,
                                          int stacks, int slices, int r, int g, int b, int a, int packedLight) {
        if (stacks <= 0 || slices <= 0) {
            return;
        }
        UnitSphereGrid grid = getUnitSphereGrid(stacks, slices);
        org.joml.Matrix4f mat = pose.pose();
        int count = grid.vertexCount;
        float[] nx = grid.nx;
        float[] ny = grid.ny;
        float[] nz = grid.nz;
        float[] u = grid.u;
        float[] v = grid.v;
        int overlay = OverlayTexture.NO_OVERLAY;

        for (int k = 0; k < count; k++) {
            float vx = nx[k];
            float vy = ny[k];
            float vz = nz[k];
            buffer.addVertex(mat, vx * radius, vy * radius, vz * radius)
                    .setColor(r, g, b, a).setUv(u[k], v[k]).setOverlay(overlay).setLight(packedLight)
                    .setNormal(pose, vx, vy, vz);
        }
    }
}
