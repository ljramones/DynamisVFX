package org.dynamisvfx.vulkan.shader;

public final class VfxEmitShaderSource {
    private VfxEmitShaderSource() {
    }

    public static final String GLSL = """
        #version 450
        layout(local_size_x = 256) in;

        layout(std430, set=1, binding=0) buffer PositionBuffer  { vec4 positions[];  };
        layout(std430, set=1, binding=1) buffer VelocityBuffer  { vec4 velocities[]; };
        layout(std430, set=1, binding=2) buffer ColorBuffer     { vec4 colors[];     };
        layout(std430, set=1, binding=3) buffer AttribBuffer    { vec4 attribs[];    };
        layout(std430, set=1, binding=4) buffer MetaBuffer      { uvec4 meta[];      };

        layout(std430, set=2, binding=1) buffer FreeListBuffer {
            uint writeHead;
            uint readHead;
            uint slots[];
        } freeList;
        layout(std430, set=2, binding=2) buffer AliveCountBuffer { uint count; } aliveCount;
        layout(std430, set=2, binding=0) buffer EmitterDescBuffer {
            uint shapeType;
            vec4 shape0;
            vec4 shape1;
            vec4 velocity0;
            vec4 velocity1;
            vec2 sizeRange;
            vec2 lifetimeRange;
            vec4 initColor;
            uint flags;
        } emitter;

        layout(push_constant) uniform PushConstants {
            uint spawnCount;
            uint seed;
            uint emitterID;
            uint frameIndex;
        } push;

        // PCG hash for per-particle RNG
        uint pcg(uint v) {
            uint state = v * 747796405u + 2891336453u;
            uint word = ((state >> ((state >> 28u) + 4u)) ^ state) * 277803737u;
            return (word >> 22u) ^ word;
        }

        float rand01(uint v) {
            return float(v & 0x00ffffffu) / 16777216.0;
        }

        void main() {
            uint spawnIdx = gl_GlobalInvocationID.x;
            if (spawnIdx >= push.spawnCount) return;

            uint slot = freeList.slots[freeList.readHead + spawnIdx];
            uint rng = pcg(push.seed ^ spawnIdx);

            // Sample position from emitter shape (simple sphere fallback)
            vec3 sampledPos = vec3(0.0);
            float r = emitter.shape0.w * rand01(rng);
            sampledPos += vec3(r, 0.0, 0.0);

            // Sample velocity from init range
            float speedMin = emitter.velocity0.w;
            float speedMax = emitter.velocity1.w;
            float speed = mix(speedMin, speedMax, rand01(rng ^ 0x9e3779b9u));
            vec3 dir = normalize(max(abs(emitter.velocity0.xyz), vec3(1e-5)) * sign(emitter.velocity0.xyz + vec3(1e-5)));
            vec3 sampledVel = dir * speed;

            // Sample size
            float sampledSize = mix(emitter.sizeRange.x, emitter.sizeRange.y, rand01(rng ^ 0x7f4a7c15u));

            positions[slot]  = vec4(sampledPos, 0.0);
            velocities[slot] = vec4(sampledVel, 1.0);
            colors[slot]     = emitter.initColor;
            attribs[slot]    = vec4(sampledSize, 0.0, 0.0, 0.0);
            meta[slot]       = uvec4(push.emitterID, rng, 0, 0);

            atomicAdd(aliveCount.count, 1);
            atomicAdd(freeList.readHead, 1);
        }
        """;
}
