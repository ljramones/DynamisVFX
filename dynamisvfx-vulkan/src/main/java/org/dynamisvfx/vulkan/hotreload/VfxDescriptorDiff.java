package org.dynamisvfx.vulkan.hotreload;

import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.core.validate.EffectValidator;
import org.dynamisvfx.core.validate.ValidationError;

import java.util.List;
import java.util.Objects;

public final class VfxDescriptorDiff {
    private VfxDescriptorDiff() {
    }

    public static VfxReloadCategory classify(
        ParticleEmitterDescriptor current,
        ParticleEmitterDescriptor updated
    ) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(updated, "updated");

        List<ValidationError> errors = EffectValidator.validate(updated);
        boolean hasErrors = errors.stream().anyMatch(e -> e.severity() == ValidationError.Severity.ERROR);
        if (hasErrors) {
            return VfxReloadCategory.FORCES_ONLY;
        }

        if (current.shape() == null || updated.shape() == null) {
            return VfxReloadCategory.FULL_RESPAWN;
        }
        if (current.shape().type() != updated.shape().type()) {
            return VfxReloadCategory.FULL_RESPAWN;
        }
        if (current.renderer() == null || updated.renderer() == null) {
            return VfxReloadCategory.FULL_RESPAWN;
        }
        if (current.renderer().type() != updated.renderer().type()) {
            return VfxReloadCategory.FULL_RESPAWN;
        }
        if (current.rate() == null || updated.rate() == null) {
            return VfxReloadCategory.FULL_RESPAWN;
        }
        if (current.rate().burstCount() != updated.rate().burstCount()) {
            return VfxReloadCategory.FULL_RESPAWN;
        }

        boolean rendererChanged =
            current.renderer().blendMode() != updated.renderer().blendMode()
                || current.renderer().softParticles() != updated.renderer().softParticles()
                || !Objects.equals(current.renderer().textureAtlasId(), updated.renderer().textureAtlasId());
        if (rendererChanged) {
            return VfxReloadCategory.RENDERER_CHANGED;
        }

        if (!Objects.equals(current.forces(), updated.forces())) {
            return VfxReloadCategory.FORCES_ONLY;
        }

        return VfxReloadCategory.FORCES_ONLY;
    }
}
