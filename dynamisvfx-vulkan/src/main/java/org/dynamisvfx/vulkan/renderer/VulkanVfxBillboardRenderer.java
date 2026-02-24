package org.dynamisvfx.vulkan.renderer;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxBillboardFragmentShaderSource;
import org.dynamisvfx.vulkan.shader.VfxBillboardVertexShaderSource;

import java.util.Objects;

public final class VulkanVfxBillboardRenderer {
    private final VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline;

    private boolean lastSoftParticles;
    private float lastSoftRange;
    private int lastFrameCount;
    private float lastFrameRate;

    private VulkanVfxBillboardRenderer(VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxBillboardRenderer create(
        long device,
        long renderPass,
        VulkanVfxDescriptorSetLayout layout,
        BlendMode blendMode
    ) {
        Objects.requireNonNull(layout, "layout");
        Objects.requireNonNull(blendMode, "blendMode");

        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };

        int pushConstantSizeBytes = (Integer.BYTES * 2) + (Float.BYTES * 2);
        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles handles = VulkanVfxRendererPipelineUtil.create(
            device,
            renderPass,
            VfxBillboardVertexShaderSource.GLSL,
            VfxBillboardFragmentShaderSource.GLSL,
            setLayouts,
            pushConstantSizeBytes,
            blendMode
        );

        return new VulkanVfxBillboardRenderer(handles);
    }

    public void recordDraws(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        boolean softParticles,
        float softRange,
        int frameCount,
        float frameRate
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");

        if (frameCount <= 0) {
            throw new IllegalArgumentException("frameCount must be > 0");
        }
        if (frameRate < 0f) {
            throw new IllegalArgumentException("frameRate must be >= 0");
        }
        if (softRange <= 0f) {
            throw new IllegalArgumentException("softRange must be > 0");
        }

        // Placeholder for real graphics path:
        // vkCmdBindPipeline(GRAPHICS)
        // vkCmdBindDescriptorSets(set0, set1, set3)
        // vkCmdPushConstants(softParticles, softRange, frameCount, frameRate)
        // vkCmdDrawIndirect(resources.renderBuffers().indirectCommandBuffer().bufferHandle(), ...)
        descriptorSets.set1(frameIndex);
        descriptorSets.set3(frameIndex);
        resources.renderBuffers().indirectCommandBuffer().bufferHandle();
        long ignoredSet0 = set0;
        if (ignoredSet0 == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid set0 handle");
        }

        this.lastSoftParticles = softParticles;
        this.lastSoftRange = softRange;
        this.lastFrameCount = frameCount;
        this.lastFrameRate = frameRate;
    }

    public void destroy(long device) {
        VulkanVfxRendererPipelineUtil.destroy(device, pipeline);
    }

    public VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline() {
        return pipeline;
    }

    public boolean lastSoftParticles() {
        return lastSoftParticles;
    }

    public float lastSoftRange() {
        return lastSoftRange;
    }

    public int lastFrameCount() {
        return lastFrameCount;
    }

    public float lastFrameRate() {
        return lastFrameRate;
    }
}
