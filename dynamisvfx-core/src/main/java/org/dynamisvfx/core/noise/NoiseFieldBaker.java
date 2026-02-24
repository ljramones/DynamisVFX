package org.dynamisvfx.core.noise;

import com.cognitivedynamics.noisegen.FastNoiseLite;
import com.cognitivedynamics.noisegen.spatial.TurbulenceNoise;

public final class NoiseFieldBaker {
    private NoiseFieldBaker() {
    }

    public static float[] bake(CurlFieldConfig config, int gridX, int gridY, int gridZ) {
        if (gridX <= 0 || gridY <= 0 || gridZ <= 0) {
            throw new IllegalArgumentException("Grid dimensions must be positive");
        }

        FastNoiseLite noise = new FastNoiseLite(config.seed());
        noise.SetFrequency(config.frequency());

        TurbulenceNoise turbulence = new TurbulenceNoise(noise);
        float[] field = new float[gridX * gridY * gridZ * 3];
        int writeIndex = 0;

        for (int z = 0; z < gridZ; z++) {
            for (int y = 0; y < gridY; y++) {
                for (int x = 0; x < gridX; x++) {
                    float sampleX = x / (float) Math.max(1, gridX - 1);
                    float sampleY = y / (float) Math.max(1, gridY - 1);
                    float sampleZ = z / (float) Math.max(1, gridZ - 1);

                    float[] curl = turbulence.curlFBm3D(sampleX, sampleY, sampleZ, config.octaves());
                    field[writeIndex++] = curl[0] * config.strength();
                    field[writeIndex++] = curl[1] * config.strength();
                    field[writeIndex++] = curl[2] * config.strength();
                }
            }
        }

        return field;
    }
}
