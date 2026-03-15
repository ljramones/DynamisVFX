package org.dynamisengine.vfx.api;

public record RendererDescriptor(
    RendererType type,
    BlendMode blendMode,
    String textureAtlasId,
    int frameCount,
    boolean softParticles,
    boolean lightEmitting
) {
}
