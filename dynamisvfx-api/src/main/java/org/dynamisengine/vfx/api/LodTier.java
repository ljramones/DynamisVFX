package org.dynamisengine.vfx.api;

public record LodTier(
    float minDistance,
    float maxDistance,
    float simulationScale,
    float emissionScale
) {
}
