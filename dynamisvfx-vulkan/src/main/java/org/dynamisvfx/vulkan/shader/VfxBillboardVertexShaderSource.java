package org.dynamisvfx.vulkan.shader;

public final class VfxBillboardVertexShaderSource {
    private VfxBillboardVertexShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(std430, set=3, binding=0) buffer DrawIndexBuffer { uint drawIndices[]; };
        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];   };
        layout(std430, set=1, binding=2) buffer ColorBuffer     { vec4 colors[];      };
        layout(std430, set=1, binding=3) buffer AttribBuffer    { vec4 attribs[];     };

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

        layout(location=0) out vec2 outUV;
        layout(location=1) out vec4 outColor;
        layout(location=2) out float outAge;

        const vec2 CORNERS[6] = vec2[6](
            vec2(-1, -1), vec2( 1, -1), vec2(-1,  1),
            vec2(-1,  1), vec2( 1, -1), vec2( 1,  1)
        );
        const vec2 UVS[6] = vec2[6](
            vec2(0,0), vec2(1,0), vec2(0,1),
            vec2(0,1), vec2(1,0), vec2(1,1)
        );

        void main() {
            uint particleIdx = drawIndices[gl_InstanceIndex];
            vec3 worldPos    = positions[particleIdx].xyz;
            float age        = positions[particleIdx].w;
            float size       = attribs[particleIdx].x;
            float rotation   = attribs[particleIdx].y;

            vec3 right = vec3(frame.view[0][0], frame.view[1][0], frame.view[2][0]);
            vec3 up    = vec3(frame.view[0][1], frame.view[1][1], frame.view[2][1]);

            float cosR = cos(rotation), sinR = sin(rotation);
            vec2 corner = CORNERS[gl_VertexIndex];
            vec2 rotated = vec2(
                corner.x * cosR - corner.y * sinR,
                corner.x * sinR + corner.y * cosR
            );

            vec3 offset = (right * rotated.x + up * rotated.y) * size * 0.5;
            vec4 viewPos = frame.view * vec4(worldPos + offset, 1.0);

            gl_Position = frame.projection * viewPos;
            outUV       = UVS[gl_VertexIndex];
            outColor    = colors[particleIdx];
            outAge      = age;
        }
        """;
}
