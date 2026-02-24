package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.EmitterShapeDescriptor;

public final class EmitterShape {
    private EmitterShape() {
    }

    public static EmitterShapeDescriptor point() {
        return EmitterShapeBuilder.point();
    }

    public static EmitterShapeDescriptor sphere(float radius) {
        return EmitterShapeBuilder.sphere(radius);
    }

    public static EmitterShapeDescriptor cone(float radius, float height) {
        return EmitterShapeBuilder.cone(radius, height);
    }

    public static EmitterShapeDescriptor meshSurface(String meshId) {
        return EmitterShapeBuilder.meshSurface(meshId);
    }

    public static EmitterShapeDescriptor spline(String splineId) {
        return EmitterShapeBuilder.spline(splineId);
    }
}
