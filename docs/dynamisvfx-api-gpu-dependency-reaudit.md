# DynamisVFX API -> DynamisGPU API Dependency Re-Audit

Date: 2026-03-10
Scope: `dynamisvfx-api` only

## Checkpoint A (after `eda6167`)

Result at that point:

- `dynamisvfx-api` still required `dynamis.gpu.api`
- remaining dependency points were:
  - `VfxDrawContext.indirectBuffer()`
  - `VfxDrawContext.bindlessHeap()`
  - `VfxIndirectCommandSink.from(IndirectCommandBuffer)`
  - `VfxDescriptorBindingWriter.from(DescriptorWriter)`

Recommendation at that point: Stage E2 should wait.

## Checkpoint B (after legacy-bridge extraction)

Follow-up tightening applied:

- removed `VfxIndirectCommandSink.from(IndirectCommandBuffer)`
- removed `VfxDescriptorBindingWriter.from(DescriptorWriter)`
- made `VfxDrawContext` typed-only (`VfxIndirectCommandSink` / `VfxDescriptorBindingWriter`)
- removed `requires dynamis.gpu.api` from `dynamisvfx-api/module-info.java`

### Re-audit result

Search over `dynamisvfx-api/src/main/java/org/dynamisvfx/api` and `module-info.java` shows no remaining references to:

- `org.dynamisgpu.api.*`
- `IndirectCommandBuffer`
- `DescriptorWriter`

## Architectural judgment

`dynamisvfx-api` no longer depends on `dynamis.gpu.api` at the stable exported API boundary.

The former JPMS blocker has been removed from the API surface.

## Stage E2 readiness recommendation

Recommendation: **Stage E2 can proceed**.

Constraint for Stage E2:

- keep VFX API typed-first and avoid reintroducing GPU execution types into `org.dynamisvfx.api`.
