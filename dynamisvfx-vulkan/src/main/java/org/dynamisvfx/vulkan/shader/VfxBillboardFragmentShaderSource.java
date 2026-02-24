package org.dynamisvfx.vulkan.shader;

public final class VfxBillboardFragmentShaderSource {
    private VfxBillboardFragmentShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(location=0) in  vec2 inUV;
        layout(location=1) in  vec4 inColor;
        layout(location=2) in  float inAge;
        layout(location=0) out vec4 outColor;

        layout(set=3, binding=3) uniform sampler2D particleAtlas;
        layout(set=3, binding=4) uniform sampler2D depthBuffer;

        layout(push_constant) uniform PushConstants {
            uint  softParticles;
            float softRange;
            uint  frameCount;
            float frameRate;
        } push;

        void main() {
            uint frame = uint(inAge * push.frameRate * float(push.frameCount))
                         % max(push.frameCount, 1u);
            float frameU = (inUV.x + float(frame)) / max(float(push.frameCount), 1.0);

            vec4 texColor = texture(particleAtlas, vec2(frameU, inUV.y));
            outColor = texColor * inColor;

            if (push.softParticles == 1u) {
                vec2 screenUV = gl_FragCoord.xy / vec2(textureSize(depthBuffer, 0));
                float sceneDepth = texture(depthBuffer, screenUV).r;
                float particleDepth = gl_FragCoord.z;
                float diff = abs(sceneDepth - particleDepth);
                float fade = clamp(diff / max(push.softRange, 0.0001), 0.0, 1.0);
                outColor.a *= fade;
            }

            if (outColor.a < 0.01) discard;
        }
        """;
}
