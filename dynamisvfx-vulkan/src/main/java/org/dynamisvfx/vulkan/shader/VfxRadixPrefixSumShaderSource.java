package org.dynamisvfx.vulkan.shader;

public final class VfxRadixPrefixSumShaderSource {
    private VfxRadixPrefixSumShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, binding=0) buffer Histogram { uint histogram[]; };
        layout(std430, binding=1) buffer Prefix    { uint prefix[];    };

        shared uint temp[512];

        void main() {
            // Standard work-efficient parallel prefix sum (Blelloch scan)
            // Input: 256 histogram values
            // Output: 256 exclusive prefix sums
           uint tid = gl_LocalInvocationID.x;
           temp[tid] = histogram[tid];
           barrier();

           for (uint offset = 1u; offset < 256u; offset <<= 1u) {
               uint value = temp[tid];
               if (tid >= offset) {
                   value += temp[tid - offset];
               }
               barrier();
               temp[tid] = value;
               barrier();
           }

           prefix[tid] = (tid == 0u) ? 0u : temp[tid - 1u];
        }
        """;
}
