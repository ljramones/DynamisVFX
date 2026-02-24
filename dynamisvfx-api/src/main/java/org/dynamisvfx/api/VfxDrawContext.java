package org.dynamisvfx.api;

import org.dynamisgpu.api.gpu.DescriptorWriter;
import org.dynamisgpu.api.gpu.IndirectCommandBuffer;

public interface VfxDrawContext {
    IndirectCommandBuffer indirectBuffer();

    DescriptorWriter bindlessHeap();

    long frameIndex();
}
