package org.dynamisvfx.bench;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.core.builder.EffectBuilder;
import org.dynamisvfx.core.builder.EmissionRate;
import org.dynamisvfx.core.builder.EmitterShape;
import org.dynamisvfx.core.builder.Force;
import org.dynamisvfx.core.builder.ParticleInit;
import org.dynamisvfx.core.builder.Renderer;
import org.dynamisvfx.test.mock.MockVfxService;

public final class BenchmarkFixtures {
    private BenchmarkFixtures() {
    }

    public static ParticleEmitterDescriptor fireBurst(int maxParticles) {
        return EffectBuilder.emitter("bench_fire")
            .shape(EmitterShape.sphere(0.5f))
            .rate(EmissionRate.burst(maxParticles))
            .init(ParticleInit.builder()
                .lifetime(1.0f, 3.0f)
                .velocityRange(1.0f, 8.0f)
                .sizeRange(0.05f, 0.2f)
                .build())
            .force(Force.gravity(9.8f))
            .force(Force.drag(0.3f))
            .force(Force.curlNoise(0.02f, 1.2f))
            .renderer(Renderer.billboard()
                .texture("fx/fire.ktx2")
                .blend(BlendMode.ADDITIVE)
                .build())
            .build();
    }

    public static ParticleEmitterDescriptor smoke(int maxParticles) {
        return EffectBuilder.emitter("bench_smoke")
            .shape(EmitterShape.sphere(1.0f))
            .rate(EmissionRate.continuous(Math.max(1.0f, maxParticles / 8.0f)))
            .init(ParticleInit.builder()
                .lifetime(2.0f, 5.0f)
                .velocityRange(0.2f, 1.0f)
                .sizeRange(0.3f, 1.0f)
                .build())
            .force(Force.gravity(1.0f))
            .renderer(Renderer.billboard()
                .texture("fx/smoke.ktx2")
                .blend(BlendMode.ALPHA)
                .build())
            .build();
    }

    public static MockVfxService mockService() {
        return new MockVfxService();
    }

    public static float[] identityMatrix() {
        return new float[] {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
    }
}
