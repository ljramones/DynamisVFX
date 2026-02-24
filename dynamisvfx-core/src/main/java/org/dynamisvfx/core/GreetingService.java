package org.dynamisvfx.core;

public class GreetingService {
    public String greet(String name) {
        if (name == null || name.isBlank()) {
            return "Hello from DynamisVFX";
        }
        return "Hello " + name + " from DynamisVFX";
    }
}
