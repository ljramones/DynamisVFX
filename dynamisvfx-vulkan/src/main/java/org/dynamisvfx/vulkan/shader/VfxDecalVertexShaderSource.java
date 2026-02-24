package org.dynamisvfx.vulkan.shader;

public final class VfxDecalVertexShaderSource {
    private VfxDecalVertexShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        const vec3 CUBE_VERTS[36] = vec3[36](
            vec3(-1,-1,-1), vec3( 1,-1,-1), vec3( 1, 1,-1),
            vec3(-1,-1,-1), vec3( 1, 1,-1), vec3(-1, 1,-1),
            vec3(-1,-1, 1), vec3( 1, 1, 1), vec3( 1,-1, 1),
            vec3(-1,-1, 1), vec3(-1, 1, 1), vec3( 1, 1, 1),
            vec3(-1,-1,-1), vec3(-1, 1,-1), vec3(-1, 1, 1),
            vec3(-1,-1,-1), vec3(-1, 1, 1), vec3(-1,-1, 1),
            vec3( 1,-1,-1), vec3( 1, 1, 1), vec3( 1, 1,-1),
            vec3( 1,-1,-1), vec3( 1,-1, 1), vec3( 1, 1, 1),
            vec3(-1,-1,-1), vec3(-1,-1, 1), vec3( 1,-1, 1),
            vec3(-1,-1,-1), vec3( 1,-1, 1), vec3( 1,-1,-1),
            vec3(-1, 1,-1), vec3( 1, 1, 1), vec3(-1, 1, 1),
            vec3(-1, 1,-1), vec3( 1, 1,-1), vec3( 1, 1, 1)
        );

        struct DecalInstance {
            mat4 inverseTransform;
            vec4 color;
            vec4 params;
        };

        layout(std430, set=3, binding=5) buffer DecalInstanceBuffer {
            DecalInstance decals[];
        };

        layout(std430, set=3, binding=0) buffer DrawIndexBuffer { uint drawIndices[]; };

        layout(set=0, binding=0) uniform FrameUniforms {
            mat4 view;
            mat4 projection;
            vec4 cameraPos;
            vec4 frustumPlanes[6];
            float deltaTime;
            float totalTime;
            uint frameIndex;
            uint pad;
        } frame;

        layout(location=0) out vec3 outDecalLocalPos;
        layout(location=1) out flat uint outDecalIdx;

        void main() {
            uint decalIdx  = drawIndices[gl_InstanceIndex];
            vec3 localPos  = CUBE_VERTS[gl_VertexIndex];

            mat4 decalWorld = inverse(decals[decalIdx].inverseTransform);
            vec4 worldPos   = decalWorld * vec4(localPos, 1.0);

            gl_Position      = frame.projection * frame.view * worldPos;
            outDecalLocalPos = localPos;
            outDecalIdx      = decalIdx;
        }
        """;
}
