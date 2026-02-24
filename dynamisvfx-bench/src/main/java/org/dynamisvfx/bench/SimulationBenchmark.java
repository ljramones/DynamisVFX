package org.dynamisvfx.bench;

import org.dynamisvfx.api.EffectDescriptor;
import org.dynamisvfx.core.ParticleSimulationCore;
import org.openjdk.jmh.annotations.Benchmark;

public class SimulationBenchmark {
    private static final ParticleSimulationCore CORE = new ParticleSimulationCore();
    private static final EffectDescriptor EFFECT = () -> "benchmark";

    @Benchmark
    public String describeEffect() {
        return CORE.describe(EFFECT);
    }
}
