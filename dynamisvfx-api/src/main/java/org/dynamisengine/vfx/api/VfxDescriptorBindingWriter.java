package org.dynamisengine.vfx.api;

/**
 * VFX-owned typed seam for descriptor/bindless writes used during VFX draw recording.
 */
public interface VfxDescriptorBindingWriter {
    void writeStorageBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range);

    void writeUniformBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range);

    void writeSampledImage(long descriptorSet, int binding, int arrayElement, long imageView, long sampler);
}
