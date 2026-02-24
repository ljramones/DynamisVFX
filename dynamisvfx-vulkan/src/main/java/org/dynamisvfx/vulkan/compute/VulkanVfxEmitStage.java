package org.dynamisvfx.vulkan.compute;

import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.emitter.PackedEmitterDescriptor;
import org.dynamisvfx.vulkan.emitter.VulkanEmitterDescriptorPacker;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.dynamisvfx.vulkan.shader.VfxEmitShaderSource;
import org.lwjgl.vulkan.VK10;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public final class VulkanVfxEmitStage {
    private static final int LOCAL_SIZE_X = 256;

    private final VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline;
    private final Map<Integer, Integer> uploadedDescriptorHashes = new HashMap<>();

    private int lastDispatchGroupCount;
    private long lastBoundSet0;
    private long lastBoundSet1;
    private long lastBoundSet2;

    private VulkanVfxEmitStage(VulkanVfxComputePipelineUtil.VulkanComputePipelineHandles pipeline) {
        this.pipeline = pipeline;
    }

    public static VulkanVfxEmitStage create(
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
            VfxEmitShaderSource.GLSL,
            setLayouts,
            Integer.BYTES * 4
        );

        return new VulkanVfxEmitStage(handles);
    }

    public void dispatch(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        long set0,
        int frameIndex,
        int spawnCount,
        long seed,
        int emitterID
    ) {
        if (spawnCount <= 0) {
            return;
        }
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");

        int groups = (spawnCount + LOCAL_SIZE_X - 1) / LOCAL_SIZE_X;

        // Placeholder for real Vulkan calls:
        // vkCmdBindPipeline(COMPUTE)
        // vkCmdBindDescriptorSets(set0, set1, set2)
        // vkCmdPushConstants(spawnCount, seed, emitterID, frameIndex)
        // vkCmdDispatch(groups, 1, 1)
        this.lastDispatchGroupCount = groups;
        this.lastBoundSet0 = set0;
        this.lastBoundSet1 = descriptorSets.set1(frameIndex);
        this.lastBoundSet2 = descriptorSets.set2(frameIndex);

        long ignoredSeed = seed;
        int ignoredEmitterId = emitterID;
        if (ignoredSeed == Long.MIN_VALUE || ignoredEmitterId == Integer.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid emit push constants");
        }
    }

    public void uploadEmitterDescriptor(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        PackedEmitterDescriptor packed,
        VulkanMemoryOps memoryOps
    ) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(packed, "packed");
        Objects.requireNonNull(memoryOps, "memoryOps");

        int descriptorHash = java.util.Arrays.hashCode(packed.data());
        Integer existing = uploadedDescriptorHashes.get(resources.handle().id());
        if (existing != null && existing == descriptorHash) {
            return;
        }

        // Placeholder: stage packed descriptor bytes into EmitterDescriptorBuffer.
        ByteBuffer upload = ByteBuffer.wrap(packed.data());
        if (upload.remaining() != PackedEmitterDescriptor.SIZE_BYTES) {
            throw new IllegalStateException("Packed descriptor size mismatch");
        }
        memoryOps.getClass();
        resources.controlBuffers().emitterDescriptorBuffer().buffer();

        uploadedDescriptorHashes.put(resources.handle().id(), descriptorHash);
    }

    public void uploadEmitterDescriptor(
        long commandBuffer,
        VulkanVfxEffectResources resources,
        VulkanMemoryOps memoryOps
    ) {
        PackedEmitterDescriptor packed = VulkanEmitterDescriptorPacker.pack(resources.descriptor());
        uploadEmitterDescriptor(commandBuffer, resources, packed, memoryOps);
    }

    public static void insertPostEmitBarrier(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        int srcAccess = VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int dstAccess = VK10.VK_ACCESS_SHADER_READ_BIT | VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int srcStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        int dstStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;

        // Placeholder for vkCmdPipelineBarrier after EMIT stage.
        if (srcAccess == 0 || dstAccess == 0 || srcStage == 0 || dstStage == 0) {
            throw new IllegalStateException("Invalid barrier configuration");
        }
    }

    public void destroy(long device) {
        VulkanVfxComputePipelineUtil.destroy(device, pipeline);
        uploadedDescriptorHashes.clear();
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
