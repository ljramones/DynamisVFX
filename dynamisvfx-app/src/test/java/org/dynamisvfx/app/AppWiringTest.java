package org.dynamisvfx.app;

import org.dynamisvfx.core.GreetingService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppWiringTest {
    @Test
    void appCanUseCoreModule() {
        GreetingService service = new GreetingService();
        assertEquals("Hello Codex from DynamisVFX", service.greet("Codex"));
    }
}
