package org.dynamisvfx.vulkan.parity;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.vulkan.compute.VulkanVfxSortStage;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(named = "dle.vfx.parity.tests", matches = "true")
class VulkanVfxParityTest {

    @Test
    void burstEmitterReachesExpectedCount() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(500, 1.0f, 2.0f);
        int[] alive = VulkanVfxMockRuntime.run(descriptor, 1, 0.016f, 12345L);
        assertEquals(500, alive[0]);
    }

    @Test
    void continuousEmitterGrowsMonotonically() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.continuousDescriptor(100f, 2.0f);
        int[] alive = VulkanVfxMockRuntime.run(descriptor, 60, 0.016f, 42L);

        for (int i = 1; i < 30; i++) {
            assertTrue(alive[i] >= alive[i - 1], "alive count must be monotonic in early growth phase");
        }

        int firstHalfIncrease = alive[29] - alive[0];
        int secondHalfIncrease = alive[59] - alive[29];
        assertTrue(secondHalfIncrease <= firstHalfIncrease + 1, "growth should stabilize by step 60");
    }

    @Test
    void particlesRetireAfterLifetime() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(100, 0.5f, 0.5f);
        int[] alive = VulkanVfxMockRuntime.run(descriptor, 40, 0.016f, 100L);

        assertTrue(alive[30] <= 5, "alive count around 0.5s should be near zero");
        assertEquals(0, alive[39]);
    }

    @Test
    void deterministicSameSeedSameResult() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(200, 0.8f, 1.4f);
        int[] first = VulkanVfxMockRuntime.run(descriptor, 60, 0.016f, 777L);
        int[] second = VulkanVfxMockRuntime.run(descriptor, 60, 0.016f, 777L);

        assertArrayEquals(first, second);
    }

    @Test
    void additiveBlendSkipsSortStage() {
        VulkanVfxDescriptorSetLayout layout = VulkanVfxDescriptorSetLayout.create(1L);
        VulkanVfxSortStage sortStage = VulkanVfxSortStage.create(1L, layout, 1024);
        sortStage.dispatchWithMockDistances(1L, BlendMode.ADDITIVE, new float[] {4f, 1f, 9f});

        assertTrue(sortStage.lastSkipped(), "additive blend must skip sort");
        assertEquals(0, sortStage.lastSortedKeys().length);
        assertEquals(0, sortStage.lastSortedIndices().length);

        sortStage.destroy(1L);
        layout.destroy(1L);
    }

    @Test
    void sortedOutputKeysAreMonotonic() {
        VulkanVfxDescriptorSetLayout layout = VulkanVfxDescriptorSetLayout.create(1L);
        VulkanVfxSortStage sortStage = VulkanVfxSortStage.create(1L, layout, 1024);
        sortStage.dispatchWithMockDistances(1L, BlendMode.ALPHA, new float[] {
            25f, 9f, 16f, 1f, 36f, 4f
        });

        int[] sorted = sortStage.lastSortedKeys();
        assertTrue(sorted.length > 0);
        for (int i = 1; i < sorted.length; i++) {
            assertTrue(sorted[i] >= sorted[i - 1], "sorted keys must be non-decreasing");
        }

        sortStage.destroy(1L);
        layout.destroy(1L);
    }

    @Test
    void cullCompactProducesNonZeroDrawCount() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(500, 1.0f, 2.0f);
        int instanceCount = VulkanVfxMockRuntime.runCullCompactDrawCount(descriptor, 0.016f, 12345L);
        assertTrue(instanceCount > 0, "instanceCount should be > 0 for a visible burst");
    }

    @Test
    void debrisHandoffFiredAfterTwoFrameDelay() {
        int[] events = VulkanVfxMockRuntime.runDebrisHandoffTimeline(4);
        assertEquals(0, events[0], "no debris events should be readable on frame 1");
        assertEquals(0, events[1], "no debris events should be readable on frame 2");
        assertTrue(events[2] > 0, "debris events should appear on frame 3+");
        assertTrue(events[3] > 0, "debris events should continue on subsequent frames");
    }
}
