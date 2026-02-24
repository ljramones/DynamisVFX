# DynamisVFX Wishlist — The "Holy Shit" List

---

## Particle Simulation Core
- GPU compute SoA particle simulation — 10M+ particles at 60fps
- Multi-stage compute pipeline (emit → simulate → sort → render) in single frame
- Persistent particle state across frames with GPU-side lifetime management
- Deterministic simulation replay (same seed = same result, always)
- Sub-frame interpolation for high-velocity particles
- Particle LOD — full sim near camera, reduced sim at distance, impostors beyond
- Particle sleeping — zero GPU cost for inactive emitters
- Signed distance field collision — particles collide with arbitrary scene geometry
- Depth buffer collision — screen-space particle/world intersection without SDF
- Particle–particle interaction (attraction, repulsion, vortex fields)
- GPU radix sort for painter's-algorithm transparency correctness
- Parallel prefix sum for compaction — zero gaps in live particle arrays

---

## Emitter System
- Point, sphere, cone, box, hemisphere, torus emitter primitives
- Mesh-surface emitters — emit from any loaded mesh surface with weighted area sampling
- Spline emitters — particles follow and spawn along Animis spline paths
- Skeletal emitters — attach emitter to any bone in a skinned mesh via Animis
- Volume emitters — fill arbitrary convex hull with particles
- Burst + continuous + on-event emission modes
- Emitter inheritance — child emitters spawned from parent particle death
- GPU-driven emitter culling — zero cost for off-screen emitters
- Emitter LOD distance scaling — automatically reduce rate at distance
- Emitter pooling — pre-allocated emitter budget, no runtime allocation

---

## Renderer Variants
- Camera-facing billboards (spherical + cylindrical)
- Velocity-stretched billboards — motion blur baked into particle shape
- Ribbon/trail renderer — smooth spline through particle history, variable width
- Mesh particle renderer — full instanced mesh per particle (debris, shells, leaves)
- Beam renderer — lightning, laser, energy arcs between two points
- Sprite sheet animation — UV-animated particles with frame blending
- Soft particles — depth-fade at intersection with scene geometry
- Distortion particles — screen-space refraction/heat haze volumes
- Light-emitting particles — each particle contributes to local illumination budget
- Volumetric particle rendering — density-accumulated fog/smoke/explosion volumes
- Decal particles — particles that stamp projected decals on world geometry on impact

---

## VFX Graph (Node-Based Effect Authoring)
- Data-driven effect descriptor — full effect defined in JSON/binary, zero code
- Node types: emitter, force, collision, renderer, event, condition, arithmetic
- Event graph — particle death triggers secondary emitter, chain reactions
- GPU-evaluated parameter curves — time/velocity/age-based value curves on GPU
- Effect composition — layer multiple sub-effects into one coherent VFX asset
- Hot-reload — live effect parameter update without restart
- Effect LOD graph — different node graphs at different view distances
- Scriptable forces — user-defined compute shader force nodes injected at runtime

---

## Forces and Fields
- Gravity, drag, turbulence, vortex, attractor, repulsor — all GPU-evaluated
- Vector field forces — 3D texture-sampled directional force volumes
- Wind zones — directional + gusty, interacts with particle mass/drag properties
- Explosion force — radial impulse with falloff curve
- Magnetic field simulation — charge-based particle deflection
- Fluid velocity field coupling — particles advected by fluid sim output (future)
- Noise-driven turbulence — FastNoiseLiteNouveau integration for coherent field noise
- FastNoiseLiteNouveau integration — curl noise, 4D FBm, node graph, domain warp,
  turbulence fields, and wavelet noise available for all force/field evaluations
  via com.cognitivedynamics:fastnoiselitenouveau:1.1.1

---

## Collision and Physics
- DynamisCollision integration — particle bounce, stick, slide on physics world
- Per-particle material response — restitution + friction per collision surface tag
- Particle spawns on collision — impact sparks, dust puffs, blood splatter
- Debris system — rigid body fragments handed off to DynamisCollision on spawn
- Particle trails leave decals — footprints, scorch marks, blood pools on impact
- Continuous collision detection for high-velocity particles

---

## Decal System
- Projected decals — box/frustum projection onto arbitrary world geometry
- Deferred decals — rendered in deferred pass, respect G-buffer normals
- Mesh decals — conformed decal geometry baked to surface (wounds, damage)
- Decal atlas management — automatic atlas packing, mip generation
- Decal lifetime + fade — age-based opacity, pooled GPU lifetime buffer
- Decal clustering — spatial hash for O(1) decal lookup per fragment
- Layered decal blending — up to 8 decal layers per surface with blend modes

---

## Destruction and Debris
- Voronoi fracture pre-computation — mesh shattered into N fragments at asset time
- Runtime fracture triggering — any fragment activates on impact above threshold
- GPU-driven fragment culling — only simulate visible/nearby fragments
- Fragment material inheritance — each shard carries parent material properties
- Procedural debris — generate plausible fragment shapes without pre-computation
- Structural integrity simulation — connected-component graph, propagating fracture
- Debris pooling — finite fragment budget, oldest recycled first

---

## Lens Effects
- Physically-based lens flares — aperture shape, ghost positions from optics model
- Anamorphic lens streaks — horizontal light streaks on bright sources
- Light shafts / god rays — screen-space ray marching from occluded light sources
- Dirt mask — camera lens dirt overlay modulated by bloom intensity
- Chromatic aberration on impact — transient fringe on screen hit events
- Camera shake integration — engine camera event triggers VFX camera response

---

## Environmental VFX
- Rain system — GPU-simulated streaks + surface ripple spawn on impact
- Snow system — wind-responsive flakes + accumulation decal stamp
- Dust / sand — wind-driven ground-level particle sheets
- Smoke volumes — GPU voxel sim with buoyancy, cooling, diffusion
- Fire simulation — GPU-evolved temperature field driving particle color/opacity
- Waterfall + mist — sustained emitter with SDF collision + spray spawn
- Footstep FX — surface-tag-driven automatic dust/splash/snow crunch on step

---

## Performance and Tooling
- Per-effect GPU timing — exact compute + render cost per active effect
- VFX budget system — total particle budget enforced globally, effects compete
- Occlusion-aware LOD — GPU occlusion query drives effect quality tier
- Effect profiler output — CSV export of per-effect frame costs over N frames
- Visual parity test harness — deterministic frame capture for regression testing
- Effect asset validator — catches broken curves, missing textures, over-budget emitters at load time
- RenderDoc marker injection — every effect pass named and bracketed for capture

---

## Dependencies

| Library | Version | Role |
|---|---|---|
| dynamis-gpu-api | 1.0.1 | GPU staging, indirect buffers, compute dispatch |
| dynamis-gpu-vulkan | 1.0.1 | Vulkan implementations |
| vectrix | 1.10.9 | Math — positions, velocities, splines |
| fastnoiselitenouveau | 1.1.1 | Noise — turbulence, curl, 4D FBm, vector fields, wind |
| animis-runtime | 1.0.0 | (optional) Spline/skeletal emitter paths |
| collision_detection | 1.1.0 | (optional) Particle/debris physics handoff |
