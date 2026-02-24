package org.dynamisvfx.test;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.VfxHandle;
import org.dynamisvfx.core.builder.EffectBuilder;
import org.dynamisvfx.core.builder.EmissionRate;
import org.dynamisvfx.core.builder.EmitterShape;
import org.dynamisvfx.core.builder.Force;
import org.dynamisvfx.core.builder.ParticleInit;
import org.dynamisvfx.core.builder.Renderer;
import org.dynamisvfx.test.assertions.VfxAssertions;
import org.dynamisvfx.test.harness.Matrix4fUtil;
import org.dynamisvfx.test.mock.MockVfxDrawContext;
import org.dynamisvfx.test.mock.MockVfxFrameContext;
import org.dynamisvfx.test.mock.MockVfxService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MockVfxServiceLifecycleTest {
    @Test
    void lifecycleAndCallRecording() {
        MockVfxService service = new MockVfxService();
        ParticleEmitterDescriptor descriptor = fireDescriptor();

        VfxHandle handle = service.spawn(descriptor, Matrix4fUtil.identity());
        VfxAssertions.assertHandleAlive(service, handle);

        MockVfxFrameContext frameContext = new MockVfxFrameContext();
        MockVfxDrawContext drawContext = new MockVfxDrawContext();

        for (int i = 0; i < 10; i++) {
            frameContext.frameIndex(i);
            drawContext.frameIndex(i);
            service.simulate(List.of(handle), 1.0f / 60.0f, frameContext);
            service.recordDraws(List.of(handle), drawContext);
        }

        assertEquals(10, service.simulateCalls().size());
        assertEquals(10, service.drawCalls().size());
        VfxAssertions.assertDrawCallsRecorded(drawContext, 10);

        service.despawn(handle);
        VfxAssertions.assertHandleStale(service, handle);
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
