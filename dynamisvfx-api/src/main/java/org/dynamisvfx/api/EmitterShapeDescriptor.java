package org.dynamisvfx.api;

public record EmitterShapeDescriptor(
    EmitterShapeType type,
    float[] dimensions,
    String sourceMeshId,
    String sourceSplineId
) {
}
