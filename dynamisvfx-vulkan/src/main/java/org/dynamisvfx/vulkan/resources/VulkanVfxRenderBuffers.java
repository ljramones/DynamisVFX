package org.dynamisvfx.vulkan.resources;

import org.dynamisgpu.api.gpu.IndirectCommandBuffer;
import org.dynamisgpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxRenderBuffers {
    private static final int DEBRIS_CANDIDATE_BYTES = 48;
    private static final int INDIRECT_COMMAND_BYTES = 16;

    private final VulkanBufferAlloc drawIndexBuffer;
    private final VulkanBufferAlloc sortKeyBuffer;
    private final VulkanBufferAlloc[] debrisReadbackBuffers;
    private final VulkanBufferAlloc indirectCommandStagingBuffer;
    private final IndirectCommandBuffer indirectCommandBuffer;
    private final int usageFlags;

    private VulkanVfxRenderBuffers(
        VulkanBufferAlloc drawIndexBuffer,
        VulkanBufferAlloc sortKeyBuffer,
        VulkanBufferAlloc[] debrisReadbackBuffers,
        VulkanBufferAlloc indirectCommandStagingBuffer,
        IndirectCommandBuffer indirectCommandBuffer,
        int usageFlags
    ) {
        this.drawIndexBuffer = drawIndexBuffer;
        this.sortKeyBuffer = sortKeyBuffer;
        this.debrisReadbackBuffers = debrisReadbackBuffers;
        this.indirectCommandStagingBuffer = indirectCommandStagingBuffer;
        this.indirectCommandBuffer = indirectCommandBuffer;
        this.usageFlags = usageFlags;
    }

    public static VulkanVfxRenderBuffers allocate(
        VfxBufferConfig config,
        VulkanMemoryOps memoryOps,
        IndirectCommandBuffer indirectBuffer
    ) {
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(memoryOps, "memoryOps");
        Objects.requireNonNull(indirectBuffer, "indirectBuffer");

        int usage = VK10.VK_BUFFER_USAGE_STORAGE_BUFFER_BIT;

        VulkanBufferAlloc sortKey = config.needsSort()
            ? allocateStorageBuffer(memoryOps, config.maxParticles() * Float.BYTES, usage)
            : null;

        VulkanBufferAlloc[] debrisRing = new VulkanBufferAlloc[3];
        int debrisBytes = config.maxDebrisCandidates() * DEBRIS_CANDIDATE_BYTES;
        for (int i = 0; i < debrisRing.length; i++) {
            debrisRing[i] = allocateStorageBuffer(memoryOps, debrisBytes, usage);
        }

        return new VulkanVfxRenderBuffers(
            allocateStorageBuffer(memoryOps, config.maxParticles() * Integer.BYTES, usage),
            sortKey,
            debrisRing,
            allocateStorageBuffer(memoryOps, INDIRECT_COMMAND_BYTES, usage),
            indirectBuffer,
            usage
        );
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        indirectCommandBuffer.destroy();
        // Placeholder for explicit vkDestroyBuffer/vkFreeMemory once Vulkan device context is wired.
    }

    public VulkanBufferAlloc drawIndexBuffer() {
        return drawIndexBuffer;
    }

    public VulkanBufferAlloc sortKeyBuffer() {
        return sortKeyBuffer;
    }

    public VulkanBufferAlloc[] debrisReadbackBuffers() {
        return debrisReadbackBuffers.clone();
    }

    public VulkanBufferAlloc indirectCommandStagingBuffer() {
        return indirectCommandStagingBuffer;
    }

    public IndirectCommandBuffer indirectCommandBuffer() {
        return indirectCommandBuffer;
    }

    public int usageFlags() {
        return usageFlags;
    }

    private static VulkanBufferAlloc allocateStorageBuffer(VulkanMemoryOps memoryOps, int sizeBytes, int usageFlags) {
        memoryOps.getClass();
        return new VulkanBufferAlloc(0L, 0L);
    }
}
