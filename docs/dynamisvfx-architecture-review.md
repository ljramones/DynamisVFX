# DynamisVFX Architecture Review

## Repo Overview

- Repository: `DynamisVFX`
- Modules:
  - `dynamisvfx-api`
  - `dynamisvfx-core`
  - `dynamisvfx-vulkan`
  - `dynamisvfx-test`
  - `dynamisvfx-bench`
- Build/runtime profile:
  - Java 21 target, Maven multi-module
  - Vulkan backend module depends on both `dynamis-gpu-api` and `dynamis-gpu-vulkan`

Grounded structure read:

- `dynamisvfx-api` defines effect descriptors, service interfaces, frame/draw contexts, and physics handoff contracts.
- `dynamisvfx-core` provides builder/serialization/validation/lod/noise prep utilities.
- `dynamisvfx-vulkan` implements compute/renderer/budget/hot-reload/debris-readback systems.
- Integration docs (`docs/dle-dynamisvfx-integration-design.md`) place LightEngine as pass orchestrator and DynamisVFX as simulation/render-feature participant.

## Strict Ownership Statement

### What DynamisVFX should own

DynamisVFX should own **visual-effects feature authority**:

- Effect descriptors and runtime effect-instance lifecycle (`spawn`, `despawn`, `updateTransform`).
- Effect-local simulation and scheduling semantics (particle/effect update logic local to VFX).
- Effect-local render data generation (instance lists/sort keys/indirect intent) for downstream rendering.
- Feature-level policies like VFX budget allocation and VFX-specific hot-reload classification.
- Consumption of external events/data (world/physics/collision) for effect playback, without taking authority over those systems.

### What DynamisVFX must not own

DynamisVFX must **not** own:

- Global render-planning or frame-graph authority (LightEngine concern).
- Generic GPU resource/orchestration authority (DynamisGPU concern).
- World authority/tick orchestration (WorldEngine concern).
- Physics/collision simulation authority (Physics/Collision concerns).
- Session/content authority (Session/Content concerns).
- Gameplay/scripting control-plane authority.

## Dependency Rules

### Allowed dependencies for DynamisVFX

- `DynamisGPU` as execution substrate dependency.
- `DynamisLightEngine` integration through host-facing contracts and pass orchestration hooks.
- `DynamisPhysics`/`DynamisCollision` via consumption boundaries (for example `PhysicsHandoff`) rather than simulation ownership.
- `DynamisContent` via asset/effect descriptor consumption boundaries.

### Forbidden dependencies for DynamisVFX

- Dependence on WorldEngine internals for world-state authority.
- Dependence on Physics/Collision internals to drive simulation policy.
- Embedding render graph ownership inside VFX runtime.
- Owning generic GPU allocator/scheduler policy that belongs in DynamisGPU.

### Who may depend on DynamisVFX

- LightEngine or host runtime orchestration layer for render integration.
- World/feature systems that spawn/update/despawn effects.
- Physics/collision consumers only through handoff events and readbacks.

## Public vs Internal Boundary Assessment

### Canonical public boundary

Primary public seam should be:

- `dynamisvfx-api` descriptors + `VfxService` + handoff contracts.
- `dynamisvfx-core` authoring/validation/serialization utilities.

This shape is largely present.

### Boundary concerns in current public surface

1. `dynamisvfx-api` contains low-level GPU-facing details in otherwise engine-facing contracts:
   - `VfxFrameContext.commandBuffer()` exposes raw backend command buffer handle shape (`long`).
   - `VfxDrawContext` directly references `IndirectCommandBuffer` and `DescriptorWriter` from DynamisGPU.

   This is workable for now but broadens API coupling and risks freezing backend assumptions into feature API contracts.

2. `dynamisvfx-vulkan` consumes `org.dynamisgpu.vulkan.memory.VulkanMemoryOps` directly in service/stage signatures.

   This is a concrete dependency on backend internals rather than a narrower execution seam. It is the strongest current coupling risk.

### Internal/implementation areas (appropriate)

The following are appropriate as internal backend implementation details in VFX Vulkan module:

- compute stage objects
- renderer variant pipelines
- descriptor/pipeline helper utilities
- debris readback ring implementation details
- benchmark harnesses and parity tests

## Policy Leakage / Overlap Findings

### DynamisLightEngine overlap

- Intended boundary is mostly clean in docs: LightEngine orchestrates pass ordering and VFX records draws.
- Risk remains where integration assumptions live in VFX docs/code comments (ordering/depth/G-buffer access rules) and can drift into implicit render-policy ownership if not kept host-controlled.

### DynamisGPU overlap (primary risk)

- VFX Vulkan module manages substantial Vulkan resource/pipeline details and imports `dynamis-gpu-vulkan` internals (`VulkanMemoryOps`).
- This is the main overlap candidate with DynamisGPU execution/orchestration ownership.
- Current state is functional but should be treated as constrained coupling, not a model to broaden.

### DynamisWorldEngine overlap

- No strong evidence of world authority ownership in repo code.
- VFX appears to consume runtime contexts rather than own global world lifecycle.

### DynamisPhysics / DynamisCollision overlap

- `PhysicsHandoff` and `DebrisSpawnEvent` represent a clean consumption seam in principle: VFX emits candidates/events; external systems decide simulation authority.
- This is consistent with the desired Physics↔Collision seam and should remain one-way.

### DynamisContent overlap

- Descriptor/content usage appears consumption-oriented, not content authority ownership.
- No significant runtime content-registry authority detected in VFX modules.

## Ratification Result

**Result: ratified with constraints**

Why:

- VFX feature ownership is broadly coherent: descriptors, effect lifecycle, effect-local simulation, render-data generation, and external handoff seams are all present.
- Major architectural risk is backend coupling breadth, not role confusion:
  - API layer exposes low-level GPU handle shapes.
  - Vulkan module depends on DynamisGPU Vulkan internals.
- No strong evidence that VFX is attempting world/session/physics authority ownership.

## Recommended Next Step

1. Keep current seams stable for now (no immediate broad refactor).
2. In a later targeted tightening pass, evaluate whether `dynamisvfx-api` should narrow raw backend-handle exposure.
3. In that same later pass, evaluate whether VFX Vulkan can consume a narrower DynamisGPU seam instead of direct `VulkanMemoryOps` dependency.
4. Proceed next with **DynamisSky** (or `DynamisTerrain`) to continue graphics-feature boundary ratification around LightEngine without prematurely refactoring VFX/GPU seams.

This review is a boundary-ratification document only and does not propose immediate package moves or API-breaking changes.
