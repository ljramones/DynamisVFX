package org.dynamisvfx.vulkan.noise;

import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class VulkanVfxNoiseField3D {
    private static final AtomicLong HANDLE_IDS = new AtomicLong(50_000L);

    private final long imageView;
    private final long sampler;
    private final VulkanVfxNoiseFieldConfig config;
    private byte[] lastUploadedRgba16f;

    private VulkanVfxNoiseField3D(long imageView, long sampler, VulkanVfxNoiseFieldConfig config) {
        this.imageView = imageView;
        this.sampler = sampler;
        this.config = config;
    }

    public static VulkanVfxNoiseField3D allocate(
        long device,
        VulkanMemoryOps memoryOps,
        VulkanVfxNoiseFieldConfig config
    ) {
        if (device == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        Objects.requireNonNull(memoryOps, "memoryOps");
        Objects.requireNonNull(config, "config");
        return new VulkanVfxNoiseField3D(HANDLE_IDS.getAndIncrement(), HANDLE_IDS.getAndIncrement(), config);
    }

    public long imageView() {
        return imageView;
    }

    public long sampler() {
        return sampler;
    }

    public VulkanVfxNoiseFieldConfig config() {
        return config;
    }

    public void setUploadedData(byte[] rgba16f) {
        this.lastUploadedRgba16f = rgba16f == null ? null : rgba16f.clone();
    }

    public byte[] lastUploadedData() {
        return lastUploadedRgba16f == null ? null : lastUploadedRgba16f.clone();
    }

    public void destroy(long device, VulkanMemoryOps memoryOps) {
        if (device == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        Objects.requireNonNull(memoryOps, "memoryOps");
        lastUploadedRgba16f = null;
    }
}
