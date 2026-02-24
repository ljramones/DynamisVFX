package org.dynamisvfx.api;

public record LodTier(
    float minDistance,
    float maxDistance,
    float simulationScale,
    float emissionScale
) {
}
