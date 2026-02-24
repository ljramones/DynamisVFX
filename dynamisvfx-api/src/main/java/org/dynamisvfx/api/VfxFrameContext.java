package org.dynamisvfx.api;

public interface VfxFrameContext {
    long commandBuffer();

    Mat4f cameraView();

    Mat4f cameraProjection();

    float[] frustumPlanes();

    long frameIndex();
}
