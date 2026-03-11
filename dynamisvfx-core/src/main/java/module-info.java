module org.dynamisvfx.core {
    requires org.dynamisvfx.api;
    requires org.vectrix;
    requires fastnoiselitenouveau;
    requires com.fasterxml.jackson.databind;

    exports org.dynamisvfx.core;
    exports org.dynamisvfx.core.builder;
    exports org.dynamisvfx.core.noise;
    exports org.dynamisvfx.core.serial;
    exports org.dynamisvfx.core.validate;
}
