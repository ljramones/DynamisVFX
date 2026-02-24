# DynamicLightEngine ↔ DynamisVFX Integration Design

## Status

VulkanVfxIntegration exists and passes the parity gate. The following
gaps remain before the integration is production-ready.

---

## 1. Current State Audit

### Working
- VulkanVfxIntegration created at engine startup
- simulate() called after shadow pass in VulkanFrameCommandOrchestrator
- recordDraws() called during main pass in VulkanMainPassRecorderCore
- DynamisGPU handles (indirect buffer, memory ops) passed through correctly
- Parity gate green: BindlessParityCheckTest, BackendParityIntegrationTest

### Connected in Name Only
- PhysicsHandoffAdapter logs debris events but does not call
  DynamisCollision.spawnRigidBody() — TODO comment in place
- VFX draw calls share the engine's main IndirectCommandBuffer with mesh
  draws — no slot range separation, ordering not enforced
- softParticles flag has no effect — depth buffer sampler binding
  at set=3 binding=4 is unresolved

### Genuinely Missing
- Depth buffer not exposed as readable sampler during VFX render pass
- G-buffer normal not exposed to DECAL renderer
- No particle atlas texture registration path into DLE bindless heap
- No particle-to-light cluster integration point
- Render pass ordering assumed but not enforced in command orchestrator

---

## 2. Integration Steps

### Step A — Indirect Buffer Separation

**Problem:** VFX draw calls share the engine's main IndirectCommandBuffer
with mesh draws. There is no slot range enforcement — VFX writes could
clobber mesh indirect commands under load.

**Solution:** Allocate a dedicated VfxIndirectCommandBuffer at engine
startup, separate from the main mesh indirect buffer. VulkanVfxIntegration
owns it. During recordDraws(), VFX commands go into the VFX buffer.
During the main pass, the engine records two vkCmdDrawIndirect calls:
one for meshes, one for VFX.

**Files:**
```
engine-impl-vulkan/.../vfx/VulkanVfxIndirectResources.java
  — allocates VFX-specific IndirectCommandBuffer via DynamisGPU
  — holds maxVfxDrawCalls slot count (default 4096)
  — owned by VulkanVfxIntegration, created at service init
```

**Change in VulkanVfxIntegration.create():**
```java
VulkanVfxIndirectResources vfxIndirect = VulkanVfxIndirectResources.create(
    device, memoryOps, MAX_VFX_DRAW_CALLS);
VulkanVfxService service = VulkanVfxService.create(
    device, renderPass,
    vfxIndirect.indirectBuffer(),   // VFX-only buffer
    bindlessHeap, memoryOps, ...);
```

**Change in VulkanMainPassRecorderCore:**
```
// After mesh draws:
vkCmdDrawIndirect(cmd, vfxIndirect.buffer(), 0, vfxDrawCount, stride);
```

---

### Step B — Depth Buffer Exposure for Soft Particles

**Problem:** The BILLBOARD fragment shader samples the depth buffer
at set=3 binding=4 for soft particle depth fade. This binding is
currently unresolved — the descriptor is written with a null/dummy
image, so softParticles has no effect.

**Solution:** After the main opaque pass completes and before VFX
recordDraws(), the engine must:
1. Transition the depth attachment from DEPTH_STENCIL_ATTACHMENT_OPTIMAL
   to SHADER_READ_ONLY_OPTIMAL
2. Bind the depth image view as a combined image sampler in VFX
   descriptor set 3, binding 4
3. After VFX draws complete, transition depth back to
   DEPTH_STENCIL_ATTACHMENT_OPTIMAL for any subsequent passes

**New class:**
```
engine-impl-vulkan/.../vfx/VulkanVfxDepthSamplerBridge.java

  static void transitionForVfxRead(long commandBuffer,
      long depthImage, VkImageLayout currentLayout)
  // Inserts image barrier: DEPTH_STENCIL_ATTACHMENT_OPTIMAL →
  //                         SHADER_READ_ONLY_OPTIMAL

  static void transitionAfterVfxRead(long commandBuffer,
      long depthImage)
  // Inserts image barrier back to DEPTH_STENCIL_ATTACHMENT_OPTIMAL

  static void writeDepthDescriptor(long device,
      VulkanVfxDescriptorSets descriptorSets,
      long depthImageView, long depthSampler,
      int frameIndex)
  // Updates set=3 binding=4 with the real depth image view
```

**Change in VulkanMainPassRecorderCore.record():**
```
// After opaque draws, before VFX draws:
VulkanVfxDepthSamplerBridge.transitionForVfxRead(cmd,
    backendResources.depthImage(), currentDepthLayout);
VulkanVfxDepthSamplerBridge.writeDepthDescriptor(device,
    vfxDescriptorSets, depthImageView, depthSampler, frameIndex);

vfxIntegration.recordDraws(activeHandles, drawCtx);

VulkanVfxDepthSamplerBridge.transitionAfterVfxRead(cmd,
    backendResources.depthImage());
```

---

### Step C — G-Buffer Normal Exposure for Decal Renderer

**Problem:** The DECAL fragment shader reads the G-buffer normal at
set=3 binding=6 to reject surfaces with oblique projection angles.
This binding is currently unresolved — decals render black projections.

**Solution:** DLE's deferred pass already writes a world-space normal
to a G-buffer attachment (VK_FORMAT_R16G16B16A16_SFLOAT). After the
geometry pass and before the VFX pass, bind this attachment as a
sampler in VFX descriptor set 3, binding=6.

**New class:**
```
engine-impl-vulkan/.../vfx/VulkanVfxGBufferBridge.java

  static void writeNormalDescriptor(long device,
      VulkanVfxDescriptorSets descriptorSets,
      long gBufferNormalImageView, long sampler,
      int frameIndex)
  // Updates set=3 binding=6 with the G-buffer normal image view
  // Called once per frame before VFX recordDraws()

  static void transitionNormalForVfxRead(long commandBuffer,
      long normalImage)
  // Barrier: COLOR_ATTACHMENT_OPTIMAL → SHADER_READ_ONLY_OPTIMAL

  static void transitionNormalAfterVfxRead(long commandBuffer,
      long normalImage)
  // Barrier back to COLOR_ATTACHMENT_OPTIMAL
```

**Note:** DECAL renderer is the only consumer. If no active effects
use RendererType.DECAL, skip the transition to avoid unnecessary
pipeline stalls. VulkanVfxIntegration.hasActiveDecals() provides
this check.

---

### Step D — Particle Texture Registration into Bindless Heap

**Problem:** RendererDescriptor holds a texture string ID (e.g.
"fx/fire_atlas.ktx2"). There is currently no path to resolve this
string ID to a VkImageView and register it in DLE's bindless heap
so the VFX shaders can sample it.

**Solution:** Add a VfxTextureRegistry that resolves string IDs to
bindless heap slots. The engine registers textures at asset load
time. VulkanVfxEffectResources looks up the slot at spawn time and
writes it into descriptor set 3, binding=3 (particleAtlas).

**New classes:**
```
engine-impl-vulkan/.../vfx/VulkanVfxTextureRegistry.java

  // Called at asset load time by the engine's texture loader
  void registerTexture(String id, long imageView, long sampler)

  // Called at VFX effect spawn time
  // Returns bindless heap slot index, or throws if not registered
  int resolveSlot(String textureId)

  // Returns a 1x1 white placeholder slot for effects
  // with unregistered textures — prevents null binding
  int fallbackSlot()
```

**Change in VulkanVfxEffectResources.allocate():**
```java
int atlasSlot = textureRegistry.resolveSlot(
    descriptor.renderer().textureId());
// Write to set=3 binding=3 via VkWriteDescriptorSet
descriptorSets.writeAtlasSlot(device, atlasSlot, frameIndex);
```

**Change in VulkanVfxIntegration.create():**
```java
VulkanVfxTextureRegistry textureRegistry =
    new VulkanVfxTextureRegistry(bindlessHeap);
// Register fallback 1x1 white texture
textureRegistry.registerFallback(device, memoryOps);
// Expose for engine asset loader to call registerTexture()
vfxIntegration.textureRegistry() → VulkanVfxTextureRegistry
```

---

### Step E — DynamisCollision Physics Handoff

**Problem:** VulkanVfxPhysicsHandoffAdapter logs debris events but
does not spawn rigid bodies. DynamisCollision 1.1.0 has the API —
it just needs to be wired.

**Solution:** VulkanVfxPhysicsHandoffAdapter receives the collision
system at construction. On onDebrisSpawn(), it calls
DynamisCollision.spawnRigidBody() with the event data.

**Change in VulkanVfxPhysicsHandoffAdapter:**
```java
public final class VulkanVfxPhysicsHandoffAdapter implements PhysicsHandoff {

    private final CollisionWorld collisionWorld;  // DynamisCollision 1.1.0
    private final VulkanSceneMeshLifecycle meshLifecycle;

    public VulkanVfxPhysicsHandoffAdapter(
        CollisionWorld collisionWorld,
        VulkanSceneMeshLifecycle meshLifecycle
    ) { ... }

    @Override
    public void onDebrisSpawn(DebrisSpawnEvent event) {
        // Resolve mesh shape from meshId
        CollisionShape shape = meshLifecycle.getCollisionShape(
            event.meshId());
        if (shape == null) {
            shape = CollisionShape.sphere(0.1f); // fallback
        }

        RigidBodyConfig config = RigidBodyConfig.builder()
            .shape(shape)
            .mass(event.mass())
            .worldTransform(event.worldTransform())
            .linearVelocity(event.velocity())
            .angularVelocity(event.angularVelocity())
            .materialTag(event.materialTag())
            .build();

        collisionWorld.spawnRigidBody(config);
    }
}
```

**Change in VulkanVfxIntegration.create():**
```java
vfxService.setPhysicsHandoff(
    new VulkanVfxPhysicsHandoffAdapter(
        ctx.collisionWorld(),       // DynamisCollision
        ctx.sceneMeshLifecycle()    // mesh shape resolver
    )
);
```

---

### Step F — Render Pass Ordering Enforcement

**Problem:** VFX draws are ordered after opaque mesh draws by
convention, but this is not enforced in VulkanFrameCommandOrchestrator.
If ordering changes during a refactor, transparent VFX will render
incorrectly (drawing behind opaque geometry or before the depth
buffer is populated).

**Solution:** Explicit ordering contract enforced by the orchestrator.
Add a VfxRenderPhase enum and assert phase preconditions.

**New enum:**
```java
public enum VfxRenderPhase {
    NOT_STARTED,
    COMPUTE_COMPLETE,    // after simulate(), before main pass
    OPAQUE_COMPLETE,     // after opaque mesh draws
    VFX_COMPLETE,        // after recordDraws()
    POST_PROCESS         // after VFX, before resolve
}
```

**Change in VulkanFrameCommandOrchestrator.recordFrame():**
```java
// Enforce ordering with assertions (stripped in production):
assert phase == NOT_STARTED;
vfxIntegration.simulate(...);
phase = COMPUTE_COMPLETE;

// ... shadow pass, opaque draws ...

assert phase == COMPUTE_COMPLETE;
// opaque draws complete
phase = OPAQUE_COMPLETE;

assert phase == OPAQUE_COMPLETE;
VulkanVfxDepthSamplerBridge.transitionForVfxRead(...);
vfxIntegration.recordDraws(...);
VulkanVfxDepthSamplerBridge.transitionAfterVfxRead(...);
phase = VFX_COMPLETE;

assert phase == VFX_COMPLETE;
// post process
phase = POST_PROCESS;
```

---

## 3. Implementation Order

```
Step A  — Indirect buffer separation          (no visual change, correctness fix)
Step B  — Depth buffer / soft particles       (visual: soft particle edges)
Step C  — G-buffer normal / decals            (visual: decal projection works)
Step D  — Texture registration                (visual: actual textures on particles)
Step E  — DynamisCollision handoff            (gameplay: debris spawns rigid bodies)
Step F  — Render pass ordering enforcement    (safety: prevents future regressions)
```

Each step compiles, passes the full parity gate, and commits before
the next begins.

---

## 4. Parity Gate Command

Run after each step:

```bash
cd ~/tripsnew/DynamisLightEngine
MVK_CONFIG_USE_METAL_ARGUMENT_BUFFERS=1 \
mvn -pl engine-host-sample test \
  -Ddle.bindless.parity.tests=true \
  -Dvk.validation=true
```

Must remain BUILD SUCCESS throughout.

---

## 5. New Tests Required

```
VfxDepthSamplerBridgeTest
  — assert depth image transitions to SHADER_READ_ONLY_OPTIMAL before VFX
  — assert transition back after VFX draws complete

VfxTextureRegistryTest
  — assert resolveSlot returns correct bindless heap index
  — assert fallbackSlot returns valid slot when id not registered
  — assert registerTexture overwrites previous registration for same id

VfxPhysicsHandoffIntegrationTest
  — assert onDebrisSpawn calls collisionWorld.spawnRigidBody exactly once
  — assert fallback sphere shape used when meshId not found
  — assert 2-frame delay preserved end-to-end

VfxRenderPhaseOrderingTest
  — assert AssertionError thrown if recordDraws called before simulate
  — assert AssertionError thrown if simulate called twice in same frame
```

---

## 6. Files Changed Summary

```
DynamisVFX (dynamisvfx-vulkan):
  No changes — integration lives entirely in DLE

DynamicLightEngine (engine-impl-vulkan):
  NEW  vfx/VulkanVfxIndirectResources.java
  NEW  vfx/VulkanVfxDepthSamplerBridge.java
  NEW  vfx/VulkanVfxGBufferBridge.java
  NEW  vfx/VulkanVfxTextureRegistry.java
  MOD  vfx/VulkanVfxPhysicsHandoffAdapter.java  (wire DynamisCollision)
  MOD  vfx/VulkanVfxIntegration.java            (wire all new components)
  MOD  VulkanMainPassRecorderCore.java           (depth/gbuffer transitions)
  MOD  VulkanFrameCommandOrchestrator.java       (phase ordering enforcement)
  NEW  VfxRenderPhase.java

DynamicLightEngine (engine-host-sample):
  NEW  VfxDepthSamplerBridgeTest.java
  NEW  VfxTextureRegistryTest.java
  NEW  VfxPhysicsHandoffIntegrationTest.java
  NEW  VfxRenderPhaseOrderingTest.java
```
