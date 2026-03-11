# DynamisVFX JPMS Boundary Plan

## Scope

This is a planning-only slice for JPMS/package boundary tightening in `DynamisVFX`.

Goal: define the minimal stable public surface before any `module-info.java` implementation, so we do not freeze backend/internal seams too early.

## Grounded Current State

- Repository is multi-module (`dynamisvfx-api`, `dynamisvfx-core`, `dynamisvfx-vulkan`, `dynamisvfx-test`, `dynamisvfx-bench`) and currently has no JPMS descriptors.
- `DynamisLightEngine` currently imports both VFX API and Vulkan implementation classes (for example `org.dynamisvfx.vulkan.VulkanVfxService`, `org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout`).
- Vulkan package space contains substantial backend details (`compute`, `descriptor`, `renderer`, `resources`, `internal.gpu`, `shader`, `hotreload`, etc.).

Implication: we should first lock an explicit minimal public boundary model, then introduce JPMS in a compatibility-preserving sequence.

## 1) Intended Public VFX Packages

### Stable public feature API (intended external consumption)

- `org.dynamisvfx.api`

This package currently carries feature-facing descriptors and service/runtime contracts (effect descriptors, frame/draw context contracts, service surface, physics handoff contracts).

### Provisional public API split (defer until we actually split package layout)

The following package split is desirable long-term but is not implemented yet:

- `org.dynamisvfx.api.effect`
- `org.dynamisvfx.api.runtime`
- optional `org.dynamisvfx.api.spi` (only if real cross-repo SPI use appears)

For now, the safe boundary is to treat `org.dynamisvfx.api` as the only stable public package.

## 2) Intended Internal / Backend-only Packages

These are implementation/internal and should not be exported as stable public JPMS surface:

- `org.dynamisvfx.core.*`
- `org.dynamisvfx.vulkan.*`
- `org.dynamisvfx.vulkan.internal.*`
- backend-support packages under Vulkan module (`compute`, `descriptor`, `renderer`, `resources`, `shader`, `hotreload`, `physics`, `noise`, `indirect`, `budget`, etc.)

Policy intent:

- Feature consumers should use `dynamisvfx-api` contracts.
- LightEngine should not depend on VFX Vulkan internals as a stable boundary.
- Vulkan/backend classes remain implementation details behind integration seams.

## 3) Safe Initial `module-info.java` Design

Introduce JPMS in two safe stages.

### Stage 1 (safe first lock): API module only

Add descriptor only for `dynamisvfx-api` first:

```java
module org.dynamisvfx.api {
    requires dynamis.gpu.api;

    exports org.dynamisvfx.api;
}
```

Why this first:

- minimal risk
- locks only obvious feature-facing public surface
- avoids prematurely freezing `core`/`vulkan` package shape

### Stage 2 (after LightEngine seam cleanup for VFX)

Add descriptors for `core` and `vulkan` with minimal exports.

Candidate shape:

```java
module org.dynamisvfx.core {
    requires org.dynamisvfx.api;
    requires org.vectrix;
    requires com.cognitivedynamics.fastnoiselitenouveau;
    requires com.fasterxml.jackson.databind;

    exports org.dynamisvfx.core;
}
```

```java
module org.dynamisvfx.vulkan {
    requires org.dynamisvfx.api;
    requires org.dynamisvfx.core;
    requires dynamis.gpu.api;
    requires dynamis.gpu.vulkan;
    requires org.lwjgl;
    requires org.lwjgl.vulkan;

    exports org.dynamisvfx.vulkan;
}
```

Notes:

- Do not export subpackages in `vulkan` during first JPMS implementation pass.
- If temporary compatibility requires additional exports, prefer qualified exports to known consumer modules and document them explicitly.

## 4) Explicit Deferrals (must not be locked yet)

Do not lock these in the JPMS planning/first implementation pass:

- GPU execution API shapes between VFX and DynamisGPU
- final LightEngine ↔ VFX integration contract shape
- render-graph participation semantics / global phase-policy contracts
- broad API package reorganization (`api.effect`, `api.runtime`, `api.spi` split)
- backend export-hardening for all Vulkan subpackages in one shot

Reason: these seams are still being tightened incrementally; early hard-locking would freeze transitional shapes.

## Minimal Stable Surface Answer

Smallest stable public VFX surface today:

- `org.dynamisvfx.api` only

Everything else should be treated as internal/backend implementation until VFX integration cleanup reaches parity with Terrain/Sky seam maturity.

## Recommended Next Implementation Slice

JPMS implementation (narrow, non-breaking):

1. Add `module-info.java` for `dynamisvfx-api` only.
2. Keep behavior unchanged.
3. Do not introduce new public Vulkan exports.
4. Validate `DynamisLightEngine` compatibility paths remain functional.

After that, reassess `core`/`vulkan` JPMS descriptors in a separate slice.
