package org.dynamisvfx.api;

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
}
