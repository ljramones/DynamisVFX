package org.dynamisvfx.vulkan.resources;

import org.dynamisgpu.api.error.GpuErrorCode;
import org.dynamisgpu.api.error.GpuException;
import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.RendererType;

public record VfxBufferConfig(
    int maxParticles,
    boolean needsSort,
    boolean needsRibbon,
    boolean needsMesh,
    int maxDebrisCandidates
) {
    public static final int DEFAULT_MAX_PARTICLES = 65_536;
    public static final int HARD_MAX_PARTICLES = 1_048_576;
    public static final int DEFAULT_MAX_DEBRIS_CANDIDATES = 256;

    public static VfxBufferConfig defaults() {
        return new VfxBufferConfig(DEFAULT_MAX_PARTICLES, true, false, false, DEFAULT_MAX_DEBRIS_CANDIDATES);
    }

    public static VfxBufferConfig of(ParticleEmitterDescriptor desc) throws GpuException {
        int maxParticles = DEFAULT_MAX_PARTICLES;
        validateMaxParticles(maxParticles);

        RendererType type = desc.renderer() == null ? RendererType.BILLBOARD : desc.renderer().type();
        BlendMode blendMode = desc.renderer() == null ? BlendMode.ALPHA : desc.renderer().blendMode();

        boolean needsSort = blendMode != BlendMode.ADDITIVE;
        boolean needsRibbon = type == RendererType.RIBBON;
        boolean needsMesh = type == RendererType.MESH;

        return new VfxBufferConfig(maxParticles, needsSort, needsRibbon, needsMesh, DEFAULT_MAX_DEBRIS_CANDIDATES);
    }

    public static void validateMaxParticles(int maxParticles) throws GpuException {
        if (maxParticles <= 0 || maxParticles > HARD_MAX_PARTICLES) {
            throw new GpuException(
                GpuErrorCode.INVALID_ARGUMENT,
                "maxParticles must be > 0 and <= " + HARD_MAX_PARTICLES + ", got " + maxParticles,
                true
            );
        }
        if ((maxParticles & (maxParticles - 1)) != 0) {
            throw new GpuException(
                GpuErrorCode.INVALID_ARGUMENT,
                "maxParticles must be a power of two, got " + maxParticles,
                true
            );
        }
    }
}
