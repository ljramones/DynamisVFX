package org.dynamisvfx.api;

public record PhysicsHandoffDescriptor(
    boolean enabled,
    float speedThreshold,
    String meshId,
    String materialTag,
    float mass
) {
}
