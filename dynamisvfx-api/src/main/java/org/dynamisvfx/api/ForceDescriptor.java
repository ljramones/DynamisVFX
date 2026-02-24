package org.dynamisvfx.api;

public record ForceDescriptor(
    ForceType type,
    float strength,
    Vec3f direction,
    NoiseForceConfig noiseConfig
) {
}
