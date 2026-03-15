package org.dynamisengine.vfx.vulkan.physics;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VulkanVfxDebrisReadbackRingTest {

    // --- VulkanVfxDebrisCandidate record tests ---

    @Test
    void debrisCandidateRecordFields() {
        VulkanVfxDebrisCandidate c = new VulkanVfxDebrisCandidate(
            1f, 2f, 3f, 4f,
            5f, 6f, 7f, 8f,
            9, 10, 11, 12
        );
        assertEquals(1f, c.px());
        assertEquals(2f, c.py());
        assertEquals(3f, c.pz());
        assertEquals(4f, c.mass());
        assertEquals(5f, c.vx());
        assertEquals(6f, c.vy());
        assertEquals(7f, c.vz());
        assertEquals(8f, c.angularSpeed());
        assertEquals(9, c.meshId());
        assertEquals(10, c.materialTag());
        assertEquals(11, c.emitterId());
        assertEquals(12, c.flags());
        assertEquals(48, VulkanVfxDebrisCandidate.SIZE_BYTES);
    }

    @Test
    void debrisCandidateToSpawnEvent() {
        VulkanVfxDebrisCandidate c = new VulkanVfxDebrisCandidate(
            1f, 2f, 3f, 5f,
            10f, 20f, 30f, 99f,
            42, 7, 3, 0
        );
        var event = c.toSpawnEvent(null);
        assertNotNull(event);
        assertEquals(5f, event.mass());
        assertArrayEquals(new float[]{10f, 20f, 30f}, event.velocity());
        assertArrayEquals(new float[]{0f, 99f, 0f}, event.angularVelocity());
        assertEquals("42", event.meshId());
        assertEquals("7", event.materialTag());
        assertEquals(3, event.sourceEmitterId());
        // null transform yields 16-element zero array
        assertEquals(16, event.worldTransform().length);
    }

    @Test
    void debrisCandidateToSpawnEventDefensiveCopy() {
        float[] transform = new float[16];
        transform[0] = 1f;
        VulkanVfxDebrisCandidate c = new VulkanVfxDebrisCandidate(
            0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0, 0, 0, 0
        );
        var event = c.toSpawnEvent(transform);
        transform[0] = 999f;
        assertEquals(1f, event.worldTransform()[0],
            "toSpawnEvent should defensively copy the transform array");
    }

    // --- ReadbackBuffer tests (using allocateForTest) ---

    @Test
    void readbackBufferWriteAndReadCandidates() {
        var buf = VulkanVfxDebrisReadbackBuffer.allocateForTest(4);
        assertEquals(4, buf.maxCandidates());

        var c1 = new VulkanVfxDebrisCandidate(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9, 10, 11, 0);
        var c2 = new VulkanVfxDebrisCandidate(10f, 20f, 30f, 40f, 50f, 60f, 70f, 80f, 90, 100, 110, 1);
        buf.writeMockCandidates(List.of(c1, c2));

        List<VulkanVfxDebrisCandidate> read = buf.readCandidates();
        assertEquals(2, read.size());
        assertEquals(c1, read.get(0));
        assertEquals(c2, read.get(1));
    }

    @Test
    void readbackBufferClampsToMaxCandidates() {
        var buf = VulkanVfxDebrisReadbackBuffer.allocateForTest(1);
        var c1 = new VulkanVfxDebrisCandidate(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0, 0, 0, 0);
        var c2 = new VulkanVfxDebrisCandidate(1f, 1f, 1f, 2f, 1f, 1f, 1f, 1f, 1, 1, 1, 1);
        buf.writeMockCandidates(List.of(c1, c2));

        List<VulkanVfxDebrisCandidate> read = buf.readCandidates();
        assertEquals(1, read.size(), "Should clamp to maxCandidates");
    }

    @Test
    void readbackBufferEmptyOnFreshAllocation() {
        var buf = VulkanVfxDebrisReadbackBuffer.allocateForTest(8);
        assertTrue(buf.readCandidates().isEmpty());
    }

    @Test
    void readbackBufferResetClearsCount() {
        var buf = VulkanVfxDebrisReadbackBuffer.allocateForTest(4);
        var c = new VulkanVfxDebrisCandidate(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 0, 0, 0, 0);
        buf.writeMockCandidates(List.of(c));
        assertEquals(1, buf.readCandidates().size());

        buf.resetOnGpu(1L); // non-zero command buffer handle
        assertTrue(buf.readCandidates().isEmpty());
    }

    @Test
    void readbackBufferRejectZeroMaxCandidates() {
        assertThrows(IllegalArgumentException.class,
            () -> VulkanVfxDebrisReadbackBuffer.allocateForTest(0));
    }

    // --- Ring buffer tests (using allocateForTest) ---

    @Test
    void readbackRingFrameIndexing() {
        var ring = VulkanVfxDebrisReadbackRing.allocateForTest(4);

        // Write at frame 0, read at frame 2 should return frame 0's buffer
        var writeAt0 = ring.writeBuffer(0);
        var readAt2 = ring.readBuffer(2);
        assertSame(writeAt0, readAt2,
            "readBuffer(N) should return writeBuffer(N-2) for 3-frame ring");
    }

    @Test
    void readbackRingWriteReadWrapsCorrectly() {
        var ring = VulkanVfxDebrisReadbackRing.allocateForTest(4);

        // Frame 3 write should map to slot 0 (3 % 3 == 0)
        // Frame 5 read should map to slot 0 ((5-2) % 3 == 0)
        assertSame(ring.writeBuffer(3), ring.readBuffer(5));

        // Frame 4 write -> slot 1, frame 6 read -> slot 1
        assertSame(ring.writeBuffer(4), ring.readBuffer(6));
    }

    @Test
    void readbackRingEndToEnd() {
        var ring = VulkanVfxDebrisReadbackRing.allocateForTest(4);
        var candidate = new VulkanVfxDebrisCandidate(
            1f, 2f, 3f, 1f, 0f, 0f, 0f, 0f, 42, 0, 0, 0
        );

        // Write candidates into frame 0's buffer
        ring.writeBuffer(0).writeMockCandidates(List.of(candidate));

        // At frame 2, we read back frame 0's data
        List<VulkanVfxDebrisCandidate> read = ring.readBuffer(2).readCandidates();
        assertEquals(1, read.size());
        assertEquals(42, read.get(0).meshId());
    }
}
