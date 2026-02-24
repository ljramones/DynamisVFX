package org.dynamisvfx.core.noise;

public record CurlFieldConfig(
    float frequency,
    int octaves,
    float strength,
    int seed
) {
}
