package org.dynamisengine.vfx.vulkan.hotreload;

import org.dynamisengine.gpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.api.VfxHandle;
import org.dynamisengine.vfx.core.validate.EffectValidator;
import org.dynamisengine.vfx.core.validate.ValidationError;
import org.dynamisengine.vfx.vulkan.compute.VulkanVfxSimulateStage;
import org.dynamisengine.vfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisengine.vfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisengine.vfx.vulkan.force.PackedForceBuffer;
import org.dynamisengine.vfx.vulkan.force.VulkanForceFieldPacker;
import org.dynamisengine.vfx.vulkan.resources.VulkanVfxEffectResources;

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
