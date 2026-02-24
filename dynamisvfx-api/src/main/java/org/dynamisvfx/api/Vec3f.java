package org.dynamisvfx.api;

public record Vec3f(float x, float y, float z) {
    public static final Vec3f ZERO = new Vec3f(0.0f, 0.0f, 0.0f);
}
