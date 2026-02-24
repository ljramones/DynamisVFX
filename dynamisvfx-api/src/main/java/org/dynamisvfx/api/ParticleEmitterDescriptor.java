package org.dynamisvfx.api;

import java.util.List;

public record ParticleEmitterDescriptor(
    String id,
    EmitterShapeDescriptor shape,
    EmissionRateDescriptor rate,
    ParticleInitDescriptor init,
    List<ForceDescriptor> forces,
    RendererDescriptor renderer,
    LodDescriptor lod,
    PhysicsHandoffDescriptor physics
) implements EffectDescriptor {
}
