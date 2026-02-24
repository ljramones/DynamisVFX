# DynamisVFX

DynamisVFX is a multi-module Java library for real-time VFX focused on GPU particle simulation, emitter pipelines, and Vulkan-backed rendering integration.

## Vision

Build a high-performance, data-driven VFX stack that supports large-scale particle effects, deterministic simulation workflows, and modular renderer backends.

## Scope

### In

- GPU compute particle simulation (SoA state, compute dispatch, lifecycle management)
- Emitter families (point, sphere, cone, mesh-surface, spline)
- Renderer variants (billboard, stretched, ribbon/trail, mesh particles)
- VFX graph/effect descriptors (node-based or data-driven)
- Decals, destruction/debris, impact effects, environmental effects
- Lens flares and light shafts (screen-space)

### Out

- Volumetric fog (owned by DynamicLightEngine)
- Post-process effects (owned by render graph)
- Macro weather orchestration
- Water surface systems (separate candidate library)

## Roadmap Highlights

- Particle simulation core: multi-stage compute (`emit -> simulate -> sort -> render`), deterministic replay, LOD and sleeping
- Emitters: burst/continuous/on-event modes, inheritance chains, GPU-driven culling
- Renderers: soft particles, distortion, volumetric smoke/fire, decal-on-impact particles
- VFX graph: hot reload, event chaining, GPU parameter curves, effect composition
- Forces/fields: turbulence, vortex, vector fields, wind zones, explosion/magnetic forces
- Noise stack: FastNoiseLiteNouveau for curl noise, 4D FBm, domain warp, and turbulence fields
- Physics and decals: collision responses, debris handoff hooks, projected/deferred/mesh decals
- Tooling: per-effect GPU timing, budget enforcement, regression capture, RenderDoc markers

## Modules

- `dynamisvfx-api`: effect descriptors and contracts (pure Java)
- `dynamisvfx-core`: simulation logic, buffers, emitter implementations
- `dynamisvfx-vulkan`: Vulkan-specific GPU integration
- `dynamisvfx-test`: deterministic test utilities and mock-oriented support
- `dynamisvfx-bench`: JMH benchmarks for throughput and performance tuning

## Requirements

- JDK 21+ (current repo toolchain runs on JDK 25)
- Maven 3.9+

## Build

```bash
mvn test
```

Compile only:

```bash
mvn -DskipTests compile
```

## Key Dependencies

- `org.vectrix:vectrix:1.10.9`
- `com.cognitivedynamics:fastnoiselitenouveau:1.1.1`
- `org.dynamisgpu:dynamis-gpu-api:1.0.1`
- `org.dynamisgpu:dynamis-gpu-vulkan:1.0.1`
- LWJGL 3.4.1 (BOM-managed)

## Documentation

- Full wishlist and feature backlog: `wish_list.md`
