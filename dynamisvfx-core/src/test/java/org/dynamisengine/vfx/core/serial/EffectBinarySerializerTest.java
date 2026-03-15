package org.dynamisengine.vfx.core.serial;

import org.dynamisengine.vfx.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectBinarySerializerTest {

    private ParticleEmitterDescriptor minimal() {
        return new ParticleEmitterDescriptor(
            "smoke",
            new EmitterShapeDescriptor(EmitterShapeType.POINT, null, null, null),
            new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 10.0f, 0, null),
            new ParticleInitDescriptor(1.0f, 2.0f, 0.0f, 1.0f, 0.5f, 1.0f,
                new float[]{0, 1, 0}, new float[]{1, 1, 1}, 1.0f),
            List.of(),
            new RendererDescriptor(RendererType.BILLBOARD, BlendMode.ALPHA, "tex", 1, false, false),
            null,
            null
        );
    }

    @Test
    void roundTripMinimalDescriptor() {
        ParticleEmitterDescriptor desc = minimal();
        byte[] bytes = EffectBinarySerializer.toBytes(desc);
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(bytes);
        assertEquals(desc.id(), result.id());
        assertEquals(desc.shape().type(), result.shape().type());
        assertNull(result.lod());
        assertNull(result.physics());
        assertTrue(result.forces().isEmpty());
    }

    @Test
    void roundTripFullDescriptor() {
        NoiseForceConfig noiseConfig = new NoiseForceConfig(0.5f, 1.0f, 4, 2.0f, 0.5f, 1.0f, 42);
        List<ForceDescriptor> forces = List.of(
            new ForceDescriptor(ForceType.GRAVITY, 9.8f, new float[]{0, -1, 0}, null),
            new ForceDescriptor(ForceType.CURL_NOISE, 1.0f, new float[]{0, 0, 0}, noiseConfig)
        );
        LodDescriptor lod = new LodDescriptor(
            List.of(new LodTier(0, 50, 1.0f, 1.0f), new LodTier(50, 100, 0.5f, 0.5f)),
            true, 200.0f
        );
        PhysicsHandoffDescriptor physics = new PhysicsHandoffDescriptor(true, 5.0f, "mesh1", "stone", 2.0f);

        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "full",
            new EmitterShapeDescriptor(EmitterShapeType.SPHERE, new float[]{5.0f, 5.0f, 5.0f}, null, null),
            new EmissionRateDescriptor(EmissionMode.BURST, 0, 50, null),
            new ParticleInitDescriptor(0.5f, 3.0f, 1.0f, 5.0f, 0.1f, 2.0f,
                new float[]{1, 0, 0}, new float[]{1, 0, 0}, 0.8f),
            forces,
            new RendererDescriptor(RendererType.RIBBON, BlendMode.ADDITIVE, "atlas", 8, true, true),
            lod, physics
        );

        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(EffectBinarySerializer.toBytes(desc));
        assertEquals("full", result.id());
        assertEquals(2, result.forces().size());
        assertEquals(ForceType.CURL_NOISE, result.forces().get(1).type());
        assertNotNull(result.forces().get(1).noiseConfig());
        assertEquals(42, result.forces().get(1).noiseConfig().seed());
        assertEquals(2, result.lod().tiers().size());
        assertTrue(result.lod().allowSleeping());
        assertEquals(200.0f, result.lod().sleepingDistance());
        assertTrue(result.physics().enabled());
        assertEquals("stone", result.physics().materialTag());
    }

    @Test
    void roundTripMultipleForces() {
        List<ForceDescriptor> forces = List.of(
            new ForceDescriptor(ForceType.GRAVITY, 9.8f, new float[]{0, -1, 0}, null),
            new ForceDescriptor(ForceType.DRAG, 0.5f, new float[]{0, 0, 0}, null),
            new ForceDescriptor(ForceType.WIND, 3.0f, new float[]{1, 0, 0}, null),
            new ForceDescriptor(ForceType.VORTEX, 2.0f, new float[]{0, 1, 0}, null)
        );
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "multi", minimal().shape(), minimal().rate(), minimal().init(),
            forces, minimal().renderer(), null, null
        );
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(EffectBinarySerializer.toBytes(desc));
        assertEquals(4, result.forces().size());
        assertEquals(ForceType.GRAVITY, result.forces().get(0).type());
        assertEquals(ForceType.DRAG, result.forces().get(1).type());
        assertEquals(ForceType.WIND, result.forces().get(2).type());
        assertEquals(ForceType.VORTEX, result.forces().get(3).type());
    }

    @Test
    void roundTripCurlNoiseForce() {
        NoiseForceConfig cfg = new NoiseForceConfig(0.3f, 2.0f, 6, 2.5f, 0.4f, 1.5f, 999);
        ForceDescriptor curl = new ForceDescriptor(ForceType.CURL_NOISE, 1.5f, new float[]{0, 0, 0}, cfg);
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "curl", minimal().shape(), minimal().rate(), minimal().init(),
            List.of(curl), minimal().renderer(), null, null
        );
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(EffectBinarySerializer.toBytes(desc));
        NoiseForceConfig rc = result.forces().get(0).noiseConfig();
        assertNotNull(rc);
        assertEquals(0.3f, rc.frequency());
        assertEquals(2.0f, rc.amplitude());
        assertEquals(6, rc.octaves());
        assertEquals(2.5f, rc.lacunarity());
        assertEquals(0.4f, rc.gain());
        assertEquals(1.5f, rc.timeScale());
        assertEquals(999, rc.seed());
    }

    @Test
    void roundTripLodTiers() {
        LodDescriptor lod = new LodDescriptor(
            List.of(
                new LodTier(0, 30, 1.0f, 1.0f),
                new LodTier(30, 80, 0.5f, 0.5f),
                new LodTier(80, 200, 0.25f, 0.1f)
            ), false, 300.0f
        );
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "lod", minimal().shape(), minimal().rate(), minimal().init(),
            List.of(), minimal().renderer(), lod, null
        );
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(EffectBinarySerializer.toBytes(desc));
        assertEquals(3, result.lod().tiers().size());
        assertEquals(0f, result.lod().tiers().get(0).minDistance());
        assertEquals(30f, result.lod().tiers().get(0).maxDistance());
        assertEquals(80f, result.lod().tiers().get(1).maxDistance());
        assertEquals(200f, result.lod().tiers().get(2).maxDistance());
        assertEquals(0.25f, result.lod().tiers().get(2).simulationScale());
    }

    @Test
    void roundTripPhysicsHandoff() {
        PhysicsHandoffDescriptor physics = new PhysicsHandoffDescriptor(true, 10.0f, "debris", "metal", 0.5f);
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "phys", minimal().shape(), minimal().rate(), minimal().init(),
            List.of(), minimal().renderer(), null, physics
        );
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(EffectBinarySerializer.toBytes(desc));
        assertNotNull(result.physics());
        assertTrue(result.physics().enabled());
        assertEquals(10.0f, result.physics().speedThreshold());
        assertEquals("debris", result.physics().meshId());
        assertEquals("metal", result.physics().materialTag());
        assertEquals(0.5f, result.physics().mass());
    }

    @Test
    void nullOptionalFieldsHandled() {
        ParticleEmitterDescriptor desc = minimal();
        assertNull(desc.lod());
        assertNull(desc.physics());
        byte[] bytes = EffectBinarySerializer.toBytes(desc);
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(bytes);
        assertNull(result.lod());
        assertNull(result.physics());
        for (ForceDescriptor f : result.forces()) {
            assertNull(f.noiseConfig());
        }
    }

    @Test
    void emptyForcesListRoundTrip() {
        ParticleEmitterDescriptor desc = minimal();
        assertTrue(desc.forces().isEmpty());
        ParticleEmitterDescriptor result = EffectBinarySerializer.fromBytes(EffectBinarySerializer.toBytes(desc));
        assertNotNull(result.forces());
        assertTrue(result.forces().isEmpty());
    }
}
