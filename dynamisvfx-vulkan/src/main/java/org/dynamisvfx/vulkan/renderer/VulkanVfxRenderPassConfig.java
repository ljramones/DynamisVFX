package org.dynamisvfx.vulkan.renderer;

public final class VulkanVfxRenderPassConfig {
    private VulkanVfxRenderPassConfig() {
    }

    // VFX billboard renderer expects:
    // - Color attachment: swapchain format, load=LOAD, store=STORE
    // - Depth attachment: D32_SFLOAT, load=LOAD, store=DONT_CARE
    // - Subpass: 1 color + 1 depth
    public static final boolean REQUIRES_DEPTH_SAMPLER = true;
    public static final boolean WRITES_DEPTH = false;
    public static final int SUBPASS_INDEX = 0;
}
