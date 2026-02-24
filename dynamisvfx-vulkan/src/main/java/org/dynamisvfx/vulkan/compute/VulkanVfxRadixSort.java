package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.shader.VfxRadixHistogramShaderSource;
import org.dynamisvfx.vulkan.shader.VfxRadixPrefixSumShaderSource;
import org.dynamisvfx.vulkan.shader.VfxRadixScatterShaderSource;

import java.util.Arrays;
import java.util.Objects;

public final class VulkanVfxRadixSort {
    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles histogramPipeline;
    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles prefixPipeline;
    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles scatterPipeline;

    private final int maxParticles;
    private final int[] keysPing;
    private final int[] keysPong;
    private final int[] indicesPing;
    private final int[] indicesPong;

    private int[] sortedKeys = new int[0];
    private int[] sortedIndices = new int[0];

    private VulkanVfxRadixSort(
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles histogramPipeline,
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles prefixPipeline,
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles scatterPipeline,
        int maxParticles
    ) {
        this.histogramPipeline = histogramPipeline;
        this.prefixPipeline = prefixPipeline;
        this.scatterPipeline = scatterPipeline;
        this.maxParticles = maxParticles;
        this.keysPing = new int[maxParticles];
        this.keysPong = new int[maxParticles];
        this.indicesPing = new int[maxParticles];
        this.indicesPong = new int[maxParticles];
    }

    public static VulkanVfxRadixSort create(long device, VulkanVfxDescriptorSetLayout layout, int maxParticles) {
        Objects.requireNonNull(layout, "layout");
        if (maxParticles <= 0) {
            throw new IllegalArgumentException("maxParticles must be > 0");
        }

        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };

        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles histogram = VulkanVfxComputePipelineUtil.create(
            device,
            VfxRadixHistogramShaderSource.GLSL,
            setLayouts,
            Integer.BYTES * 2
        );

        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles prefix = VulkanVfxComputePipelineUtil.create(
            device,
            VfxRadixPrefixSumShaderSource.GLSL,
            setLayouts,
            0
        );

        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles scatter = VulkanVfxComputePipelineUtil.create(
            device,
            VfxRadixScatterShaderSource.GLSL,
            setLayouts,
            Integer.BYTES * 2
        );

        return new VulkanVfxRadixSort(histogram, prefix, scatter, maxParticles);
    }

    public void sort(long commandBuffer, int particleCount) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        if (particleCount < 0 || particleCount > maxParticles) {
            throw new IllegalArgumentException("particleCount must be in [0, maxParticles]");
        }

        radixSort(keysPing, indicesPing, particleCount);
        sortedKeys = Arrays.copyOf(keysPing, particleCount);
        sortedIndices = Arrays.copyOf(indicesPing, particleCount);
    }

    public void loadInputs(int[] keys, int[] indices, int particleCount) {
        if (particleCount < 0 || particleCount > maxParticles) {
            throw new IllegalArgumentException("particleCount must be in [0, maxParticles]");
        }
        System.arraycopy(keys, 0, keysPing, 0, particleCount);
        System.arraycopy(indices, 0, indicesPing, 0, particleCount);
    }

    public int[] sortedKeys() {
        return Arrays.copyOf(sortedKeys, sortedKeys.length);
    }

    public int[] sortedIndices() {
        return Arrays.copyOf(sortedIndices, sortedIndices.length);
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, histogramPipeline);
        VulkanVfxComputePipelineUtil.destroy(device, prefixPipeline);
        VulkanVfxComputePipelineUtil.destroy(device, scatterPipeline);
        Arrays.fill(keysPing, 0);
        Arrays.fill(keysPong, 0);
        Arrays.fill(indicesPing, 0);
        Arrays.fill(indicesPong, 0);
        sortedKeys = new int[0];
        sortedIndices = new int[0];
    }

    private void radixSort(int[] keys, int[] indices, int count) {
        int[] inKeys = keys;
        int[] outKeys = keysPong;
        int[] inIndices = indices;
        int[] outIndices = indicesPong;

        for (int pass = 0; pass < 4; pass++) {
            int shift = pass * 8;
            int[] histogram = new int[256];
            for (int i = 0; i < count; i++) {
                int bucket = (inKeys[i] >>> shift) & 0xFF;
                histogram[bucket]++;
            }

            int[] prefix = new int[256];
            int running = 0;
            for (int i = 0; i < 256; i++) {
                prefix[i] = running;
                running += histogram[i];
            }

            for (int i = 0; i < count; i++) {
                int key = inKeys[i];
                int bucket = (key >>> shift) & 0xFF;
                int pos = prefix[bucket]++;
                outKeys[pos] = key;
                outIndices[pos] = inIndices[i];
            }

            int[] tmpKeys = inKeys;
            inKeys = outKeys;
            outKeys = tmpKeys;

            int[] tmpIndices = inIndices;
            inIndices = outIndices;
            outIndices = tmpIndices;
        }

        System.arraycopy(inKeys, 0, keysPing, 0, count);
        System.arraycopy(inIndices, 0, indicesPing, 0, count);
    }
}
