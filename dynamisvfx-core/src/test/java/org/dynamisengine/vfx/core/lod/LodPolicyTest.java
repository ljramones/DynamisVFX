package org.dynamisengine.vfx.core.lod;

import org.dynamisengine.vfx.api.LodDescriptor;
import org.dynamisengine.vfx.api.LodTier;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LodPolicyTest {

    private LodDescriptor threeTiers() {
        return new LodDescriptor(
            List.of(
                new LodTier(0, 50, 1.0f, 1.0f),
                new LodTier(50, 100, 0.5f, 0.5f),
                new LodTier(100, 200, 0.25f, 0.1f)
            ), false, 300
        );
    }

    @Test
    void activeTierSelectsClosestTier() {
        LodTier tier = LodPolicy.activeTier(threeTiers(), 75);
        assertNotNull(tier);
        assertEquals(50f, tier.minDistance());
        assertEquals(100f, tier.maxDistance());
    }

    @Test
    void distanceBeyondAllTiersReturnsLastTier() {
        LodTier tier = LodPolicy.activeTier(threeTiers(), 500);
        assertNotNull(tier);
        // Beyond all tiers returns last tier
        assertEquals(100f, tier.minDistance());
        assertEquals(200f, tier.maxDistance());
    }

    @Test
    void distanceWithinFirstTier() {
        LodTier tier = LodPolicy.activeTier(threeTiers(), 25);
        assertNotNull(tier);
        assertEquals(0f, tier.minDistance());
        assertEquals(50f, tier.maxDistance());
        assertEquals(1.0f, tier.simulationScale());
    }

    @Test
    void distanceAtBoundary() {
        LodTier tier = LodPolicy.activeTier(threeTiers(), 50);
        assertNotNull(tier);
        // 50 is within both tier 0 (0-50) and tier 1 (50-100); sorted by minDistance, first match wins
        assertEquals(0f, tier.minDistance());
    }

    @Test
    void singleTierAlwaysSelected() {
        LodDescriptor single = new LodDescriptor(
            List.of(new LodTier(0, 100, 1.0f, 1.0f)),
            false, 200
        );
        assertNotNull(LodPolicy.activeTier(single, 0));
        assertNotNull(LodPolicy.activeTier(single, 50));
        // Beyond range returns last (only) tier
        assertNotNull(LodPolicy.activeTier(single, 200));
    }
}
