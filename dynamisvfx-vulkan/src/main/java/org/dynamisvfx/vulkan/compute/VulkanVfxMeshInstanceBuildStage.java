package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.renderer.VulkanVfxMeshInstanceBuffer;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.Objects;

public final class VulkanVfxMeshInstanceBuildStage {
    private static final int LOCAL_SIZE_X = 256;

    private static final String SHADER = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=3, binding=0) buffer DrawIndexBuffer { uint drawIndices[]; };
        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];   };
        layout(std430, set=1, binding=3) buffer AttribBuffer    { vec4 attribs[];     };
        layout(std430, set=3, binding=5) buffer MeshInstances   { mat4 instances[];   };

        layout(push_constant) uniform PushConstants {
            uint maxParticles;
        } push;

        mat4 composeTRS(vec3 position, float rotation, float uniformScale) {
            float c = cos(rotation);
            float s = sin(rotation);
            mat4 m = mat4(1.0);
            m[0][0] = c * uniformScale;  m[0][1] = -s * uniformScale;
            m[1][0] = s * uniformScale;  m[1][1] =  c * uniformScale;
            m[2][2] = uniformScale;
            m[3][0] = position.x;
            m[3][1] = position.y;
            m[3][2] = position.z;
            return m;
        }

        void main() {
            uint idx = gl_GlobalInvocationID.x;
            if (idx >= push.maxParticles) return;

            uint particleIdx = drawIndices[idx];
            vec3 pos = positions[particleIdx].xyz;
            float size = attribs[particleIdx].x;
            float rotation = attribs[particleIdx].y;
            instances[idx] = composeTRS(pos, rotation, max(size, 0.0001));
        }
        """;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;
    private int lastDispatchGroupCount;

    private VulkanVfxMeshInstanceBuildStage(
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline
    ) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxMeshInstanceBuildStage create(long device, VulkanVfxDescriptorSetLayout layout) {
        Objects.requireNonNull(layout, "layout");
        long[] setLayouts = new long[] {
            layout.set0Layout(),
            layout.set1Layout(),
            layout.set2Layout(),
            layout.set3Layout()
        };

        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles handles = VulkanVfxComputePipelineUtil.create(
            device,
            SHADER,
            setLayouts,
            Integer.BYTES
        );
        return new VulkanVfxMeshInstanceBuildStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxMeshInstanceBuffer instanceBuffer,
        VulkanVfxDescriptorSets descriptorSets,
        int frameIndex
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(instanceBuffer, "instanceBuffer");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must be >= 0");
        }
        if (instanceBuffer.maxParticles() != resources.config().maxParticles()) {
            throw new IllegalArgumentException("instanceBuffer maxParticles must match effect maxParticles");
        }

        int maxParticles = resources.config().maxParticles();
        int groups = (maxParticles + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;

        // Placeholder for vkCmdBindPipeline + vkCmdBindDescriptorSets(set1,set3) + push constants + dispatch.
        descriptorSets.set1(frameIndex);
        descriptorSets.set3(frameIndex);
        instanceBuffer.bufferHandle();
        this.lastDispatchGroupCount = groups;
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, pipeline);
        lastDispatchGroupCount = 0;
    }

    public int lastDispatchGroupCount() {
        return lastDispatchGroupCount;
    }
}
