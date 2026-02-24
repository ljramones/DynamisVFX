package org.dynamisvfx.api;

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
