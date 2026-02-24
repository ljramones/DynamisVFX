package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.RendererType;

public final class Renderer {
    private Renderer() {
    }

    public static RendererBuilder billboard() {
        return new RendererBuilder(RendererType.BILLBOARD);
    }

    public static RendererBuilder ribbon() {
        return new RendererBuilder(RendererType.RIBBON);
    }

    public static RendererBuilder mesh() {
        return new RendererBuilder(RendererType.MESH);
    }

    public static RendererBuilder beam() {
        return new RendererBuilder(RendererType.BEAM);
    }

    public static RendererBuilder decal() {
        return new RendererBuilder(RendererType.DECAL);
    }
}
