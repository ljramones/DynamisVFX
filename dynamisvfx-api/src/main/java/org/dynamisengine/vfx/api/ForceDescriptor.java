package org.dynamisengine.vfx.api;

public record ForceDescriptor(
    ForceType type,
    float strength,
    float[] direction,
    NoiseForceConfig noiseConfig
) {
}
