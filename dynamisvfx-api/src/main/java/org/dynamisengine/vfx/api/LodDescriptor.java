package org.dynamisengine.vfx.api;

import java.util.List;

public record LodDescriptor(
    List<LodTier> tiers,
    boolean allowSleeping,
    float sleepingDistance
) {
}
