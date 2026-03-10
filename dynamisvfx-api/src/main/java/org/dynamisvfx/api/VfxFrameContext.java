package org.dynamisvfx.api;

public interface VfxFrameContext {
    @Deprecated(since = "0.1.0")
    long commandBuffer();

    /**
     * Preferred typed command-buffer seam.
     */
    default VfxCommandBufferRef commandBufferRef() {
        return new VfxCommandBufferRef(commandBuffer());
    }

    float[] cameraView();

    float[] cameraProjection();

    float[] frustumPlanes();

    long frameIndex();
}
