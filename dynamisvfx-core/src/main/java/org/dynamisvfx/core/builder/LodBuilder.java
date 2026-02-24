package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.LodDescriptor;
import org.dynamisvfx.api.LodTier;

import java.util.ArrayList;
import java.util.List;

public final class LodBuilder {
    private final List<LodTier> tiers = new ArrayList<>();
    private boolean allowSleeping;
    private float sleepingDistance;

    public LodBuilder addTier(float minDistance, float maxDistance, float simulationScale, float emissionScale) {
        tiers.add(new LodTier(minDistance, maxDistance, simulationScale, emissionScale));
        return this;
    }

    public LodBuilder allowSleeping(boolean value) {
        this.allowSleeping = value;
        return this;
    }

    public LodBuilder sleepingDistance(float value) {
        this.sleepingDistance = value;
        return this;
    }

    public LodDescriptor build() {
        return new LodDescriptor(List.copyOf(tiers), allowSleeping, sleepingDistance);
    }
}
