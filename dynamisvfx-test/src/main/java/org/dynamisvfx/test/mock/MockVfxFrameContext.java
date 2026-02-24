package org.dynamisvfx.test.mock;

import org.dynamisvfx.api.VfxFrameContext;

import java.util.Arrays;

public final class MockVfxFrameContext implements VfxFrameContext {
    private long commandBuffer;
    private float[] cameraView = identity();
    private float[] cameraProjection = identity();
    private float[] frustumPlanes = new float[24];
    private long frameIndex;

    @Override
    public long commandBuffer() {
        return commandBuffer;
    }

    public MockVfxFrameContext commandBuffer(long value) {
        this.commandBuffer = value;
        return this;
    }

    @Override
    public float[] cameraView() {
        return Arrays.copyOf(cameraView, cameraView.length);
    }

    public MockVfxFrameContext cameraView(float[] value) {
        this.cameraView = Arrays.copyOf(value, value.length);
        return this;
    }

    @Override
    public float[] cameraProjection() {
        return Arrays.copyOf(cameraProjection, cameraProjection.length);
    }

    public MockVfxFrameContext cameraProjection(float[] value) {
        this.cameraProjection = Arrays.copyOf(value, value.length);
        return this;
    }

    @Override
    public float[] frustumPlanes() {
        return Arrays.copyOf(frustumPlanes, frustumPlanes.length);
    }

    public MockVfxFrameContext frustumPlanes(float[] value) {
        this.frustumPlanes = Arrays.copyOf(value, value.length);
        return this;
    }

    @Override
    public long frameIndex() {
        return frameIndex;
    }

    public MockVfxFrameContext frameIndex(long value) {
        this.frameIndex = value;
        return this;
    }

    private static float[] identity() {
        return new float[] {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
    }
}
