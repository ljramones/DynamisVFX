package org.dynamisengine.vfx.test;

import org.dynamisengine.vfx.api.BlendMode;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.core.builder.EffectBuilder;
import org.dynamisengine.vfx.core.builder.EmissionRate;
import org.dynamisengine.vfx.core.builder.EmitterShape;
import org.dynamisengine.vfx.core.builder.Force;
import org.dynamisengine.vfx.core.builder.ParticleInit;
import org.dynamisengine.vfx.core.builder.Renderer;
import org.dynamisengine.vfx.test.assertions.VfxAssertions;
import org.dynamisengine.vfx.test.harness.DeterministicSimHarness;
import org.dynamisengine.vfx.test.harness.Matrix4fUtil;
import org.dynamisengine.vfx.test.harness.SimResult;
import org.dynamisengine.vfx.test.mock.MockVfxService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DeterministicHarnessTest {
    @Test
    void deterministicSequenceForSameSeed() {
        ParticleEmitterDescriptor fire = fireDescriptor();

        SimResult first = DeterministicSimHarness.builder()
            .service(new MockVfxService())
            .effect(fire, Matrix4fUtil.identity())
            .steps(60)
            .deltaTime(1f / 60f)
            .seed(12345L)
            .build()
            .run();

        SimResult second = DeterministicSimHarness.builder()
            .service(new MockVfxService())
            .effect(fire, Matrix4fUtil.identity())
            .steps(60)
            .deltaTime(1f / 60f)
            .seed(12345L)
            .build()
            .run();

        assertEquals(60, first.steps().size());
        assertEquals(60, second.steps().size());
        assertEquals(first.steps(), second.steps());

        VfxAssertions.assertParticleCountInRange(first, 400, 600);
    }

    private static ParticleEmitterDescriptor fireDescriptor() {
        return EffectBuilder.emitter("fire_burst")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.burst(500))
            .init(ParticleInit.builder()
                .lifetime(0.8f, 1.4f)
                .velocityRange(2f, 6f)
                .sizeRange(0.1f, 0.3f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.drag(0.4f))
            .force(Force.curlNoise(0.02f, 1.5f))
            .renderer(Renderer.billboard()
                .texture("fx/fire_atlas.ktx2")
                .blend(BlendMode.ADDITIVE)
                .softParticles(true)
                .build())
            .build();
    }
}
