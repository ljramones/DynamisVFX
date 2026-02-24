package org.dynamisvfx.vulkan.renderer;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.compute.VulkanVfxDecalInstanceBuildStage;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxDecalFragmentShaderSource;
import org.dynamisvfx.vulkan.shader.VfxDecalVertexShaderSource;

import java.util.Objects;

public final class VulkanVfxDecalRenderer {
    public static final int DECAL_BOX_VERTEX_COUNT = 36;

    private final VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline;
    private final VulkanVfxDecalInstanceBuildStage instanceBuildStage;

    private int lastAliveCount;
    private float[] lastScreenSize = new float[] {1f, 1f};
    private float lastNormalThreshold;

    private VulkanVfxDecalRenderer(
        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline,
        VulkanVfxDecalInstanceBuildStage instanceBuildStage
    ) {
        this.pipeline = pipeline;
        this.instanceBuildStage = instanceBuildStage;
    }

    public static VulkanVfxDecalRenderer create(
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

        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline =
            VulkanVfxRendererPipelineUtil.create(
                device,
                renderPass,
                VfxDecalVertexShaderSource.GLSL,
                VfxDecalFragmentShaderSource.GLSL,
                setLayouts,
                (Float.BYTES * 2) + Float.BYTES,
                BlendMode.ALPHA
            );

        VulkanVfxDecalInstanceBuildStage buildStage = VulkanVfxDecalInstanceBuildStage.create(device, layout);
        return new VulkanVfxDecalRenderer(pipeline, buildStage);
    }

    public void buildInstanceBuffer(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDecalInstanceBuffer instanceBuffer,
        VulkanVfxDescriptorSets descriptorSets,
        int frameIndex
    ) {
        instanceBuildStage.dispatch(commandBuffer, resources, instanceBuffer, descriptorSets, frameIndex);
    }

    public void recordDraws(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDecalInstanceBuffer instanceBuffer,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        int aliveCount,
        float[] screenSize,
        float normalThreshold
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(instanceBuffer, "instanceBuffer");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        if (set0 == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid set0 handle");
        }
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must be >= 0");
        }
        if (aliveCount < 0) {
            throw new IllegalArgumentException("aliveCount must be >= 0");
        }
        float[] safeScreenSize = normalizeScreenSize(screenSize);
        if (normalThreshold < -1f || normalThreshold > 1f) {
            throw new IllegalArgumentException("normalThreshold must be in [-1, 1]");
        }
        if (instanceBuffer.maxParticles() != resources.config().maxParticles()) {
            throw new IllegalArgumentException("instanceBuffer maxParticles must match effect maxParticles");
        }

        // Placeholder graphics path:
        // - depth test ON, depth write OFF
        // - front-face culling for inside-box projection
        // - alpha blend
        // - binds gBufferNormal sampler at set=3 binding=6
        // vkCmdDraw(vertexCount=36, instanceCount=aliveCount)
        descriptorSets.set1(frameIndex);
        descriptorSets.set3(frameIndex);
        instanceBuffer.bufferHandle();

        this.lastAliveCount = aliveCount;
        this.lastScreenSize = safeScreenSize;
        this.lastNormalThreshold = normalThreshold;
    }

    public void destroy(long device) {
        instanceBuildStage.destroy(device);
        VulkanVfxRendererPipelineUtil.destroy(device, pipeline);
        lastAliveCount = 0;
        lastScreenSize = new float[] {1f, 1f};
        lastNormalThreshold = 0f;
    }

    public VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline() {
        return pipeline;
    }

    public VulkanVfxDecalInstanceBuildStage instanceBuildStage() {
        return instanceBuildStage;
    }

    public int lastAliveCount() {
        return lastAliveCount;
    }

    public float[] lastScreenSize() {
        return lastScreenSize.clone();
    }

    public float lastNormalThreshold() {
        return lastNormalThreshold;
    }

    private static float[] normalizeScreenSize(float[] screenSize) {
        if (screenSize == null || screenSize.length < 2) {
            return new float[] {1f, 1f};
        }
        float w = Math.max(1f, screenSize[0]);
        float h = Math.max(1f, screenSize[1]);
        return new float[] {w, h};
    }
}
