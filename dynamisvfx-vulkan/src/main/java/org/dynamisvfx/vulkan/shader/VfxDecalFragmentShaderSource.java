package org.dynamisvfx.vulkan.shader;

public final class VfxDecalFragmentShaderSource {
    private VfxDecalFragmentShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(location=0) in  vec3      inDecalLocalPos;
        layout(location=1) in  flat uint inDecalIdx;
        layout(location=0) out vec4      outColor;

        layout(set=3, binding=3) uniform sampler2D particleAtlas;
        layout(set=3, binding=4) uniform sampler2D depthBuffer;
        layout(set=3, binding=6) uniform sampler2D gBufferNormal;

        struct DecalInstance {
            mat4 inverseTransform;
            vec4 color;
            vec4 params;
        };

        layout(std430, set=3, binding=5) buffer DecalInstanceBuffer {
            DecalInstance decals[];
        };

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
            vec2 screenSize;
            float normalThreshold;
        } push;

        void main() {
            vec2 screenUV   = gl_FragCoord.xy / max(push.screenSize, vec2(1.0));
            float depth     = texture(depthBuffer, screenUV).r;

            vec4 clipPos    = vec4(screenUV * 2.0 - 1.0, depth, 1.0);
            vec4 viewPos    = inverse(frame.projection) * clipPos;
            viewPos        /= viewPos.w;
            vec4 worldPos   = inverse(frame.view) * viewPos;

            vec4 decalLocal = decals[inDecalIdx].inverseTransform * worldPos;
            if (any(greaterThan(abs(decalLocal.xyz), vec3(1.0)))) discard;

            vec3 surfaceNormal = texture(gBufferNormal, screenUV).xyz * 2.0 - 1.0;
            vec3 decalDown = normalize(mat3(decals[inDecalIdx].inverseTransform) * vec3(0, 1, 0));
            if (dot(surfaceNormal, decalDown) < push.normalThreshold) discard;

            vec2 decalUV  = decalLocal.xz * 0.5 + 0.5;
            vec4 atlasUV  = decals[inDecalIdx].params;
            vec2 finalUV  = atlasUV.xy + decalUV * atlasUV.zw;

            vec4 texColor = texture(particleAtlas, finalUV);
            outColor      = texColor * decals[inDecalIdx].color;
            if (outColor.a < 0.01) discard;
        }
        """;
}
