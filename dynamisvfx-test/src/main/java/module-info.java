module org.dynamisengine.vfx.test {
    requires org.dynamisengine.vfx.api;
    requires org.dynamisengine.vfx.core;
    requires dynamis.gpu.test;

    exports org.dynamisengine.vfx.test.assertions;
    exports org.dynamisengine.vfx.test.harness;
    exports org.dynamisengine.vfx.test.mock;
    exports org.dynamisengine.vfx.testsupport;
}
