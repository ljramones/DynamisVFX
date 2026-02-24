package org.dynamisvfx.vulkan.emitter;

import org.dynamisvfx.api.EmitterShapeDescriptor;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.ParticleInitDescriptor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Objects;

public final class VulkanEmitterDescriptorPacker {
    private VulkanEmitterDescriptorPacker() {
    }

    public static PackedEmitterDescriptor pack(ParticleEmitterDescriptor descriptor) {
        Objects.requireNonNull(descriptor, "descriptor");

        ByteBuffer buf = ByteBuffer.allocate(PackedEmitterDescriptor.SIZE_BYTES)
            .order(ByteOrder.LITTLE_ENDIAN);

        EmitterShapeDescriptor shape = descriptor.shape();
        ParticleInitDescriptor init = descriptor.init();

        // shape type (uint)
        buf.putInt(shape != null && shape.type() != null ? shape.type().ordinal() : 0);

        // shape params (vec4 x2 = 32 bytes)
        float[] dims = shape == null ? null : shape.dimensions();
        float d0 = valueAt(dims, 0);
        float d1 = valueAt(dims, 1);
        float d2 = valueAt(dims, 2);

        // shape vec4 #1 (origin xyz + primary radius)
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);
        buf.putFloat(d0);

        // shape vec4 #2 (secondary params)
        buf.putFloat(d1);
        buf.putFloat(d2);
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);

        float[] direction = init == null ? null : init.initialDirection();

        // velocity range vec4: xyz=dir, w=minSpeed
        buf.putFloat(valueAt(direction, 0));
        buf.putFloat(valueAt(direction, 1));
        buf.putFloat(valueAt(direction, 2));
        buf.putFloat(init == null ? 0.0f : init.speedMin());

        // velocity range vec4 #2: w=maxSpeed
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);
        buf.putFloat(0.0f);
        buf.putFloat(init == null ? 0.0f : init.speedMax());

        // size range vec2: min, max
        buf.putFloat(init == null ? 0.0f : init.sizeMin());
        buf.putFloat(init == null ? 0.0f : init.sizeMax());

        // lifetime range vec2: min, max
        buf.putFloat(init == null ? 0.0f : init.lifetimeMinSeconds());
        buf.putFloat(init == null ? 0.0f : init.lifetimeMaxSeconds());

        // init color vec4
        float[] color = init == null ? null : init.colorRgb();
        buf.putFloat(valueAt(color, 0));
        buf.putFloat(valueAt(color, 1));
        buf.putFloat(valueAt(color, 2));
        buf.putFloat(init == null ? 1.0f : init.alpha());

        // flags (uint)
        int flags = 0;
        if (descriptor.physics() != null && descriptor.physics().enabled()) {
            flags |= 1;
        }
        if (descriptor.lod() != null) {
            flags |= 2;
        }
        buf.putInt(flags);

        // Remaining bytes are zero-filled by allocated array.
        byte[] bytes = Arrays.copyOf(buf.array(), PackedEmitterDescriptor.SIZE_BYTES);
        return new PackedEmitterDescriptor(bytes);
    }

    private static float valueAt(float[] values, int index) {
        return values != null && index < values.length ? values[index] : 0.0f;
    }
}
