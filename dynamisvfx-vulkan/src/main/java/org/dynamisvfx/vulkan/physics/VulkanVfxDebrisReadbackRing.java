package org.dynamisvfx.vulkan.physics;

import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;

import java.util.Objects;

public final class VulkanVfxDebrisReadbackRing {
    private static final int RING_SIZE = 3;
    private final VulkanVfxDebrisReadbackBuffer[] ring;

    private VulkanVfxDebrisReadbackRing(VulkanVfxDebrisReadbackBuffer[] ring) {
        this.ring = ring;
    }

    public static VulkanVfxDebrisReadbackRing allocate(
        VulkanMemoryOps memoryOps,
        int maxCandidatesPerFrame
    ) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        VulkanVfxDebrisReadbackBuffer[] ring = new VulkanVfxDebrisReadbackBuffer[RING_SIZE];
        for (int i = 0; i < RING_SIZE; i++) {
            ring[i] = VulkanVfxDebrisReadbackBuffer.allocate(memoryOps, maxCandidatesPerFrame);
        }
        return new VulkanVfxDebrisReadbackRing(ring);
    }

    public static VulkanVfxDebrisReadbackRing allocateForTest(int maxCandidatesPerFrame) {
        VulkanVfxDebrisReadbackBuffer[] ring = new VulkanVfxDebrisReadbackBuffer[RING_SIZE];
        for (int i = 0; i < RING_SIZE; i++) {
            ring[i] = VulkanVfxDebrisReadbackBuffer.allocateForTest(maxCandidatesPerFrame);
        }
        return new VulkanVfxDebrisReadbackRing(ring);
    }

    public VulkanVfxDebrisReadbackBuffer writeBuffer(long frameIndex) {
        return ring[Math.floorMod((int) frameIndex, RING_SIZE)];
    }

    public VulkanVfxDebrisReadbackBuffer readBuffer(long frameIndex) {
        return ring[Math.floorMod((int) (frameIndex - 2), RING_SIZE)];
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        for (VulkanVfxDebrisReadbackBuffer buffer : ring) {
            buffer.destroy(memoryOps);
        }
    }
}
