package org.dynamisvfx.vulkan.shader;

public final class VfxRadixScatterShaderSource {
    private VfxRadixScatterShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, binding=0) buffer KeysIn     { uint keysIn[];     };
        layout(std430, binding=1) buffer KeysOut    { uint keysOut[];    };
        layout(std430, binding=2) buffer IndicesIn  { uint indicesIn[];  };
        layout(std430, binding=3) buffer IndicesOut { uint indicesOut[]; };
        layout(std430, binding=4) buffer Prefix     { uint prefix[];     };

        layout(push_constant) uniform PushConstants {
            uint count;
            uint bitShift;
        } push;

        void main() {
            uint idx = gl_GlobalInvocationID.x;
            if (idx >= push.count) return;
            uint bucket = (keysIn[idx] >> push.bitShift) & 0xFFu;
            uint pos = atomicAdd(prefix[bucket], 1u);
            keysOut[pos] = keysIn[idx];
            indicesOut[pos] = indicesIn[idx];
        }
        """;
}
