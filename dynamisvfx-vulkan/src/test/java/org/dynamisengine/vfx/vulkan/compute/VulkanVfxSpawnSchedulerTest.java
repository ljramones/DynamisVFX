package org.dynamisengine.vfx.vulkan.compute;

import org.dynamisengine.vfx.api.EmissionMode;
import org.dynamisengine.vfx.api.EmissionRateDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class VulkanVfxSpawnSchedulerTest {

    private VulkanVfxSpawnScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new VulkanVfxSpawnScheduler();
    }

    @Test
    void continuousEmissionSchedulesCorrectCount() {
        // 100 PPS * 0.1s = 10 particles
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 100f, 0, null);
        int count = scheduler.computeSpawnCount(rate, 0.1f, 1000);
        assertEquals(10, count);
    }

    @Test
    void burstEmissionSchedulesFullBurst() {
        var rate = new EmissionRateDescriptor(EmissionMode.BURST, 0f, 50, null);
        int count = scheduler.computeSpawnCount(rate, 0.016f, 1000);
        assertEquals(50, count);
    }

    @Test
    void burstEmissionFiresOnlyOnce() {
        var rate = new EmissionRateDescriptor(EmissionMode.BURST, 0f, 50, null);
        scheduler.computeSpawnCount(rate, 0.016f, 1000);
        int second = scheduler.computeSpawnCount(rate, 0.016f, 1000);
        assertEquals(0, second, "Burst should only fire once");
    }

    @Test
    void burstResetAllowsReFiring() {
        var rate = new EmissionRateDescriptor(EmissionMode.BURST, 0f, 25, null);
        scheduler.computeSpawnCount(rate, 0.016f, 1000);
        scheduler.reset();
        int count = scheduler.computeSpawnCount(rate, 0.016f, 1000);
        assertEquals(25, count);
    }

    @Test
    void zeroRateProducesNoSpawns() {
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 0f, 0, null);
        int count = scheduler.computeSpawnCount(rate, 1.0f, 1000);
        assertEquals(0, count);
    }

    @Test
    void nullRateProducesNoSpawns() {
        assertEquals(0, scheduler.computeSpawnCount(null, 0.1f, 100));
    }

    @Test
    void zeroDeltaTimeProducesNoSpawns() {
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 100f, 0, null);
        assertEquals(0, scheduler.computeSpawnCount(rate, 0f, 100));
    }

    @Test
    void zeroFreeSlotsProducesNoSpawns() {
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 100f, 0, null);
        assertEquals(0, scheduler.computeSpawnCount(rate, 0.1f, 0));
    }

    @Test
    void schedulerAccumulatesFractionalParticles() {
        // 10 PPS * 0.05s = 0.5 particles per frame -> 0 first call
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 10f, 0, null);
        int first = scheduler.computeSpawnCount(rate, 0.05f, 1000);
        assertEquals(0, first);

        // Second call: 0.5 + 0.5 = 1.0 -> 1 particle
        int second = scheduler.computeSpawnCount(rate, 0.05f, 1000);
        assertEquals(1, second);
    }

    @Test
    void spawnCountClampedToFreeSlots() {
        var rate = new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 1000f, 0, null);
        int count = scheduler.computeSpawnCount(rate, 1.0f, 5);
        assertEquals(5, count);
    }

    @Test
    void burstClampedToFreeSlots() {
        var rate = new EmissionRateDescriptor(EmissionMode.BURST, 0f, 100, null);
        int count = scheduler.computeSpawnCount(rate, 0.016f, 10);
        assertEquals(10, count);
    }

    @Test
    void eventModeProducesNoSpawns() {
        var rate = new EmissionRateDescriptor(EmissionMode.EVENT, 0f, 0, "explosion");
        int count = scheduler.computeSpawnCount(rate, 0.1f, 1000);
        assertEquals(0, count);
    }
}
