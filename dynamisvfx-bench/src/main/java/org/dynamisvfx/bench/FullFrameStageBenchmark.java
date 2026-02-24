package org.dynamisvfx.bench;

import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.VfxHandle;
import org.dynamisvfx.test.mock.MockVfxDrawContext;
import org.dynamisvfx.test.mock.MockVfxFrameContext;
import org.dynamisvfx.test.mock.MockVfxService;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class FullFrameStageBenchmark {
    @Param({"1", "8", "32"})
    public int activeEffectCount;

    private MockVfxService service;
    private List<VfxHandle> handles;
    private MockVfxFrameContext frameCtx;
    private MockVfxDrawContext drawCtx;
    private long frame;

    @Setup
    public void setup() {
        service = BenchmarkFixtures.mockService();
        handles = new ArrayList<>();
        frameCtx = new MockVfxFrameContext();
        drawCtx = new MockVfxDrawContext();

        ParticleEmitterDescriptor descriptor = BenchmarkFixtures.fireBurst(65536);
        for (int i = 0; i < activeEffectCount; i++) {
            VfxHandle h = service.spawn(descriptor, BenchmarkFixtures.identityMatrix());
            if (h != null) {
                handles.add(h);
            }
        }
    }

    @Benchmark
    public void fullFrameSimulate() {
        frameCtx.frameIndex(frame++);
        service.simulate(handles, 0.016f, frameCtx);
    }

    @Benchmark
    public void fullFrameRecordDraws() {
        drawCtx.frameIndex(frame++);
        service.recordDraws(handles, drawCtx);
    }

    @TearDown
    public void tearDown() {
        for (VfxHandle handle : handles) {
            service.despawn(handle);
        }
    }
}
