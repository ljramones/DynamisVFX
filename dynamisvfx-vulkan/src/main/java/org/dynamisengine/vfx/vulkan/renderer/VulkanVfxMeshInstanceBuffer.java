package org.dynamisengine.vfx.vulkan.renderer;

import org.dynamisengine.gpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisengine.gpu.vulkan.memory.VulkanBufferOps;

import java.util.Objects;

public final class VulkanVfxMeshInstanceBuffer {
    public static final int STRIDE_BYTES = 64;

    private final VulkanBufferAlloc buffer;
    private final int maxParticles;

    private VulkanVfxMeshInstanceBuffer(VulkanBufferAlloc buffer, int maxParticles) {
        this.buffer = buffer;
        this.maxParticles = maxParticles;
    }

    public static VulkanVfxMeshInstanceBuffer allocate(VulkanBufferOps memoryOps, int maxParticles) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        if (maxParticles <= 0) {
            throw new IllegalArgumentException("maxParticles must be > 0");
        }

        memoryOps.getClass();
        return new VulkanVfxMeshInstanceBuffer(new VulkanBufferAlloc(0L, 0L), maxParticles);
    }

    public void destroy(VulkanBufferOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        memoryOps.getClass();
    }

    public long bufferHandle() {
        return buffer.buffer();
    }

    public long memoryHandle() {
        return buffer.memory();
    }

    public int maxParticles() {
        return maxParticles;
    }
}
