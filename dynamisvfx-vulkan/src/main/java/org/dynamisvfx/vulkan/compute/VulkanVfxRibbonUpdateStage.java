package org.dynamisvfx.vulkan.compute;

import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.renderer.VulkanVfxRibbonHistoryBuffer;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.Objects;

public final class VulkanVfxRibbonUpdateStage {
    private static final int LOCAL_SIZE_X = 256;

    private static final String SHADER = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=1, binding=0) buffer PositionBuffer { vec4 positions[]; };
        layout(std430, set=3, binding=5) buffer RibbonHistory  { vec4 history[];   };

        layout(push_constant) uniform PushConstants {
            uint maxParticles;
            uint historyLength;
        } push;

        void main() {
            uint particleIdx = gl_GlobalInvocationID.x;
            if (particleIdx >= push.maxParticles) return;

            uint base = particleIdx * push.historyLength;
            for (uint i = push.historyLength - 1u; i > 0u; --i) {
                history[base + i] = history[base + i - 1u];
            }
            vec4 p = positions[particleIdx];
            history[base] = vec4(p.xyz, p.w < 1.0 ? p.w : -1.0);
        }
        """;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;
    private int lastDispatchGroupCount;

    private VulkanVfxRibbonUpdateStage(VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxRibbonUpdateStage create(long device, VulkanVfxDescriptorSetLayout layout) {
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
            Integer.BYTES * 2
        );
        return new VulkanVfxRibbonUpdateStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxRibbonHistoryBuffer historyBuffer,
        int frameIndex
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(historyBuffer, "historyBuffer");
        if (frameIndex < 0) {
            throw new IllegalArgumentException("frameIndex must be >= 0");
        }
        if (historyBuffer.maxParticles() != resources.config().maxParticles()) {
            throw new IllegalArgumentException("history buffer maxParticles must match effect maxParticles");
        }

        int groups = (resources.config().maxParticles() + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;
        // Placeholder for vkCmdBindPipeline + vkCmdBindDescriptorSets + vkCmdPushConstants + vkCmdDispatch.
        historyBuffer.bufferHandle();
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
