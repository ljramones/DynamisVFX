package org.dynamisvfx.vulkan.resources;

import org.dynamisgpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxSoaBuffers {
    private static final int VEC4_BYTES = 16;

    private final VulkanBufferAlloc positionBuffer;
    private final VulkanBufferAlloc velocityBuffer;
    private final VulkanBufferAlloc colorBuffer;
    private final VulkanBufferAlloc attribBuffer;
    private final VulkanBufferAlloc metaBuffer;

    private final int maxParticles;
    private final int usageFlags;

    private VulkanVfxSoaBuffers(
        VulkanBufferAlloc positionBuffer,
        VulkanBufferAlloc velocityBuffer,
        VulkanBufferAlloc colorBuffer,
        VulkanBufferAlloc attribBuffer,
        VulkanBufferAlloc metaBuffer,
        int maxParticles,
        int usageFlags
    ) {
        this.positionBuffer = positionBuffer;
        this.velocityBuffer = velocityBuffer;
        this.colorBuffer = colorBuffer;
        this.attribBuffer = attribBuffer;
        this.metaBuffer = metaBuffer;
        this.maxParticles = maxParticles;
        this.usageFlags = usageFlags;
    }

    public static VulkanVfxSoaBuffers allocate(VfxBufferConfig config, VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(memoryOps, "memoryOps");

        int usage = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;
        int bytesPerBuffer = config.maxParticles() * VEC4_BYTES;

        return new VulkanVfxSoaBuffers(
            allocateStorageBuffer(memoryOps, bytesPerBuffer, usage),
            allocateStorageBuffer(memoryOps, bytesPerBuffer, usage),
            allocateStorageBuffer(memoryOps, bytesPerBuffer, usage),
            allocateStorageBuffer(memoryOps, bytesPerBuffer, usage),
            allocateStorageBuffer(memoryOps, bytesPerBuffer, usage),
            config.maxParticles(),
            usage
        );
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        // Placeholder for explicit vkDestroyBuffer/vkFreeMemory once Vulkan device context is wired.
    }

    public long totalBytes() {
        return (long) maxParticles * VEC4_BYTES * 5L;
    }

    public VulkanBufferAlloc positionBuffer() {
        return positionBuffer;
    }

    public VulkanBufferAlloc velocityBuffer() {
        return velocityBuffer;
    }

    public VulkanBufferAlloc colorBuffer() {
        return colorBuffer;
    }

    public VulkanBufferAlloc attribBuffer() {
        return attribBuffer;
    }

    public VulkanBufferAlloc metaBuffer() {
        return metaBuffer;
    }

    public int maxParticles() {
        return maxParticles;
    }

    public int usageFlags() {
        return usageFlags;
    }

    private static VulkanBufferAlloc allocateStorageBuffer(VulkanMemoryOps memoryOps, int sizeBytes, int usageFlags) {
        memoryOps.getClass();
        return new VulkanBufferAlloc(0L, 0L);
    }
}
