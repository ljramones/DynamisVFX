package org.dynamisvfx.vulkan.shader;

public final class VfxRadixHistogramShaderSource {
    private VfxRadixHistogramShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, binding=0) buffer Keys      { uint keys[];       };
        layout(std430, binding=1) buffer Histogram { uint histogram[];  };

        layout(push_constant) uniform PushConstants {
            uint count;
            uint bitShift;
        } push;

        shared uint localHistogram[256];

        void main() {
            localHistogram[gl_LocalInvocationID.x] = 0u;
            barrier();

            uint idx = gl_GlobalInvocationID.x;
            if (idx < push.count) {
                uint bucket = (keys[idx] >> push.bitShift) & 0xFFu;
                atomicAdd(localHistogram[bucket], 1u);
            }
            barrier();

            atomicAdd(histogram[gl_LocalInvocationID.x], localHistogram[gl_LocalInvocationID.x]);
        }
        """;
}
