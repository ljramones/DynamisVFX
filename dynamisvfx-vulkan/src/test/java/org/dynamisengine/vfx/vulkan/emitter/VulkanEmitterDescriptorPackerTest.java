package org.dynamisengine.vfx.vulkan.emitter;

import org.dynamisengine.vfx.api.BlendMode;
import org.dynamisengine.vfx.api.EmissionMode;
import org.dynamisengine.vfx.api.EmissionRateDescriptor;
import org.dynamisengine.vfx.api.EmitterShapeDescriptor;
import org.dynamisengine.vfx.api.EmitterShapeType;
import org.dynamisengine.vfx.api.LodDescriptor;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.api.ParticleInitDescriptor;
import org.dynamisengine.vfx.api.PhysicsHandoffDescriptor;
import org.dynamisengine.vfx.api.RendererDescriptor;
import org.dynamisengine.vfx.api.RendererType;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class VulkanEmitterDescriptorPackerTest {

    private static ParticleEmitterDescriptor fullDescriptor(
        PhysicsHandoffDescriptor physics, LodDescriptor lod
    ) {
        var shape = new EmitterShapeDescriptor(EmitterShapeType.SPHERE, new float[]{5f, 2f, 1f}, null, null);
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 100f, 10, null);
        var init = new ParticleInitDescriptor(0.5f, 2.0f, 1f, 5f, 0.1f, 1f,
            new float[]{0f, 1f, 0f}, new float[]{1f, 0.5f, 0.2f}, 0.8f);
        var renderer = new RendererDescriptor(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas", 1, false, false);
        return new ParticleEmitterDescriptor("test", shape, rate, init, List.of(), renderer, lod, physics);
    }

    @Test
    void packProducesNonNullBuffer() {
        PackedEmitterDescriptor packed = VulkanEmitterDescriptorPacker.pack(fullDescriptor(null, null));
        assertNotNull(packed);
        assertNotNull(packed.data());
    }

    @Test
    void packProducesCorrectBufferSize() {
        PackedEmitterDescriptor packed = VulkanEmitterDescriptorPacker.pack(fullDescriptor(null, null));
        assertEquals(PackedEmitterDescriptor.SIZE_BYTES, packed.data().length);
    }

    @Test
    void packHandlesNullOptionalFields() {
        var desc = new ParticleEmitterDescriptor("test", null, null, null, null, null, null, null);
        PackedEmitterDescriptor packed = VulkanEmitterDescriptorPacker.pack(desc);
        assertNotNull(packed);
        assertEquals(PackedEmitterDescriptor.SIZE_BYTES, packed.data().length);
    }

    @Test
    void packIncludesPhysicsFlag() {
        var physics = new PhysicsHandoffDescriptor(true, 10f, "mesh", "stone", 1.0f);
        PackedEmitterDescriptor packed = VulkanEmitterDescriptorPacker.pack(fullDescriptor(physics, null));

        ByteBuffer buf = ByteBuffer.wrap(packed.data()).order(ByteOrder.LITTLE_ENDIAN);
        // flags offset: 4 + 32 + 32 + 8 + 8 + 16 = 100
        int flags = buf.getInt(100);
        assertEquals(1, flags & 1, "physics bit should be set");
    }

    @Test
    void packIncludesLodFlag() {
        var lod = new LodDescriptor(List.of(), false, 100f);
        PackedEmitterDescriptor packed = VulkanEmitterDescriptorPacker.pack(fullDescriptor(null, lod));

        ByteBuffer buf = ByteBuffer.wrap(packed.data()).order(ByteOrder.LITTLE_ENDIAN);
        int flags = buf.getInt(100);
        assertEquals(2, flags & 2, "lod bit should be set");
    }
}
