package org.dynamisvfx.api;

import org.dynamisgpu.api.gpu.DescriptorWriter;

/**
 * VFX-owned typed seam for descriptor/bindless writes used during VFX draw recording.
 */
public interface VfxDescriptorBindingWriter {
    void writeStorageBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range);

    void writeUniformBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range);

    void writeSampledImage(long descriptorSet, int binding, int arrayElement, long imageView, long sampler);

    static VfxDescriptorBindingWriter from(DescriptorWriter writer) {
        if (writer == null) {
            throw new IllegalArgumentException("writer must not be null");
        }
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
}
