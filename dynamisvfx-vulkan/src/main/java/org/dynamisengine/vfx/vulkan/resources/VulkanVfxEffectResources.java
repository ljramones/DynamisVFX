package org.dynamisengine.vfx.vulkan.resources;

import org.dynamisengine.gpu.api.error.GpuException;
import org.dynamisengine.gpu.api.gpu.IndirectCommandBuffer;
import org.dynamisengine.gpu.vulkan.memory.VulkanBufferOps;
import org.dynamisengine.gpu.vulkan.memory.VulkanImageOps;
import org.dynamisengine.vfx.api.ForceDescriptor;
import org.dynamisengine.vfx.api.ForceType;
import org.dynamisengine.vfx.api.NoiseForceConfig;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.api.VfxHandle;
import org.dynamisengine.vfx.vulkan.noise.VulkanVfxNoiseField3D;
import org.dynamisengine.vfx.vulkan.noise.VulkanVfxNoiseFieldConfig;
import org.dynamisengine.vfx.vulkan.noise.VulkanVfxNoiseFieldUploader;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class VulkanVfxEffectResources {
    private final VfxBufferConfig config;
    private volatile ParticleEmitterDescriptor descriptor;
    private final VulkanVfxSoaBuffers soaBuffers;
    private final VulkanVfxControlBuffers controlBuffers;
    private final VulkanVfxRenderBuffers renderBuffers;
    private final VfxHandle handle;
    private VulkanVfxNoiseField3D noiseField;
    private VulkanVfxNoiseFieldConfig noiseFieldConfig;

    private ByteBuffer freeListInitData;
    private ByteBuffer aliveCountInitData;

    private VulkanVfxEffectResources(
        VfxBufferConfig config,
        ParticleEmitterDescriptor descriptor,
        VulkanVfxSoaBuffers soaBuffers,
        VulkanVfxControlBuffers controlBuffers,
        VulkanVfxRenderBuffers renderBuffers,
        VfxHandle handle
    ) {
        this.config = config;
        this.descriptor = descriptor;
        this.soaBuffers = soaBuffers;
        this.controlBuffers = controlBuffers;
        this.renderBuffers = renderBuffers;
        this.handle = handle;
    }

    public static VulkanVfxEffectResources allocate(
        VfxHandle handle,
        ParticleEmitterDescriptor descriptor,
        VulkanBufferOps bufferOps,
        VulkanImageOps imageOps,
        IndirectCommandBuffer indirectBuffer
    ) throws GpuException {
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(bufferOps, "bufferOps");
        Objects.requireNonNull(imageOps, "imageOps");
        Objects.requireNonNull(indirectBuffer, "indirectBuffer");

        VfxBufferConfig config = VfxBufferConfig.of(descriptor);

        VulkanVfxSoaBuffers soa = VulkanVfxSoaBuffers.allocate(config, bufferOps);
        VulkanVfxControlBuffers control = VulkanVfxControlBuffers.allocate(config, bufferOps);
        VulkanVfxRenderBuffers render = VulkanVfxRenderBuffers.allocate(config, bufferOps, indirectBuffer);

        VulkanVfxEffectResources resources = new VulkanVfxEffectResources(config, descriptor, soa, control, render, handle);
        NoiseForceConfig noiseConfig = findCurlNoiseConfig(descriptor);
        if (noiseConfig != null) {
            resources.noiseFieldConfig = VulkanVfxNoiseFieldConfig.from(noiseConfig);
            resources.noiseField = VulkanVfxNoiseField3D.allocate(1L, imageOps, resources.noiseFieldConfig);
        }
        return resources;
    }

    public void destroy(VulkanBufferOps bufferOps, VulkanImageOps imageOps) {
        Objects.requireNonNull(bufferOps, "bufferOps");
        Objects.requireNonNull(imageOps, "imageOps");
        if (noiseField != null) {
            noiseField.destroy(1L, imageOps);
            noiseField = null;
        }
        renderBuffers.destroy(bufferOps);
        controlBuffers.destroy(bufferOps);
        soaBuffers.destroy(bufferOps);
        freeListInitData = null;
        aliveCountInitData = null;
    }

    public void initializeGpuState(VulkanBufferOps bufferOps, VulkanImageOps imageOps, long commandBuffer) {
        Objects.requireNonNull(bufferOps, "bufferOps");
        Objects.requireNonNull(imageOps, "imageOps");

        freeListInitData = ByteBuffer.allocateDirect(config.maxParticles() * Integer.BYTES)
            .order(ByteOrder.nativeOrder());
        for (int i = 0; i < config.maxParticles(); i++) {
            freeListInitData.putInt(i);
        }
        freeListInitData.flip();

        aliveCountInitData = ByteBuffer.allocateDirect(Integer.BYTES)
            .order(ByteOrder.nativeOrder())
            .putInt(0);
        aliveCountInitData.flip();

        // Placeholder: upload freeListInitData and aliveCountInitData via VulkanBufferOps
        // once VkDevice/VkQueue/context plumbing is wired.
        long ignored = commandBuffer;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid command buffer");
        }

        if (noiseField != null && noiseFieldConfig != null) {
            VulkanVfxNoiseFieldUploader.bakeAndUpload(commandBuffer, noiseField, noiseFieldConfig, imageOps);
        }
    }

    public VfxBufferConfig config() {
        return config;
    }

    public VulkanVfxSoaBuffers soaBuffers() {
        return soaBuffers;
    }

    public ParticleEmitterDescriptor descriptor() {
        return descriptor;
    }

    public void updateDescriptor(ParticleEmitterDescriptor updated) {
        this.descriptor = Objects.requireNonNull(updated, "updated");
    }

    public VulkanVfxNoiseField3D noiseField() {
        return noiseField;
    }

    public VulkanVfxNoiseFieldConfig noiseFieldConfig() {
        return noiseFieldConfig;
    }

    public VulkanVfxControlBuffers controlBuffers() {
        return controlBuffers;
    }

    public VulkanVfxRenderBuffers renderBuffers() {
        return renderBuffers;
    }

    public VfxHandle handle() {
        return handle;
    }

    public ByteBuffer freeListInitData() {
        return freeListInitData == null ? null : freeListInitData.asReadOnlyBuffer();
    }

    public ByteBuffer aliveCountInitData() {
        return aliveCountInitData == null ? null : aliveCountInitData.asReadOnlyBuffer();
    }

    private static NoiseForceConfig findCurlNoiseConfig(ParticleEmitterDescriptor descriptor) {
        if (descriptor.forces() == null) {
            return null;
        }
        for (ForceDescriptor force : descriptor.forces()) {
            if (force != null && force.type() == ForceType.CURL_NOISE) {
                return force.noiseConfig();
            }
        }
        return null;
    }
}
