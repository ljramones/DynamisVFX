# DynamisVFX

DynamisVFX is a multi-module Java VFX library focused on GPU-resident particle simulation, Vulkan execution, deterministic validation, and engine integration.

## Current Status

DynamisVFX is implemented and integrated.

- `dynamisvfx-api` complete: descriptor contracts, frame/draw contexts, handle lifecycle, physics handoff surface.
- `dynamisvfx-core` complete: fluent builders, JSON/binary serialization, validation, LOD policy, noise baking utilities.
- `dynamisvfx-test` complete: mock VFX service, deterministic simulation harness, assertion utilities.
- `dynamisvfx-vulkan` complete: compute pipeline, renderer variants, hot-reload, budget allocator, debris readback, noise-field upload.
- `dynamisvfx-bench` complete: JMH throughput and scheduling benchmarks.

DynamicLightEngine integration is complete and parity gates are green.

## Scope

### In

- GPU compute particle simulation (SoA state, dispatch lifecycle, retire/emit/simulate/sort/cull)
- Emitters: point, sphere, cone, mesh-surface, spline
- Renderers: billboard, ribbon, mesh, beam, decal
- Data-driven effect descriptors and fluent descriptor builders
- Debris/destruction handoff via physics callback interface
- Environmental/impact/lens-oriented VFX primitives

### Out

- Volumetric fog (owned by DynamicLightEngine)
- Post-process stack (owned by render graph)
- Macro weather orchestration
- Water systems (separate library candidate)

## Vulkan Implementation Snapshot

### Compute Stages

- Stage 1: `RETIRE`
- Stage 2: `EMIT`
- Stage 3: `SIMULATE`
- Stage 4: `SORT` (GPU radix sort, transparent path)
- Stage 5: `CULL + COMPACT + INDIRECT WRITE`

### Renderer Variants

- `BILLBOARD`
- `RIBBON`
- `MESH`
- `BEAM`
- `DECAL`

### Runtime Systems

- 3-frame async debris readback ring for `PhysicsHandoff`
- Hot-reload categorization (`FORCES_ONLY`, `RENDERER_CHANGED`, `FULL_RESPAWN`)
- Background pipeline swap path
- Global particle budget allocator (`REJECT`, `CLAMP`, `EVICT_OLDEST`)
- Curl-noise 3D field baking/upload via FastNoiseLiteNouveau

## Module Layout

```text
dynamisvfx-parent
  dynamisvfx-api
  dynamisvfx-core
  dynamisvfx-vulkan
  dynamisvfx-test
  dynamisvfx-bench
```

## Build and Verification

### Full compile

```bash
mvn -DskipTests compile
```

### Full tests

```bash
mvn test
```

### Bench module compile/package

```bash
mvn -pl dynamisvfx-bench -am -DskipTests compile
mvn -pl dynamisvfx-bench package -DskipTests
```

### JMH smoke run

```bash
java -jar dynamisvfx-bench/target/dynamisvfx-bench.jar -wi 1 -i 1 -f 1 -t 1
```

If your environment restricts forked process networking, run JMH with `-f 0`.

## Key Dependencies

- `org.vectrix:vectrix:1.10.9`
- `com.cognitivedynamics:fastnoiselitenouveau:1.1.1`
- `org.dynamisgpu:dynamis-gpu-api:1.0.1`
- `org.dynamisgpu:dynamis-gpu-vulkan:1.0.1`
- `org.dynamisgpu:dynamis-gpu-test:1.0.1`
- LWJGL `3.4.1` (BOM-managed)
- JMH `1.37`

## Java and Tooling

- Maven 3.9+
- Project compiles with `maven.compiler.release=21`
- Validated in this environment with JDK 25

## Docs

- Wishlist and backlog: `wish_list.md`
- Vulkan design spec: `docs/dynamisvfx-vulkan-design.md`
