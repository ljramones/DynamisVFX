package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.EmissionMode;
import org.dynamisvfx.api.EmissionRateDescriptor;

public final class EmissionRateBuilder {
    private EmissionRateBuilder() {
    }

    public static EmissionRateDescriptor burst(int count) {
        return new EmissionRateDescriptor(EmissionMode.BURST, 0.0f, count, null);
    }

    public static EmissionRateDescriptor continuous(float particlesPerSecond) {
        return new EmissionRateDescriptor(EmissionMode.CONTINUOUS, particlesPerSecond, 0, null);
    }

    public static EmissionRateDescriptor onEvent(String eventKey, int burstCount) {
        return new EmissionRateDescriptor(EmissionMode.EVENT, 0.0f, burstCount, eventKey);
    }
}
