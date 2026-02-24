package org.dynamisvfx.vulkan.renderer;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.vulkan.compute.VulkanVfxMeshInstanceBuildStage;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxMeshFragmentShaderSource;
import org.dynamisvfx.vulkan.shader.VfxMeshVertexShaderSource;

import java.util.Objects;

public final class VulkanVfxMeshRenderer {
    public static final int VERTEX_STRIDE_BYTES = 32;
    public static final int POSITION_OFFSET_BYTES = 0;
    public static final int NORMAL_OFFSET_BYTES = 12;
    public static final int UV_OFFSET_BYTES = 24;

    private final VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline;
    private final VulkanVfxMeshInstanceBuildStage instanceBuildStage;

    private int lastMeshIndexCount;
    private int lastAliveCount;
    private float[] lastLightDir = new float[] {0f, -1f, 0f};
    private float lastAmbientStrength;

    private VulkanVfxMeshRenderer(
        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline,
        VulkanVfxMeshInstanceBuildStage instanceBuildStage
    ) {
        this.pipeline = pipeline;
        this.instanceBuildStage = instanceBuildStage;
    }

    public static VulkanVfxMeshRenderer create(
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

        VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline =
            VulkanVfxRendererPipelineUtil.create(
                device,
                renderPass,
                VfxMeshVertexShaderSource.GLSL,
                VfxMeshFragmentShaderSource.GLSL,
                setLayouts,
                Float.BYTES * 4,
                blendMode
            );

        VulkanVfxMeshInstanceBuildStage buildStage = VulkanVfxMeshInstanceBuildStage.create(device, layout);
        return new VulkanVfxMeshRenderer(pipeline, buildStage);
    }

    public void buildInstanceBuffer(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxMeshInstanceBuffer instanceBuffer,
        VulkanVfxDescriptorSets descriptorSets,
        int frameIndex
    ) {
        instanceBuildStage.dispatch(commandBuffer, resources, instanceBuffer, descriptorSets, frameIndex);
    }

    public void recordDraws(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxMeshInstanceBuffer instanceBuffer,
        long meshVertexBuffer,
        long meshIndexBuffer,
        int meshIndexCount,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        int aliveCount,
        float[] lightDir,
        float ambientStrength
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(instanceBuffer, "instanceBuffer");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        if (meshVertexBuffer == 0L || meshIndexBuffer == 0L) {
            throw new IllegalArgumentException("mesh buffers must be non-zero");
        }
        if (meshIndexCount <= 0) {
            throw new IllegalArgumentException("meshIndexCount must be > 0");
        }
        if (aliveCount < 0) {
            throw new IllegalArgumentException("aliveCount must be >= 0");
        }
        if (ambientStrength < 0f || ambientStrength > 1f) {
            throw new IllegalArgumentException("ambientStrength must be in [0, 1]");
        }

        float[] normalizedLightDir = normalizeLightDir(lightDir);

        // Placeholder graphics path:
        // vkCmdBindPipeline(GRAPHICS)
        // vkCmdBindVertexBuffers(meshVertexBuffer)
        // vkCmdBindIndexBuffer(meshIndexBuffer)
        // vkCmdBindDescriptorSets(set0,set1,set3)
        // vkCmdPushConstants(lightDir.xyz, ambientStrength)
        // vkCmdDrawIndexed(meshIndexCount, aliveCount, 0, 0, 0)
        long ignoredSet0 = set0;
        if (ignoredSet0 == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid set0 handle");
        }
        descriptorSets.set1(frameIndex);
        descriptorSets.set3(frameIndex);
        instanceBuffer.bufferHandle();

        this.lastMeshIndexCount = meshIndexCount;
        this.lastAliveCount = aliveCount;
        this.lastLightDir = normalizedLightDir;
        this.lastAmbientStrength = ambientStrength;
    }

    public void destroy(long device) {
        instanceBuildStage.destroy(device);
        VulkanVfxRendererPipelineUtil.destroy(device, pipeline);
        lastMeshIndexCount = 0;
        lastAliveCount = 0;
        lastLightDir = new float[] {0f, -1f, 0f};
        lastAmbientStrength = 0f;
    }

    public VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles pipeline() {
        return pipeline;
    }

    public VulkanVfxMeshInstanceBuildStage instanceBuildStage() {
        return instanceBuildStage;
    }

    public int lastMeshIndexCount() {
        return lastMeshIndexCount;
    }

    public int lastAliveCount() {
        return lastAliveCount;
    }

    public float[] lastLightDir() {
        return lastLightDir.clone();
    }

    public float lastAmbientStrength() {
        return lastAmbientStrength;
    }

    private static float[] normalizeLightDir(float[] lightDir) {
        if (lightDir == null || lightDir.length < 3) {
            return new float[] {0f, -1f, 0f};
        }
        float x = lightDir[0];
        float y = lightDir[1];
        float z = lightDir[2];
        float len = (float) Math.sqrt((x * x) + (y * y) + (z * z));
        if (len <= 1e-6f) {
            return new float[] {0f, -1f, 0f};
        }
        return new float[] {x / len, y / len, z / len};
    }
}
