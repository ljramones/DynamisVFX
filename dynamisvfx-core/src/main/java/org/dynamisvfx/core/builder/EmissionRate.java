package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.EmissionRateDescriptor;

public final class EmissionRate {
    private EmissionRate() {
    }

    public static EmissionRateDescriptor burst(int count) {
        return EmissionRateBuilder.burst(count);
    }

    public static EmissionRateDescriptor continuous(float particlesPerSecond) {
        return EmissionRateBuilder.continuous(particlesPerSecond);
    }

    public static EmissionRateDescriptor onEvent(String eventKey, int burstCount) {
        return EmissionRateBuilder.onEvent(eventKey, burstCount);
    }
}
