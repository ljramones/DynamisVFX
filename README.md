# DynamisVFX

DynamisVFX is a multi-module Java library for real-time VFX systems focused on GPU particle simulation, emitter pipelines, and Vulkan-backed rendering integration.

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

- Wishlist and planned feature track: `wish_list.md`
