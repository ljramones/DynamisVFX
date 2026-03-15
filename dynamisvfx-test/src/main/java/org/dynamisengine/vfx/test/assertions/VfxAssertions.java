package org.dynamisengine.vfx.test.assertions;

import org.dynamisengine.vfx.api.VfxHandle;
import org.dynamisengine.vfx.test.harness.SimResult;
import org.dynamisengine.vfx.test.mock.MockPhysicsHandoff;
import org.dynamisengine.vfx.test.mock.MockVfxDrawContext;
import org.dynamisengine.vfx.test.mock.MockVfxService;

public final class VfxAssertions {
    private VfxAssertions() {
    }

    public static void assertParticleCountInRange(SimResult result, int min, int max) {
        int value = result.steps().get(result.steps().size() - 1).stats().activeParticleCount();
        if (value < min || value > max) {
            throw new AssertionError("Expected particle count in range [" + min + ", " + max + "], got " + value);
        }
    }

    public static void assertNoDebrisSpawnedBefore(SimResult result, int step, MockPhysicsHandoff handoff) {
        if (step < 0) {
            throw new AssertionError("step must be >= 0");
        }
        int before = Math.min(step, result.steps().size());
        if (before > 0 && handoff.eventCount() > 0) {
            throw new AssertionError("Expected no debris spawned before step " + step + ", found " + handoff.eventCount());
        }
    }

    public static void assertDrawCallsRecorded(MockVfxDrawContext context, int min) {
        int total = context.totalDrawCalls();
        if (total < min) {
            throw new AssertionError("Expected at least " + min + " draw calls, got " + total);
        }
    }

    public static void assertHandleAlive(MockVfxService service, VfxHandle handle) {
        if (!service.isHandleAlive(handle)) {
            throw new AssertionError("Expected handle to be alive: " + handle.effectId());
        }
    }

    public static void assertHandleStale(MockVfxService service, VfxHandle handle) {
        if (!service.isHandleStale(handle)) {
            throw new AssertionError("Expected handle to be stale: " + handle.effectId());
        }
    }
}
