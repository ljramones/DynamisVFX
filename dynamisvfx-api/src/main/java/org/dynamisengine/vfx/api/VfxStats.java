package org.dynamisengine.vfx.api;

public record VfxStats(
    int activeEffectCount,
    int activeParticleCount,
    int sleepingEmitterCount,
    int culledParticleCount,
    long gpuMemoryBytes,
    VfxBudgetStats budgetStats
) {
}
