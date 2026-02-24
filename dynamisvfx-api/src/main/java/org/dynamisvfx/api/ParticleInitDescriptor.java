package org.dynamisvfx.api;

public record ParticleInitDescriptor(
    float lifetimeMinSeconds,
    float lifetimeMaxSeconds,
    float speedMin,
    float speedMax,
    float sizeMin,
    float sizeMax,
    Vec3f initialDirection,
    Vec3f colorRgb,
    float alpha
) {
}
