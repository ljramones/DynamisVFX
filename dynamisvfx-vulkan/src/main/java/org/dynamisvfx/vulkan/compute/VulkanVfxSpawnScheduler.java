package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.api.EmissionMode;
import org.dynamisvfx.api.EmissionRateDescriptor;

public final class VulkanVfxSpawnScheduler {
    private float fractionalAccumulator;
    private boolean burstConsumed;

    public int computeSpawnCount(
        EmissionRateDescriptor rate,
        float deltaTime,
        int freeSlots
    ) {
        if (rate == null || freeSlots <= 0 || deltaTime <= 0.0f) {
            return 0;
        }

        int spawnCount;
        if (rate.mode() == EmissionMode.BURST) {
            if (burstConsumed) {
                spawnCount = 0;
            } else {
                burstConsumed = true;
                spawnCount = rate.burstCount();
            }
        } else if (rate.mode() == EmissionMode.CONTINUOUS) {
            float exact = rate.particlesPerSecond() * deltaTime + fractionalAccumulator;
            spawnCount = (int) Math.floor(exact);
            fractionalAccumulator = exact - spawnCount;
        } else {
            spawnCount = 0;
        }

        if (spawnCount < 0) {
            spawnCount = 0;
        }
        return Math.min(spawnCount, freeSlots);
    }

    public void reset() {
        fractionalAccumulator = 0.0f;
        burstConsumed = false;
    }
}
