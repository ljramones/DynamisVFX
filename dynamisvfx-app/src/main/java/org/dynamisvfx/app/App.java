package org.dynamisvfx.app;

import org.dynamisvfx.core.GreetingService;

public class App {
    public static void main(String[] args) {
        String name = args.length > 0 ? args[0] : "";
        GreetingService service = new GreetingService();
        System.out.println(service.greet(name));
    }
}
