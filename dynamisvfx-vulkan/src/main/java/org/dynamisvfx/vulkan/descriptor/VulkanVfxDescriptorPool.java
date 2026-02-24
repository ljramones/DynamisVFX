package org.dynamisvfx.vulkan.descriptor;

import org.lwjgl.vulkan.VK10;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class VulkanVfxDescriptorPool {
    public record PoolSize(int descriptorType, int count) {}

    private static final AtomicLong HANDLE_IDS = new AtomicLong(2000L);

    private final long poolHandle;
    private final int maxEffects;
    private final int framesInFlight;
    private final int maxSets;
    private final List<PoolSize> poolSizes;

    private VulkanVfxDescriptorPool(long poolHandle, int maxEffects, int framesInFlight, int maxSets, List<PoolSize> poolSizes) {
        this.poolHandle = poolHandle;
        this.maxEffects = maxEffects;
        this.framesInFlight = framesInFlight;
        this.maxSets = maxSets;
        this.poolSizes = poolSizes;
    }

    public static VulkanVfxDescriptorPool create(long device, int maxEffects, int framesInFlight) {
        if (maxEffects <= 0) {
            throw new IllegalArgumentException("maxEffects must be > 0");
        }
        if (framesInFlight <= 0) {
            throw new IllegalArgumentException("framesInFlight must be > 0");
        }

        int perFrameEffectSets = maxEffects * framesInFlight;
        int uniformBuffers = framesInFlight;
        int storageBuffers = (5 + 4 + 4) * perFrameEffectSets;
        int storageImages = perFrameEffectSets;
        int combinedSamplers = 3 * perFrameEffectSets;

        List<PoolSize> sizes = List.of(
            new PoolSize(VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, uniformBuffers),
            new PoolSize(VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, storageBuffers),
            new PoolSize(VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, storageImages),
            new PoolSize(VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, combinedSamplers)
        );

        int maxSets = framesInFlight + (3 * perFrameEffectSets);
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }

        return new VulkanVfxDescriptorPool(HANDLE_IDS.getAndIncrement(), maxEffects, framesInFlight, maxSets, sizes);
    }

    public void destroy(long device) {
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
    }

    public long poolHandle() {
        return poolHandle;
    }

    public int maxEffects() {
        return maxEffects;
    }

    public int framesInFlight() {
        return framesInFlight;
    }

    public int maxSets() {
        return maxSets;
    }

    public List<PoolSize> poolSizes() {
        return poolSizes;
    }
}
