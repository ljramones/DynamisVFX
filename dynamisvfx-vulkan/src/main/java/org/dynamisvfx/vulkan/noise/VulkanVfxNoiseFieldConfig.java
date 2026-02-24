package org.dynamisvfx.vulkan.noise;

import org.dynamisvfx.api.NoiseForceConfig;

public record VulkanVfxNoiseFieldConfig(
    int gridX,
    int gridY,
    int gridZ,
    float worldScale,
    float strength,
    long seed,
    int octaves,
    float frequency
) {
    public static VulkanVfxNoiseFieldConfig defaults() {
        return new VulkanVfxNoiseFieldConfig(64, 64, 64, 100f, 1f, 12345L, 3, 0.02f);
    }

    public static VulkanVfxNoiseFieldConfig from(NoiseForceConfig apiConfig) {
        if (apiConfig == null) {
            return defaults();
        }
        return new VulkanVfxNoiseFieldConfig(
            64,
            64,
            64,
            100f,
            apiConfig.amplitude() <= 0f ? 1f : apiConfig.amplitude(),
            apiConfig.seed(),
            Math.max(1, apiConfig.octaves()),
            apiConfig.frequency() <= 0f ? 0.02f : apiConfig.frequency()
        );
    }
}
