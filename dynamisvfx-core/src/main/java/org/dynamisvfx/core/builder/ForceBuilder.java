package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.ForceDescriptor;
import org.dynamisvfx.api.ForceType;
import org.dynamisvfx.api.NoiseForceConfig;

public final class ForceBuilder {
    private ForceBuilder() {
    }

    public static ForceDescriptor gravity(float strength) {
        return new ForceDescriptor(ForceType.GRAVITY, strength, new float[] {0.0f, -1.0f, 0.0f}, null);
    }

    public static ForceDescriptor drag(float strength) {
        return new ForceDescriptor(ForceType.DRAG, strength, new float[] {0.0f, 0.0f, 0.0f}, null);
    }

    public static ForceDescriptor curlNoise(float frequency, float strength) {
        NoiseForceConfig config = new NoiseForceConfig(frequency, 1.0f, 4, 2.0f, 0.5f, 1.0f, 1337);
        return new ForceDescriptor(ForceType.CURL_NOISE, strength, new float[] {0.0f, 0.0f, 0.0f}, config);
    }

    public static ForceDescriptor wind(float strength, float x, float y, float z) {
        return new ForceDescriptor(ForceType.WIND, strength, new float[] {x, y, z}, null);
    }
}
