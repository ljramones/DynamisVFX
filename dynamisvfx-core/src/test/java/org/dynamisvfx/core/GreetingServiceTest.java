package org.dynamisvfx.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingServiceTest {
    @Test
    void greetUsesFallbackWhenNameIsBlank() {
        GreetingService service = new GreetingService();
        assertEquals("Hello from DynamisVFX", service.greet(" "));
    }
}
