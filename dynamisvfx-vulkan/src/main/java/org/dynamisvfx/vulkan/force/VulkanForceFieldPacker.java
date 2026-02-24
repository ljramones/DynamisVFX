package org.dynamisvfx.vulkan.force;

import org.dynamisvfx.api.ForceDescriptor;
import org.dynamisvfx.api.ForceType;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.logging.Logger;

public final class VulkanForceFieldPacker {
    private static final Logger LOG = Logger.getLogger(VulkanForceFieldPacker.class.getName());

    private VulkanForceFieldPacker() {
    }

    public static PackedForceBuffer pack(List<ForceDescriptor> forces) {
        byte[] data = new byte[PackedForceBuffer.SIZE_BYTES];
        if (forces == null || forces.isEmpty()) {
            return new PackedForceBuffer(data, 0);
        }

        int packedCount = 0;
        for (ForceDescriptor force : forces) {
            if (force == null || force.type() == null) {
                continue;
            }
            if (force.type() == ForceType.CURL_NOISE) {
                continue;
            }
            if (packedCount >= PackedForceBuffer.MAX_FORCES) {
                LOG.warning("Force list exceeds max of " + PackedForceBuffer.MAX_FORCES + "; extra forces are ignored");
                break;
            }

            PackedForceEntry entry = packEntry(force);
            System.arraycopy(entry.data(), 0, data, packedCount * PackedForceEntry.SIZE_BYTES, PackedForceEntry.SIZE_BYTES);
            packedCount++;
        }

        return new PackedForceBuffer(data, packedCount);
    }

    private static PackedForceEntry packEntry(ForceDescriptor force) {
        ByteBuffer buf = ByteBuffer.allocate(PackedForceEntry.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN);

        int type = mapForceType(force.type());
        float strength = force.strength();
        float[] direction = force.direction();

        float dirX = get(direction, 0);
        float dirY = get(direction, 1);
        float dirZ = get(direction, 2);

        float originX = 0.0f;
        float originY = 0.0f;
        float originZ = 0.0f;

        if (force.type() == ForceType.ATTRACTOR) {
            originX = dirX;
            originY = dirY;
            originZ = dirZ;
            dirX = 0.0f;
            dirY = 0.0f;
            dirZ = 0.0f;
        }

        // std430-compatible 32-byte packing
        buf.putInt(type);
        buf.putFloat(strength);
        buf.putFloat(dirX);
        buf.putFloat(dirY);
        buf.putFloat(dirZ);
        buf.putFloat(originX);
        buf.putFloat(originY);
        buf.putFloat(originZ);
        return new PackedForceEntry(buf.array());
    }

    private static int mapForceType(ForceType type) {
        return switch (type) {
            case GRAVITY -> 0;
            case DRAG -> 1;
            case ATTRACTOR -> 2;
            case WIND -> 3;
            default -> 255;
        };
    }

    private static float get(float[] values, int index) {
        return values != null && index < values.length ? values[index] : 0.0f;
    }
}
