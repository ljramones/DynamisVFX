package org.dynamisvfx.vulkan.renderer;

import org.dynamisvfx.api.BlendMode;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public final class VulkanVfxRendererPipelineUtil {
    private static final AtomicLong HANDLE_IDS = new AtomicLong(20_000L);

    private VulkanVfxRendererPipelineUtil() {
    }

    public static VulkanRendererPipelineHandles create(
        long device,
        long renderPass,
        String vertGlsl,
        String fragGlsl,
        long[] descriptorSetLayouts,
        int pushConstantSizeBytes,
        BlendMode blendMode
    ) {
        if (device == Long.MIN_VALUE || renderPass == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid Vulkan handle");
        }
        Objects.requireNonNull(vertGlsl, "vertGlsl");
        Objects.requireNonNull(fragGlsl, "fragGlsl");
        Objects.requireNonNull(descriptorSetLayouts, "descriptorSetLayouts");
        Objects.requireNonNull(blendMode, "blendMode");
        if (descriptorSetLayouts.length != 4) {
            throw new IllegalArgumentException("descriptorSetLayouts must contain set 0..3 layouts");
        }
        if (pushConstantSizeBytes < 0) {
            throw new IllegalArgumentException("pushConstantSizeBytes must be >= 0");
        }

        // Placeholder for full graphics pipeline creation:
        // - no vertex bindings
        // - dynamic viewport/scissor
        // - depth test on, depth writes off
        // - blend state derived from blendMode
        long vertShaderModule = HANDLE_IDS.getAndIncrement();
        long fragShaderModule = HANDLE_IDS.getAndIncrement();
        long pipelineLayout = HANDLE_IDS.getAndIncrement();
        long pipeline = HANDLE_IDS.getAndIncrement();
        return new VulkanRendererPipelineHandles(pipelineLayout, pipeline, vertShaderModule, fragShaderModule);
    }

    public static void destroy(long device, VulkanRendererPipelineHandles handles) {
        if (device == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
        Objects.requireNonNull(handles, "handles");
    }

    public record VulkanRendererPipelineHandles(
        long pipelineLayout,
        long pipeline,
        long vertShaderModule,
        long fragShaderModule
    ) {
    }
}
