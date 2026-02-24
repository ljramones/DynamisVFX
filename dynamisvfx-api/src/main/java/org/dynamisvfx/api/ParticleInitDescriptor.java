package org.dynamisvfx.api;

public record ParticleInitDescriptor(
    float lifetimeMinSeconds,
    float lifetimeMaxSeconds,
    float speedMin,
    float speedMax,
    float sizeMin,
    float sizeMax,
    float[] initialDirection,
    float[] colorRgb,
    float alpha
) {
}
