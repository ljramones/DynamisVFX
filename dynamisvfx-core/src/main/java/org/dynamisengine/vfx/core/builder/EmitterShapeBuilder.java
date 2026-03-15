package org.dynamisengine.vfx.core.builder;

import org.dynamisengine.vfx.api.EmitterShapeDescriptor;
import org.dynamisengine.vfx.api.EmitterShapeType;

public final class EmitterShapeBuilder {
    private EmitterShapeBuilder() {
    }

    public static EmitterShapeDescriptor point() {
        return new EmitterShapeDescriptor(EmitterShapeType.POINT, null, null, null);
    }

    public static EmitterShapeDescriptor sphere(float radius) {
        return new EmitterShapeDescriptor(EmitterShapeType.SPHERE, new float[] {radius, radius, radius}, null, null);
    }

    public static EmitterShapeDescriptor cone(float radius, float height) {
        return new EmitterShapeDescriptor(EmitterShapeType.CONE, new float[] {radius, height, 0.0f}, null, null);
    }

    public static EmitterShapeDescriptor meshSurface(String meshId) {
        return new EmitterShapeDescriptor(EmitterShapeType.MESH_SURFACE, null, meshId, null);
    }

    public static EmitterShapeDescriptor spline(String splineId) {
        return new EmitterShapeDescriptor(EmitterShapeType.SPLINE, null, null, splineId);
    }
}
