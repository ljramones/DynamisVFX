module org.dynamisengine.vfx.vulkan {
    requires org.dynamisengine.vfx.api;
    requires org.dynamisengine.vfx.core;
    requires dynamis.gpu.api;
    requires dynamis.gpu.vulkan;
    requires org.lwjgl.vulkan;
    requires java.logging;

    exports org.dynamisengine.vfx.vulkan;
    exports org.dynamisengine.vfx.vulkan.compute to org.dynamisengine.vfx.bench;
    exports org.dynamisengine.vfx.vulkan.emitter to org.dynamisengine.vfx.bench;
    exports org.dynamisengine.vfx.vulkan.force to org.dynamisengine.vfx.bench;
}
