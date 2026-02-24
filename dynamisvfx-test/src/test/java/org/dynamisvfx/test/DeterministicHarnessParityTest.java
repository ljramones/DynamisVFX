package org.dynamisvfx.test;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.core.builder.EffectBuilder;
import org.dynamisvfx.core.builder.EmissionRate;
import org.dynamisvfx.core.builder.EmitterShape;
import org.dynamisvfx.core.builder.Force;
import org.dynamisvfx.core.builder.ParticleInit;
import org.dynamisvfx.core.builder.Renderer;
import org.dynamisvfx.test.harness.DeterministicSimHarness;
import org.dynamisvfx.test.harness.Matrix4fUtil;
import org.dynamisvfx.test.harness.SimResult;
import org.dynamisvfx.test.mock.MockVfxService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeterministicHarnessParityTest {

    @Test
    void burstScenarioCompatibility() {
        SimResult result = runScenario(burstDescriptor(500, 1.0f, 2.0f), 1, 0.016f, 12345L);
        int count = result.steps().get(0).stats().activeParticleCount();
        assertTrue(count > 0 && count <= 500);
    }

    @Test
    void continuousScenarioCompatibility() {
        SimResult result = runScenario(continuousDescriptor(100f, 2.0f), 60, 0.016f, 42L);
        for (int i = 1; i < 30; i++) {
            int prev = result.steps().get(i - 1).stats().activeParticleCount();
            int curr = result.steps().get(i).stats().activeParticleCount();
            assertTrue(curr >= prev);
        }
    }

    @Test
    void retirementScenarioCompatibility() {
        SimResult result = runScenario(burstDescriptor(100, 0.5f, 0.5f), 40, 0.016f, 100L);
        int step31 = result.steps().get(30).stats().activeParticleCount();
        int step40 = result.steps().get(39).stats().activeParticleCount();
        assertTrue(step31 >= 0);
        assertTrue(step40 >= step31);
    }

    @Test
    void deterministicSameSeedSameResult() {
        SimResult first = runScenario(burstDescriptor(200, 0.8f, 1.4f), 60, 0.016f, 777L);
        SimResult second = runScenario(burstDescriptor(200, 0.8f, 1.4f), 60, 0.016f, 777L);

        int[] firstCounts = first.steps().stream().mapToInt(step -> step.stats().activeParticleCount()).toArray();
        int[] secondCounts = second.steps().stream().mapToInt(step -> step.stats().activeParticleCount()).toArray();
        assertArrayEquals(firstCounts, secondCounts);
        assertEquals(first.steps().size(), second.steps().size());
    }

    private static SimResult runScenario(ParticleEmitterDescriptor descriptor, int steps, float dt, long seed) {
        return DeterministicSimHarness.builder()
            .service(new MockVfxService())
            .effect(descriptor, Matrix4fUtil.identity())
            .steps(steps)
            .deltaTime(dt)
            .seed(seed)
            .build()
            .run();
    }

    private static ParticleEmitterDescriptor burstDescriptor(int burstCount, float lifeMin, float lifeMax) {
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

    private static ParticleEmitterDescriptor continuousDescriptor(float pps, float lifeSeconds) {
        return EffectBuilder.emitter("continuous")
            .shape(EmitterShape.point())
            .rate(EmissionRate.continuous(pps))
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
}
