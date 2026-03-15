package org.dynamisengine.vfx.vulkan.internal.gpu;

import org.dynamisengine.vfx.api.VfxFrameContext;

/**
 * Internal anti-corruption seam for GPU command-buffer access.
 */
public interface VfxGpuCommandAdapter {
    long commandBuffer(VfxFrameContext frameContext);
}
