package org.dynamisvfx.api;

public record EmissionRateDescriptor(
    EmissionMode mode,
    float particlesPerSecond,
    int burstCount,
    String eventKey
) {
}
