package org.dynamisvfx.api;

import org.dynamisgpu.api.gpu.IndirectCommandBuffer;

/**
 * VFX-owned typed seam for writing and referencing indirect draw commands.
 */
public interface VfxIndirectCommandSink {
    void writeCommand(int slot, int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance);

    long bufferHandle();

    long countBufferHandle();

    int variantOffset(int variantIndex);

    int variantCapacity(int variantIndex);

    void destroy();

    static VfxIndirectCommandSink from(IndirectCommandBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        return new VfxIndirectCommandSink() {
            @Override
            public void writeCommand(int slot, int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
                buffer.writeCommand(slot, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
            }

            @Override
            public long bufferHandle() {
                return buffer.bufferHandle();
            }

            @Override
            public long countBufferHandle() {
                return buffer.countBufferHandle();
            }

            @Override
            public int variantOffset(int variantIndex) {
                return buffer.variantOffset(variantIndex);
            }

            @Override
            public int variantCapacity(int variantIndex) {
                return buffer.variantCapacity(variantIndex);
            }

            @Override
            public void destroy() {
                buffer.destroy();
            }
        };
    }
}
