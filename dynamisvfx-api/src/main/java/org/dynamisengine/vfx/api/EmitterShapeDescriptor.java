package org.dynamisengine.vfx.api;

public record EmitterShapeDescriptor(
    EmitterShapeType type,
    float[] dimensions,
    String sourceMeshId,
    String sourceSplineId
) {
}
