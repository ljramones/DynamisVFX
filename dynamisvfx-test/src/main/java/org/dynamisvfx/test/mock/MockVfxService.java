package org.dynamisvfx.test.mock;

import org.dynamisvfx.api.DebrisSpawnEvent;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.PhysicsHandoff;
import org.dynamisvfx.api.VfxDrawContext;
import org.dynamisvfx.api.VfxFrameContext;
import org.dynamisvfx.api.VfxBudgetStats;
import org.dynamisvfx.api.VfxHandle;
import org.dynamisvfx.api.VfxService;
import org.dynamisvfx.api.VfxStats;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public final class MockVfxService implements VfxService {
    public record SimulateCall(List<VfxHandle> activeEffects, float deltaTime, long frameIndex) {}
    public record DrawCall(List<VfxHandle> activeEffects, long frameIndex) {}

    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final Map<VfxHandle, ParticleEmitterDescriptor> liveHandles = new LinkedHashMap<>();
    private final Map<VfxHandle, float[]> transforms = new IdentityHashMap<>();
    private final Set<VfxHandle> staleHandles = Collections.newSetFromMap(new IdentityHashMap<>());
    private final List<SimulateCall> simulateCalls = new ArrayList<>();
    private final List<DrawCall> drawCalls = new ArrayList<>();

    private PhysicsHandoff physicsHandoff;
    private int simulationStep;
    private VfxStats currentStats = new VfxStats(0, 0, 0, 0, 0, new VfxBudgetStats(0, 0, 0, 0, 0, 0, 0));

    @Override
    public void simulate(List<VfxHandle> activeEffects, float deltaTime, VfxFrameContext ctx) {
        simulationStep++;
        simulateCalls.add(new SimulateCall(List.copyOf(activeEffects), deltaTime, ctx.frameIndex()));

        int activeCount = Math.max(activeEffects.size(), liveHandles.size());
        int particleCount = simulationStep * 8;
        int sleepingCount = activeCount == 0 ? 0 : simulationStep / 30;
        int culledCount = simulationStep * 2;
        long memoryBytes = (long) particleCount * 80L;
        currentStats = new VfxStats(
            activeCount,
            particleCount,
            sleepingCount,
            culledCount,
            memoryBytes,
            new VfxBudgetStats(0, 0, 0, activeCount, 0, 0, 0)
        );

        maybeEmitDeterministicDebris(activeEffects);
    }

    @Override
    public void recordDraws(List<VfxHandle> activeEffects, VfxDrawContext ctx) {
        drawCalls.add(new DrawCall(List.copyOf(activeEffects), ctx.frameIndex()));
        if (ctx instanceof MockVfxDrawContext mock) {
            mock.recordDrawCalls(activeEffects.size());
        }
    }

    @Override
    public VfxHandle spawn(ParticleEmitterDescriptor descriptor, float[] transform) {
        Objects.requireNonNull(descriptor, "descriptor");
        int id = idGenerator.getAndIncrement();
        VfxHandle handle = VfxHandle.create(id, 1, descriptor.id());
        liveHandles.put(handle, descriptor);
        transforms.put(handle, transform == null ? null : Arrays.copyOf(transform, transform.length));
        staleHandles.remove(handle);
        return handle;
    }

    @Override
    public void despawn(VfxHandle handle) {
        if (handle == null) {
            return;
        }
        liveHandles.remove(handle);
        transforms.remove(handle);
        staleHandles.add(handle);
    }

    @Override
    public void updateTransform(VfxHandle handle, float[] transform) {
        if (handle != null && liveHandles.containsKey(handle)) {
            transforms.put(handle, transform == null ? null : Arrays.copyOf(transform, transform.length));
        }
    }

    @Override
    public void setPhysicsHandoff(PhysicsHandoff handoff) {
        this.physicsHandoff = handoff;
    }

    @Override
    public VfxStats getStats() {
        return currentStats;
    }

    public Map<VfxHandle, ParticleEmitterDescriptor> liveHandles() {
        return Collections.unmodifiableMap(liveHandles);
    }

    public boolean isHandleAlive(VfxHandle handle) {
        return liveHandles.containsKey(handle);
    }

    public boolean isHandleStale(VfxHandle handle) {
        return staleHandles.contains(handle);
    }

    public List<SimulateCall> simulateCalls() {
        return Collections.unmodifiableList(simulateCalls);
    }

    public List<DrawCall> drawCalls() {
        return Collections.unmodifiableList(drawCalls);
    }

    public int stepCount() {
        return simulationStep;
    }

    private void maybeEmitDeterministicDebris(List<VfxHandle> activeEffects) {
        if (physicsHandoff == null || activeEffects.isEmpty()) {
            return;
        }
        if (simulationStep % 30 != 0) {
            return;
        }

        VfxHandle source = activeEffects.get(0);
        physicsHandoff.onDebrisSpawn(new DebrisSpawnEvent(
            identityMatrix(),
            new float[] {1.0f, 0.0f, 0.0f},
            new float[] {0.0f, 0.1f, 0.0f},
            1.0f,
            "mock_debris",
            "default",
            source.id()
        ));
    }

    private static float[] identityMatrix() {
        return new float[] {
            1.0f, 0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f, 0.0f,
            0.0f, 0.0f, 1.0f, 0.0f,
            0.0f, 0.0f, 0.0f, 1.0f
        };
    }
}
