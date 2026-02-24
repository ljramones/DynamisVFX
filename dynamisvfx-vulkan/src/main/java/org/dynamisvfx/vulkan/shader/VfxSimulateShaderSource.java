package org.dynamisvfx.vulkan.shader;

public final class VfxSimulateShaderSource {
    private VfxSimulateShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        struct ForceEntry {
            uint  type;
            float strength;
            vec3  direction;
            vec3  origin;
        };

        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];  };
        layout(std430, set=1, binding=1) buffer VelocityBuffer  { vec4 velocities[]; };
        layout(std430, set=1, binding=2) buffer ColorBuffer     { vec4 colors[];     };
        layout(std430, set=1, binding=3) buffer AttribBuffer    { vec4 attribs[];    };

        layout(std430, set=2, binding=3) buffer ForceFieldBuffer {
            uint forceCount;
            uint pad0; uint pad1; uint pad2;
            ForceEntry forces[];
        } forceBuffer;

        layout(set=2, binding=4) uniform sampler3D noiseField3D;

        layout(std140, set=0, binding=0) uniform FrameUniforms {
            mat4 view;
            mat4 projection;
            vec4 cameraPos;
            vec4 frustumPlanes[6];
            float deltaTime;
            float totalTime;
            uint frameIndex;
            uint padding;
        } frameUniforms;

        layout(push_constant) uniform PushConstants {
            uint maxParticles;
            float noiseWorldScale;
            float noiseStrength;
        } push;

        vec3 evaluateForce(ForceEntry f, vec3 pos, vec3 vel, float mass) {
            if (f.type == 0u) {  // GRAVITY
                return f.direction * f.strength;
            }
            if (f.type == 1u) {  // DRAG
                return -vel * f.strength;
            }
            if (f.type == 2u) {  // ATTRACTOR
                vec3 toOrigin = f.origin - pos;
                float dist = length(toOrigin);
                if (dist < 0.001) return vec3(0);
                float falloff = 1.0 - clamp(dist / 1.0, 0.0, 1.0);
                return normalize(toOrigin) * f.strength * falloff;
            }
            if (f.type == 3u) {  // WIND
                return f.direction * f.strength;
            }
            return vec3(0);
        }

        void main() {
            uint idx = gl_GlobalInvocationID.x;
            if (idx >= push.maxParticles) return;
            if (positions[idx].w >= 1.0) return; // dead

            vec3 pos  = positions[idx].xyz;
            vec3 vel  = velocities[idx].xyz;
            float mass = velocities[idx].w;
            float age  = positions[idx].w;

            // Accumulate forces
            vec3 acceleration = vec3(0.0);
            for (uint i = 0u; i < forceBuffer.forceCount; i++) {
                acceleration += evaluateForce(forceBuffer.forces[i], pos, vel, mass);
            }

            if (push.noiseStrength > 0.0) {
                vec3 noiseUV = (pos / max(push.noiseWorldScale, 0.0001)) * 0.5 + 0.5;
                noiseUV = clamp(noiseUV, 0.0, 1.0);
                vec3 curlVel = texture(noiseField3D, noiseUV).rgb;
                vel += curlVel * push.noiseStrength * frameUniforms.deltaTime;
            }

            // Integrate velocity and position (semi-implicit Euler)
            vel += acceleration * frameUniforms.deltaTime;
            pos += vel * frameUniforms.deltaTime;

            // Advance age — lifetime stored in attribs.w
            float lifetime = max(attribs[idx].w, 0.001);
            age += frameUniforms.deltaTime / lifetime;

            positions[idx].xyz  = pos;
            positions[idx].w    = age;
            velocities[idx].xyz = vel;
        }
        """;
}
