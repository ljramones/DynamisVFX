package org.dynamisvfx.vulkan.descriptor;

import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicLong;

public final class VulkanVfxDescriptorSets {
    private static final AtomicLong HANDLE_IDS = new AtomicLong(3000L);
    private static final long DUMMY_NOISE_IMAGE_VIEW = 9001L;

    private final long[] set1PerFrame;
    private final long[] set2PerFrame;
    private final long[] set3PerFrame;
    private final int framesInFlight;

    private boolean written;

    private VulkanVfxDescriptorSets(long[] set1PerFrame, long[] set2PerFrame, long[] set3PerFrame, int framesInFlight) {
        this.set1PerFrame = set1PerFrame;
        this.set2PerFrame = set2PerFrame;
        this.set3PerFrame = set3PerFrame;
        this.framesInFlight = framesInFlight;
    }

    public static VulkanVfxDescriptorSets allocate(
        long device,
        VulkanVfxDescriptorPool pool,
        VulkanVfxDescriptorSetLayout layout,
        VulkanVfxEffectResources resources,
        int framesInFlight
    ) {
        if (framesInFlight <= 0) {
            throw new IllegalArgumentException("framesInFlight must be > 0");
        }
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        if (pool.framesInFlight() != framesInFlight) {
            throw new IllegalArgumentException("Descriptor pool framesInFlight mismatch");
        }

        layout.set1Layout();
        resources.config();

        long[] set1 = new long[framesInFlight];
        long[] set2 = new long[framesInFlight];
        long[] set3 = new long[framesInFlight];

        for (int i = 0; i < framesInFlight; i++) {
            set1[i] = HANDLE_IDS.getAndIncrement();
            set2[i] = HANDLE_IDS.getAndIncrement();
            set3[i] = HANDLE_IDS.getAndIncrement();
        }

        return new VulkanVfxDescriptorSets(set1, set2, set3, framesInFlight);
    }

    public void writeAll(long device, VulkanVfxEffectResources resources) {
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }

        resources.soaBuffers();
        resources.controlBuffers();
        resources.renderBuffers();
        if (resources.noiseField() != null) {
            resources.noiseField().imageView();
        } else {
            long ignoredDummy = DUMMY_NOISE_IMAGE_VIEW;
            if (ignoredDummy == 0L) {
                throw new IllegalStateException("Dummy noise image view not initialized");
            }
        }
        written = true;
    }

    public void destroy(long device) {
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        Arrays.fill(set1PerFrame, 0L);
        Arrays.fill(set2PerFrame, 0L);
        Arrays.fill(set3PerFrame, 0L);
        written = false;
    }

    public long set1(int frameIndex) {
        return set1PerFrame[normalizeFrame(frameIndex)];
    }

    public long set2(int frameIndex) {
        return set2PerFrame[normalizeFrame(frameIndex)];
    }

    public long set3(int frameIndex) {
        return set3PerFrame[normalizeFrame(frameIndex)];
    }

    public boolean written() {
        return written;
    }

    private int normalizeFrame(int frameIndex) {
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must be >= 0");
        }
        return frameIndex % framesInFlight;
    }
}
