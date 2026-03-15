package org.dynamisengine.vfx.test;

import org.dynamisengine.vfx.api.BlendMode;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.api.VfxHandle;
import org.dynamisengine.vfx.core.builder.EffectBuilder;
import org.dynamisengine.vfx.core.builder.EmissionRate;
import org.dynamisengine.vfx.core.builder.EmitterShape;
import org.dynamisengine.vfx.core.builder.Force;
import org.dynamisengine.vfx.core.builder.ParticleInit;
import org.dynamisengine.vfx.core.builder.Renderer;
import org.dynamisengine.vfx.test.assertions.VfxAssertions;
import org.dynamisengine.vfx.test.harness.Matrix4fUtil;
import org.dynamisengine.vfx.test.mock.MockVfxDrawContext;
import org.dynamisengine.vfx.test.mock.MockVfxFrameContext;
import org.dynamisengine.vfx.test.mock.MockVfxService;
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
