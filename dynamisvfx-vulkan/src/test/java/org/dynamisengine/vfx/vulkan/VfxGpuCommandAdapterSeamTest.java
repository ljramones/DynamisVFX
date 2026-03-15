package org.dynamisengine.vfx.vulkan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.dynamisengine.vfx.api.VfxFrameContext;
import org.dynamisengine.vfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisengine.vfx.vulkan.internal.gpu.VfxGpuCommandAdapter;
import org.junit.jupiter.api.Test;

class VfxGpuCommandAdapterSeamTest {
    @Test
    void resolveCommandBufferRoutesThroughInternalAdapter() {
        TrackingAdapter adapter = new TrackingAdapter();
        VulkanVfxDescriptorSetLayout layout = VulkanVfxDescriptorSetLayout.create(1L);
        VulkanVfxService service = new VulkanVfxService(1L, null, layout, adapter);

        long resolved = service.resolveCommandBuffer(new MinimalFrameContext());

        assertTrue(adapter.called);
        assertEquals(777L, resolved);

        service.destroy();
        layout.destroy(1L);
    }

    private static final class TrackingAdapter implements VfxGpuCommandAdapter {
        private boolean called;

        @Override
        public long commandBuffer(final VfxFrameContext frameContext) {
            this.called = true;
            return 777L;
        }
    }

    private static final class MinimalFrameContext implements VfxFrameContext {
        @Override
        public long commandBuffer() {
            return 42L;
        }

        @Override
        public float[] cameraView() {
            return new float[16];
        }

        @Override
        public float[] cameraProjection() {
            return new float[16];
        }

        @Override
        public float[] frustumPlanes() {
            return new float[24];
        }

        @Override
        public long frameIndex() {
            return 0L;
        }
    }
}
