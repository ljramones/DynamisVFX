package org.dynamisvfx.test.mock;

import org.dynamisgpu.api.gpu.DescriptorWriter;
import org.dynamisgpu.api.gpu.IndirectCommandBuffer;
import org.dynamisgpu.test.mock.MockDescriptorWriter;
import org.dynamisgpu.test.mock.MockIndirectCommandBuffer;
import org.dynamisvfx.api.VfxDrawContext;

import java.util.HashMap;
import java.util.Map;

public final class MockVfxDrawContext implements VfxDrawContext {
    private final MockIndirectCommandBuffer indirectBuffer = new MockIndirectCommandBuffer(1L, 2L, new int[] {0}, new int[] {4096});
    private final MockDescriptorWriter bindlessHeap = new MockDescriptorWriter();
    private final Map<Long, Integer> drawCallsPerFrame = new HashMap<>();
    private long frameIndex;

    @Override
    public IndirectCommandBuffer indirectBuffer() {
        return indirectBuffer;
    }

    @Override
    public DescriptorWriter bindlessHeap() {
        return bindlessHeap;
    }

    @Override
    public long frameIndex() {
        return frameIndex;
    }

    public MockVfxDrawContext frameIndex(long value) {
        this.frameIndex = value;
        return this;
    }

    public void recordDrawCalls(int count) {
        drawCallsPerFrame.merge(frameIndex, count, Integer::sum);
    }

    public int drawCallsForFrame(long frame) {
        return drawCallsPerFrame.getOrDefault(frame, 0);
    }

    public int totalDrawCalls() {
        return drawCallsPerFrame.values().stream().mapToInt(Integer::intValue).sum();
    }

    public void resetFrame(long frame) {
        drawCallsPerFrame.remove(frame);
    }
}
