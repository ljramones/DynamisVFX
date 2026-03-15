package org.dynamisengine.vfx.core.builder;

import org.dynamisengine.vfx.api.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EffectBuilderTest {

    @Test
    void buildMinimalEffect() {
        ParticleEmitterDescriptor desc = EffectBuilder.emitter("spark")
            .shape(EmitterShapeBuilder.point())
            .rate(EmissionRateBuilder.continuous(100))
            .init(new ParticleInitBuilder().build())
            .renderer(Renderer.billboard().texture("spark_atlas").build())
            .build();

        assertEquals("spark", desc.id());
        assertEquals(EmitterShapeType.POINT, desc.shape().type());
        assertEquals(EmissionMode.CONTINUOUS, desc.rate().mode());
        assertNotNull(desc.init());
        assertNotNull(desc.renderer());
        assertTrue(desc.forces().isEmpty());
        assertNull(desc.lod());
        assertNull(desc.physics());
    }

    @Test
    void buildWithForces() {
        ParticleEmitterDescriptor desc = EffectBuilder.emitter("debris")
            .shape(EmitterShapeBuilder.point())
            .rate(EmissionRateBuilder.burst(20))
            .init(new ParticleInitBuilder().build())
            .renderer(Renderer.billboard().build())
            .force(ForceBuilder.gravity(9.8f))
            .force(ForceBuilder.drag(0.5f))
            .build();

        assertEquals(2, desc.forces().size());
        assertEquals(ForceType.GRAVITY, desc.forces().get(0).type());
        assertEquals(ForceType.DRAG, desc.forces().get(1).type());
    }

    @Test
    void buildWithLod() {
        LodDescriptor lod = new LodBuilder()
            .addTier(0, 50, 1.0f, 1.0f)
            .addTier(50, 150, 0.5f, 0.5f)
            .allowSleeping(true)
            .sleepingDistance(300)
            .build();

        ParticleEmitterDescriptor desc = EffectBuilder.emitter("fire")
            .shape(EmitterShapeBuilder.sphere(2.0f))
            .rate(EmissionRateBuilder.continuous(200))
            .init(new ParticleInitBuilder().lifetime(0.5f, 2.0f).build())
            .renderer(Renderer.billboard().build())
            .lod(lod)
            .build();

        assertNotNull(desc.lod());
        assertEquals(2, desc.lod().tiers().size());
        assertTrue(desc.lod().allowSleeping());
    }

    @Test
    void buildWithPhysicsHandoff() {
        PhysicsHandoffDescriptor physics = new PhysicsHandoffBuilder()
            .enabled(true)
            .speedThreshold(5.0f)
            .meshId("chunk")
            .materialTag("rock")
            .mass(0.3f)
            .build();

        ParticleEmitterDescriptor desc = EffectBuilder.emitter("explosion")
            .shape(EmitterShapeBuilder.point())
            .rate(EmissionRateBuilder.burst(50))
            .init(new ParticleInitBuilder().build())
            .renderer(Renderer.mesh().build())
            .physics(physics)
            .build();

        assertNotNull(desc.physics());
        assertTrue(desc.physics().enabled());
        assertEquals("rock", desc.physics().materialTag());
    }

    @Test
    void buildWithCurlNoiseForce() {
        ParticleEmitterDescriptor desc = EffectBuilder.emitter("magic")
            .shape(EmitterShapeBuilder.sphere(1.0f))
            .rate(EmissionRateBuilder.continuous(50))
            .init(new ParticleInitBuilder().build())
            .renderer(Renderer.billboard().build())
            .force(ForceBuilder.curlNoise(0.5f, 2.0f))
            .build();

        assertEquals(1, desc.forces().size());
        assertEquals(ForceType.CURL_NOISE, desc.forces().get(0).type());
        assertNotNull(desc.forces().get(0).noiseConfig());
    }

    @Test
    void allRendererTypes() {
        assertEquals(RendererType.BILLBOARD, Renderer.billboard().build().type());
        assertEquals(RendererType.RIBBON, Renderer.ribbon().trailLength(10).build().type());
        assertEquals(RendererType.MESH, Renderer.mesh().build().type());
        assertEquals(RendererType.BEAM, Renderer.beam().build().type());
        assertEquals(RendererType.DECAL, Renderer.decal().build().type());
    }

    @Test
    void allShapeTypes() {
        assertEquals(EmitterShapeType.POINT, EmitterShapeBuilder.point().type());
        assertEquals(EmitterShapeType.SPHERE, EmitterShapeBuilder.sphere(1.0f).type());
        assertEquals(EmitterShapeType.CONE, EmitterShapeBuilder.cone(1.0f, 2.0f).type());
    }

    @Test
    void burstAndContinuousEmission() {
        EmissionRateDescriptor burst = EmissionRateBuilder.burst(100);
        assertEquals(EmissionMode.BURST, burst.mode());
        assertEquals(100, burst.burstCount());

        EmissionRateDescriptor continuous = EmissionRateBuilder.continuous(500);
        assertEquals(EmissionMode.CONTINUOUS, continuous.mode());
        assertEquals(500, continuous.particlesPerSecond());
    }
}
