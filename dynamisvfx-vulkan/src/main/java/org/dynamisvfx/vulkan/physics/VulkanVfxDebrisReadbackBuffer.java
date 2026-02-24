package org.dynamisvfx.vulkan.physics;

import org.dynamisgpu.vulkan.memory.VulkanBufferAlloc;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VulkanVfxDebrisReadbackBuffer {
    public static final int HEADER_BYTES = 16;
    public static final int DEFAULT_MAX_CANDIDATES = 256;

    private final VulkanBufferAlloc buffer;
    private final int maxCandidates;
    private final ByteBuffer hostMirror;

    private VulkanVfxDebrisReadbackBuffer(VulkanBufferAlloc buffer, int maxCandidates, ByteBuffer hostMirror) {
        this.buffer = buffer;
        this.maxCandidates = maxCandidates;
        this.hostMirror = hostMirror;
    }

    public static VulkanVfxDebrisReadbackBuffer allocate(VulkanMemoryOps memoryOps, int maxCandidates) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        return allocateInternal(maxCandidates);
    }

    public static VulkanVfxDebrisReadbackBuffer allocateForTest(int maxCandidates) {
        return allocateInternal(maxCandidates);
    }

    private static VulkanVfxDebrisReadbackBuffer allocateInternal(int maxCandidates) {
        if (maxCandidates <= 0) {
            throw new IllegalArgumentException("maxCandidates must be > 0");
        }
        int bytes = HEADER_BYTES + (maxCandidates * VulkanVfxDebrisCandidate.SIZE_BYTES);
        ByteBuffer mirror = ByteBuffer.allocateDirect(bytes).order(ByteOrder.LITTLE_ENDIAN);
        mirror.putInt(0).putInt(0).putInt(0).putInt(0);
        mirror.position(0);
        return new VulkanVfxDebrisReadbackBuffer(new VulkanBufferAlloc(0L, 0L), maxCandidates, mirror);
    }

    public List<VulkanVfxDebrisCandidate> readCandidates(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        return readCandidates();
    }

    public List<VulkanVfxDebrisCandidate> readCandidates() {

        ByteBuffer read = hostMirror.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        int count = Math.max(0, Math.min(maxCandidates, read.getInt(0)));
        List<VulkanVfxDebrisCandidate> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int offset = HEADER_BYTES + (i * VulkanVfxDebrisCandidate.SIZE_BYTES);
            out.add(VulkanVfxDebrisCandidate.readFrom(read, offset));
        }
        return out;
    }

    public void resetOnGpu(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        hostMirror.putInt(0, 0);
    }

    public void writeMockCandidates(List<VulkanVfxDebrisCandidate> candidates) {
        Objects.requireNonNull(candidates, "candidates");
        int count = Math.min(maxCandidates, candidates.size());
        hostMirror.putInt(0, count);
        for (int i = 0; i < count; i++) {
            int offset = HEADER_BYTES + (i * VulkanVfxDebrisCandidate.SIZE_BYTES);
            writeCandidate(hostMirror, offset, candidates.get(i));
        }
    }

    public void destroy(VulkanMemoryOps memoryOps) {
        Objects.requireNonNull(memoryOps, "memoryOps");
        memoryOps.getClass();
        hostMirror.putInt(0, 0);
    }

    public long bufferHandle() {
        return buffer.buffer();
    }

    public int maxCandidates() {
        return maxCandidates;
    }

    private static void writeCandidate(ByteBuffer target, int offset, VulkanVfxDebrisCandidate c) {
        ByteBuffer out = target.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        out.position(offset);
        out.putFloat(c.px());
        out.putFloat(c.py());
        out.putFloat(c.pz());
        out.putFloat(c.mass());
        out.putFloat(c.vx());
        out.putFloat(c.vy());
        out.putFloat(c.vz());
        out.putFloat(c.angularSpeed());
        out.putInt(c.meshId());
        out.putInt(c.materialTag());
        out.putInt(c.emitterId());
        out.putInt(c.flags());
    }
}
