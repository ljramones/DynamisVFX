package org.dynamisvfx.vulkan.physics;

import org.dynamisvfx.vulkan.compute.VulkanVfxComputePipelineUtil;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxDebrisCandidateShaderSource;

import java.util.List;
import java.util.Objects;

public final class VulkanVfxDebrisCandidateWriter {
    private static final int LOCAL_SIZE_X = 256;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;

    private VulkanVfxDebrisCandidateWriter(VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxDebrisCandidateWriter create(long device, VulkanVfxDescriptorSetLayout layout) {
        Objects.requireNonNull(layout, "layout");
        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles handles = VulkanVfxComputePipelineUtil.create(
            device,
            VfxDebrisCandidateShaderSource.GLSL,
            setLayouts,
            Integer.BYTES + Float.BYTES + Float.BYTES + Integer.BYTES
        );
        return new VulkanVfxDebrisCandidateWriter(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        VulkanVfxDebrisReadbackBuffer writeBuffer,
        long set0,
        int frameIndex,
        float debrisAgeThreshold,
        float debrisSpeedThreshold
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        Objects.requireNonNull(writeBuffer, "writeBuffer");

        writeBuffer.resetOnGpu(commandBuffer);
        descriptorSets.set1(frameIndex);
        descriptorSets.set3(frameIndex);
        long ignoredSet0 = set0;
        if (ignoredSet0 == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid set0 handle");
        }

        int maxParticles = resources.config().maxParticles();
        int groups = (maxParticles + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
        if (groups < 0) {
            throw new IllegalStateException("Invalid dispatch group count");
        }

        // Placeholder: deterministic single candidate generation when thresholds are met.
        if (debrisAgeThreshold <= 1.0f && debrisSpeedThreshold <= 10.0f) {
            VulkanVfxDebrisCandidate candidate = new VulkanVfxDebrisCandidate(
                0f, 0f, 0f, 1f,
                7f, 0f, 0f, 0f,
                1, 1, resources.handle().id(), 1
            );
            writeBuffer.writeMockCandidates(List.of(candidate));
        }
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, pipeline);
    }
}
