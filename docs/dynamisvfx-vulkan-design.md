# DynamisVFX Vulkan Design

## 1. Descriptor Set Layout

Four descriptor sets, fixed assignments across all VFX pipelines:

```
Set 0 — Per-frame global
  binding 0: uniform buffer — FrameUniforms (camera, frustum, deltaTime, frameIndex)

Set 1 — Per-effect SoA particle state
  binding 0: SSBO — PositionBuffer     (vec4[]: xyz=pos, w=normalizedAge)
  binding 1: SSBO — VelocityBuffer     (vec4[]: xyz=vel, w=mass)
  binding 2: SSBO — ColorBuffer        (vec4[]: rgba)
  binding 3: SSBO — AttribBuffer       (vec4[]: x=size, y=rotation, z=frameIndex, w=flags)
  binding 4: SSBO — MetaBuffer         (uvec4[]: x=emitterID, y=seed, z=userA, w=userB)

Set 2 — Per-effect compute control
  binding 0: SSBO — EmitterDescriptorBuffer  (packed emitter params)
  binding 1: SSBO — ForceFieldBuffer         (active forces this frame)
  binding 2: SSBO — FreeListBuffer           (uint[]: free slot indices)
  binding 3: SSBO — AliveCountBuffer         (uint: atomic alive count)
  binding 4: storage image — NoiseField3D    (rgba16f 3D texture: xyz=curl velocity)

Set 3 — Per-effect render + indirect
  binding 0: SSBO — DrawIndexBuffer          (uint[]: compacted alive+visible indices)
  binding 1: SSBO — IndirectCommandBuffer    (VkDrawIndirectCommand)
  binding 2: SSBO — SortKeyBuffer            (float[]: camera distance per particle)
  binding 3: combined image sampler — ParticleAtlas
  binding 4: combined image sampler — DepthBuffer (soft particles)
```

---

## 2. SoA Buffer Sizing

```
MaxParticles per effect: configured at spawn time, power of two
Default budget: 65536 per effect (64K × 80 bytes = 5MB per effect)
Hard cap: 1048576 per effect (1M × 80 bytes = 80MB per effect)
Global particle budget: enforced by VfxBudgetAllocator
```

All five SoA buffers allocated once at effect spawn. Never reallocated — fixed size for GPU-side lifetime. FreeList pre-populated with all slots at allocation time.

---

## 3. Compute Stage Contracts

### Stage 1 — RETIRE
```glsl
layout(local_size_x = 256) in;

// Reads: PositionBuffer (w = normalizedAge)
// Writes: FreeListBuffer (push dead slots), AliveCountBuffer (atomic decrement)

void main() {
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= push.maxParticles) return;
    if (positions[idx].w >= 1.0) {
        uint freeSlot = atomicAdd(freeList.writeHead, 1);
        freeList.slots[freeSlot] = idx;
        atomicAdd(aliveCount.count, -1);
    }
}
```

### Stage 2 — EMIT
```glsl
layout(local_size_x = 256) in;

// Reads: EmitterDescriptorBuffer, FreeListBuffer
// Writes: All 5 SoA buffers (new particle data into free slots)
// Push constant: spawnCount this frame, RNG seed, emitterID

void main() {
    uint spawnIdx = gl_GlobalInvocationID.x;
    if (spawnIdx >= push.spawnCount) return;
    uint slot = freeList.slots[freeList.readHead + spawnIdx];
    // Initialize particle at slot from emitter descriptor + RNG
    positions[slot]  = sampleEmitterShape(push.seed, spawnIdx);
    velocities[slot] = sampleInitVelocity(push.seed, spawnIdx);
    colors[slot]     = emitter.initColor;
    attribs[slot]    = vec4(sampleSize(push.seed), 0.0, 0.0, 0.0);
    meta[slot]       = uvec4(push.emitterID, push.seed ^ spawnIdx, 0, 0);
    atomicAdd(aliveCount.count, 1);
}
```

### Stage 3 — SIMULATE
```glsl
layout(local_size_x = 256) in;

// Reads: PositionBuffer, VelocityBuffer, ForceFieldBuffer, NoiseField3D
// Writes: PositionBuffer (updated pos + age), VelocityBuffer (updated vel)
// Push constant: deltaTime, frameIndex

void main() {
    uint idx = gl_GlobalInvocationID.x;
    if (idx >= push.maxParticles) return;
    if (positions[idx].w >= 1.0) return; // dead — skip

    vec3 pos = positions[idx].xyz;
    vec3 vel = velocities[idx].xyz;
    float mass = velocities[idx].w;

    // Apply forces
    for (int i = 0; i < push.forceCount; i++) {
        vel += evaluateForce(forces[i], pos, vel, mass, push.deltaTime);
    }

    // Sample curl noise field
    vec3 curlVel = texture(noiseField3D, pos * push.noiseScale).xyz;
    vel += curlVel * push.noiseStrength * push.deltaTime;

    // Integrate
    pos += vel * push.deltaTime;

    // Age
    float age = positions[idx].w + (push.deltaTime / lifetimes[idx]);

    positions[idx]  = vec4(pos, age);
    velocities[idx] = vec4(vel, mass);
}
```

### Stage 4 — SORT (conditional, transparent only)
```
Algorithm: GPU radix sort (32-bit float keys, 256-wide histogram)
Input:  PositionBuffer, push.cameraPos
Output: SortKeyBuffer (float distance), DrawIndexBuffer (sorted indices)
Passes: 4 radix passes × 8 bits = 32-bit sort
Skip condition: BlendMode == ADDITIVE — no sort needed
Implementation: standard 4-pass 8-bit LSD radix sort in compute
Each pass: histogram → prefix sum → scatter
Workgroup size: 256
```

### Stage 5 — CULL + COMPACT + INDIRECT WRITE
```glsl
layout(local_size_x = 256) in;

// Reads: PositionBuffer, AttribBuffer, SortKeyBuffer (if sorted)
// Writes: DrawIndexBuffer (compacted), IndirectCommandBuffer
// Push constant: frustumPlanes[6], maxParticles

shared uint localCount;
shared uint localIndices[256];

void main() {
    // Frustum sphere test per particle
    // Prefix sum compaction within workgroup
    // Atomic global compaction into DrawIndexBuffer
    // Last workgroup writes VkDrawIndirectCommand.instanceCount
}
```

---

## 4. Barrier Chain

```
vkCmdDispatch RETIRE
  → VK_ACCESS_SHADER_WRITE_BIT (FreeList, AliveCount)
  → VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT

memory barrier

vkCmdDispatch EMIT
  → VK_ACCESS_SHADER_WRITE_BIT (all 5 SoA, FreeList read head)
  → VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT

memory barrier

vkCmdDispatch SIMULATE
  → VK_ACCESS_SHADER_WRITE_BIT (PositionBuffer, VelocityBuffer)
  → VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT

memory barrier

[conditional] vkCmdDispatch SORT ×4 passes
  → memory barrier between each pass

memory barrier

vkCmdDispatch CULL+COMPACT+INDIRECT
  → VK_ACCESS_SHADER_WRITE_BIT (DrawIndexBuffer, IndirectCommandBuffer)
  → VK_PIPELINE_STAGE_COMPUTE_SHADER_BIT

execution barrier
  → VK_PIPELINE_STAGE_DRAW_INDIRECT_BIT (IndirectCommandBuffer read)
  → VK_PIPELINE_STAGE_VERTEX_SHADER_BIT (DrawIndexBuffer read)

vkCmdDrawIndirect
```

---

## 5. Renderer Pipeline Variants

Five pipelines, all sharing Set 0 and Set 1:

```
BILLBOARD
  Vertex:   gl_InstanceIndex → DrawIndexBuffer → particle slot
            reads pos/size/rotation from SoA
            generates screen-aligned quad in vertex shader (no geometry shader)
  Fragment: texture atlas sample, soft particle depth fade
  Topology: TRIANGLE_LIST (6 vertices per instance, generated in VS)

RIBBON
  Vertex:   reads ordered history buffer (ring buffer of positions)
  Fragment: UV along ribbon length, width falloff
  Requires: RibbonHistoryBuffer (separate SSBO, per-effect)

MESH
  Vertex:   instanced mesh draw, gl_InstanceIndex → particle transform
  Fragment: standard PBR-lite, inherits particle color as tint
  Requires: MeshHandle registered with DynamisGPU

BEAM
  Vertex:   two endpoints from BeamDescriptor, tessellated segments
  Fragment: animated UV scroll, additive blend
  Requires: BeamEndpointBuffer (vec4[2] per beam instance)

DECAL
  Vertex:   box projection volume
  Fragment: G-buffer read, normal-aligned projection
  Requires: Deferred pass integration point in DynamicLightEngine
```

---

## 6. DynamisGPU Integration Points

```
VulkanVfxEffectResources owns per-effect DynamisGPU handles:

  IndirectCommandBuffer indirectBuffer
    → allocated via DynamisGPU IndirectCommandBuffer.allocate()
    → slot registered in DynamicLightEngine's main indirect draw buffer
    → written by Stage 5 compute

  BindlessHeap slots:
    → particleAtlasSlot  — texture descriptor for particle atlas
    → depthBufferSlot    — depth texture for soft particles
    → noiseField3DSlot   — 3D curl noise texture descriptor

  DeviceBuffer allocations (via DynamisGPU VulkanMemoryOps):
    → 5× SoA particle buffers
    → FreeListBuffer
    → AliveCountBuffer
    → DrawIndexBuffer
    → SortKeyBuffer (conditionally allocated if renderer is transparent)
    → EmitterDescriptorBuffer
    → ForceFieldBuffer
    → IndirectCommandBuffer (VkDrawIndirectCommand)
```

---

## 7. PhysicsHandoff Async Readback Path

```
Problem: GPU knows when a particle meets debris criteria (age, velocity
         threshold, collision flag). CPU needs to spawn a rigid body.
         GPU→CPU readback must not stall the pipeline.

Solution: 3-frame ring of DebrisReadbackBuffers

Per frame:
  Stage 5 compute writes debris candidates to DebrisReadbackBuffer[frameIndex % 3]
  Writes candidate count to DebrisCountBuffer[frameIndex % 3]

CPU side (VfxVulkanService.simulate()):
  Read DebrisReadbackBuffer[(frameIndex - 2) % 3] — 2 frames latency
  For each debris candidate:
    Construct DebrisSpawnEvent from readback data
    Call physicsHandoff.onDebrisSpawn(event)

Buffer layout (per candidate):
  vec4 position_mass     (xyz=pos, w=mass)
  vec4 velocity          (xyz=vel, w=angularSpeed)
  uvec4 meta             (x=meshId, y=materialTag, z=emitterID, w=flags)

Max candidates per frame: 256 (configurable)
Buffer size: 256 × 48 bytes = 12KB per buffer × 3 = 36KB total
```

---

## 8. Hot-Reload Path

```
Trigger: EffectSerializer.fromJson() called with updated descriptor
         OR file watcher detects change to .vfx.json asset

Steps:
  1. Validate new descriptor via EffectValidator — reject if ERROR severity
  2. Compare with active descriptor — diff which fields changed
  3. If only ForceDescriptor changed:
       → re-upload EmitterDescriptorBuffer via DynamisGPU staging
       → no pipeline rebuild needed
  4. If RendererDescriptor changed (texture, blend mode):
       → wait for frame fence (current frame in flight completes)
       → rebuild affected pipeline variant
       → swap pipeline handle atomically
  5. If EmitterShapeDescriptor or maxParticles changed:
       → full effect respawn (despawn + spawn with new descriptor)
       → 1-frame gap acceptable

Hot-reload must never stall the render thread.
Pipeline rebuilds happen on a background thread with a swap fence.
```

---

## 9. VfxVulkanService Structure

```java
public final class VfxVulkanService implements VfxService {

    // Per-effect GPU resources, keyed by VfxHandle id
    private final Map<Integer, VulkanVfxEffectResources> effects;

    // Shared across all effects
    private final VulkanComputePipelineSet computePipelines;  // 5 stages
    private final VulkanRendererPipelineSet rendererPipelines; // 5 variants
    private final VulkanVfxDescriptorSetLayout setLayout;
    private final VfxBudgetAllocator budgetAllocator;
    private final PhysicsHandoff physicsHandoff;              // nullable

    // 3-frame ring for debris readback
    private final DebrisReadbackBuffer[] readbackRing;        // [3]

    @Override
    public void simulate(List<VfxHandle> active, float dt, VfxFrameContext ctx) {
        processDebrisReadback(ctx.frameIndex());
        for (VfxHandle h : active) {
            VulkanVfxEffectResources r = effects.get(h.id());
            dispatchRetire(r, ctx);
            dispatchEmit(r, ctx, dt);
            dispatchSimulate(r, ctx, dt);
            if (r.needsSort()) dispatchSort(r, ctx);
            dispatchCullCompactIndirect(r, ctx);
        }
    }

    @Override
    public void recordDraws(List<VfxHandle> active, VfxDrawContext ctx) {
        for (VfxHandle h : active) {
            VulkanVfxEffectResources r = effects.get(h.id());
            ctx.indirectBuffer().writeCommand(/* from r.indirectBuffer */);
        }
    }
}
```

---

## 10. Implementation Order for Codex

When implementing `dynamisvfx-vulkan`, strictly follow this order — each step compiles and passes the deterministic harness before the next begins:

```
Step 1  VulkanVfxEffectResources — SoA buffer allocation via DynamisGPU
Step 2  VulkanVfxDescriptorSetLayout — all 4 sets
Step 3  Stage 1 RETIRE compute pipeline + dispatch
Step 4  Stage 2 EMIT compute pipeline + dispatch
Step 5  Stage 3 SIMULATE compute pipeline + dispatch (gravity + drag only first)
Step 6  Stages 1-3 parity check against MockVfxService deterministic harness
Step 7  Stage 4 SORT (additive effects skip this — validate skip path first)
Step 8  Stage 5 CULL+COMPACT+INDIRECT — wire to DynamisGPU IndirectCommandBuffer
Step 9  BILLBOARD renderer pipeline — first visible particles
Step 10 RIBBON renderer pipeline
Step 11 MESH renderer pipeline
Step 12 BEAM renderer pipeline
Step 13 DECAL renderer pipeline
Step 14 PhysicsHandoff readback ring
Step 15 Hot-reload path
Step 16 VfxBudgetAllocator — global particle budget enforcement
Step 17 Noise field bake upload — wire NoiseFieldBaker output to 3D texture
Step 18 Full integration test against DynamicLightEngine
```

Each step gets its own commit. No step skips the compile + harness gate.
