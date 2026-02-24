package org.dynamisvfx.vulkan.shader;

public final class VfxBeamVertexShaderSource {
    private VfxBeamVertexShaderSource() {
    }

    public static final String GLSL = """
        #version 450

        struct BeamEndpoint {
            vec4 start;
            vec4 end;
            vec4 color;
            vec4 params;
        };

        layout(std430, set=3, binding=5) buffer BeamEndpointBuffer {
            BeamEndpoint beams[];
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
            uint  maxSegments;
            float time;
        } push;

        layout(location=0) out vec2  outUV;
        layout(location=1) out vec4  outColor;

        float hash(float n) { return fract(sin(n) * 43758.5453); }

        void main() {
            uint beamIdx  = gl_InstanceIndex;
            uint segIdx   = gl_VertexIndex / 2u;
            uint side     = gl_VertexIndex % 2u;

            vec3  start      = beams[beamIdx].start.xyz;
            vec3  end        = beams[beamIdx].end.xyz;
            float width      = beams[beamIdx].start.w;
            float noiseAmp   = beams[beamIdx].params.y;
            float noiseFreq  = beams[beamIdx].params.z;
            uint  segCount   = uint(beams[beamIdx].params.w);

            segCount = clamp(segCount, 1u, max(push.maxSegments, 1u));

            float t = float(segIdx) / float(segCount);
            vec3  pos = mix(start, end, t);

            vec3 beamAxis = normalize(end - start);
            vec3 toCamera = normalize(frame.cameraPos.xyz - pos);
            vec3 perp     = normalize(cross(beamAxis, toCamera));

            float noise = (hash(t * noiseFreq + push.time) - 0.5) * 2.0 * noiseAmp;
            pos += perp * noise;

            float edgeOffset = (side == 0u ? -1.0 : 1.0) * width * 0.5;
            pos += perp * edgeOffset;

            gl_Position = frame.projection * frame.view * vec4(pos, 1.0);

            float uvScrollOffset = fract(push.time * beams[beamIdx].params.x);
            outUV    = vec2(t + uvScrollOffset, float(side));
            outColor = beams[beamIdx].color;
        }
        """;
}
