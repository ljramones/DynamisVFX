package org.dynamisvfx.bench;

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

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Benchmark)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 2)
@Fork(1)
public class CullCompactThroughputBenchmark {
    @Param({"1024", "16384", "65536"})
    public int particleCount;

    private float[] frustumPlanes;
    private float[] positions;

    @Setup
    public void setup() {
        frustumPlanes = new float[24];
        frustumPlanes[0] = 1.0f;
        frustumPlanes[3] = 80.0f;
        frustumPlanes[4] = -1.0f;
        frustumPlanes[7] = 80.0f;
        frustumPlanes[9] = 1.0f;
        frustumPlanes[11] = 80.0f;
        frustumPlanes[13] = -1.0f;
        frustumPlanes[15] = 80.0f;
        frustumPlanes[18] = 1.0f;
        frustumPlanes[19] = 0.1f;
        frustumPlanes[22] = -1.0f;
        frustumPlanes[23] = 120.0f;

        positions = new float[particleCount * 3];
        Random rng = new Random(99L);
        for (int i = 0; i < positions.length; i++) {
            positions[i] = (rng.nextFloat() - 0.5f) * 200.0f;
        }
    }

    @Benchmark
    public int frustumTestAll() {
        int visible = 0;
        for (int i = 0; i < particleCount; i++) {
            int base = i * 3;
            if (frustumTestSphere(positions[base], positions[base + 1], positions[base + 2], 0.1f, frustumPlanes)) {
                visible++;
            }
        }
        return visible;
    }

    private static boolean frustumTestSphere(float x, float y, float z, float r, float[] planes) {
        for (int i = 0; i < 6; i++) {
            int p = i * 4;
            float d = planes[p] * x + planes[p + 1] * y + planes[p + 2] * z + planes[p + 3];
            if (d < -r) {
                return false;
            }
        }
        return true;
    }
}
