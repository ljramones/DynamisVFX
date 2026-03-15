package org.dynamisengine.vfx.core;

import org.dynamisengine.vfx.api.EffectDescriptor;

public final class ParticleSimulationCore {
    public String describe(EffectDescriptor descriptor) {
        return "Effect<" + descriptor.id() + ">";
    }
}
