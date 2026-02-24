package org.dynamisvfx.vulkan.resources;

import org.dynamisgpu.api.error.GpuException;
import org.dynamisgpu.api.gpu.IndirectCommandBuffer;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.VfxHandle;

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
        VulkanMemoryOps memoryOps,
        IndirectCommandBuffer indirectBuffer
    ) throws GpuException {
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(descriptor, "descriptor");
        Objects.requireNonNull(memoryOps, "memoryOps");
        Objects.requireNonNull(indirectBuffer, "indirectBuffer");

        VfxBufferConfig config = VfxBufferConfig.of(descriptor);

        VulkanVfxSoaBuffers soa = VulkanVfxSoaBuffers.allocate(config, memoryOps);
        VulkanVfxControlBuffers control = VulkanVfxControlBuffers.allocate(config, memoryOps);
        VulkanVfxRenderBuffers render = VulkanVfxRenderBuffers.allocate(config, memoryOps, indirectBuffer);

        return new VulkanVfxEffectResources(config, descriptor, soa, control, render, handle);
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        renderBuffers.destroy(memoryOps);
        controlBuffers.destroy(memoryOps);
        soaBuffers.destroy(memoryOps);
        freeListInitData = null;
        aliveCountInitData = null;
    }

    public void initializeGpuState(VulkanMemoryOps memoryOps, long commandBuffer) {
        Objects.requireNonNull(memoryOps, "memoryOps");

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

        // Placeholder: upload freeListInitData and aliveCountInitData via VulkanMemoryOps
        // once VkDevice/VkQueue/context plumbing is wired.
        long ignored = commandBuffer;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid command buffer");
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
}
