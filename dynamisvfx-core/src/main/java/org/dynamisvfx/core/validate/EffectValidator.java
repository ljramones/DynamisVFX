package org.dynamisvfx.core.validate;

import org.dynamisvfx.api.ForceDescriptor;
import org.dynamisvfx.api.ForceType;
import org.dynamisvfx.api.LodTier;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.RendererType;

import java.util.ArrayList;
import java.util.List;

import static org.dynamisvfx.core.validate.ValidationError.Severity.ERROR;

public final class EffectValidator {
    private EffectValidator() {
    }

    public static List<ValidationError> validate(ParticleEmitterDescriptor descriptor) {
        List<ValidationError> errors = new ArrayList<>();

        if (descriptor.id() == null || descriptor.id().isBlank()) {
            errors.add(new ValidationError("id", "Emitter id must not be empty", ERROR));
        }

        if (descriptor.init() != null && descriptor.init().lifetimeMinSeconds() > descriptor.init().lifetimeMaxSeconds()) {
            errors.add(new ValidationError("init.lifetime", "Lifetime min must be <= max", ERROR));
        }

        if (descriptor.renderer() != null
            && descriptor.renderer().type() == RendererType.RIBBON
            && descriptor.renderer().frameCount() <= 0) {
            errors.add(new ValidationError("renderer.frameCount", "RIBBON renderer requires trail length", ERROR));
        }

        if (descriptor.lod() != null && descriptor.lod().tiers() != null && !descriptor.lod().tiers().isEmpty()) {
            float prevMax = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < descriptor.lod().tiers().size(); i++) {
                LodTier tier = descriptor.lod().tiers().get(i);
                if (tier.minDistance() > tier.maxDistance()) {
                    errors.add(new ValidationError("lod.tiers[" + i + "]", "Tier min distance must be <= max distance", ERROR));
                }
                if (tier.minDistance() < prevMax) {
                    errors.add(new ValidationError("lod.tiers[" + i + "]", "LodTier distances must be monotonically increasing", ERROR));
                }
                prevMax = tier.maxDistance();
            }
        }

        if (descriptor.forces() != null) {
            for (int i = 0; i < descriptor.forces().size(); i++) {
                ForceDescriptor force = descriptor.forces().get(i);
                if (force.type() == ForceType.CURL_NOISE && force.noiseConfig() == null) {
                    errors.add(new ValidationError("forces[" + i + "].noiseConfig", "CurlNoise force requires NoiseForceConfig", ERROR));
                }
            }
        }

        return errors;
    }
}
