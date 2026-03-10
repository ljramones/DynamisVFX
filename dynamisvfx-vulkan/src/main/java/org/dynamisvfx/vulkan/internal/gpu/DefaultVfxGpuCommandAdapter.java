package org.dynamisvfx.vulkan.internal.gpu;

import org.dynamisvfx.api.VfxFrameContext;

/**
 * Default internal adapter implementation.
 */
public final class DefaultVfxGpuCommandAdapter implements VfxGpuCommandAdapter {
    @Override
    public long commandBuffer(final VfxFrameContext frameContext) {
        return frameContext.commandBufferRef().handle();
    }
}
