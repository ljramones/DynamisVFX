package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxRetireShaderSource;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxRetireStage {
    private static final int LOCAL_SIZE_X = 256;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;

    private int lastDispatchGroupCount;
    private long lastBoundCommandBuffer;
    private long lastBoundSet0;
    private long lastBoundSet1;
    private long lastBoundSet2;

    private VulkanVfxRetireStage(VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxRetireStage create(
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

        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles handles = VulkanVfxComputePipelineUtil.create(
            device,
            VfxRetireShaderSource.GLSL,
            setLayouts,
            Integer.BYTES
        );

        return new VulkanVfxRetireStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");

        int maxParticles = resources.config().maxParticles();
        int groups = (maxParticles + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;

        // Placeholder for real Vulkan calls:
        // vkCmdBindPipeline(COMPUTE)
        // vkCmdBindDescriptorSets(set0, descriptorSets.set1(frame), descriptorSets.set2(frame))
        // vkCmdPushConstants(maxParticles)
        // vkCmdDispatch(groups, 1, 1)
        this.lastDispatchGroupCount = groups;
        this.lastBoundCommandBuffer = commandBuffer;
        this.lastBoundSet0 = set0;
        this.lastBoundSet1 = descriptorSets.set1(frameIndex);
        this.lastBoundSet2 = descriptorSets.set2(frameIndex);
    }

    public static void insertPostRetireBarrier(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        int srcAccess = VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int dstAccess = VK10.VK_ACCESS_SHADER_READ_BIT | VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int srcStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        int dstStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;

        // Placeholder for vkCmdPipelineBarrier using:
        // srcAccess=shader write (FreeList/AliveCount)
        // dstAccess=shader read|write for subsequent stages
        if (srcAccess == 0 || dstAccess == 0 || srcStage == 0 || dstStage == 0) {
            throw new IllegalStateException("Invalid barrier configuration");
        }
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, pipeline);
    }

    public VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline() {
        return pipeline;
    }

    public int lastDispatchGroupCount() {
        return lastDispatchGroupCount;
    }

    public long lastBoundCommandBuffer() {
        return lastBoundCommandBuffer;
    }

    public long lastBoundSet0() {
        return lastBoundSet0;
    }

    public long lastBoundSet1() {
        return lastBoundSet1;
    }

    public long lastBoundSet2() {
        return lastBoundSet2;
    }
}
