package org.dynamisvfx.vulkan.shader;

public final class VfxSortKeyGenShaderSource {
    private VfxSortKeyGenShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=1, binding=0) buffer PositionBuffer { vec4 positions[]; };
        layout(std430, set=3, binding=2) buffer SortKeyBuffer  { uint sortKeys[];  };
        layout(std430, set=3, binding=0) buffer IndexBuffer    { uint indices[];   };

        layout(push_constant) uniform PushConstants {
            uint maxParticles;
            vec3 cameraPos;
        } push;

        uint floatToSortableUint(float f) {
            uint u = floatBitsToUint(f);
            uint mask = uint(-int(u >> 31)) | 0x80000000u;
            return u ^ mask;
        }

        void main() {
            uint idx = gl_GlobalInvocationID.x;
            if (idx >= push.maxParticles) return;
            indices[idx] = idx;
            if (positions[idx].w >= 1.0) {
                sortKeys[idx] = 0u;
                return;
            }
            vec3 diff = positions[idx].xyz - push.cameraPos;
            float dist = dot(diff, diff);
            sortKeys[idx] = floatToSortableUint(dist);
        }
        """;
}
