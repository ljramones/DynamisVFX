# DynamisVFX API -> DynamisGPU API Dependency Re-Audit

Date: 2026-03-10
Reference change: `eda6167`
Scope: `dynamisvfx-api` only

## Summary

`dynamisvfx-api` **still requires** `dynamis.gpu.api`.

The dependency is no longer broad, but it is still present in the stable API package through:

- deprecated compatibility methods on `VfxDrawContext`
- public static adapter factories on new typed VFX interfaces

Current module descriptor:

- `dynamisvfx-api/src/main/java/module-info.java:2`
  - `requires dynamis.gpu.api;`

## Exact Remaining Public Dependency Points

### 1) `VfxDrawContext` legacy compatibility methods

File: `dynamisvfx-api/src/main/java/org/dynamisvfx/api/VfxDrawContext.java`

- `IndirectCommandBuffer indirectBuffer();` (`:25`)
- `DescriptorWriter bindlessHeap();` (`:31`)

Classification:

- `indirectBuffer()` -> **compatibility/transitional dependency**
- `bindlessHeap()` -> **compatibility/transitional dependency** (historically a leak; now reduced by typed replacement and deprecation)

Reasoning:

- both are deprecated and retained for compatibility
- both still place `dynamis.gpu.api` types directly in public VFX API signatures

### 2) `VfxIndirectCommandSink` static GPU adapter factory

File: `dynamisvfx-api/src/main/java/org/dynamisvfx/api/VfxIndirectCommandSink.java`

- `static VfxIndirectCommandSink from(IndirectCommandBuffer buffer)` (`:21`)

Classification:

- **compatibility/transitional dependency**

Reasoning:

- this is an adapter bridge from legacy GPU type to typed VFX surface
- useful in transition, but still requires GPU API type in exported package

### 3) `VfxDescriptorBindingWriter` static GPU adapter factory

File: `dynamisvfx-api/src/main/java/org/dynamisvfx/api/VfxDescriptorBindingWriter.java`

- `static VfxDescriptorBindingWriter from(DescriptorWriter writer)` (`:15`)

Classification:

- **boundary leak** (via public stable API)

Reasoning:

- this keeps a descriptor-writer execution concept visible at VFX API layer
- while typed methods are VFX-owned, the public factory still anchors API-level coupling to GPU execution API

## Architectural Judgment

- The dependency is now narrower than before `eda6167`, but not eliminated.
- `dynamisvfx-api -> dynamis.gpu.api` is still **transitional**, not a clean long-term stable boundary.
- The strongest remaining leak is the public `DescriptorWriter` adapter factory.

## Stage E2 Readiness Recommendation

Recommendation: **Stage E2 should wait for one more tightening slice.**

Rationale:

- Stage E2 (additional JPMS boundary locking for VFX core/vulkan) should not proceed while `dynamisvfx-api` still exposes GPU API types in exported public signatures.
- One additional narrow API slice should first move/contain remaining GPU-typed compatibility bridges so the API boundary is stable enough to lock further.

## Minimal Next Tightening Target (for follow-up slice)

Keep this narrow and compatibility-safe:

1. Keep deprecated legacy methods temporarily, but avoid introducing new API usage.
2. Relocate or contain public static GPU adapter factories so they do not remain primary stable API entry points.
3. Re-audit module dependency; if no exported signatures require GPU API types, remove `requires dynamis.gpu.api` from `dynamisvfx-api`.
