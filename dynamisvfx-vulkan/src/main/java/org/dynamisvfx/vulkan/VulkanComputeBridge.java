package org.dynamisvfx.vulkan;

import org.dynamisvfx.core.ParticleSimulationCore;

public final class VulkanComputeBridge {
    private final ParticleSimulationCore core = new ParticleSimulationCore();

    public String backendName() {
        return "vulkan:" + core.getClass().getSimpleName();
    }
}
