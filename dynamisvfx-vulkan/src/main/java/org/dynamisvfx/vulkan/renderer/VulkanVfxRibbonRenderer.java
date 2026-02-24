package org.dynamisvfx.vulkan.renderer;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxRibbonFragmentShaderSource;
import org.dynamisvfx.vulkan.shader.VfxRibbonVertexShaderSource;

import java.util.Objects;

public final class VulkanVfxRibbonRenderer {
    private final VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline;

    private int lastVertexCount;
    private int lastInstanceCount;

    private VulkanVfxRibbonRenderer(VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxRibbonRenderer create(
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

        int pushConstantSizeBytes = Integer.BYTES + (Float.BYTES * 2);
        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles handles = VulkanVfxRendererPipelineUtil.create(
            device,
            renderPass,
            VfxRibbonVertexShaderSource.GLSL,
            VfxRibbonFragmentShaderSource.GLSL,
            setLayouts,
            pushConstantSizeBytes,
            blendMode
        );

        return new VulkanVfxRibbonRenderer(handles);
    }

    public void recordDraws(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxRibbonHistoryBuffer historyBuffer,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        float ribbonWidth,
        float widthTaper,
        int aliveCount
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(historyBuffer, "historyBuffer");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        if (aliveCount < 0) {
            throw new IllegalArgumentException("aliveCount must be >= 0");
        }
        if (ribbonWidth <= 0f) {
            throw new IllegalArgumentException("ribbonWidth must be > 0");
        }
        if (widthTaper < 0f || widthTaper > 1f) {
            throw new IllegalArgumentException("widthTaper must be in [0, 1]");
        }
        if (historyBuffer.maxParticles() != resources.config().maxParticles()) {
            throw new IllegalArgumentException("history buffer maxParticles must match effect maxParticles");
        }

        // Placeholder graphics recording path:
        // vkCmdBindPipeline(GRAPHICS)
        // vkCmdBindDescriptorSets(set0, set1, set3 with binding 5 history buffer)
        // vkCmdPushConstants(historyLength, ribbonWidth, widthTaper)
        // vkCmdDraw(vertexCount=historyLength*2, instanceCount=aliveCount, firstVertex=0, firstInstance=0)
        long ignoredSet0 = set0;
        if (ignoredSet0 == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid set0 handle");
        }
        descriptorSets.set1(frameIndex);
        descriptorSets.set3(frameIndex);
        historyBuffer.bufferHandle();

        this.lastVertexCount = historyBuffer.historyLength() * 2;
        this.lastInstanceCount = aliveCount;
    }

    public void destroy(long device) {
        VulkanVfxRendererPipelineUtil.destroy(device, pipeline);
        lastVertexCount = 0;
        lastInstanceCount = 0;
    }

    public VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline() {
        return pipeline;
    }

    public int lastVertexCount() {
        return lastVertexCount;
    }

    public int lastInstanceCount() {
        return lastInstanceCount;
    }
}
