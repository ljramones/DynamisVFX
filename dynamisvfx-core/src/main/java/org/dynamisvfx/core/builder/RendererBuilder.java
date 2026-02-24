package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.RendererDescriptor;
import org.dynamisvfx.api.RendererType;

public final class RendererBuilder {
    private final RendererType type;
    private BlendMode blendMode = BlendMode.ALPHA;
    private String textureAtlasId;
    private int frameCount;
    private boolean softParticles;
    private boolean lightEmitting;

    RendererBuilder(RendererType type) {
        this.type = type;
    }

    public RendererBuilder texture(String atlasId) {
        this.textureAtlasId = atlasId;
        return this;
    }

    public RendererBuilder blend(BlendMode mode) {
        this.blendMode = mode;
        return this;
    }

    public RendererBuilder frameCount(int value) {
        this.frameCount = value;
        return this;
    }

    public RendererBuilder trailLength(int value) {
        this.frameCount = value;
        return this;
    }

    public RendererBuilder softParticles(boolean value) {
        this.softParticles = value;
        return this;
    }

    public RendererBuilder lightEmitting(boolean value) {
        this.lightEmitting = value;
        return this;
    }

    public RendererDescriptor build() {
        return new RendererDescriptor(type, blendMode, textureAtlasId, frameCount, softParticles, lightEmitting);
    }
}
