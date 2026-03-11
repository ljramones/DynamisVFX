package org.dynamisvfx.test.mock;

import org.dynamisgpu.test.mock.MockDescriptorWriter;
import org.dynamisgpu.test.mock.MockIndirectCommandBuffer;
import org.dynamisvfx.api.VfxDescriptorBindingWriter;
import org.dynamisvfx.api.VfxDrawContext;
import org.dynamisvfx.api.VfxIndirectCommandSink;

import java.util.HashMap;
import java.util.Map;

public final class MockVfxDrawContext implements VfxDrawContext {
    private final MockIndirectCommandBuffer indirectBuffer = new MockIndirectCommandBuffer(1L, 2L, new int[] {0}, new int[] {4096});
    private final MockDescriptorWriter bindlessHeap = new MockDescriptorWriter();
    private final VfxIndirectCommandSink indirectSink = new VfxIndirectCommandSink() {
        @Override
        public void writeCommand(int slot, int indexCount, int instanceCount, int firstIndex, int vertexOffset, int firstInstance) {
            indirectBuffer.writeCommand(slot, indexCount, instanceCount, firstIndex, vertexOffset, firstInstance);
        }

        @Override
        public long bufferHandle() {
            return indirectBuffer.bufferHandle();
        }

        @Override
        public long countBufferHandle() {
            return indirectBuffer.countBufferHandle();
        }

        @Override
        public int variantOffset(int variantIndex) {
            return indirectBuffer.variantOffset(variantIndex);
        }

        @Override
        public int variantCapacity(int variantIndex) {
            return indirectBuffer.variantCapacity(variantIndex);
        }

        @Override
        public void destroy() {
            indirectBuffer.destroy();
        }
    };
    private final VfxDescriptorBindingWriter descriptorWriter = new VfxDescriptorBindingWriter() {
        @Override
        public void writeStorageBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range) {
            bindlessHeap.writeStorageBuffer(descriptorSet, binding, arrayElement, bufferHandle, offset, range);
        }

        @Override
        public void writeUniformBuffer(long descriptorSet, int binding, int arrayElement, long bufferHandle, long offset, long range) {
            bindlessHeap.writeUniformBuffer(descriptorSet, binding, arrayElement, bufferHandle, offset, range);
        }

        @Override
        public void writeSampledImage(long descriptorSet, int binding, int arrayElement, long imageView, long sampler) {
            bindlessHeap.writeSampledImage(descriptorSet, binding, arrayElement, imageView, sampler);
        }
    };
    private final Map<Long, Integer> drawCallsPerFrame = new HashMap<>();
    private long frameIndex;

    @Override
    public VfxIndirectCommandSink indirectCommandSink() {
        return indirectSink;
    }

    @Override
    public VfxDescriptorBindingWriter bindlessHeapWriter() {
        return descriptorWriter;
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
