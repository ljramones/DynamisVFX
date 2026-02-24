package org.dynamisvfx.api;

import java.util.List;

public interface VfxService {
    void simulate(List<VfxHandle> activeEffects, float deltaTime, VfxFrameContext ctx);

    void recordDraws(List<VfxHandle> activeEffects, VfxDrawContext ctx);

    VfxHandle spawn(ParticleEmitterDescriptor descriptor, Mat4f transform);

    void despawn(VfxHandle handle);

    void updateTransform(VfxHandle handle, Mat4f transform);

    void setPhysicsHandoff(PhysicsHandoff handoff);

    VfxStats getStats();
}
