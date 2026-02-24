package org.dynamisvfx.core.lod;

import org.dynamisvfx.api.LodDescriptor;
import org.dynamisvfx.api.LodTier;

import java.util.Comparator;
import java.util.List;

public final class LodPolicy {
    private LodPolicy() {
    }

    public static LodTier activeTier(LodDescriptor descriptor, float cameraDistance) {
        if (descriptor == null || descriptor.tiers() == null || descriptor.tiers().isEmpty()) {
            return null;
        }

        List<LodTier> tiers = descriptor.tiers().stream()
            .sorted(Comparator.comparing(LodTier::minDistance))
            .toList();

        for (LodTier tier : tiers) {
            if (cameraDistance >= tier.minDistance() && cameraDistance <= tier.maxDistance()) {
                return tier;
            }
        }

        return tiers.get(tiers.size() - 1);
    }

    public static int activeTierIndex(LodPolicyConfig config, float cameraDistance) {
        if (config == null || config.tierDistanceThresholds() == null || config.tierDistanceThresholds().length == 0) {
            return 0;
        }

        int index = 0;
        for (float threshold : config.tierDistanceThresholds()) {
            if (cameraDistance >= threshold) {
                index++;
            }
        }
        return index;
    }
}
