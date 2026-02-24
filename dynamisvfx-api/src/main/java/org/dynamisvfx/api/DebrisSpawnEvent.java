package org.dynamisvfx.api;

public record DebrisSpawnEvent(
    float[] worldTransform,
    float[] velocity,
    float[] angularVelocity,
    float mass,
    String meshId,
    String materialTag,
    int sourceEmitterId
) {
}
