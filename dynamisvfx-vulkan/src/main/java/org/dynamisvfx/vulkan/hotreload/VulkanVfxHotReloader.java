package org.dynamisvfx.vulkan.hotreload;

import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.VfxHandle;
import org.dynamisvfx.core.validate.EffectValidator;
import org.dynamisvfx.core.validate.ValidationError;
import org.dynamisvfx.vulkan.compute.VulkanVfxSimulateStage;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.force.PackedForceBuffer;
import org.dynamisvfx.vulkan.force.VulkanForceFieldPacker;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.List;
import java.util.Objects;

public final class VulkanVfxHotReloader {
    private final VulkanVfxPipelineSwapper pipelineSwapper;
    private final int framesInFlight;

    private VulkanVfxHotReloader(VulkanVfxPipelineSwapper pipelineSwapper, int framesInFlight) {
        this.pipelineSwapper = pipelineSwapper;
        this.framesInFlight = framesInFlight;
    }

    public static VulkanVfxHotReloader create(
        long device,
        long renderPass,
        VulkanVfxDescriptorSetLayout layout,
        int framesInFlight
    ) {
        if (framesInFlight <= 0) {
            throw new IllegalArgumentException("framesInFlight must be > 0");
        }
        return new VulkanVfxHotReloader(new VulkanVfxPipelineSwapper(device, renderPass, layout), framesInFlight);
    }

    public VfxReloadCategory reload(
        VfxHandle handle,
        ParticleEmitterDescriptor updatedDescriptor,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets,
        VulkanVfxSimulateStage simulateStage,
        VulkanMemoryOps memoryOps,
        long commandBuffer,
        long frameIndex
    ) {
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(updatedDescriptor, "updatedDescriptor");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");
        Objects.requireNonNull(simulateStage, "simulateStage");
        Objects.requireNonNull(memoryOps, "memoryOps");

        List<ValidationError> errors = EffectValidator.validate(updatedDescriptor);
        boolean hasError = errors.stream().anyMatch(e -> e.severity() == ValidationError.Severity.ERROR);
        if (hasError) {
            return VfxReloadCategory.FORCES_ONLY;
        }

        VfxReloadCategory category = VfxDescriptorDiff.classify(resources.descriptor(), updatedDescriptor);
        switch (category) {
            case FORCES_ONLY -> {
                PackedForceBuffer packed = VulkanForceFieldPacker.pack(updatedDescriptor.forces());
                simulateStage.uploadForceBuffer(commandBuffer, resources, packed, memoryOps);
                resources.updateDescriptor(updatedDescriptor);
            }
            case RENDERER_CHANGED -> {
                pipelineSwapper.submitRebuild(handle.id(), updatedDescriptor.renderer());
                resources.updateDescriptor(updatedDescriptor);
            }
            case FULL_RESPAWN -> {
                // Caller handles respawn lifecycle.
            }
            default -> throw new IllegalStateException("Unhandled category: " + category);
        }

        pipelineSwapper.pollAndSwap(resources, frameIndex, framesInFlight);
        return category;
    }

    public void tick(VulkanVfxEffectResources resources, long frameIndex) {
        Objects.requireNonNull(resources, "resources");
        pipelineSwapper.pollAndSwap(resources, frameIndex, framesInFlight);
    }

    public void destroy() {
        pipelineSwapper.shutdown();
    }
}
