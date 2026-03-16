module org.dynamisengine.vfx.bench {
    requires org.dynamisengine.vfx.api;
    requires org.dynamisengine.vfx.core;
    requires org.dynamisengine.vfx.vulkan;
    requires org.dynamisengine.vfx.test;
    requires dynamis.gpu.test;
    requires jmh.core;

    exports org.dynamisengine.vfx.bench;
}
