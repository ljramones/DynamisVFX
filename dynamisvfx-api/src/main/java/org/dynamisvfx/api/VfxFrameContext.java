package org.dynamisvfx.api;

public interface VfxFrameContext {
    long commandBuffer();

    float[] cameraView();

    float[] cameraProjection();

    float[] frustumPlanes();

    long frameIndex();
}
