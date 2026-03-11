package org.dynamisvfx.api;

import org.dynamisgpu.api.gpu.DescriptorWriter;
import org.dynamisgpu.api.gpu.IndirectCommandBuffer;

public interface VfxDrawContext {
    /**
     * Preferred typed VFX-owned seam for draw command writing.
     */
    default VfxIndirectCommandSink indirectCommandSink() {
        return VfxIndirectCommandSink.from(indirectBuffer());
    }

    /**
     * Preferred typed VFX-owned seam for descriptor/bindless writes.
     */
    default VfxDescriptorBindingWriter bindlessHeapWriter() {
        return VfxDescriptorBindingWriter.from(bindlessHeap());
    }

    /**
     * Legacy compatibility path.
     */
    @Deprecated(since = "0.1.0")
    IndirectCommandBuffer indirectBuffer();

    /**
     * Legacy compatibility path.
     */
    @Deprecated(since = "0.1.0")
    DescriptorWriter bindlessHeap();

    long frameIndex();
}
