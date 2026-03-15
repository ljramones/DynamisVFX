package org.dynamisengine.vfx.testsupport;

import org.dynamisengine.vfx.core.ParticleSimulationCore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimulationSanityTest {
    @Test
    void describeFormatsEffectId() {
        ParticleSimulationCore core = new ParticleSimulationCore();
        assertEquals("Effect<smoke>", core.describe(new TestEffectDescriptor("smoke")));
    }
}
