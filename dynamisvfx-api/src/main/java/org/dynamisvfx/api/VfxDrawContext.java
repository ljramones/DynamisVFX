package org.dynamisvfx.api;

public interface VfxDrawContext {
    IndirectCommandBuffer indirectBuffer();

    BindlessHeap bindlessHeap();

    long frameIndex();
}
