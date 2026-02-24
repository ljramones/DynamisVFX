package org.dynamisvfx.vulkan.resources;

import org.dynamisgpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxControlBuffers {
    private static final int FORCE_ENTRY_BYTES = 32;
    private static final int MAX_FORCES = 32;
    private static final int EMITTER_DESCRIPTOR_BYTES = 256;

    private final VulkanBufferAlloc freeListBuffer;
    private final VulkanBufferAlloc aliveCountBuffer;
    private final VulkanBufferAlloc emitterDescriptorBuffer;
    private final VulkanBufferAlloc forceFieldBuffer;
    private final int maxParticles;
    private final int usageFlags;

    private VulkanVfxControlBuffers(
        VulkanBufferAlloc freeListBuffer,
        VulkanBufferAlloc aliveCountBuffer,
        VulkanBufferAlloc emitterDescriptorBuffer,
        VulkanBufferAlloc forceFieldBuffer,
        int maxParticles,
        int usageFlags
    ) {
        this.freeListBuffer = freeListBuffer;
        this.aliveCountBuffer = aliveCountBuffer;
        this.emitterDescriptorBuffer = emitterDescriptorBuffer;
        this.forceFieldBuffer = forceFieldBuffer;
        this.maxParticles = maxParticles;
        this.usageFlags = usageFlags;
    }

    public static VulkanVfxControlBuffers allocate(VfxBufferConfig config, VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(memoryOps, "memoryOps");

        int usage = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;

        return new VulkanVfxControlBuffers(
            allocateStorageBuffer(memoryOps, config.maxParticles() * Integer.BYTES, usage),
            allocateStorageBuffer(memoryOps, Integer.BYTES, usage),
            allocateStorageBuffer(memoryOps, EMITTER_DESCRIPTOR_BYTES, usage),
            allocateStorageBuffer(memoryOps, FORCE_ENTRY_BYTES * MAX_FORCES, usage),
            config.maxParticles(),
            usage
        );
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        // Placeholder for explicit vkDestroyBuffer/vkFreeMemory once Vulkan device context is wired.
    }

    public VulkanBufferAlloc freeListBuffer() {
        return freeListBuffer;
    }

    public VulkanBufferAlloc aliveCountBuffer() {
        return aliveCountBuffer;
    }

    public VulkanBufferAlloc emitterDescriptorBuffer() {
        return emitterDescriptorBuffer;
    }

    public VulkanBufferAlloc forceFieldBuffer() {
        return forceFieldBuffer;
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
