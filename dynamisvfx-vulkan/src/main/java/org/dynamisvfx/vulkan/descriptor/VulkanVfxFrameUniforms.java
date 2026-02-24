package org.dynamisvfx.vulkan.descriptor;

import org.dynamisvfx.api.VfxFrameContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

public final class VulkanVfxFrameUniforms {
    public static final int SIZE_BYTES = 256;
    private static final int MATRIX_FLOATS = 16;
    private static final int FRUSTUM_PLANES = 6;
    private static final int PLANE_FLOATS = 4;

    private VulkanVfxFrameUniforms() {
    }

    public static void write(ByteBuffer buf, VfxFrameContext ctx, float totalTime) {
        Objects.requireNonNull(buf, "buf");
        Objects.requireNonNull(ctx, "ctx");

        if (buf.capacity() < SIZE_BYTES) {
            throw new IllegalArgumentException("Frame uniform buffer must be at least " + SIZE_BYTES + " bytes");
        }

        buf.clear();
        buf.order(ByteOrder.nativeOrder());

        putMat4(buf, ctx.cameraView());
        putMat4(buf, ctx.cameraProjection());

        // cameraPos vec4 (xyz unknown at API boundary, w unused)
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);

        putFrustumPlanes(buf, ctx.frustumPlanes());

        // deltaTime not currently exposed by VfxFrameContext; written as 0 for now.
        buf.putFloat(0.0f);
        buf.putFloat(totalTime);
        buf.putInt((int) ctx.frameIndex());
        buf.putInt(0);

        buf.flip();
    }

    public static ByteBuffer createBuffer() {
        return ByteBuffer.allocateDirect(SIZE_BYTES).order(ByteOrder.nativeOrder());
    }

    private static void putMat4(ByteBuffer buf, float[] matrix) {
        float[] src = normalizeArray(matrix, MATRIX_FLOATS);
        for (float value : src) {
            buf.putFloat(value);
        }
    }

    private static void putFrustumPlanes(ByteBuffer buf, float[] planes) {
        float[] src = normalizeArray(planes, FRUSTUM_PLANES * PLANE_FLOATS);
        for (float value : src) {
            buf.putFloat(value);
        }
    }

    private static float[] normalizeArray(float[] input, int expected) {
        if (input == null) {
            return new float[expected];
        }
        if (input.length == expected) {
            return input;
        }
        return Arrays.copyOf(input, expected);
    }
}
