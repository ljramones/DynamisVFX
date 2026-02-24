package org.dynamisvfx.vulkan.emitter;

import java.util.Arrays;

public record PackedEmitterDescriptor(byte[] data) {
    public static final int SIZE_BYTES = 256;

    public PackedEmitterDescriptor {
        if (data == null || data.length != SIZE_BYTES) {
            throw new IllegalArgumentException("PackedEmitterDescriptor must be exactly " + SIZE_BYTES + " bytes");
        }
        data = Arrays.copyOf(data, data.length);
    }

    @Override
    public byte[] data() {
        return Arrays.copyOf(data, data.length);
    }
}
