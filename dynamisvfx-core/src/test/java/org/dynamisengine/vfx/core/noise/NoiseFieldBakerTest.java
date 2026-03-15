package org.dynamisengine.vfx.core.noise;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NoiseFieldBakerTest {

    private CurlFieldConfig defaultConfig(int seed) {
        return new CurlFieldConfig(0.5f, 4, 1.0f, seed);
    }

    @Test
    void bakeProducesCorrectArraySize() {
        float[] field = NoiseFieldBaker.bake(defaultConfig(42), 4, 4, 4);
        assertEquals(4 * 4 * 4 * 3, field.length);
    }

    @Test
    void bakeDeterministicWithSameSeed() {
        CurlFieldConfig cfg = defaultConfig(123);
        float[] a = NoiseFieldBaker.bake(cfg, 4, 4, 4);
        float[] b = NoiseFieldBaker.bake(cfg, 4, 4, 4);
        assertArrayEquals(a, b);
    }

    @Test
    void bakeDifferentSeedsProduceDifferentOutput() {
        float[] a = NoiseFieldBaker.bake(defaultConfig(1), 4, 4, 4);
        float[] b = NoiseFieldBaker.bake(defaultConfig(9999), 4, 4, 4);
        boolean allEqual = true;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) {
                allEqual = false;
                break;
            }
        }
        assertFalse(allEqual, "Different seeds should produce different output");
    }

    @Test
    void bakeProducesNonZeroValues() {
        float[] field = NoiseFieldBaker.bake(defaultConfig(42), 8, 8, 8);
        boolean hasNonZero = false;
        for (float v : field) {
            if (v != 0.0f) {
                hasNonZero = true;
                break;
            }
        }
        assertTrue(hasNonZero, "Baked noise field should contain non-zero values");
    }
}
