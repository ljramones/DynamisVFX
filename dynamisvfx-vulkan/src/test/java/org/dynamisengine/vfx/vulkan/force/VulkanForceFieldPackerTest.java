package org.dynamisengine.vfx.vulkan.force;

import org.dynamisengine.vfx.api.ForceDescriptor;
import org.dynamisengine.vfx.api.ForceType;
import org.dynamisengine.vfx.api.NoiseForceConfig;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VulkanForceFieldPackerTest {

    @Test
    void packEmptyForceList() {
        PackedForceBuffer result = VulkanForceFieldPacker.pack(List.of());
        assertEquals(0, result.forceCount());
        assertEquals(PackedForceBuffer.SIZE_BYTES, result.data().length);
    }

    @Test
    void packSingleGravityForce() {
        var gravity = new ForceDescriptor(ForceType.GRAVITY, -9.8f, new float[]{0f, -1f, 0f}, null);
        PackedForceBuffer result = VulkanForceFieldPacker.pack(List.of(gravity));

        assertEquals(1, result.forceCount());
        ByteBuffer buf = ByteBuffer.wrap(result.data()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, buf.getInt(0)); // GRAVITY -> 0
        assertEquals(-9.8f, buf.getFloat(4));
    }

    @Test
    void packMultipleForces() {
        var gravity = new ForceDescriptor(ForceType.GRAVITY, -9.8f, new float[]{0f, -1f, 0f}, null);
        var wind = new ForceDescriptor(ForceType.WIND, 3f, new float[]{1f, 0f, 0f}, null);
        PackedForceBuffer result = VulkanForceFieldPacker.pack(List.of(gravity, wind));

        assertEquals(2, result.forceCount());
        ByteBuffer buf = ByteBuffer.wrap(result.data()).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(3, buf.getInt(32)); // WIND -> 3
        assertEquals(3f, buf.getFloat(36));
    }

    @Test
    void packCurlNoiseForceWithConfig() {
        var noise = new NoiseForceConfig(1f, 2f, 4, 2f, 0.5f, 1f, 42);
        var curlNoise = new ForceDescriptor(ForceType.CURL_NOISE, 5f, new float[]{0f, 0f, 0f}, noise);
        PackedForceBuffer result = VulkanForceFieldPacker.pack(List.of(curlNoise));

        assertEquals(0, result.forceCount());
    }

    @Test
    void packProducesCorrectEntryCount() {
        var gravity = new ForceDescriptor(ForceType.GRAVITY, -9.8f, new float[]{0f, -1f, 0f}, null);
        var drag = new ForceDescriptor(ForceType.DRAG, 0.1f, null, null);
        var wind = new ForceDescriptor(ForceType.WIND, 2f, new float[]{1f, 0f, 0f}, null);
        PackedForceBuffer result = VulkanForceFieldPacker.pack(List.of(gravity, drag, wind));

        assertEquals(3, result.forceCount());
    }
}
