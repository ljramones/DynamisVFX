package org.dynamisvfx.vulkan.physics;

import org.dynamisvfx.api.DebrisSpawnEvent;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Objects;

public record VulkanVfxDebrisCandidate(
    float px, float py, float pz, float mass,
    float vx, float vy, float vz, float angularSpeed,
    int meshId, int materialTag, int emitterId, int flags
) {
    public static final int SIZE_BYTES = 48;

    public static VulkanVfxDebrisCandidate readFrom(ByteBuffer buf, int offset) {
        Objects.requireNonNull(buf, "buf");
        if (offset < 0 || offset + SIZE_BYTES > buf.capacity()) {
            throw new IllegalArgumentException("offset out of range for debris candidate read");
        }

        ByteBuffer read = buf.duplicate().order(ByteOrder.LITTLE_ENDIAN);
        read.position(offset);

        return new VulkanVfxDebrisCandidate(
            read.getFloat(),
            read.getFloat(),
            read.getFloat(),
            read.getFloat(),
            read.getFloat(),
            read.getFloat(),
            read.getFloat(),
            read.getFloat(),
            read.getInt(),
            read.getInt(),
            read.getInt(),
            read.getInt()
        );
    }

    public DebrisSpawnEvent toSpawnEvent(float[] worldTransform) {
        float[] transform = worldTransform == null ? new float[16] : worldTransform.clone();
        float[] velocity = new float[] {vx, vy, vz};
        float[] angularVelocity = new float[] {0f, angularSpeed, 0f};
        return new DebrisSpawnEvent(
            transform,
            velocity,
            angularVelocity,
            mass,
            String.valueOf(meshId),
            String.valueOf(materialTag),
            emitterId
        );
    }
}
