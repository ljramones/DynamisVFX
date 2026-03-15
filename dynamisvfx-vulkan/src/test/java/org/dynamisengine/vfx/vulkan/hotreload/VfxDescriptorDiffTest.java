package org.dynamisengine.vfx.vulkan.hotreload;

import org.dynamisengine.vfx.api.BlendMode;
import org.dynamisengine.vfx.api.EmissionMode;
import org.dynamisengine.vfx.api.EmissionRateDescriptor;
import org.dynamisengine.vfx.api.EmitterShapeDescriptor;
import org.dynamisengine.vfx.api.EmitterShapeType;
import org.dynamisengine.vfx.api.ForceDescriptor;
import org.dynamisengine.vfx.api.ForceType;
import org.dynamisengine.vfx.api.ParticleEmitterDescriptor;
import org.dynamisengine.vfx.api.ParticleInitDescriptor;
import org.dynamisengine.vfx.api.RendererDescriptor;
import org.dynamisengine.vfx.api.RendererType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VfxDescriptorDiffTest {

    private static EmitterShapeDescriptor shape(EmitterShapeType type) {
        return new EmitterShapeDescriptor(type, new float[]{1f, 1f, 1f}, null, null);
    }

    private static EmissionRateDescriptor rate(int burstCount) {
        return new EmissionRateDescriptor(EmissionMode.CONTINUOUS, 100f, burstCount, null);
    }

    private static RendererDescriptor renderer(RendererType type, BlendMode blend, String atlas, boolean soft) {
        return new RendererDescriptor(type, blend, atlas, 1, soft, false);
    }

    private static ParticleInitDescriptor init() {
        return new ParticleInitDescriptor(0.5f, 2.0f, 1f, 5f, 0.1f, 1f,
            new float[]{0f, 1f, 0f}, new float[]{1f, 1f, 1f}, 1f);
    }

    private static ParticleEmitterDescriptor desc(
        EmitterShapeDescriptor shape,
        EmissionRateDescriptor rate,
        RendererDescriptor renderer,
        List<ForceDescriptor> forces
    ) {
        return new ParticleEmitterDescriptor("test-emitter", shape, rate, init(), forces, renderer, null, null);
    }

    @Test
    void identicalDescriptorsReturnForcesOnly() {
        var shape = shape(EmitterShapeType.SPHERE);
        var rate = rate(10);
        var rend = renderer(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas1", false);
        var forces = List.of(new ForceDescriptor(ForceType.GRAVITY, -9.8f, new float[]{0, -1, 0}, null));
        var a = desc(shape, rate, rend, forces);
        var b = desc(shape, rate, rend, forces);

        assertEquals(VfxReloadCategory.FORCES_ONLY, VfxDescriptorDiff.classify(a, b));
    }

    @Test
    void forcesOnlyChangeClassifiesCorrectly() {
        var shape = shape(EmitterShapeType.SPHERE);
        var rate = rate(10);
        var rend = renderer(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas1", false);
        var a = desc(shape, rate, rend, List.of(new ForceDescriptor(ForceType.GRAVITY, -9.8f, new float[]{0, -1, 0}, null)));
        var b = desc(shape, rate, rend, List.of(new ForceDescriptor(ForceType.WIND, 5f, new float[]{1, 0, 0}, null)));

        assertEquals(VfxReloadCategory.FORCES_ONLY, VfxDescriptorDiff.classify(a, b));
    }

    @Test
    void rendererChangeClassifiesCorrectly() {
        var shape = shape(EmitterShapeType.SPHERE);
        var rate = rate(10);
        var forces = List.<ForceDescriptor>of();
        var rendA = renderer(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas1", false);
        var rendB = renderer(RendererType.BILLBOARD, BlendMode.ALPHA, "atlas1", false);
        var a = desc(shape, rate, rendA, forces);
        var b = desc(shape, rate, rendB, forces);

        assertEquals(VfxReloadCategory.RENDERER_CHANGED, VfxDescriptorDiff.classify(a, b));
    }

    @Test
    void shapeChangeRequiresFullRespawn() {
        var rate = rate(10);
        var rend = renderer(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas1", false);
        var forces = List.<ForceDescriptor>of();
        var a = desc(shape(EmitterShapeType.SPHERE), rate, rend, forces);
        var b = desc(shape(EmitterShapeType.BOX), rate, rend, forces);

        assertEquals(VfxReloadCategory.FULL_RESPAWN, VfxDescriptorDiff.classify(a, b));
    }

    @Test
    void emissionRateChangeClassification() {
        var shape = shape(EmitterShapeType.SPHERE);
        var rend = renderer(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas1", false);
        var forces = List.<ForceDescriptor>of();
        var a = desc(shape, rate(10), rend, forces);
        var b = desc(shape, rate(20), rend, forces);

        assertEquals(VfxReloadCategory.FULL_RESPAWN, VfxDescriptorDiff.classify(a, b));
    }

    @Test
    void lodChangeClassification() {
        var shape = shape(EmitterShapeType.SPHERE);
        var rate = rate(10);
        var forces = List.<ForceDescriptor>of();
        var rendA = renderer(RendererType.BILLBOARD, BlendMode.ADDITIVE, "atlas1", false);
        var rendB = renderer(RendererType.MESH, BlendMode.ADDITIVE, "atlas1", false);
        var a = desc(shape, rate, rendA, forces);
        var b = desc(shape, rate, rendB, forces);

        assertEquals(VfxReloadCategory.FULL_RESPAWN, VfxDescriptorDiff.classify(a, b));
    }
}
