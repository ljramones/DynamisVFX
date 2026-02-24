package org.dynamisvfx.vulkan.renderer;

import org.dynamisgpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;

import java.util.Objects;

public final class VulkanVfxRibbonHistoryBuffer {
    public static final int DEFAULT_HISTORY_LENGTH = 16;
    private static final int VEC4_BYTES = 16;

    private final VulkanBufferAlloc buffer;
    private final int maxParticles;
    private final int historyLength;

    private VulkanVfxRibbonHistoryBuffer(VulkanBufferAlloc buffer, int maxParticles, int historyLength) {
        this.buffer = buffer;
        this.maxParticles = maxParticles;
        this.historyLength = historyLength;
    }

    public static VulkanVfxRibbonHistoryBuffer allocate(
        VulkanMemoryOps memoryOps,
        int maxParticles,
        int historyLength
    ) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        if (maxParticles <= 0) {
            throw new IllegalArgumentException("maxParticles must be > 0");
        }
        if (historyLength <= 0) {
            throw new IllegalArgumentException("historyLength must be > 0");
        }

        memoryOps.getClass();
        VulkanBufferAlloc alloc = new VulkanBufferAlloc(0L, 0L);
        return new VulkanVfxRibbonHistoryBuffer(alloc, maxParticles, historyLength);
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        memoryOps.getClass();
    }

    public long bufferHandle() {
        return buffer.buffer();
    }

    public long memoryHandle() {
        return buffer.memory();
    }

    public int historyLength() {
        return historyLength;
    }

    public int maxParticles() {
        return maxParticles;
    }

    public long totalBytes() {
        return (long) maxParticles * historyLength * VEC4_BYTES;
    }
}
