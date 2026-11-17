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
import net.minecraft.util.Mth;

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
        root.addOrReplaceChild("core", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("inner_shell", CubeListBuilder.create(), PartPose.ZERO);
        root.addOrReplaceChild("outer_shell", CubeListBuilder.create(), PartPose.ZERO);
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
        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, age * 25.0F);
        renderSmoothSphere(poseStack, buffer, 0.36F, 16, 24, light, overlay,
                0.80F * r, 0.96F * g, 1.0F * b, 0.95F * a);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.ZP, 35.0F);
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, -age * 18.0F);
        renderSmoothSphere(poseStack, buffer, 0.48F, 16, 24, light, overlay,
                0.20F * r, 0.68F * g, 1.0F * b, 0.55F * a);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.rotateDegrees(com.mojang.math.Axis.XP, -35.0F);
        poseStack.rotateDegrees(com.mojang.math.Axis.YP, age * 12.0F);
        renderSmoothSphere(poseStack, buffer, 0.58F, 16, 24, light, overlay,
                0.45F * r, 0.85F * g, 1.0F * b, 0.25F * a);
        poseStack.popPose();
    }

    public static void renderSmoothSphere(PoseStack poseStack, VertexConsumer buffer, float radius,
                                          int stacks, int slices, int light, int overlay,
                                          float r, float g, float b, float a) {
        PoseStack.Pose pose = poseStack.last();
        org.joml.Matrix4f mat = pose.pose();

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

                float nx1 = sinPhi1 * cosT1, ny1 = cosPhi1, nz1 = sinPhi1 * sinT1;
                float nx2 = sinPhi2 * cosT1, ny2 = cosPhi2, nz2 = sinPhi2 * sinT1;
                float nx3 = sinPhi2 * cosT2, ny3 = cosPhi2, nz3 = sinPhi2 * sinT2;
                float nx4 = sinPhi1 * cosT2, ny4 = cosPhi1, nz4 = sinPhi1 * sinT2;

                buffer.addVertex(mat, nx1 * radius, ny1 * radius, nz1 * radius)
                        .setColor(r, g, b, a).setUv(u1, v1).setOverlay(overlay).setLight(light)
                        .setNormal(pose, nx1, ny1, nz1);
                buffer.addVertex(mat, nx2 * radius, ny2 * radius, nz2 * radius)
                        .setColor(r, g, b, a).setUv(u1, v2).setOverlay(overlay).setLight(light)
                        .setNormal(pose, nx2, ny2, nz2);
                buffer.addVertex(mat, nx3 * radius, ny3 * radius, nz3 * radius)
                        .setColor(r, g, b, a).setUv(u2, v2).setOverlay(overlay).setLight(light)
                        .setNormal(pose, nx3, ny3, nz3);
                buffer.addVertex(mat, nx4 * radius, ny4 * radius, nz4 * radius)
                        .setColor(r, g, b, a).setUv(u2, v1).setOverlay(overlay).setLight(light)
                        .setNormal(pose, nx4, ny4, nz4);
            }
        }
    }
}
