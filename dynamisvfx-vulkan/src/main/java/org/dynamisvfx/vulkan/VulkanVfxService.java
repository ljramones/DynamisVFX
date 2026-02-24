package org.dynamisvfx.vulkan;

import org.dynamisgpu.api.gpu.IndirectCommandBuffer;
import org.dynamisgpu.vulkan.memory.VulkanMemoryOps;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.PhysicsHandoff;
import org.dynamisvfx.api.VfxDrawContext;
import org.dynamisvfx.api.VfxFrameContext;
import org.dynamisvfx.api.VfxHandle;
import org.dynamisvfx.api.VfxService;
import org.dynamisvfx.api.VfxStats;
import org.dynamisvfx.vulkan.compute.VulkanVfxCullCompactStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxEmitStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxRetireStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxSimulateStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxSortStage;
import org.dynamisvfx.vulkan.compute.VulkanVfxSpawnScheduler;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSets;
import org.dynamisvfx.vulkan.indirect.VulkanVfxIndirectWriter;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public final class VulkanVfxService implements VfxService {
    private static final float[] DEFAULT_TRANSFORM = new float[16];

    private final long device;
    private final VulkanMemoryOps memoryOps;
    private final VulkanVfxRetireStage retireStage;
    private final VulkanVfxEmitStage emitStage;
    private final VulkanVfxSimulateStage simulateStage;
    private final VulkanVfxSortStage sortStage;
    private final VulkanVfxCullCompactStage cullCompactStage;
    private final VulkanVfxSpawnScheduler spawnScheduler;

    private final AtomicInteger nextHandleId = new AtomicInteger(1);
    private final Map<Integer, EffectState> effects = new HashMap<>();
    private final Map<Integer, Integer> generationById = new HashMap<>();

    private PhysicsHandoff physicsHandoff;

    public VulkanVfxService(long device, VulkanMemoryOps memoryOps, VulkanVfxDescriptorSetLayout layout) {
        this.device = device;
        this.memoryOps = memoryOps;
        this.retireStage = VulkanVfxRetireStage.create(device, layout);
        this.emitStage = VulkanVfxEmitStage.create(device, layout);
        this.simulateStage = VulkanVfxSimulateStage.create(device, layout);
        this.sortStage = VulkanVfxSortStage.create(device, layout, 1_048_576);
        this.cullCompactStage = VulkanVfxCullCompactStage.create(device, layout);
        this.spawnScheduler = new VulkanVfxSpawnScheduler();
    }

    @Override
    public void simulate(List<VfxHandle> activeEffects, float deltaTime, VfxFrameContext ctx) {
        Objects.requireNonNull(activeEffects, "activeEffects");
        Objects.requireNonNull(ctx, "ctx");

        long commandBuffer = ctx.commandBuffer();
        long set0 = 1L; // Per-frame shared set placeholder until frame-set allocator is wired.
        int frameIndex = (int) ctx.frameIndex();
        float[] frustum = normalizeFrustum(ctx.frustumPlanes());
        float[] cameraPos = extractCameraPos(ctx.cameraView());

        for (VfxHandle handle : activeEffects) {
            EffectState state = effects.get(handle.id());
            if (state == null || state.resources == null || state.descriptorSets == null) {
                continue;
            }

            VulkanVfxEffectResources resources = state.resources;
            VulkanVfxDescriptorSets sets = state.descriptorSets;

            VulkanVfxIndirectWriter.resetIndirectCommand(commandBuffer, resources);
            retireStage.dispatch(commandBuffer, resources, sets, set0, frameIndex);
            VulkanVfxRetireStage.insertPostRetireBarrier(commandBuffer);

            int freeSlots = Math.max(0, resources.config().maxParticles() - state.aliveCount);
            int spawnCount = spawnScheduler.computeSpawnCount(state.descriptor.rate(), deltaTime, freeSlots);
            emitStage.dispatch(commandBuffer, resources, sets, set0, frameIndex, spawnCount, state.seed, handle.id());
            VulkanVfxEmitStage.insertPostEmitBarrier(commandBuffer);

            simulateStage.dispatch(commandBuffer, resources, sets, set0, frameIndex, state.descriptor.forces(), memoryOps);
            VulkanVfxSimulateStage.insertPostSimulateBarrier(commandBuffer);

            boolean needsSort = resources.config().needsSort();
            if (needsSort) {
                sortStage.dispatch(
                    commandBuffer,
                    resources,
                    sets,
                    state.descriptor.renderer().blendMode(),
                    cameraPos,
                    Math.max(state.aliveCount, spawnCount),
                    frameIndex
                );
                VulkanVfxSortStage.insertPostSortBarrier(commandBuffer);
            }

            cullCompactStage.dispatch(commandBuffer, resources, sets, set0, frameIndex, needsSort, frustum);
            VulkanVfxCullCompactStage.insertPostCullBarrier(commandBuffer);

            state.aliveCount = Math.min(resources.config().maxParticles(), Math.max(0, state.aliveCount + spawnCount));
            state.lastDrawInstanceCount = cullCompactStage.lastInstanceCount();
        }
    }

    @Override
    public void recordDraws(List<VfxHandle> activeEffects, VfxDrawContext ctx) {
        Objects.requireNonNull(activeEffects, "activeEffects");
        Objects.requireNonNull(ctx, "ctx");

        IndirectCommandBuffer out = ctx.indirectBuffer();
        int slot = 0;
        for (VfxHandle handle : activeEffects) {
            EffectState state = effects.get(handle.id());
            if (state == null || state.resources == null) {
                continue;
            }
            VulkanVfxIndirectWriter.writeDrawCommand(out, slot++, Math.max(0, state.lastDrawInstanceCount));
        }
    }

    @Override
    public VfxHandle spawn(ParticleEmitterDescriptor descriptor, float[] transform) {
        Objects.requireNonNull(descriptor, "descriptor");
        int id = nextHandleId.getAndIncrement();
        int generation = generationById.getOrDefault(id, 0);
        VfxHandle handle = VfxHandle.create(id, generation, descriptor.id());
        effects.put(id, new EffectState(handle, descriptor, normalizedTransform(transform), System.nanoTime()));
        return handle;
    }

    @Override
    public void despawn(VfxHandle handle) {
        if (handle == null) {
            return;
        }
        EffectState state = effects.remove(handle.id());
        generationById.put(handle.id(), handle.generation() + 1);
        if (state != null && state.resources != null) {
            state.resources.destroy(memoryOps);
        }
    }

    @Override
    public void updateTransform(VfxHandle handle, float[] transform) {
        if (handle == null) {
            return;
        }
        EffectState state = effects.get(handle.id());
        if (state == null) {
            return;
        }
        state.transform = normalizedTransform(transform);
    }

    @Override
    public void setPhysicsHandoff(PhysicsHandoff handoff) {
        this.physicsHandoff = handoff;
    }

    @Override
    public VfxStats getStats() {
        int activeEffects = effects.size();
        int activeParticles = effects.values().stream().mapToInt(s -> Math.max(0, s.aliveCount)).sum();
        long gpuBytes = effects.values().stream()
            .filter(s -> s.resources != null)
            .mapToLong(s -> (long) s.resources.config().maxParticles() * 80L)
            .sum();
        return new VfxStats(activeEffects, activeParticles, 0, 0, gpuBytes);
    }

    public void registerEffectResources(
        VfxHandle handle,
        VulkanVfxEffectResources resources,
        VulkanVfxDescriptorSets descriptorSets
    ) {
        Objects.requireNonNull(handle, "handle");
        Objects.requireNonNull(resources, "resources");
        Objects.requireNonNull(descriptorSets, "descriptorSets");

        EffectState state = effects.get(handle.id());
        if (state == null) {
            throw new IllegalStateException("Handle not registered in service: " + handle.id());
        }
        state.resources = resources;
        state.descriptorSets = descriptorSets;
    }

    public void destroy() {
        for (EffectState state : effects.values()) {
            if (state.resources != null) {
                state.resources.destroy(memoryOps);
            }
            if (state.descriptorSets != null) {
                state.descriptorSets.destroy(device);
            }
        }
        effects.clear();
        retireStage.destroy(device);
        emitStage.destroy(device);
        simulateStage.destroy(device);
        sortStage.destroy(device);
        cullCompactStage.destroy(device);
        physicsHandoff = null;
    }

    public PhysicsHandoff physicsHandoff() {
        return physicsHandoff;
    }

    private static float[] normalizedTransform(float[] transform) {
        if (transform == null || transform.length != 16) {
            return DEFAULT_TRANSFORM.clone();
        }
        return transform.clone();
    }

    private static float[] normalizeFrustum(float[] planes) {
        float[] out = new float[24];
        if (planes == null) {
            return out;
        }
        System.arraycopy(planes, 0, out, 0, Math.min(24, planes.length));
        return out;
    }

    private static float[] extractCameraPos(float[] viewMatrix) {
        // Placeholder camera extraction until matrix utility wiring is added.
        if (viewMatrix == null || viewMatrix.length < 16) {
            return new float[] {0f, 0f, 0f};
        }
        return new float[] {-viewMatrix[12], -viewMatrix[13], -viewMatrix[14]};
    }

    private static final class EffectState {
        private final VfxHandle handle;
        private final ParticleEmitterDescriptor descriptor;
        private final long seed;
        private float[] transform;

        private VulkanVfxEffectResources resources;
        private VulkanVfxDescriptorSets descriptorSets;
        private int aliveCount;
        private int lastDrawInstanceCount;

        private EffectState(VfxHandle handle, ParticleEmitterDescriptor descriptor, float[] transform, long seed) {
            this.handle = handle;
            this.descriptor = descriptor;
            this.transform = transform;
            this.seed = seed;
        }
    }
}
