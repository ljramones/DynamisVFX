package org.dynamisvfx.vulkan.renderer;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.shader.VfxBeamFragmentShaderSource;
import org.dynamisvfx.vulkan.shader.VfxBeamVertexShaderSource;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxBeamRenderer {
    private final VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline;

    private int lastBeamCount;
    private int lastMaxSegments;
    private float lastTime;

    private VulkanVfxBeamRenderer(VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxBeamRenderer create(
        long device,
        long renderPass,
        VulkanVfxDescriptorSetLayout layout
    ) {
        Objects.requireNonNull(layout, "layout");
        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };

        int pushConstantSizeBytes = Integer.BYTES + Float.BYTES;
        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles handles = VulkanVfxRendererPipelineUtil.create(
            device,
            renderPass,
            VfxBeamVertexShaderSource.GLSL,
            VfxBeamFragmentShaderSource.GLSL,
            setLayouts,
            pushConstantSizeBytes,
            BlendMode.ADDITIVE,
            VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_STRIP
        );
        return new VulkanVfxBeamRenderer(handles);
    }

    public void recordDraws(
        long commandBuffer,
        VulkanVfxBeamEndpointBuffer endpointBuffer,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        int beamCount,
        int maxSegments,
        float time
    ) {
        if (beamCount <= 0) {
            return;
        }
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(endpointBuffer, "endpointBuffer");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        if (set0 == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid set0 handle");
        }
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must be >= 0");
        }
        if (beamCount > endpointBuffer.maxBeams()) {
            throw new IllegalArgumentException("beamCount exceeds endpoint buffer capacity");
        }
        if (maxSegments <= 0) {
            throw new IllegalArgumentException("maxSegments must be > 0");
        }

        // Placeholder graphics path:
        // vkCmdBindPipeline(GRAPHICS)
        // vkCmdBindDescriptorSets(set0,set3)
        // vkCmdPushConstants(maxSegments,time)
        // vkCmdDraw(vertexCount=maxSegments*2, instanceCount=beamCount, firstVertex=0, firstInstance=0)
        descriptorSets.set3(frameIndex);
        endpointBuffer.bufferHandle();

        lastBeamCount = beamCount;
        lastMaxSegments = maxSegments;
        lastTime = time;
    }

    public void destroy(long device) {
        VulkanVfxRendererPipelineUtil.destroy(device, pipeline);
        lastBeamCount = 0;
        lastMaxSegments = 0;
        lastTime = 0f;
    }

    public VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline() {
        return pipeline;
    }

    public int lastBeamCount() {
        return lastBeamCount;
    }

    public int lastMaxSegments() {
        return lastMaxSegments;
    }

    public float lastTime() {
        return lastTime;
    }
}
