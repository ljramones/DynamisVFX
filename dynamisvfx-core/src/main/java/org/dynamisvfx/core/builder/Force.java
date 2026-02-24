package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.ForceDescriptor;

public final class Force {
    private Force() {
    }

    public static ForceDescriptor gravity(float strength) {
        return ForceBuilder.gravity(strength);
    }

    public static ForceDescriptor drag(float strength) {
        return ForceBuilder.drag(strength);
    }

    public static ForceDescriptor curlNoise(float frequency, float strength) {
        return ForceBuilder.curlNoise(frequency, strength);
    }

    public static ForceDescriptor wind(float strength, float x, float y, float z) {
        return ForceBuilder.wind(strength, x, y, z);
    }
}
