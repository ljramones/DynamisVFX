module org.dynamisengine.vfx.core {
    requires org.dynamisengine.vfx.api;
    requires org.dynamisengine.vectrix;
    requires fastnoiselitenouveau;
    requires com.fasterxml.jackson.databind;

    exports org.dynamisengine.vfx.core;
    exports org.dynamisengine.vfx.core.builder;
    exports org.dynamisengine.vfx.core.noise;
    exports org.dynamisengine.vfx.core.serial;
    exports org.dynamisengine.vfx.core.validate;
}
