package org.dynamisvfx.vulkan.indirect;

import org.dynamisgpu.api.gpu.IndirectCommandBuffer;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;
import org.lwjgl.vulkan.VK10;

import java.util.Objects;

public final class VulkanVfxIndirectWriter {
    private VulkanVfxIndirectWriter() {
    }

    public static final int BILLBOARD_INDEX_COUNT = 6;

    public static void resetIndirectCommand(long commandBuffer, VulkanVfxEffectResources resources) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }
        Objects.requireNonNull(resources, "resources");

        resources.renderBuffers().indirectCommandBuffer()
            .writeCommand(0, BILLBOARD_INDEX_COUNT, 0, 0, 0, 0);
    }

    public static void writeDrawCommand(IndirectCommandBuffer indirectBuffer, int slot, int instanceCount) {
        Objects.requireNonNull(indirectBuffer, "indirectBuffer");
        if (slot < 0) {
            throw new IllegalArgumentException("slot must be >= 0");
        }
        if (instanceCount < 0) {
            throw new IllegalArgumentException("instanceCount must be >= 0");
        }

        indirectBuffer.writeCommand(slot, BILLBOARD_INDEX_COUNT, instanceCount, 0, 0, 0);
    }

    public static void insertPreDrawBarrier(long commandBuffer) {
        if (commandBuffer == 0L) {
            throw new IllegalArgumentException("commandBuffer must be non-zero");
        }

        int srcAccess = VK10.VK_ACCESS_SHADER_WRITE_BIT;
        int dstAccess = VK10.VK_ACCESS_INDIRECT_COMMAND_READ_BIT | VK10.VK_ACCESS_SHADER_READ_BIT;
        int srcStage = VK10.VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT;
        int dstStage = VK10.VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT | VK10.VK_PIPELINE_STAGE_VERTEX_SHADER_BIT;
        if (srcAccess == 0 || dstAccess == 0 || srcStage == 0 || dstStage == 0) {
            throw new IllegalStateException("Invalid pre-draw barrier configuration");
        }
    }
}
