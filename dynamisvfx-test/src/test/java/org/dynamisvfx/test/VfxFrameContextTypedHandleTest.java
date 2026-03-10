package org.dynamisvfx.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.dynamisvfx.test.mock.MockVfxFrameContext;
import org.junit.jupiter.api.Test;

class VfxFrameContextTypedHandleTest {
    @Test
    void typedCommandBufferRefMatchesLegacyHandle() {
        MockVfxFrameContext ctx = new MockVfxFrameContext().commandBuffer(99L);

        assertEquals(99L, ctx.commandBuffer());
        assertEquals(99L, ctx.commandBufferRef().handle());
    }
}
