package org.dynamisvfx.vulkan.shader;

public final class VfxRibbonVertexShaderSource {
    private VfxRibbonVertexShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(std430, set=3, binding=0) buffer DrawIndexBuffer { uint drawIndices[]; };
        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];   };
        layout(std430, set=1, binding=2) buffer ColorBuffer     { vec4 colors[];      };
        layout(std430, set=1, binding=3) buffer AttribBuffer    { vec4 attribs[];     };
        layout(std430, set=3, binding=5) buffer RibbonHistory   { vec4 history[];     };

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

        layout(push_constant) uniform PushConstants {
            uint  historyLength;
            float ribbonWidth;
            float widthTaper;
        } push;

        layout(location=0) out vec2  outUV;
        layout(location=1) out vec4  outColor;
        layout(location=2) out float outAlpha;

        void main() {
            uint particleIdx  = drawIndices[gl_InstanceIndex];
            uint historyFrame = gl_VertexIndex / 2u;
            uint side         = gl_VertexIndex % 2u;

            uint histIdx = particleIdx * push.historyLength + historyFrame;
            vec4 histEntry = history[histIdx];

            if (histEntry.w < 0.0) {
                gl_Position = vec4(0.0);
                return;
            }

            vec3 pos = histEntry.xyz;

            vec3 tangent = vec3(1, 0, 0);
            if (historyFrame + 1u < push.historyLength) {
                vec4 next = history[particleIdx * push.historyLength + historyFrame + 1u];
                if (next.w >= 0.0) tangent = normalize(next.xyz - pos);
            }

            vec3 toCamera = normalize(frame.cameraPos.xyz - pos);
            vec3 ribbonNormal = normalize(cross(tangent, toCamera));

            float t = float(historyFrame) / max(float(push.historyLength - 1u), 1.0);
            float width = push.ribbonWidth * mix(1.0, 0.0, t * push.widthTaper);

            float offset = (side == 0u ? -1.0 : 1.0) * width * 0.5;
            vec3 worldPos = pos + ribbonNormal * offset;

            gl_Position = frame.projection * frame.view * vec4(worldPos, 1.0);

            float uvX = float(historyFrame) / max(float(push.historyLength - 1u), 1.0);
            outUV      = vec2(uvX, float(side));
            outColor   = colors[particleIdx];
            outAlpha   = 1.0 - t;
        }
        """;
}
