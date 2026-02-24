package org.dynamisvfx.vulkan.compute;

import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisvfx.api.ForceDescriptor;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.force.PackedForceBuffer;
import org.dynamisvfx.vulkan.force.VulkanForceFieldPacker;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxSimulateShaderSource;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class VulkanVfxSimulateStage {
    private static final int LOCAL_SIZE_X = 256;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;
    private final Map<Integer, Integer> uploadedForceHashes = new HashMap<>();

    private int lastDispatchGroupCount;
    private long lastBoundSet0;
    private long lastBoundSet1;
    private long lastBoundSet2;

    private VulkanVfxSimulateStage(VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxSimulateStage create(
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
            VfxSimulateShaderSource.GLSL,
            setLayouts,
            Integer.BYTES + Float.BYTES + Float.BYTES
        );

        return new VulkanVfxSimulateStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        List<ForceDescriptor> forces,
        VulkanMemoryOps memoryOps
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        Objects.requireNonNull(memoryOps, "memoryOps");

        PackedForceBuffer packed = VulkanForceFieldPacker.pack(forces);
        uploadForceBuffer(commandBuffer, resources, packed, memoryOps);

        int maxParticles = resources.config().maxParticles();
        int groups = (maxParticles + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;

        // Placeholder for real Vulkan calls:
        // vkCmdBindPipeline(COMPUTE)
        // vkCmdBindDescriptorSets(set0, set1, set2)
        // vkCmdPushConstants(maxParticles, noiseScale, noiseStrength)
        // vkCmdDispatch(groups, 1, 1)
        this.lastDispatchGroupCount = groups;
        this.lastBoundSet0 = set0;
        this.lastBoundSet1 = descriptorSets.set1(frameIndex);
        this.lastBoundSet2 = descriptorSets.set2(frameIndex);
    }

    public void uploadForceBuffer(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        PackedForceBuffer packed,
        VulkanMemoryOps memoryOps
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(packed, "packed");
        Objects.requireNonNull(memoryOps, "memoryOps");

        int hash = Arrays.hashCode(packed.data()) ^ packed.forceCount();
        Integer existing = uploadedForceHashes.get(resources.handle().id());
        if (existing != null && existing == hash) {
            return;
        }

        ByteBuffer upload = ByteBuffer.allocateDirect(Integer.BYTES * 4 + PackedForceBuffer.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN);
        upload.putInt(packed.forceCount());
        upload.putInt(0);
        upload.putInt(0);
        upload.putInt(0);
        upload.put(packed.data());
        upload.flip();

        // Placeholder: stage upload buffer into ForceFieldBuffer.
        memoryOps.getClass();
        resources.controlBuffers().forceFieldBuffer().buffer();

        uploadedForceHashes.put(resources.handle().id(), hash);
    }

    public static void insertPostSimulateBarrier(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        int srcAccess = VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int dstAccess = VK10.VK_ACCESS_SHADER_READ_BIT | VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int srcStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        int dstStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;

        // Placeholder for vkCmdPipelineBarrier after SIMULATE stage.
        if (srcAccess == 0 || dstAccess == 0 || srcStage == 0 || dstStage == 0) {
            throw new IllegalStateException("Invalid barrier configuration");
        }
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, pipeline);
        uploadedForceHashes.clear();
    }

    public VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline() {
        return pipeline;
    }

    public int lastDispatchGroupCount() {
        return lastDispatchGroupCount;
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
