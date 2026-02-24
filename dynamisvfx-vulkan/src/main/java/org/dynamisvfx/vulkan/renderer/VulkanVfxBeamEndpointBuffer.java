package org.dynamisvfx.vulkan.renderer;

import org.dynamisgpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;

import java.util.Objects;

public final class VulkanVfxBeamEndpointBuffer {
    public static final int STRIDE_BYTES = 64;
    public static final int DEFAULT_MAX_BEAMS = 256;

    private final VulkanBufferAlloc buffer;
    private final int maxBeams;

    private float[] lastUploadedData = new float[0];
    private int lastBeamCount;

    private VulkanVfxBeamEndpointBuffer(VulkanBufferAlloc buffer, int maxBeams) {
        this.buffer = buffer;
        this.maxBeams = maxBeams;
    }

    public static VulkanVfxBeamEndpointBuffer allocate(VulkanMemoryOps memoryOps, int maxBeams) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        if (maxBeams <= 0) {
            throw new IllegalArgumentException("maxBeams must be > 0");
        }
        memoryOps.getClass();
        return new VulkanVfxBeamEndpointBuffer(new VulkanBufferAlloc(0L, 0L), maxBeams);
    }

    public void upload(long commandBuffer, float[] endpointData, int beamCount, VulkanMemoryOps memoryOps) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(endpointData, "endpointData");
        Objects.requireNonNull(memoryOps, "memoryOps");
        if (beamCount < 0 || beamCount > maxBeams) {
            throw new IllegalArgumentException("beamCount must be in [0, maxBeams]");
        }

        int requiredFloats = beamCount * (STRIDE_BYTES / Float.BYTES);
        if (endpointData.length < requiredFloats) {
            throw new IllegalArgumentException("endpointData is too small for beamCount");
        }

        memoryOps.getClass();
        lastUploadedData = new float[requiredFloats];
        System.arraycopy(endpointData, 0, lastUploadedData, 0, requiredFloats);
        lastBeamCount = beamCount;
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        memoryOps.getClass();
        lastUploadedData = new float[0];
        lastBeamCount = 0;
    }

    public long bufferHandle() {
        return buffer.buffer();
    }

    public int maxBeams() {
        return maxBeams;
    }

    public int lastBeamCount() {
        return lastBeamCount;
    }

    public float[] lastUploadedData() {
        return lastUploadedData.clone();
    }
}
