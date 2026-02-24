package org.dynamisvfx.api;

public record EmitterShapeDescriptor(
    EmitterShapeType type,
    Vec3f dimensions,
    String sourceMeshId,
    String sourceSplineId
) {
}
