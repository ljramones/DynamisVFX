package org.dynamisvfx.bench;

import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.vulkan.compute.VulkanVfxSpawnScheduler;
import org.dynamisvfx.vulkan.emitter.PackedEmitterDescriptor;
import org.dynamisvfx.vulkan.emitter.VulkanEmitterDescriptorPacker;
import org.dynamisvfx.vulkan.force.PackedForceBuffer;
import org.dynamisvfx.vulkan.force.VulkanForceFieldPacker;
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
public class EmitThroughputBenchmark {
    @Param({"1024", "16384", "65536"})
    public int maxParticles;

    private ParticleEmitterDescriptor descriptor;
    private VulkanVfxSpawnScheduler scheduler;

    @Setup
    public void setup() {
        descriptor = BenchmarkFixtures.fireBurst(maxParticles);
        scheduler = new VulkanVfxSpawnScheduler();
    }

    @Benchmark
    public int spawnSchedulerBurst() {
        scheduler.reset();
        return scheduler.computeSpawnCount(descriptor.rate(), 0.016f, maxParticles);
    }

    @Benchmark
    public PackedEmitterDescriptor emitterDescriptorPack() {
        return VulkanEmitterDescriptorPacker.pack(descriptor);
    }

    @Benchmark
    public PackedForceBuffer forceFieldPack() {
        return VulkanForceFieldPacker.pack(descriptor.forces());
    }
}
