package org.dynamisvfx.vulkan.shader;

public final class VfxMeshVertexShaderSource {
    private VfxMeshVertexShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(location=0) in vec3 inPosition;
        layout(location=1) in vec3 inNormal;
        layout(location=2) in vec2 inUV;

        layout(std430, set=3, binding=5) buffer MeshInstanceBuffer {
            mat4 instanceTransforms[];
        };

        layout(std430, set=3, binding=0) buffer DrawIndexBuffer { uint drawIndices[]; };
        layout(std430, set=1, binding=2) buffer ColorBuffer     { vec4 colors[];      };

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

        layout(location=0) out vec3 outNormal;
        layout(location=1) out vec2 outUV;
        layout(location=2) out vec4 outTint;

        void main() {
            mat4 model    = instanceTransforms[gl_InstanceIndex];
            uint partIdx  = drawIndices[gl_InstanceIndex];

            vec4 worldPos = model * vec4(inPosition, 1.0);
            gl_Position   = frame.projection * frame.view * worldPos;

            mat3 normalMat = transpose(inverse(mat3(model)));
            outNormal = normalize(normalMat * inNormal);
            outUV     = inUV;
            outTint   = colors[partIdx];
        }
        """;
}
