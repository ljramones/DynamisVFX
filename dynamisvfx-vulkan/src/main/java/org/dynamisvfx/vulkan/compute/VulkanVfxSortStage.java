package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxSortKeyGenShaderSource;
import org.lwjgl.vulkan.VK10;

import java.util.Arrays;
import java.util.Objects;

public final class VulkanVfxSortStage {
    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles keyGenPipeline;
    private final VulkanVfxRadixSort radixSort;

    private boolean lastSkipped;
    private int[] lastSortedKeys = new int[0];
    private int[] lastSortedIndices = new int[0];

    private VulkanVfxSortStage(
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles keyGenPipeline,
        VulkanVfxRadixSort radixSort
    ) {
        this.keyGenPipeline = keyGenPipeline;
        this.radixSort = radixSort;
    }

    public static VulkanVfxSortStage create(long device, VulkanVfxDescriptorSetLayout layout, int maxParticles) {
        Objects.requireNonNull(layout, "layout");

        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };

        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles keyGen = VulkanVfxComputePipelineUtil.create(
            device,
            VfxSortKeyGenShaderSource.GLSL,
            setLayouts,
            Integer.BYTES + (Float.BYTES * 3)
        );

        VulkanVfxRadixSort radixSort = VulkanVfxRadixSort.create(device, layout, maxParticles);
        return new VulkanVfxSortStage(keyGen, radixSort);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        BlendMode blendMode,
        float[] cameraPos,
        int aliveCount,
        int frameIndex
    ) {
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");

        if (blendMode == BlendMode.ADDITIVE) {
            lastSkipped = true;
            lastSortedKeys = new int[0];
            lastSortedIndices = new int[0];
            return;
        }

        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        int count = Math.max(0, Math.min(aliveCount, resources.config().maxParticles()));
        int[] keys = new int[count];
        int[] indices = new int[count];

        float camX = valueAt(cameraPos, 0);
        float camY = valueAt(cameraPos, 1);
        float camZ = valueAt(cameraPos, 2);
        int cameraBias = Math.round((camX + camY + camZ) * 1000f);

        // Placeholder for sort-key generation based on particle distance.
        for (int i = 0; i < count; i++) {
            float distSquared = (float) Math.pow(count - i, 2);
            int key = floatToSortableUint(distSquared) ^ cameraBias;
            keys[i] = key;
            indices[i] = i;
        }

        radixSort.loadInputs(keys, indices, count);
        radixSort.sort(commandBuffer, count);

        lastSkipped = false;
        lastSortedKeys = radixSort.sortedKeys();
        lastSortedIndices = radixSort.sortedIndices();

        long ignoredSet1 = descriptorSets.set1(frameIndex);
        if (ignoredSet1 == Long.MIN_VALUE) {
            throw new IllegalStateException("Invalid descriptor set");
        }
    }

    public void dispatchWithMockDistances(long commandBuffer, BlendMode blendMode, float[] distances) {
        if (blendMode == BlendMode.ADDITIVE) {
            lastSkipped = true;
            lastSortedKeys = new int[0];
            lastSortedIndices = new int[0];
            return;
        }

        int count = distances == null ? 0 : distances.length;
        int[] keys = new int[count];
        int[] indices = new int[count];
        for (int i = 0; i < count; i++) {
            keys[i] = floatToSortableUint(distances[i]);
            indices[i] = i;
        }

        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        radixSort.loadInputs(keys, indices, count);
        radixSort.sort(commandBuffer, count);

        lastSkipped = false;
        lastSortedKeys = radixSort.sortedKeys();
        lastSortedIndices = radixSort.sortedIndices();
    }

    public static void insertPostSortBarrier(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        int srcAccess = VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int dstAccess = VK10.VK_ACCESS_SHADER_READ_BIT | VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int srcStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        int dstStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;

        if (srcAccess == 0 || dstAccess == 0 || srcStage == 0 || dstStage == 0) {
            throw new IllegalStateException("Invalid barrier configuration");
        }
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, keyGenPipeline);
        radixSort.destroy(device);
        lastSkipped = false;
        lastSortedKeys = new int[0];
        lastSortedIndices = new int[0];
    }

    public boolean lastSkipped() {
        return lastSkipped;
    }

    public int[] lastSortedKeys() {
        return Arrays.copyOf(lastSortedKeys, lastSortedKeys.length);
    }

    public int[] lastSortedIndices() {
        return Arrays.copyOf(lastSortedIndices, lastSortedIndices.length);
    }

    private static int floatToSortableUint(float f) {
        int u = Float.floatToRawIntBits(f);
        int mask = (u >> 31) | 0x80000000;
        return u ^ mask;
    }

    private static float valueAt(float[] values, int index) {
        return values != null && index < values.length ? values[index] : 0.0f;
    }
}
