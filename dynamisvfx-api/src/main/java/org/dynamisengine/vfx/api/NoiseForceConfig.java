package org.dynamisengine.vfx.api;

public record NoiseForceConfig(
    float frequency,
    float amplitude,
    int octaves,
    float lacunarity,
    float gain,
    float timeScale,
    int seed
) {
}
