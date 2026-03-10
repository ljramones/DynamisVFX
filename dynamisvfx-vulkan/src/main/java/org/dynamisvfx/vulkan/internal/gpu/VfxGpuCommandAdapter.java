package org.dynamisvfx.vulkan.internal.gpu;

import org.dynamisvfx.api.VfxFrameContext;

/**
 * Internal anti-corruption seam for GPU command-buffer access.
 */
public interface VfxGpuCommandAdapter {
    long commandBuffer(VfxFrameContext frameContext);
}
