package org.dynamisvfx.api;

import java.util.List;

public interface VfxService {
    void simulate(List<VfxHandle> activeEffects, float deltaTime, VfxFrameContext ctx);

    void recordDraws(List<VfxHandle> activeEffects, VfxDrawContext ctx);

    VfxHandle spawn(ParticleEmitterDescriptor descriptor, float[] transform);

    void despawn(VfxHandle handle);

    void updateTransform(VfxHandle handle, float[] transform);

    void setPhysicsHandoff(PhysicsHandoff handoff);

    VfxStats getStats();
}
