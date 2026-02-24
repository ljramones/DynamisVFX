package org.dynamisvfx.vulkan.shader;

public final class VfxDebrisCandidateShaderSource {
    private VfxDebrisCandidateShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        struct DebrisCandidate {
            vec4 positionMass;
            vec4 velocity;
            uvec4 meta;
        };

        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];  };
        layout(std430, set=1, binding=1) buffer VelocityBuffer  { vec4 velocities[]; };
        layout(std430, set=1, binding=4) buffer MetaBuffer      { uvec4 meta[];      };

        layout(std430, set=3, binding=7) buffer DebrisReadbackBuffer {
            uint candidateCount;
            uint pad0; uint pad1; uint pad2;
            DebrisCandidate candidates[];
        };

        layout(push_constant) uniform PushConstants {
            uint  maxParticles;
            float debrisAgeThreshold;
            float debrisSpeedThreshold;
            uint  maxCandidates;
        } push;

        void main() {
            uint idx = gl_GlobalInvocationID.x;
            if (idx >= push.maxParticles) return;
            if (positions[idx].w >= 1.0) return;

            float age   = positions[idx].w;
            float speed = length(velocities[idx].xyz);
            uint  flags = meta[idx].w;

            bool isDebris = (flags & 1u) != 0u
                && age > push.debrisAgeThreshold
                && speed > push.debrisSpeedThreshold;
            if (!isDebris) return;

            uint slot = atomicAdd(candidateCount, 1u);
            if (slot >= push.maxCandidates) return;

            candidates[slot].positionMass = vec4(positions[idx].xyz, velocities[idx].w);
            candidates[slot].velocity     = vec4(velocities[idx].xyz, 0.0);
            candidates[slot].meta         = meta[idx];
        }
        """;
}
