package org.dynamisvfx.vulkan.compute;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class VulkanVfxComputePipelineUtil {
    private static final AtomicLong HANDLE_IDS = new AtomicLong(10_000L);

    private VulkanVfxComputePipelineUtil() {
    }

    public static VulkanComputePipelineHandles create(
        long device,
        String glslSource,
        long[] descriptorSetLayouts,
        int pushConstantSizeBytes
    ) {
        if (device == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        Objects.requireNonNull(glslSource, "glslSource");
        Objects.requireNonNull(descriptorSetLayouts, "descriptorSetLayouts");
        if (descriptorSetLayouts.length != 4) {
            throw new IllegalArgumentException("descriptorSetLayouts must contain set 0..3 layouts");
        }
        if (pushConstantSizeBytes < 0) {
            throw new IllegalArgumentException("pushConstantSizeBytes must be >= 0");
        }

        // Placeholder handle creation for Step 3 contract wiring.
        // Real Vulkan creation (shaderc->SPIR-V, vkCreateShaderModule, vkCreatePipelineLayout,
        // vkCreateComputePipelines) is integrated when device context wiring lands.
        long shaderModule = HANDLE_IDS.getAndIncrement();
        long pipelineLayout = HANDLE_IDS.getAndIncrement();
        long pipeline = HANDLE_IDS.getAndIncrement();
        return new VulkanComputePipelineHandles(pipelineLayout, pipeline, shaderModule);
    }

    public static void destroy(long device, VulkanComputePipelineHandles handles) {
        if (device == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        Objects.requireNonNull(handles, "handles");
        // Placeholder for vkDestroyPipeline, vkDestroyPipelineLayout, vkDestroyShaderModule.
    }

    public record VulkanComputePipelineHandles(
        long pipelineLayout,
        long pipeline,
        long shaderModule
    ) {
    }
}
