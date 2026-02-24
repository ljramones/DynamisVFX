package org.dynamisvfx.vulkan.shader;

public final class VfxBeamFragmentShaderSource {
    private VfxBeamFragmentShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        layout(location=0) in  vec2 inUV;
        layout(location=1) in  vec4 inColor;
        layout(location=0) out vec4 outColor;

        layout(set=3, binding=3) uniform sampler2D particleAtlas;

        void main() {
            vec4 texColor = texture(particleAtlas, inUV);
            outColor = texColor * inColor;
            if (outColor.a < 0.01) discard;
        }
        """;
}
