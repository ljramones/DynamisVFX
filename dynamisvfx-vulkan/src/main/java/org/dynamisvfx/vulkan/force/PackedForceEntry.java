package org.dynamisvfx.vulkan.force;

import java.util.Arrays;

public record PackedForceEntry(byte[] data) {
    public static final int SIZE_BYTES = 32;

    public PackedForceEntry {
        if (data == null || data.length != SIZE_BYTES) {
            throw new IllegalArgumentException("PackedForceEntry must be exactly " + SIZE_BYTES + " bytes");
        }
        data = Arrays.copyOf(data, data.length);
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }
}
