package org.dynamisengine.vfx.core.validate;

import org.dynamisengine.vfx.api.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EffectValidatorTest {

    private ParticleEmitterDescriptor valid() {
        return new ParticleEmitterDescriptor(
            "fire",
            new EmitterShapeDescriptor(EmitterShapeType.POINT, null, null, null),
            new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 100, 0, null),
            new ParticleInitDescriptor(1.0f, 2.0f, 0, 1, 0.5f, 1.0f,
                new float[]{0, 1, 0}, new float[]{1, 1, 1}, 1.0f),
            List.of(),
            new RendererDescriptor(RendererType.BILLBOARD, BlendMode.ALPHA, "tex", 1, false, false),
            null, null
        );
    }

    @Test
    void validDescriptorPassesValidation() {
        List<ValidationError> errors = EffectValidator.validate(valid());
        assertTrue(errors.isEmpty());
    }

    @Test
    void validDescriptorReturnsEmptyErrorList() {
        List<ValidationError> errors = EffectValidator.validate(valid());
        assertNotNull(errors);
        assertTrue(errors.isEmpty());
    }

    @Test
    void emptyEmitterIdFails() {
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "", valid().shape(), valid().rate(), valid().init(),
            valid().forces(), valid().renderer(), null, null
        );
        List<ValidationError> errors = EffectValidator.validate(desc);
        assertTrue(errors.stream().anyMatch(e -> e.field().equals("id")));
    }

    @Test
    void lifetimeMinGreaterThanMaxFails() {
        ParticleInitDescriptor badInit = new ParticleInitDescriptor(
            5.0f, 1.0f, 0, 1, 0.5f, 1.0f,
            new float[]{0, 1, 0}, new float[]{1, 1, 1}, 1.0f
        );
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "test", valid().shape(), valid().rate(), badInit,
            valid().forces(), valid().renderer(), null, null
        );
        List<ValidationError> errors = EffectValidator.validate(desc);
        assertTrue(errors.stream().anyMatch(e -> e.field().equals("init.lifetime")));
    }

    @Test
    void ribbonRendererRequiresFrameCount() {
        RendererDescriptor ribbon = new RendererDescriptor(
            RendererType.RIBBON, BlendMode.ALPHA, "tex", 0, false, false
        );
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "test", valid().shape(), valid().rate(), valid().init(),
            valid().forces(), ribbon, null, null
        );
        List<ValidationError> errors = EffectValidator.validate(desc);
        assertTrue(errors.stream().anyMatch(e -> e.field().equals("renderer.frameCount")));
    }

    @Test
    void lodTiersNotMonotonicFails() {
        LodDescriptor lod = new LodDescriptor(
            List.of(
                new LodTier(0, 50, 1.0f, 1.0f),
                new LodTier(30, 100, 0.5f, 0.5f) // overlaps: 30 < 50
            ), false, 200
        );
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "test", valid().shape(), valid().rate(), valid().init(),
            valid().forces(), valid().renderer(), lod, null
        );
        List<ValidationError> errors = EffectValidator.validate(desc);
        assertTrue(errors.stream().anyMatch(e -> e.message().contains("monotonically")));
    }

    @Test
    void curlNoiseForceWithoutConfigFails() {
        ForceDescriptor bad = new ForceDescriptor(ForceType.CURL_NOISE, 1.0f, new float[]{0, 0, 0}, null);
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "test", valid().shape(), valid().rate(), valid().init(),
            List.of(bad), valid().renderer(), null, null
        );
        List<ValidationError> errors = EffectValidator.validate(desc);
        assertTrue(errors.stream().anyMatch(e -> e.field().contains("noiseConfig")));
    }

    @Test
    void multipleValidationErrorsAccumulated() {
        ParticleInitDescriptor badInit = new ParticleInitDescriptor(
            5.0f, 1.0f, 0, 1, 0.5f, 1.0f,
            new float[]{0, 1, 0}, new float[]{1, 1, 1}, 1.0f
        );
        ForceDescriptor badForce = new ForceDescriptor(ForceType.CURL_NOISE, 1.0f, new float[]{0, 0, 0}, null);
        ParticleEmitterDescriptor desc = new ParticleEmitterDescriptor(
            "", valid().shape(), valid().rate(), badInit,
            List.of(badForce), valid().renderer(), null, null
        );
        List<ValidationError> errors = EffectValidator.validate(desc);
        assertTrue(errors.size() >= 3);
    }
}
