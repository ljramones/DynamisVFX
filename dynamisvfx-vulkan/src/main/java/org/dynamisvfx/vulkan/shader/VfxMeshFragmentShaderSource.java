package org.dynamisvfx.vulkan.shader;

public final class VfxMeshFragmentShaderSource {
    private VfxMeshFragmentShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(location=0) in  vec3 inNormal;
        layout(location=1) in  vec2 inUV;
        layout(location=2) in  vec4 inTint;
        layout(location=0) out vec4 outColor;

        layout(set=3, binding=3) uniform sampler2D particleAtlas;

        layout(push_constant) uniform PushConstants {
            vec3  lightDir;
            float ambientStrength;
        } push;

        void main() {
            vec4 texColor  = texture(particleAtlas, inUV);
            float diffuse  = max(dot(normalize(inNormal), -normalize(push.lightDir)), 0.0);
            float lighting = push.ambientStrength + (1.0 - push.ambientStrength) * diffuse;
            outColor = texColor * inTint * lighting;
            if (outColor.a < 0.01) discard;
        }
        """;
}
