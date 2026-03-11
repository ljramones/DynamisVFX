package org.dynamisvfx.api;

import org.dynamisgpu.api.gpu.DescriptorWriter;
import org.dynamisgpu.api.gpu.IndirectCommandBuffer;

public interface VfxDrawContext {
    /**
     * Preferred typed VFX-owned seam for draw command writing.
     */
    default VfxIndirectCommandSink indirectCommandSink() {
        IndirectCommandBuffer buffer = indirectBuffer();
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

    /**
     * Preferred typed VFX-owned seam for descriptor/bindless writes.
     */
    default VfxDescriptorBindingWriter bindlessHeapWriter() {
        DescriptorWriter writer = bindlessHeap();
        return new VfxDescriptorBindingWriter() {
            @Override
            public void writeStorageBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range) {
                writer.writeStorageBuffer(descriptorSet, binding, arrayElement, bufferHandle, offset, range);
            }

            @Override
            public void writeUniformBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range) {
                writer.writeUniformBuffer(descriptorSet, binding, arrayElement, bufferHandle, offset, range);
            }

            @Override
            public void writeSampledImage(long descriptorSet, int binding, int arrayElement, long imageView, long sampler) {
                writer.writeSampledImage(descriptorSet, binding, arrayElement, imageView, sampler);
            }
        };
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
