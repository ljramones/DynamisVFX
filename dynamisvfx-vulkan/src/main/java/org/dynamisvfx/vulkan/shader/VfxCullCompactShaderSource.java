package org.dynamisvfx.vulkan.shader;

public final class VfxCullCompactShaderSource {
    private VfxCullCompactShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=1, binding=0) buffer PositionBuffer { vec4 positions[]; };
        layout(std430, set=1, binding=3) buffer AttribBuffer   { vec4 attribs[];   };
        layout(std430, set=3, binding=0) buffer DrawIndexBuffer{ uint drawIndices[];};
        layout(std430, set=3, binding=1) buffer IndirectBuffer {
            uint vertexCount;
            uint instanceCount;
            uint firstVertex;
            uint firstInstance;
        } indirectBuffer;
        layout(std430, set=3, binding=2) buffer SortedIndexBuffer { uint sortedIndices[]; };

        layout(push_constant) uniform PushConstants {
            uint  maxParticles;
            uint  useSortedIndices;
            float frustumPlanes[24];
        } push;

        bool frustumTest(vec3 pos, float radius) {
            for (int i = 0; i < 6; i++) {
                vec3 n = vec3(push.frustumPlanes[i*4],
                              push.frustumPlanes[i*4+1],
                              push.frustumPlanes[i*4+2]);
                float d = push.frustumPlanes[i*4+3];
                if (dot(n, pos) + d < -radius) return false;
            }
            return true;
        }

        shared uint localCount;
        shared uint localBase;

        void main() {
            if (gl_LocalInvocationID.x == 0u) localCount = 0u;
            barrier();

            uint idx = gl_GlobalInvocationID.x;
            uint particleIdx = (push.useSortedIndices == 1u)
                ? sortedIndices[idx]
                : idx;

            bool alive   = idx < push.maxParticles && positions[particleIdx].w < 1.0;
            float size   = alive ? attribs[particleIdx].x : 0.0;
            bool visible = alive && frustumTest(positions[particleIdx].xyz, size);

            uint localSlot = 0u;
            if (visible) localSlot = atomicAdd(localCount, 1u);
            barrier();

            if (gl_LocalInvocationID.x == 0u)
                localBase = atomicAdd(indirectBuffer.instanceCount, localCount);
            barrier();

            if (visible)
                drawIndices[localBase + localSlot] = particleIdx;
        }
        """;
}
