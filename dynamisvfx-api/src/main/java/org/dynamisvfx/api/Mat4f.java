package org.dynamisvfx.api;

import java.util.Arrays;

public final class Mat4f {
    private static final int ELEMENT_COUNT = 16;
    private final float[] elements;

    private Mat4f(float[] elements) {
        this.elements = elements;
    }

    public static Mat4f identity() {
        return fromArray(new float[] {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        });
    }

    public static Mat4f fromArray(float[] values) {
        if (values == null || values.length != ELEMENT_COUNT) {
            throw new IllegalArgumentException("Matrix requires exactly 16 values");
        }
        return new Mat4f(Arrays.copyOf(values, values.length));
    }

    public float[] toArray() {
        return Arrays.copyOf(elements, elements.length);
    }
}
