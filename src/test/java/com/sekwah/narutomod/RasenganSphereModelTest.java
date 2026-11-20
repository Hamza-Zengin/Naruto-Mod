package com.sekwah.narutomod;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.sekwah.narutomod.client.model.jutsu.RasenganSphereModel;
import net.minecraft.util.Mth;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Headless unit tests verifying that RasenganSphereModel's cached UnitSphereGrid
 * and PoseStack.Pose renderSmoothSphere overload produce vertices, normals, and UVs
 * matching the original trigonometric sphere formula within 1e-6f.
 */
public class RasenganSphereModelTest {

    private static final float EPSILON = 1e-6f;

    @Test
    void testCachedUnitSphereGridMatchesTrigonometricFormula() {
        int[][] configs = {
                {24, 36},
                {20, 32},
                {18, 26},
                {18, 28},
                {20, 28},
                {18, 24},
                {7, 10},
                {10, 14},
                {14, 20}
        };

        for (int[] cfg : configs) {
            int stacks = cfg[0];
            int slices = cfg[1];

            RasenganSphereModel.UnitSphereGrid grid = RasenganSphereModel.getUnitSphereGrid(stacks, slices);
            assertNotNull(grid);
            assertEquals(stacks, grid.stacks);
            assertEquals(slices, grid.slices);
            assertEquals(stacks * slices * 4, grid.vertexCount);

            // Repeated lookup must return the exact cached instance
            assertSame(grid, RasenganSphereModel.getUnitSphereGrid(stacks, slices),
                    "UnitSphereGrid should be cached per (stacks, slices) pair");

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

                    float[] expectedNx = {
                            sinPhi1 * cosT1, sinPhi2 * cosT1, sinPhi2 * cosT2, sinPhi1 * cosT2
                    };
                    float[] expectedNy = {
                            cosPhi1, cosPhi2, cosPhi2, cosPhi1
                    };
                    float[] expectedNz = {
                            sinPhi1 * sinT1, sinPhi2 * sinT1, sinPhi2 * sinT2, sinPhi1 * sinT2
                    };
                    float[] expectedU = {u1, u1, u2, u2};
                    float[] expectedV = {v1, v2, v2, v1};

                    for (int corner = 0; corner < 4; corner++) {
                        assertEquals(expectedNx[corner], grid.nx[idx], EPSILON, "nx mismatch at idx=" + idx);
                        assertEquals(expectedNy[corner], grid.ny[idx], EPSILON, "ny mismatch at idx=" + idx);
                        assertEquals(expectedNz[corner], grid.nz[idx], EPSILON, "nz mismatch at idx=" + idx);
                        assertEquals(expectedU[corner], grid.u[idx], EPSILON, "u mismatch at idx=" + idx);
                        assertEquals(expectedV[corner], grid.v[idx], EPSILON, "v mismatch at idx=" + idx);
                        idx++;
                    }
                }
            }
        }
    }

    @Test
    void testNonPositiveDimensionsReturnEmptyGrid() {
        assertSame(RasenganSphereModel.UnitSphereGrid.EMPTY, RasenganSphereModel.getUnitSphereGrid(0, 10));
        assertSame(RasenganSphereModel.UnitSphereGrid.EMPTY, RasenganSphereModel.getUnitSphereGrid(10, 0));
        assertSame(RasenganSphereModel.UnitSphereGrid.EMPTY, RasenganSphereModel.getUnitSphereGrid(-3, 8));
        assertEquals(0, RasenganSphereModel.UnitSphereGrid.EMPTY.vertexCount);
    }

    @Test
    void testRenderSmoothSpherePoseOverloadEmitsExactScaledVertices() {
        final int stacks = 7;
        final int slices = 10;
        final float radius = 0.45F;

        List<float[]> recordedPositions = new ArrayList<>();
        List<float[]> recordedUvs = new ArrayList<>();
        List<float[]> recordedNormals = new ArrayList<>();

        VertexConsumer recordingConsumer = (VertexConsumer) Proxy.newProxyInstance(
                VertexConsumer.class.getClassLoader(),
                new Class<?>[]{VertexConsumer.class},
                (proxy, method, args) -> {
                    String name = method.getName();
                    if ("addVertex".equals(name) && args != null && args.length == 4
                            && args[1] instanceof Float x && args[2] instanceof Float y && args[3] instanceof Float z) {
                        recordedPositions.add(new float[]{x, y, z});
                    } else if ("setUv".equals(name) && args != null && args.length == 2
                            && args[0] instanceof Float u && args[1] instanceof Float v) {
                        recordedUvs.add(new float[]{u, v});
                    } else if ("setNormal".equals(name) && args != null && args.length == 4
                            && args[1] instanceof Float nx && args[2] instanceof Float ny && args[3] instanceof Float nz) {
                        recordedNormals.add(new float[]{nx, ny, nz});
                    }
                    if (method.getReturnType().isAssignableFrom(VertexConsumer.class)) {
                        return proxy;
                    }
                    return null;
                }
        );

        PoseStack poseStack = new PoseStack();
        RasenganSphereModel.renderSmoothSphere(
                poseStack.last(), recordingConsumer, radius, stacks, slices,
                15728880, 655360, 1.0F, 0.5F, 0.25F, 0.8F
        );

        RasenganSphereModel.UnitSphereGrid grid = RasenganSphereModel.getUnitSphereGrid(stacks, slices);
        assertEquals(grid.vertexCount, recordedPositions.size());
        assertEquals(grid.vertexCount, recordedUvs.size());
        assertEquals(grid.vertexCount, recordedNormals.size());

        for (int k = 0; k < grid.vertexCount; k++) {
            float[] pos = recordedPositions.get(k);
            float[] uv = recordedUvs.get(k);
            float[] norm = recordedNormals.get(k);

            assertEquals(grid.nx[k] * radius, pos[0], EPSILON);
            assertEquals(grid.ny[k] * radius, pos[1], EPSILON);
            assertEquals(grid.nz[k] * radius, pos[2], EPSILON);
            assertEquals(grid.u[k], uv[0], EPSILON);
            assertEquals(grid.v[k], uv[1], EPSILON);
            assertEquals(grid.nx[k], norm[0], EPSILON);
            assertEquals(grid.ny[k], norm[1], EPSILON);
            assertEquals(grid.nz[k], norm[2], EPSILON);
        }
    }
}
