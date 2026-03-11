# DynamisVFX API → DynamisGPU API Dependency Audit

## Scope

Audit of why `dynamisvfx-api` currently requires `dynamis.gpu.api`, and whether that dependency is justified at the stable feature-facing API boundary.

This is review-only (no implementation changes).

## Current JPMS State

`dynamisvfx-api` currently declares:

```java
module org.dynamisvfx.api {
    requires dynamis.gpu.api;

    exports org.dynamisvfx.api;
}
```

## Exact Public Types Causing the Dependency

Direct references to `org.dynamisgpu.api` in `org.dynamisvfx.api` are limited to:

- `org.dynamisvfx.api.VfxDrawContext`
  - `IndirectCommandBuffer indirectBuffer();`
  - `DescriptorWriter bindlessHeap();`

No other public API type in `dynamisvfx-api` references `org.dynamisgpu.api` types.

## Dependency Point Classification

### 1) `VfxDrawContext.indirectBuffer()` (`IndirectCommandBuffer`)

Classification: **compatibility/transitional dependency**

Rationale:

- This method gives VFX draw recording access to a GPU-command abstraction.
- It is useful for current LightEngine/VFX integration and not a raw backend handle.
- However, it is execution-oriented coupling in a feature API surface and likely belongs behind a narrower feature-owned draw intent contract long-term.

### 2) `VfxDrawContext.bindlessHeap()` (`DescriptorWriter`)

Classification: **boundary leak** (with transitional usage)

Rationale:

- This exposes a GPU descriptor-writing capability directly at stable feature API level.
- It pulls low-level resource binding concerns into VFX API contracts rather than keeping them behind feature-owned data contracts or LightEngine/GPU execution boundaries.
- It is likely retained for integration convenience today, but architecturally it is the clearest stable-boundary leak.

## Architectural Fit Assessment

### Is `dynamisvfx-api -> dynamis.gpu.api` acceptable for now?

**Acceptable temporarily, with constraints.**

Constraints:

- No expansion of `org.dynamisgpu.api` type usage in `org.dynamisvfx.api`.
- New/updated API contracts should prefer feature-owned typed wrappers/contracts over additional GPU API exposure.
- `VfxDrawContext` should be treated as an integration bridge, not the long-term shape for feature-facing portability.

### Raw handles vs typed wrappers note

- Current API already moved frame command buffer toward a typed wrapper (`VfxCommandBufferRef`) and deprecated raw `long` command buffer access in `VfxFrameContext`.
- By contrast, `VfxDrawContext` still carries direct GPU API abstractions (`DescriptorWriter`, `IndirectCommandBuffer`). This is where tightening pressure remains.

## Stage 2 Readiness Recommendation

Recommendation: **keep temporarily, tighten before Stage 2**.

Reason:

- Stage E1 (API-only JPMS lock) is valid and safe.
- But before adding JPMS descriptors for `dynamisvfx-core`/`dynamisvfx-vulkan`, the `VfxDrawContext` GPU API coupling should be reviewed for narrowing to reduce stable-boundary leakage.

## Suggested Follow-up Audit-to-Implementation Direction (deferred)

When tightening is scheduled:

- Replace or wrap `DescriptorWriter` exposure behind a VFX-owned typed binding context.
- Evaluate whether `IndirectCommandBuffer` can be represented as a VFX-owned draw command sink abstraction.
- Keep compatibility bridges during transition; avoid broad behavior changes.

