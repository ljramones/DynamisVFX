package org.dynamisvfx.api;

/**
 * Stable VFX draw context contract. GPU-specific compatibility access is intentionally
 * not part of this exported API surface.
 */
public interface VfxDrawContext {
    VfxIndirectCommandSink indirectCommandSink();

    VfxDescriptorBindingWriter bindlessHeapWriter();

    long frameIndex();
}
