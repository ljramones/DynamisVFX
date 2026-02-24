package org.dynamisvfx.api;

import java.util.List;

public record LodDescriptor(
    List<LodTier> tiers,
    boolean allowSleeping,
    float sleepingDistance
) {
}
