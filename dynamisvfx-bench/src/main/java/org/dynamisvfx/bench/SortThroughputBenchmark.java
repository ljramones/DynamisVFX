package org.dynamisvfx.bench;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.RendererDescriptor;
import org.dynamisvfx.test.harness.DeterministicSimHarness;
import org.dynamisvfx.test.harness.SimResult;
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
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class SortThroughputBenchmark {
    @Param({"1024", "16384", "65536"})
    public int particleCount;

    private DeterministicSimHarness harness;
    private ParticleEmitterDescriptor smoke;
    private ParticleEmitterDescriptor fire;

    @Setup
    public void setup() {
        smoke = BenchmarkFixtures.smoke(particleCount);
        fire = BenchmarkFixtures.fireBurst(particleCount);
        harness = DeterministicSimHarness.builder()
            .service(BenchmarkFixtures.mockService())
            .effect(smoke, BenchmarkFixtures.identityMatrix())
            .steps(10)
            .deltaTime(1.0f / 60.0f)
            .seed(42L)
            .build();
    }

    @Benchmark
    public SimResult sortDecision() {
        return harness.run();
    }

    @Benchmark
    public boolean additiveSortSkip() {
        RendererDescriptor renderer = fire.renderer();
        return renderer != null && renderer.blendMode() == BlendMode.ADDITIVE;
    }
}
