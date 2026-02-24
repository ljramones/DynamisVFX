package org.dynamisvfx.core.builder;

public final class ParticleInit {
    private ParticleInit() {
    }

    public static ParticleInitBuilder builder() {
        return new ParticleInitBuilder();
    }
}
