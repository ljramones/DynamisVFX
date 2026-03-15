package org.dynamisengine.vfx.vulkan;

import org.dynamisengine.vfx.core.ParticleSimulationCore;

public final class VulkanComputeBridge {
    private final ParticleSimulationCore core = new ParticleSimulationCore();

    public String backendName() {
        return "vulkan:" + core.getClass().getSimpleName();
    }
}
