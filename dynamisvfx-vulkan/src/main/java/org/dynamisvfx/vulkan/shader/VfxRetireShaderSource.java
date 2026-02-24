package org.dynamisvfx.vulkan.shader;

public final class VfxRetireShaderSource {
    private VfxRetireShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=1, binding=0) buffer PositionBuffer {
            vec4 positions[];
        };
        layout(std430, set=2, binding=1) buffer FreeListBuffer {
            uint writeHead;
            uint readHead;
            uint slots[];
        };
        layout(std430, set=2, binding=2) buffer AliveCountBuffer {
            uint count;
        };

        layout(push_constant) uniform PushConstants {
            uint maxParticles;
        } push;

        void main() {
            uint idx = gl_GlobalInvocationID.x;
            if (idx >= push.maxParticles) return;
            if (positions[idx].w >= 1.0) {
                uint freeSlot = atomicAdd(writeHead, 1);
                slots[freeSlot] = idx;
                atomicAdd(count, uint(-1));
            }
        }
        """;
}
