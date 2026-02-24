# DynamisVFX Architecture Blueprint

## Architectural Choices

### Question 1: Simulation Model
**Answer:** A) Pure GPU SoA

**Why:** At 10M particles, the PCIe bus becomes a massive bottleneck if state is uploaded every frame. By keeping state in SSBOs on the GPU, the CPU mostly dispatches `vkCmdDispatch` calls.

**Implementation:** Use `MemorySegment` for initial emit data. Once particles are alive, state remains in VRAM. This also simplifies persistent state and sleeping.

### Question 2: Effect Authoring
**Answer:** C) Both (Code API that serializes to/from data descriptors)

**Why:** Data-driven descriptors are required for tools and hot reload, while code APIs are required for tests and procedural power-user effects.

**Implementation:** Java Builder API produces JSON/Binary AST descriptors. The runtime simulation only consumes descriptors.

### Question 3: Renderer Coupling
**Answer:** C) VFX registers draw calls through DynamisGPU indirect buffer

**Why:** GPU simulation writes draw call data into indirect buffers consumed by DynamicLightEngine.

**Benefit:** VFX becomes a first-class pipeline participant for shadows, depth, and G-buffer integration.

### Question 4: Effect Lifecycle
**Answer:** B) Engine owns effect handles, VFX is purely a simulation/render service

**Why:** Avoids registry bloat and GC pressure; effect lifetime ties directly to entities/events.

**Service Model:** DynamisVFX is a processing service. Engine submits active emitters and receives Vulkan dispatches/recorded draws.

### Question 5: Physics Coupling
**Answer:** C) DynamisVFX defines a `PhysicsHandoff` interface, engine provides implementation

**Why:** Keeps DynamisVFX decoupled from DynamisCollision.

**Flow:** DynamisVFX emits debris handoff events (transform/velocity). Engine creates rigid bodies in DynamisCollision.

## Architectural Impact Summary

| Component | Strategy | Key Dependency / Tool |
|---|---|---|
| Data Layout | SoA (GPU-side) | DynamisGPU SSBOs |
| Pipeline | Indirect execution | `vkCmdDispatchIndirect` |
| Interface | Data-driven AST | Jackson (JSON) / Protobuf (Binary) |
| Inter-Lib | Inversion of control | Java interfaces / callbacks |

## Module Responsibilities

```text
dynamisvfx-api
  Effect descriptors (JSON/binary AST)
  Emitter config value types
  PhysicsHandoff interface
  VfxHandle (opaque typed handle, engine-owned)
  VfxService interface (what the engine calls)
  ParticleEmitterDescriptor, ForceDescriptor, RendererDescriptor
  No GPU, no LWJGL, no Vectrix

dynamisvfx-core
  Builder API — programmatic effect construction
  Descriptor serialization (Jackson JSON, optional Protobuf)
  Simulation parameter validation
  LOD policy computation (CPU-side distance decisions)
  FastNoiseLiteNouveau — force field baking, noise curve evaluation
  Vectrix — spline math, emitter shape geometry
  No Vulkan

dynamisvfx-vulkan
  VfxVulkanService implements VfxService
  GPU SoA buffer layout and allocation
  Compute pipeline management (emit, simulate, sort, compact, cull)
  Indirect draw buffer registration via DynamisGPU
  Descriptor heap slot management via DynamisGPU
  Renderer pipelines (billboard, ribbon, mesh, beam, decal)
  PhysicsHandoff readback path (async GPU→CPU for debris spawn)
  DynamisGPU-api + DynamisGPU-vulkan
  LWJGL Vulkan

dynamisvfx-test
  MockVfxService
  Deterministic sim harness (fixed seed, N steps, assert particle counts)
  DynamisGPU-test mocks

dynamisvfx-bench
  JMH: emit throughput, sort throughput, cull overhead, indirect submission cost
```

## GPU SoA Particle State Layout

```glsl
// SSBO binding 0 — position + lifetime
layout(std430, binding = 0) buffer PositionBuffer {
    vec4 positions[];     // xyz = world pos, w = normalizedAge [0..1]
};

// SSBO binding 1 — velocity + mass
layout(std430, binding = 1) buffer VelocityBuffer {
    vec4 velocities[];    // xyz = velocity m/s, w = mass kg
};

// SSBO binding 2 — color + opacity
layout(std430, binding = 2) buffer ColorBuffer {
    vec4 colors[];        // rgba, pre-multiplied alpha
};

// SSBO binding 3 — size + rotation + flags
layout(std430, binding = 3) buffer AttribBuffer {
    vec4 attribs[];       // x = size, y = rotation rad, z = frameIndex, w = flags (uint)
};

// SSBO binding 4 — emitter ID + seed + user data
layout(std430, binding = 4) buffer MetaBuffer {
    uvec4 meta[];         // x = emitterID, y = seed, z = userDataA, w = userDataB
};
```

**Why this layout:**
- Compute stages touch only the buffers they need.
- Better cache behavior than large interleaved structs.
- Extensible by adding new SSBOs.
- Maps cleanly to Vectrix `Vector4f` for emit upload boundaries.

**Memory budget:** 80 bytes/particle. At 10M particles, ~800MB VRAM.

## Compute Pipeline Stages

```text
Stage 1 — RETIRE
  Input:  PositionBuffer (w = normalizedAge)
  Output: AliveCount (atomic decrement), FreeList (push dead slots)
  Work:   One thread per particle, age >= 1.0 → push to free list

Stage 2 — EMIT
  Input:  EmitterDescriptor SSBO, FreeList, RNG seed
  Output: All five SoA buffers (write new particle data into free slots)
  Work:   One thread per new particle this frame

Stage 3 — SIMULATE
  Input:  PositionBuffer, VelocityBuffer, ForceFieldSSBO, DeltaTime
  Output: PositionBuffer (updated), VelocityBuffer (updated)
  Work:   One thread per alive particle
  Forces: Gravity, drag, curl noise field, attractor/repulsor, wind zone

Stage 4 — SORT (conditional)
  Input:  PositionBuffer, CameraPosition
  Output: SortKeyBuffer (float distance), IndexBuffer (sorted indices)
  Work:   GPU radix sort — only runs for transparent renderer variants
  Skip:   Additive blend modes don't need sort

Stage 5 — CULL + COMPACT + INDIRECT WRITE
  Input:  PositionBuffer, FrustumPlanes, SortedIndexBuffer
  Output: DrawIndexBuffer (compacted alive+visible indices),
          VkDrawIndirectCommand (written to DynamisGPU IndirectCommandBuffer)
  Work:   Prefix sum compaction, frustum sphere test per particle
```

**Barrier chain:**

```text
RETIRE → memory barrier → EMIT → memory barrier → SIMULATE
→ (optional) SORT → memory barrier → CULL+COMPACT+INDIRECT WRITE
→ execution barrier → main pass draw
```

## Effect Descriptor Schema (Java API + JSON)

```java
// dynamisvfx-api — pure value types, no logic

public record ParticleEmitterDescriptor(
    String id,
    EmitterShapeDescriptor shape,       // point/sphere/cone/mesh/spline
    EmissionRateDescriptor rate,        // burst/continuous/event
    ParticleInitDescriptor init,        // initial velocity/size/color/lifetime
    List<ForceDescriptor> forces,       // gravity/drag/noise/attractor
    RendererDescriptor renderer,        // billboard/ribbon/mesh/beam
    LodDescriptor lod,                  // distance tiers
    PhysicsHandoffDescriptor physics    // optional debris handoff config
) {}

public record ForceDescriptor(
    ForceType type,                     // GRAVITY/DRAG/CURL_NOISE/ATTRACTOR/WIND
    float strength,
    Vector3f direction,                 // for directional forces
    NoiseForceConfig noiseConfig        // FastNoiseLiteNouveau params if type=CURL_NOISE
) {}

public record RendererDescriptor(
    RendererType type,                  // BILLBOARD/RIBBON/MESH/BEAM/DECAL
    BlendMode blendMode,                // ADDITIVE/ALPHA/PREMULTIPLIED
    String textureAtlasId,
    int frameCount,
    boolean softParticles,
    boolean lightEmitting
) {}
```

**Builder pattern in `dynamisvfx-core`:**

```java
ParticleEmitterDescriptor fire = EffectBuilder.emitter("fire_burst")
    .shape(EmitterShape.sphere(0.5f))
    .rate(EmissionRate.burst(500))
    .init(ParticleInit.builder()
        .lifetime(Range.of(0.8f, 1.4f))
        .velocity(Range.of(2f, 6f), Direction.UP)
        .size(Range.of(0.1f, 0.3f))
        .color(Gradient.from(Color.ORANGE).to(Color.TRANSPARENT))
        .build())
    .force(Force.gravity(9.8f))
    .force(Force.drag(0.4f))
    .force(Force.curlNoise(CurlConfig.defaults().frequency(0.02f).strength(1.5f)))
    .renderer(Renderer.billboard()
        .texture("fx/fire_atlas.ktx2")
        .blend(BlendMode.ADDITIVE)
        .softParticles(true)
        .build())
    .build();

// Serialize to JSON
String json = EffectSerializer.toJson(fire);

// Deserialize for hot-reload
ParticleEmitterDescriptor reloaded = EffectSerializer.fromJson(json);
```

## VfxService Interface (engine-facing)

```java
// dynamisvfx-api

public interface VfxService {

    // Engine calls once per frame with all active handles
    void simulate(List<VfxHandle> activeEffects, float deltaTime,
                  VfxFrameContext ctx);

    // Engine calls after simulate — registers indirect draw commands
    void recordDraws(List<VfxHandle> activeEffects, VfxDrawContext ctx);

    // Handle lifecycle — engine owns these
    VfxHandle spawn(ParticleEmitterDescriptor descriptor, Matrix4f transform);
    void despawn(VfxHandle handle);
    void updateTransform(VfxHandle handle, Matrix4f transform);

    // Budget query
    VfxStats getStats();
}

public interface VfxFrameContext {
    long commandBuffer();       // VkCommandBuffer handle
    Matrix4f cameraView();
    Matrix4f cameraProjection();
    float[] frustumPlanes();    // 6 planes
    long frameIndex();
}

public interface VfxDrawContext {
    IndirectCommandBuffer indirectBuffer(); // from DynamisGPU
    BindlessHeap bindlessHeap();            // from DynamisGPU
    long frameIndex();
}
```

## PhysicsHandoff Interface

```java
// dynamisvfx-api

public interface PhysicsHandoff {

    // Called by DynamisVFX when a particle meets debris criteria
    // Engine impl creates a DynamisCollision rigid body
    void onDebrisSpawn(DebrisSpawnEvent event);
}

public record DebrisSpawnEvent(
    Matrix4f worldTransform,
    Vector3f velocity,
    Vector3f angularVelocity,
    float mass,
    String meshId,          // which fragment mesh to use
    String materialTag,     // surface material for restitution/friction lookup
    int sourceEmitterId
) {}
```

Engine wiring:

```java
vfxService.setPhysicsHandoff(debrisEvent ->
    dynamisCollision.spawnRigidBody(
        debrisEvent.worldTransform(),
        debrisEvent.velocity(),
        debrisEvent.mass(),
        meshRegistry.get(debrisEvent.meshId())
    )
);
```

## VfxHandle

```java
// dynamisvfx-api — opaque, engine-owned

public final class VfxHandle {
    private final int id;
    private final int generation;   // stale detection
    private final String effectId;  // descriptor id for debugging

    // No public constructor — created only by VfxService.spawn()
    // Engine holds these, passes back to VfxService each frame
}
```

## Module Dependency Graph

```text
dynamisvfx-api
  (zero deps — pure Java 21)
       ↑
dynamisvfx-core                    dynamisvfx-test
  + Vectrix                          + dynamis-gpu-test
  + FastNoiseLiteNouveau             + JUnit 5
       ↑
dynamisvfx-vulkan
  + dynamis-gpu-api
  + dynamis-gpu-vulkan
  + LWJGL Vulkan
  + dynamisvfx-core
       ↑
dynamisvfx-bench
  + JMH
```

## What DynamicLightEngine Sees

```java
// Engine startup
VfxService vfx = new VfxVulkanService(vulkanContext, bindlessHeap, indirectBuffer);
vfx.setPhysicsHandoff(physicsHandoffImpl);

// Engine per-frame
vfx.simulate(activeEffects, deltaTime, frameContext);
// ... other render passes ...
vfx.recordDraws(activeEffects, drawContext);

// Game code
VfxHandle explosion = vfx.spawn(effects.get("explosion_large"), hitTransform);
// Engine holds explosion handle, despawns when entity dies
vfx.despawn(explosion);
```

## What Comes First

```text
1. dynamisvfx-api     — descriptors, VfxService, VfxHandle, PhysicsHandoff
2. dynamisvfx-core    — builders, serialization, validation, noise baking
3. dynamisvfx-test    — mock service, deterministic harness
4. dynamisvfx-vulkan  — SoA buffers, compute stages 1-5, renderer pipelines
5. dynamisvfx-bench   — throughput numbers
```

## GLSL ParticleState Alignment with Vectrix

```java
// Java emit-side — Vectrix types map directly to GLSL vec4
Vector4f posAge   = new Vector4f(x, y, z, 0f);      // binding 0
Vector4f velMass  = new Vector4f(vx, vy, vz, mass);  // binding 1
Vector4f color    = new Vector4f(r, g, b, a);         // binding 2
Vector4f attribs  = new Vector4f(size, rot, frame, flags); // binding 3
```

`Vector4f.putToBuffer(ByteBuffer)` writes 16 bytes aligned for `std430` `vec4`, with no padding conversion.
