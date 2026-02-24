package org.dynamisvfx.core;

import org.dynamisvfx.api.EffectDescriptor;

public final class ParticleSimulationCore {
    public String describe(EffectDescriptor descriptor) {
        return "Effect<" + descriptor.id() + ">";
    }
}
