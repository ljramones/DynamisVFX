package org.dynamisvfx.api;

public record VfxStats(
    int activeEffectCount,
    int activeParticleCount,
    int sleepingEmitterCount,
    int culledParticleCount,
    long gpuMemoryBytes
) {
}
