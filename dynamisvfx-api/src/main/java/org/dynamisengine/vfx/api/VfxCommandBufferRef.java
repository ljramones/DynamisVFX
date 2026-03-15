package org.dynamisengine.vfx.api;

/**
 * Typed VFX command buffer reference.
 */
public record VfxCommandBufferRef(long handle) {
    public static final VfxCommandBufferRef NULL = new VfxCommandBufferRef(0L);
}
