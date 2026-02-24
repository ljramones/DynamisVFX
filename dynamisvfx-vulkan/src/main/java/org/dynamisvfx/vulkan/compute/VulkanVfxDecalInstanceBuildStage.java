package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.renderer.VulkanVfxDecalInstanceBuffer;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.Objects;

public final class VulkanVfxDecalInstanceBuildStage {
    private static final int LOCAL_SIZE_X = 256;

    private static final String SHADER = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=3, binding=0) buffer DrawIndexBuffer { uint drawIndices[]; };
        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];   };
        layout(std430, set=1, binding=3) buffer AttribBuffer    { vec4 attribs[];     };
        layout(std430, set=1, binding=2) buffer ColorBuffer     { vec4 colors[];      };

        struct DecalInstance {
            mat4 inverseTransform;
            vec4 color;
            vec4 params;
        };

        layout(std430, set=3, binding=5) buffer DecalInstanceBuffer {
            DecalInstance decals[];
        };

        layout(push_constant) uniform PushConstants {
            uint maxParticles;
        } push;

        mat4 buildTRS(vec3 position, float rotation, float scale) {
            float c = cos(rotation);
            float s = sin(rotation);
            mat4 m = mat4(1.0);
            m[0][0] = c * scale;  m[0][1] = -s * scale;
            m[1][0] = s * scale;  m[1][1] =  c * scale;
            m[2][2] = scale;
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
            float size = max(attribs[particleIdx].x, 0.0001);
            float rotation = attribs[particleIdx].y;

            mat4 world = buildTRS(pos, rotation, size);
            decals[idx].inverseTransform = inverse(world);
            decals[idx].color = colors[particleIdx];
            decals[idx].params = vec4(0.0, 0.0, 1.0, 1.0);
        }
        """;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;
    private int lastDispatchGroupCount;

    private VulkanVfxDecalInstanceBuildStage(
        VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline
    ) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxDecalInstanceBuildStage create(long device, VulkanVfxDescriptorSetLayout layout) {
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
        return new VulkanVfxDecalInstanceBuildStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDecalInstanceBuffer instanceBuffer,
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

        // Placeholder for compute dispatch with sets 1 and 3.
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
