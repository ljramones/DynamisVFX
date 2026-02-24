package org.dynamisvfx.api;

public record ForceDescriptor(
    ForceType type,
    float strength,
    float[] direction,
    NoiseForceConfig noiseConfig
) {
}
