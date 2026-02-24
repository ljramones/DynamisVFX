package org.dynamisvfx.api;

public record VfxBudgetStats(
    int totalBudget,
    int usedBudget,
    int remainingBudget,
    int activeEffectCount,
    int rejectedThisFrame,
    int clampedThisFrame,
    int evictedThisFrame
) {
}
