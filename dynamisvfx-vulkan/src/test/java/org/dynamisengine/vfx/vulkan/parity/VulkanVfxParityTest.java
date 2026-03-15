package org.dynamisengine.vfx.vulkan.parity;

import org.dynamisengine.vfx.api.BlendMode;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.core.builder.EffectBuilder;
import org.dynamisengine.vfx.core.builder.EmissionRate;
import org.dynamisengine.vfx.core.builder.EmitterShape;
import org.dynamisengine.vfx.core.builder.Force;
import org.dynamisengine.vfx.core.builder.ParticleInit;
import org.dynamisengine.vfx.core.builder.Renderer;
import org.dynamisengine.vfx.vulkan.budget.VfxBudgetAllocation;
import org.dynamisengine.vfx.vulkan.budget.VfxBudgetAllocator;
import org.dynamisengine.vfx.vulkan.budget.VfxBudgetPolicy;
import org.dynamisengine.vfx.vulkan.hotreload.VfxReloadCategory;
import org.dynamisengine.vfx.vulkan.compute.VulkanVfxSortStage;
import org.dynamisengine.vfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
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

    // ── Renderer Type Coverage ──────────────────────────────────────────

    @Test
    void ribbonRendererProducesDrawCalls() {
        ParticleEmitterDescriptor descriptor = EffectBuilder.emitter("ribbon")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(100f))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.5f, 1.0f)
                .sizeRange(0.1f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.ribbon().blend(BlendMode.ALPHA).trailLength(8).build())
            .build();

        int drawCount = VulkanVfxMockRuntime.runCullCompactDrawCount(descriptor, 0.016f, 999L);
        assertTrue(drawCount > 0, "ribbon renderer should produce draw calls");
    }

    @Test
    void meshRendererProducesDrawCalls() {
        ParticleEmitterDescriptor descriptor = EffectBuilder.emitter("meshR")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.burst(50))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.5f, 1.0f)
                .sizeRange(0.1f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.mesh().blend(BlendMode.ALPHA).build())
            .build();

        int drawCount = VulkanVfxMockRuntime.runCullCompactDrawCount(descriptor, 0.016f, 1001L);
        assertTrue(drawCount > 0, "mesh renderer should produce draw calls");
    }

    @Test
    void beamRendererProducesDrawCalls() {
        ParticleEmitterDescriptor descriptor = EffectBuilder.emitter("beamR")
            .shape(EmitterShape.point())
            .rate(EmissionRate.burst(20))
            .init(ParticleInit.builder()
                .lifetime(2.0f, 2.0f)
                .velocityRange(0.2f, 0.5f)
                .sizeRange(0.05f, 0.1f)
                .build())
            .force(Force.gravity(4.0f))
            .renderer(Renderer.beam().blend(BlendMode.ADDITIVE).build())
            .build();

        int drawCount = VulkanVfxMockRuntime.runCullCompactDrawCount(descriptor, 0.016f, 2002L);
        assertTrue(drawCount > 0, "beam renderer should produce draw calls");
    }

    @Test
    void decalRendererProducesDrawCalls() {
        ParticleEmitterDescriptor descriptor = EffectBuilder.emitter("decalR")
            .shape(EmitterShape.sphere(1.0f))
            .rate(EmissionRate.burst(30))
            .init(ParticleInit.builder()
                .lifetime(3.0f, 3.0f)
                .velocityRange(0.0f, 0.1f)
                .sizeRange(0.2f, 0.5f)
                .build())
            .force(Force.gravity(0.5f))
            .renderer(Renderer.decal().blend(BlendMode.ALPHA).build())
            .build();

        int drawCount = VulkanVfxMockRuntime.runCullCompactDrawCount(descriptor, 0.016f, 3003L);
        assertTrue(drawCount > 0, "decal renderer should produce draw calls");
    }

    // ── Emitter Shape Coverage ──────────────────────────────────────────

    @Test
    void coneShapeEmitsParticles() {
        ParticleEmitterDescriptor descriptor = EffectBuilder.emitter("cone")
            .shape(EmitterShape.cone(0.5f, 2.0f))
            .rate(EmissionRate.burst(200))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.5f, 1.0f)
                .sizeRange(0.1f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        int[] alive = VulkanVfxMockRuntime.run(descriptor, 1, 0.016f, 5555L);
        assertEquals(200, alive[0], "cone shape burst should emit exact count");
    }

    @Test
    void meshSurfaceShapeEmitsParticles() {
        ParticleEmitterDescriptor descriptor = EffectBuilder.emitter("meshSurf")
            .shape(EmitterShape.meshSurface("test_mesh_id"))
            .rate(EmissionRate.burst(100))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.5f, 1.0f)
                .sizeRange(0.1f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        int[] alive = VulkanVfxMockRuntime.run(descriptor, 1, 0.016f, 6666L);
        assertEquals(100, alive[0], "meshSurface shape burst should emit exact count");
    }

    // ── Force Combinations ──────────────────────────────────────────────

    @Test
    void gravityPlusDragStabilizesVelocity() {
        ParticleEmitterDescriptor gravityOnly = EffectBuilder.emitter("gOnly")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(2.0f, 2.0f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        ParticleEmitterDescriptor gravityPlusDrag = EffectBuilder.emitter("gDrag")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(2.0f, 2.0f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.drag(2.0f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        float sigGravity = VulkanVfxMockRuntime.runPositionSignatureWithForces(gravityOnly, 60, 1f / 60f, 111L);
        float sigGravDrag = VulkanVfxMockRuntime.runPositionSignatureWithForces(gravityPlusDrag, 60, 1f / 60f, 111L);
        assertTrue(Math.abs(sigGravity - sigGravDrag) > 0.01f,
            "gravity+drag should produce different signature than gravity alone");
    }

    @Test
    void multipleForcesCombine() {
        ParticleEmitterDescriptor singleForce = EffectBuilder.emitter("single")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(1.5f, 1.5f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        ParticleEmitterDescriptor tripleForce = EffectBuilder.emitter("triple")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(1.5f, 1.5f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.drag(1.0f))
            .force(Force.wind(3.0f, 1.0f, 0.0f, 0.0f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        float sigSingle = VulkanVfxMockRuntime.runPositionSignatureWithForces(singleForce, 30, 1f / 60f, 222L);
        float sigTriple = VulkanVfxMockRuntime.runPositionSignatureWithForces(tripleForce, 30, 1f / 60f, 222L);
        assertNotEquals(sigSingle, sigTriple, "3 forces should differ from single force");
    }

    @Test
    void windForceAffectsTrajectory() {
        ParticleEmitterDescriptor gravityOnly = EffectBuilder.emitter("noWind")
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

        ParticleEmitterDescriptor withWind = EffectBuilder.emitter("withWind")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.continuous(120f))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 1.0f)
                .velocityRange(0.2f, 0.4f)
                .sizeRange(0.1f, 0.1f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.wind(5.0f, 1.0f, 0.0f, 0.0f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();

        float sigNoWind = VulkanVfxMockRuntime.runPositionSignatureWithForces(gravityOnly, 30, 1f / 60f, 333L);
        float sigWind = VulkanVfxMockRuntime.runPositionSignatureWithForces(withWind, 30, 1f / 60f, 333L);
        assertTrue(Math.abs(sigNoWind - sigWind) > 0.01f,
            "wind force should change position signature vs gravity-only");
    }

    // ── Frame Timing Edge Cases ─────────────────────────────────────────

    @Test
    void verySmallDeltaTimeStillEmits() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.continuousDescriptor(1000f, 2.0f);
        int[] alive = VulkanVfxMockRuntime.run(descriptor, 10, 0.001f, 7777L);
        assertTrue(alive[9] > 0, "1ms deltaTime with high rate should still emit particles");
    }

    @Test
    void largeDeltaTimeDoesNotLoseParticles() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(500, 2.0f, 2.0f);
        int[] alive = VulkanVfxMockRuntime.run(descriptor, 5, 0.1f, 8888L);

        // After 0.5s total with 2s lifetime, all particles should still be alive
        assertEquals(500, alive[0], "burst should spawn all on frame 0");
        assertTrue(alive[4] > 0, "particles with 2s lifetime should survive 0.5s of 100ms steps");
    }

    @Test
    void variableDeltaTimeProducesSameLifetime() {
        // Burst particles with fixed 1s lifetime, variable dt summing to ~1s
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(200, 1.0f, 1.0f);

        // Build variable dt array: alternating 8ms, 16ms, 32ms for ~120 steps
        float[] variableDt = new float[120];
        float[] options = {0.008f, 0.016f, 0.032f};
        for (int i = 0; i < variableDt.length; i++) {
            variableDt[i] = options[i % 3];
        }

        int[] alive = VulkanVfxMockRuntime.runWithVariableDeltaTime(descriptor, variableDt, 9999L);

        // Particles should eventually all retire
        assertEquals(0, alive[alive.length - 1], "particles should retire even with variable dt");
        // Early frames should have particles alive
        assertTrue(alive[0] > 0, "particles should be alive on first frame");
    }

    // ── High Volume ─────────────────────────────────────────────────────

    @Test
    void maxParticlesBurstDoesNotExceedBudget() {
        int budget = 65_536;
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(budget, 2.0f, 2.0f);
        int[] alive = VulkanVfxMockRuntime.run(descriptor, 5, 0.016f, 11111L);

        for (int count : alive) {
            assertTrue(count <= budget,
                "alive count " + count + " must not exceed budget " + budget);
        }
    }

    @Test
    void rapidSpawnDespawnCycleStable() {
        ParticleEmitterDescriptor descriptor = VulkanVfxMockRuntime.burstDescriptor(100, 0.08f, 0.08f);

        for (int cycle = 0; cycle < 10; cycle++) {
            int[] alive = VulkanVfxMockRuntime.run(descriptor, 10, 0.016f, 12345L + cycle);
            // Each cycle should end with zero particles (0.08s life, 10 * 16ms = 0.16s)
            assertEquals(0, alive[9],
                "cycle " + cycle + ": all particles should be retired by step 10");
        }
    }

    // ── Reload Edge Cases ───────────────────────────────────────────────

    @Test
    void rendererChangedCategoryTriggersCorrectly() {
        ParticleEmitterDescriptor base = VulkanVfxMockRuntime.burstDescriptor(100, 1.0f, 2.0f);
        VulkanVfxMockRuntime.ReloadFixture fixture = VulkanVfxMockRuntime.createReloadFixture(base);
        try {
            // Changing renderer type (billboard -> ribbon) is a structural change that
            // triggers FULL_RESPAWN rather than RENDERER_CHANGED, because the vertex
            // layout and pipeline differ between renderer types.
            ParticleEmitterDescriptor updated = VulkanVfxMockRuntime.withRenderer(
                base, Renderer.ribbon().blend(BlendMode.ALPHA).trailLength(4).build());
            VfxReloadCategory category = fixture.service.reloadEffect(fixture.handle, updated);

            assertEquals(VfxReloadCategory.FULL_RESPAWN, category,
                "changing billboard to ribbon should trigger FULL_RESPAWN");
            assertTrue(!fixture.service.isHandleAlive(fixture.handle),
                "old handle should be dead after full respawn");
            assertTrue(fixture.service.lastRespawnedHandle() != null,
                "a new handle should be created");
        } finally {
            fixture.close();
        }
    }

    @Test
    void rendererBlendModeChangeTriggersRendererChanged() {
        ParticleEmitterDescriptor base = VulkanVfxMockRuntime.burstDescriptor(100, 1.0f, 2.0f);
        VulkanVfxMockRuntime.ReloadFixture fixture = VulkanVfxMockRuntime.createReloadFixture(base);
        try {
            // Changing only the blend mode (same renderer type) should trigger RENDERER_CHANGED
            ParticleEmitterDescriptor updated = VulkanVfxMockRuntime.withRenderer(
                base, Renderer.billboard().blend(BlendMode.ADDITIVE).build());
            VfxReloadCategory category = fixture.service.reloadEffect(fixture.handle, updated);

            assertEquals(VfxReloadCategory.RENDERER_CHANGED, category,
                "changing blend mode within same renderer type should trigger RENDERER_CHANGED");
            assertTrue(fixture.service.isHandleAlive(fixture.handle),
                "handle should remain alive for RENDERER_CHANGED");
        } finally {
            fixture.close();
        }
    }

    // ── Physics Handoff ─────────────────────────────────────────────────

    @Test
    void multipleDebrisEventsInSequence() {
        int steps = 8;
        int[] events = VulkanVfxMockRuntime.runDebrisHandoffTimeline(steps);

        // First two frames have zero readback due to ring buffer delay
        assertEquals(0, events[0], "no debris on frame 0");
        assertEquals(0, events[1], "no debris on frame 1");

        // Frames 2+ should each have debris from writes 2 frames prior
        int debrisFrames = 0;
        for (int i = 2; i < steps; i++) {
            if (events[i] > 0) {
                debrisFrames++;
            }
        }
        assertTrue(debrisFrames >= steps - 3,
            "debris events should appear on most frames after initial delay");
    }
}
