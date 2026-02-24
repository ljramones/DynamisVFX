package org.dynamisvfx.vulkan.parity;

import org.dynamisvfx.api.EmissionRateDescriptor;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.core.builder.EffectBuilder;
import org.dynamisvfx.core.builder.EmissionRate;
import org.dynamisvfx.core.builder.EmitterShape;
import org.dynamisvfx.core.builder.Force;
import org.dynamisvfx.core.builder.ParticleInit;
import org.dynamisvfx.core.builder.Renderer;
import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.compute.VulkanVfxEmitStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxCullCompactStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxRetireStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxSimulateStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxSpawnScheduler;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class VulkanVfxMockRuntime {
    private static final float RETIRE_THRESHOLD = 0.99f;

    private VulkanVfxMockRuntime() {
    }

    public static int[] run(
        ParticleEmitterDescriptor descriptor,
        int steps,
        float deltaTime,
        long seed
    ) {
        if (steps <= 0) {
            return new int[0];
        }

        // Instantiated here to keep parity harness aligned with Stage 1-3 construction path.
        VulkanVfxDescriptorSetLayout layout = VulkanVfxDescriptorSetLayout.create(1L);
        VulkanVfxRetireStage retireStage = VulkanVfxRetireStage.create(1L, layout);
        VulkanVfxEmitStage emitStage = VulkanVfxEmitStage.create(1L, layout);
        VulkanVfxSimulateStage simulateStage = VulkanVfxSimulateStage.create(1L, layout);

        VulkanVfxSpawnScheduler scheduler = new VulkanVfxSpawnScheduler();
        Random random = new Random(seed);
        int maxParticles = 65_536;

        List<Float> normalizedAges = new ArrayList<>();
        int[] aliveCounts = new int[steps];

        for (int step = 0; step < steps; step++) {
            // Stage 1: RETIRE
            normalizedAges.removeIf(age -> age >= RETIRE_THRESHOLD);

            // Stage 2: EMIT
            int freeSlots = maxParticles - normalizedAges.size();
            int spawnCount = scheduler.computeSpawnCount(descriptor.rate(), deltaTime, freeSlots);

            for (int i = 0; i < spawnCount; i++) {
                float lifetime = sampleLifetime(descriptor, random);
                float ageAdvance = deltaTime / Math.max(lifetime, 0.001f);
                normalizedAges.add(ageAdvance);
            }

            // Stage 3: SIMULATE
            for (int i = 0; i < normalizedAges.size(); i++) {
                float lifetime = sampleLifetime(descriptor, random);
                float age = normalizedAges.get(i);
                age += deltaTime / Math.max(lifetime, 0.001f);
                normalizedAges.set(i, age);
            }

            aliveCounts[step] = normalizedAges.size();

            // Touch stage objects to keep static analysis from stripping assumptions.
            retireStage.lastDispatchGroupCount();
            emitStage.lastDispatchGroupCount();
            simulateStage.lastDispatchGroupCount();
        }

        retireStage.destroy(1L);
        emitStage.destroy(1L);
        simulateStage.destroy(1L);
        layout.destroy(1L);

        return aliveCounts;
    }

    private static float sampleLifetime(ParticleEmitterDescriptor descriptor, Random random) {
        float min = descriptor.init().lifetimeMinSeconds();
        float max = descriptor.init().lifetimeMaxSeconds();
        if (Math.abs(max - min) < 1e-6f) {
            return min;
        }
        return min + random.nextFloat() * (max - min);
    }

    // Shared fixture builders used by parity tests.
    static ParticleEmitterDescriptor burstDescriptor(int burstCount, float lifeMin, float lifeMax) {
        return EffectBuilder.emitter("burst")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.burst(burstCount))
            .init(ParticleInit.builder()
                .lifetime(lifeMin, lifeMax)
                .velocityRange(0.5f, 1.0f)
                .sizeRange(0.1f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.drag(0.25f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();
    }

    static ParticleEmitterDescriptor continuousDescriptor(float particlesPerSecond, float lifeSeconds) {
        return EffectBuilder.emitter("continuous")
            .shape(EmitterShape.point())
            .rate(EmissionRate.continuous(particlesPerSecond))
            .init(ParticleInit.builder()
                .lifetime(lifeSeconds, lifeSeconds)
                .velocityRange(0.2f, 0.8f)
                .sizeRange(0.05f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.drag(0.3f))
            .renderer(Renderer.billboard().blend(BlendMode.ALPHA).build())
            .build();
    }

    static int runCullCompactDrawCount(ParticleEmitterDescriptor descriptor, float deltaTime, long seed) {
        int[] alive = run(descriptor, 1, deltaTime, seed);
        VulkanVfxDescriptorSetLayout layout = VulkanVfxDescriptorSetLayout.create(1L);
        VulkanVfxCullCompactStage cullStage = VulkanVfxCullCompactStage.create(1L, layout);

        float[] permissiveFrustum = new float[] {
            1f, 0f, 0f, 10000f,
            -1f, 0f, 0f, 10000f,
            0f, 1f, 0f, 10000f,
            0f, -1f, 0f, 10000f,
            0f, 0f, 1f, 10000f,
            0f, 0f, -1f, 10000f
        };

        int drawCount = cullStage.dispatchWithMockAliveCount(1L, alive[0], false, permissiveFrustum);
        cullStage.destroy(1L);
        layout.destroy(1L);
        return drawCount;
    }
}
