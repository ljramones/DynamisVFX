package org.dynamisvfx.vulkan.parity;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.core.builder.EffectBuilder;
import org.dynamisvfx.core.builder.EmissionRate;
import org.dynamisvfx.core.builder.EmitterShape;
import org.dynamisvfx.core.builder.Force;
import org.dynamisvfx.core.builder.ParticleInit;
import org.dynamisvfx.core.builder.Renderer;
import org.dynamisvfx.vulkan.budget.VfxBudgetAllocation;
import org.dynamisvfx.vulkan.budget.VfxBudgetAllocator;
import org.dynamisvfx.vulkan.budget.VfxBudgetPolicy;
import org.dynamisvfx.vulkan.hotreload.VfxReloadCategory;
import org.dynamisvfx.vulkan.compute.VulkanVfxSortStage;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void forcesOnlyReloadDoesNotRespawn() {
        ParticleEmitterDescriptor base = VulkanVfxMockRuntime.burstDescriptor(100, 1.0f, 2.0f);
        VulkanVfxMockRuntime.ReloadFixture fixture = VulkanVfxMockRuntime.createReloadFixture(base);
        try {
            ParticleEmitterDescriptor updated = VulkanVfxMockRuntime.withForceStrength(base, 14.0f);
            VfxReloadCategory category = fixture.service.reloadEffect(fixture.handle, updated);

            assertEquals(VfxReloadCategory.FORCES_ONLY, category);
            assertTrue(fixture.service.isHandleAlive(fixture.handle));
            assertEquals(null, fixture.service.lastRespawnedHandle());
        } finally {
            fixture.close();
        }
    }

    @Test
    void fullRespawnReloadRestartsEffect() {
        ParticleEmitterDescriptor base = VulkanVfxMockRuntime.burstDescriptor(100, 1.0f, 2.0f);
        VulkanVfxMockRuntime.ReloadFixture fixture = VulkanVfxMockRuntime.createReloadFixture(base);
        try {
            ParticleEmitterDescriptor updated = VulkanVfxMockRuntime.withBurstCount(base, 500);
            VfxReloadCategory category = fixture.service.reloadEffect(fixture.handle, updated);

            assertEquals(VfxReloadCategory.FULL_RESPAWN, category);
            assertTrue(!fixture.service.isHandleAlive(fixture.handle));
            assertTrue(fixture.service.lastRespawnedHandle() != null);
            assertTrue(fixture.service.isHandleAlive(fixture.service.lastRespawnedHandle()));
        } finally {
            fixture.close();
        }
    }

    @Test
    void spawnRejectsWhenBudgetExhausted() {
        VfxBudgetAllocator allocator = new VfxBudgetAllocator(100, VfxBudgetPolicy.REJECT);
        VfxBudgetAllocation first = allocator.allocate(100, id -> {});
        VfxBudgetAllocation second = allocator.allocate(1, id -> {});
        assertTrue(first != null);
        assertNull(second);
    }

    @Test
    void spawnClampsToRemainingBudget() {
        VfxBudgetAllocator allocator = new VfxBudgetAllocator(100, VfxBudgetPolicy.CLAMP);
        VfxBudgetAllocation first = allocator.allocate(60, id -> {});
        VfxBudgetAllocation second = allocator.allocate(60, id -> {});
        assertTrue(first != null);
        assertEquals(60, first.allocatedParticles());
        assertTrue(second != null);
        assertEquals(40, second.allocatedParticles());
    }

    @Test
    void evictOldestMakesRoomForNew() {
        VfxBudgetAllocator allocator = new VfxBudgetAllocator(100, VfxBudgetPolicy.EVICT_OLDEST);
        final int[] evicted = new int[1];
        VfxBudgetAllocation a = allocator.allocate(100, id -> evicted[0] = id);
        VfxBudgetAllocation b = allocator.allocate(100, id -> evicted[0] = id);
        assertTrue(a != null);
        assertTrue(b != null);
        assertEquals(a.allocationId(), evicted[0]);
    }

    @Test
    void curlNoiseForceAffectsParticleVelocity() {
        ParticleEmitterDescriptor withCurl = EffectBuilder.emitter("curl")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.curlNoise(0.02f, 2.0f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        ParticleEmitterDescriptor withoutCurl = EffectBuilder.emitter("nocurl")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        float withNoise = VulkanVfxMockRuntime.runPositionSignature(withCurl, 10, 1f / 60f, 777L);
        float withoutNoise = VulkanVfxMockRuntime.runPositionSignature(withoutCurl, 10, 1f / 60f, 777L);
        assertTrue(Math.abs(withNoise - withoutNoise) > 1e-4f);
    }
}
