package org.dynamisengine.vfx.api;

public record PhysicsHandoffDescriptor(
    boolean enabled,
    float speedThreshold,
    String meshId,
    String materialTag,
    float mass
) {
}
