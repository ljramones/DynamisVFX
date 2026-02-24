package org.dynamisvfx.vulkan.force;

import java.util.Arrays;

public record PackedForceBuffer(byte[] data, int forceCount) {
    public static final int MAX_FORCES = 32;
    public static final int SIZE_BYTES = MAX_FORCES * PackedForceEntry.SIZE_BYTES;

    public PackedForceBuffer {
        if (data == null || data.length != SIZE_BYTES) {
            throw new IllegalArgumentException("PackedForceBuffer must be exactly " + SIZE_BYTES + " bytes");
        }
        if (forceCount < 0 || forceCount > MAX_FORCES) {
            throw new IllegalArgumentException("forceCount must be in [0, " + MAX_FORCES + "]");
        }
        data = Arrays.copyOf(data, data.length);
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }
}
