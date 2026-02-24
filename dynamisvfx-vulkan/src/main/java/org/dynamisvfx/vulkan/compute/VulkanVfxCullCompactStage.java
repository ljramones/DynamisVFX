package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.indirect.VulkanVfxIndirectWriter;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxCullCompactShaderSource;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxCullCompactStage {
    private static final int LOCAL_SIZE_X = 256;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;

    private int lastDispatchGroupCount;
    private int lastInstanceCount;
    private long lastBoundSet0;
    private long lastBoundSet1;
    private long lastBoundSet3;

    private VulkanVfxCullCompactStage(VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxCullCompactStage create(
        long device,
        VulkanVfxDescriptorSetLayout layout
    ) {
        Objects.requireNonNull(layout, "layout");
        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };

        int pushConstantBytes = (Integer.BYTES * 2) + (Float.BYTES * 24);
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles handles = VulkanVfxComputePipelineUtil.create(
            device,
            VfxCullCompactShaderSource.GLSL,
            setLayouts,
            pushConstantBytes
        );

        return new VulkanVfxCullCompactStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        boolean useSortedIndices,
        float[] frustumPlanes6x4
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        validateFrustum(frustumPlanes6x4);

        VulkanVfxIndirectWriter.resetIndirectCommand(commandBuffer, resources);

        int maxParticles = resources.config().maxParticles();
        int groups = (maxParticles + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;

        // Placeholder for real Vulkan dispatch:
        // vkCmdBindPipeline(COMPUTE)
        // vkCmdBindDescriptorSets(set0, set1, set3)
        // vkCmdPushConstants(maxParticles, useSortedIndices, frustumPlanes)
        // vkCmdDispatch(groups, 1, 1)
        this.lastDispatchGroupCount = groups;
        this.lastBoundSet0 = set0;
        this.lastBoundSet1 = descriptorSets.set1(frameIndex);
        this.lastBoundSet3 = descriptorSets.set3(frameIndex);

        // Placeholder visibility model until GPU readback path is wired.
        this.lastInstanceCount = useSortedIndices ? Math.max(0, maxParticles / 2) : maxParticles;
    }

    public int dispatchWithMockAliveCount(
        long commandBuffer,
        int aliveCount,
        boolean useSortedIndices,
        float[] frustumPlanes6x4
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        validateFrustum(frustumPlanes6x4);
        if (aliveCount < 0) {
            throw new IllegalArgumentException("aliveCount must be >= 0");
        }

        this.lastDispatchGroupCount = (aliveCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
        this.lastInstanceCount = useSortedIndices ? Math.max(0, aliveCount - (aliveCount / 10)) : aliveCount;
        return this.lastInstanceCount;
    }

    public static void insertPostCullBarrier(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        int srcAccess = VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int dstAccess = VK10.VK_ACCESS_INDIRECT_COMMAND_READ_BIT | VK10.VK_ACCESS_SHADER_READ_BIT;
        int srcStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        int dstStage = VK10.VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT | VK10.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
        if (srcAccess == 0 || dstAccess == 0 || srcStage == 0 || dstStage == 0) {
            throw new IllegalStateException("Invalid post-cull barrier configuration");
        }
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, pipeline);
        lastDispatchGroupCount = 0;
        lastInstanceCount = 0;
        lastBoundSet0 = 0L;
        lastBoundSet1 = 0L;
        lastBoundSet3 = 0L;
    }

    public int lastDispatchGroupCount() {
        return lastDispatchGroupCount;
    }

    public int lastInstanceCount() {
        return lastInstanceCount;
    }

    public long lastBoundSet0() {
        return lastBoundSet0;
    }

    public long lastBoundSet1() {
        return lastBoundSet1;
    }

    public long lastBoundSet3() {
        return lastBoundSet3;
    }

    private static void validateFrustum(float[] frustumPlanes6x4) {
        if (frustumPlanes6x4 == null || frustumPlanes6x4.length != 24) {
            throw new IllegalArgumentException("frustumPlanes6x4 must contain exactly 24 floats");
        }
    }
}
