package org.dynamisvfx.api;

public record DebrisSpawnEvent(
    Mat4f worldTransform,
    Vec3f velocity,
    Vec3f angularVelocity,
    float mass,
    String meshId,
    String materialTag,
    int sourceEmitterId
) {
}
