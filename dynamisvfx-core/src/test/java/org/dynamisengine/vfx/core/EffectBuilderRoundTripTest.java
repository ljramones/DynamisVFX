package org.dynamisengine.vfx.core;

import org.dynamisengine.vfx.api.BlendMode;
import org.dynamisengine.vfx.api.ForceDescriptor;
import org.dynamisengine.vfx.api.LodDescriptor;
import org.dynamisengine.vfx.api.LodTier;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.api.PhysicsHandoffDescriptor;
import org.dynamisengine.vfx.api.RendererDescriptor;
import org.dynamisengine.vfx.core.builder.EffectBuilder;
import org.dynamisengine.vfx.core.builder.EmissionRate;
import org.dynamisengine.vfx.core.builder.EmitterShape;
import org.dynamisengine.vfx.core.builder.Force;
import org.dynamisengine.vfx.core.builder.ParticleInit;
import org.dynamisengine.vfx.core.builder.Renderer;
import org.dynamisengine.vfx.core.serial.EffectSerializer;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectBuilderRoundTripTest {
    @Test
    void roundTripFireBurstDescriptor() {
        ParticleEmitterDescriptor fire = EffectBuilder.emitter("fire_burst")
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

        String json = EffectSerializer.toJson(fire);
        ParticleEmitterDescriptor reloaded = EffectSerializer.fromJson(json);

        assertDescriptorEquals(fire, reloaded);
    }

    private static void assertDescriptorEquals(ParticleEmitterDescriptor expected, ParticleEmitterDescriptor actual) {
        assertEquals(expected.id(), actual.id());

        assertEquals(expected.shape().type(), actual.shape().type());
        assertTrue(Arrays.equals(expected.shape().dimensions(), actual.shape().dimensions()));
        assertEquals(expected.shape().sourceMeshId(), actual.shape().sourceMeshId());
        assertEquals(expected.shape().sourceSplineId(), actual.shape().sourceSplineId());

        assertEquals(expected.rate(), actual.rate());

        assertEquals(expected.init().lifetimeMinSeconds(), actual.init().lifetimeMinSeconds());
        assertEquals(expected.init().lifetimeMaxSeconds(), actual.init().lifetimeMaxSeconds());
        assertEquals(expected.init().speedMin(), actual.init().speedMin());
        assertEquals(expected.init().speedMax(), actual.init().speedMax());
        assertEquals(expected.init().sizeMin(), actual.init().sizeMin());
        assertEquals(expected.init().sizeMax(), actual.init().sizeMax());
        assertTrue(Arrays.equals(expected.init().initialDirection(), actual.init().initialDirection()));
        assertTrue(Arrays.equals(expected.init().colorRgb(), actual.init().colorRgb()));
        assertEquals(expected.init().alpha(), actual.init().alpha());

        assertEquals(expected.forces().size(), actual.forces().size());
        for (int i = 0; i < expected.forces().size(); i++) {
            assertForceEquals(expected.forces().get(i), actual.forces().get(i));
        }

        assertRendererEquals(expected.renderer(), actual.renderer());
        assertLodEquals(expected.lod(), actual.lod());
        assertPhysicsEquals(expected.physics(), actual.physics());
    }

    private static void assertForceEquals(ForceDescriptor expected, ForceDescriptor actual) {
        assertEquals(expected.type(), actual.type());
        assertEquals(expected.strength(), actual.strength());
        assertTrue(Arrays.equals(expected.direction(), actual.direction()));
        if (expected.noiseConfig() == null) {
            assertNull(actual.noiseConfig());
            return;
        }
        assertEquals(expected.noiseConfig(), actual.noiseConfig());
    }

    private static void assertRendererEquals(RendererDescriptor expected, RendererDescriptor actual) {
        assertEquals(expected, actual);
    }

    private static void assertLodEquals(LodDescriptor expected, LodDescriptor actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected.allowSleeping(), actual.allowSleeping());
        assertEquals(expected.sleepingDistance(), actual.sleepingDistance());
        assertEquals(expected.tiers().size(), actual.tiers().size());
        for (int i = 0; i < expected.tiers().size(); i++) {
            LodTier e = expected.tiers().get(i);
            LodTier a = actual.tiers().get(i);
            assertEquals(e, a);
        }
    }

    private static void assertPhysicsEquals(PhysicsHandoffDescriptor expected, PhysicsHandoffDescriptor actual) {
        if (expected == null) {
            assertNull(actual);
            return;
        }
        assertEquals(expected, actual);
    }
}
