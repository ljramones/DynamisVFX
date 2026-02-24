package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.EmissionRateDescriptor;
import org.dynamisvfx.api.EmitterShapeDescriptor;
import org.dynamisvfx.api.ForceDescriptor;
import org.dynamisvfx.api.LodDescriptor;
import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.ParticleInitDescriptor;
import org.dynamisvfx.api.PhysicsHandoffDescriptor;
import org.dynamisvfx.api.RendererDescriptor;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class EffectBuilder {
    private final String id;
    private EmitterShapeDescriptor shape;
    private EmissionRateDescriptor rate;
    private ParticleInitDescriptor init;
    private final List<ForceDescriptor> forces = new ArrayList<>();
    private RendererDescriptor renderer;
    private LodDescriptor lod;
    private PhysicsHandoffDescriptor physics;

    private EffectBuilder(String id) {
        this.id = Objects.requireNonNull(id, "id");
    }

    public static EffectBuilder emitter(String id) {
        return new EffectBuilder(id);
    }

    public EffectBuilder shape(EmitterShapeDescriptor descriptor) {
        this.shape = descriptor;
        return this;
    }

    public EffectBuilder rate(EmissionRateDescriptor descriptor) {
        this.rate = descriptor;
        return this;
    }

    public EffectBuilder init(ParticleInitDescriptor descriptor) {
        this.init = descriptor;
        return this;
    }

    public EffectBuilder force(ForceDescriptor descriptor) {
        this.forces.add(descriptor);
        return this;
    }

    public EffectBuilder renderer(RendererDescriptor descriptor) {
        this.renderer = descriptor;
        return this;
    }

    public EffectBuilder lod(LodDescriptor descriptor) {
        this.lod = descriptor;
        return this;
    }

    public EffectBuilder physics(PhysicsHandoffDescriptor descriptor) {
        this.physics = descriptor;
        return this;
    }

    public ParticleEmitterDescriptor build() {
        return new ParticleEmitterDescriptor(
            id,
            Objects.requireNonNull(shape, "shape"),
            Objects.requireNonNull(rate, "rate"),
            Objects.requireNonNull(init, "init"),
            List.copyOf(forces),
            Objects.requireNonNull(renderer, "renderer"),
            lod,
            physics
        );
    }
}
