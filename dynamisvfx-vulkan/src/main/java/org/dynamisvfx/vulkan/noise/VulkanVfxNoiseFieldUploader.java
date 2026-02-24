package org.dynamisvfx.vulkan.noise;

import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisvfx.core.noise.CurlFieldConfig;
import org.dynamisvfx.core.noise.NoiseFieldBaker;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public final class VulkanVfxNoiseFieldUploader {
    private VulkanVfxNoiseFieldUploader() {
    }

    public static void bakeAndUpload(
        long commandBuffer,
        VulkanVfxNoiseField3D noiseField,
        VulkanVfxNoiseFieldConfig config,
        VulkanMemoryOps memoryOps
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(noiseField, "noiseField");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(memoryOps, "memoryOps");

        float[] curlData = NoiseFieldBaker.bake(
            new CurlFieldConfig(config.frequency(), config.octaves(), config.strength(), (int) config.seed()),
            config.gridX(),
            config.gridY(),
            config.gridZ()
        );

        int voxels = config.gridX() * config.gridY() * config.gridZ();
        ByteBuffer packed = ByteBuffer.allocate(voxels * 8).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < voxels; i++) {
            int base = i * 3;
            short hx = floatToHalf(curlData[base]);
            short hy = floatToHalf(curlData[base + 1]);
            short hz = floatToHalf(curlData[base + 2]);
            short hw = floatToHalf(1.0f);
            packed.putShort(hx).putShort(hy).putShort(hz).putShort(hw);
        }

        noiseField.setUploadedData(packed.array());
    }

    static short floatToHalf(float f) {
        int bits = Float.floatToIntBits(f);
        int sign = (bits >>> 16) & 0x8000;
        int val = ((bits & 0x7fffffff) + 0x1000) >>> 13;
        if (val >= 0x7c00) {
            val = ((bits >>> 13) & 0x3ff) | 0x7c00;
        }
        return (short) (sign | val);
    }
}
