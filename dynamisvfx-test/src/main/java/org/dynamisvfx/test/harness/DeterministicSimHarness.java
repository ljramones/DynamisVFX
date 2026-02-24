package org.dynamisvfx.test.harness;

import org.dynamisvfx.api.ParticleEmitterDescriptor;
import org.dynamisvfx.api.VfxHandle;
import org.dynamisvfx.api.VfxService;
import org.dynamisvfx.api.VfxStats;
import org.dynamisvfx.test.mock.MockVfxDrawContext;
import org.dynamisvfx.test.mock.MockVfxFrameContext;
import org.dynamisvfx.test.mock.MockVfxService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

public final class DeterministicSimHarness {
    private final VfxService service;
    private final ParticleEmitterDescriptor effect;
    private final float[] transform;
    private final int steps;
    private final float deltaTime;
    private final long seed;

    private DeterministicSimHarness(Builder builder) {
        this.service = builder.service;
        this.effect = builder.effect;
        this.transform = builder.transform;
        this.steps = builder.steps;
        this.deltaTime = builder.deltaTime;
        this.seed = builder.seed;
    }

    public static Builder builder() {
        return new Builder();
    }

    public SimResult run() {
        VfxHandle handle = service.spawn(effect, transform);
        List<VfxHandle> active = List.of(handle);

        MockVfxFrameContext frameContext = new MockVfxFrameContext();
        MockVfxDrawContext drawContext = new MockVfxDrawContext();

        List<SimStep> simSteps = new ArrayList<>(steps);
        Random random = new Random(seed);

        for (int i = 0; i < steps; i++) {
            frameContext.frameIndex(i);
            drawContext.frameIndex(i);

            float[] t = Matrix4fUtil.identity();
            t[12] = random.nextFloat();
            service.updateTransform(handle, t);

            service.simulate(active, deltaTime, frameContext);
            service.recordDraws(active, drawContext);

            VfxStats snapshot = service.getStats();
            simSteps.add(new SimStep(i, deltaTime, snapshot));
        }

        Map<VfxHandle, Boolean> states = new LinkedHashMap<>();
        if (service instanceof MockVfxService mock) {
            states.put(handle, mock.isHandleAlive(handle));
        } else {
            states.put(handle, true);
        }

        return new SimResult(List.copyOf(simSteps), Map.copyOf(states));
    }

    public static final class Builder {
        private VfxService service;
        private ParticleEmitterDescriptor effect;
        private float[] transform = Matrix4fUtil.identity();
        private int steps = 1;
        private float deltaTime = 1.0f / 60.0f;
        private long seed = 0L;

        private Builder() {
        }

        public Builder service(VfxService value) {
            this.service = Objects.requireNonNull(value, "service");
            return this;
        }

        public Builder effect(ParticleEmitterDescriptor descriptor, float[] worldTransform) {
            this.effect = Objects.requireNonNull(descriptor, "descriptor");
            this.transform = worldTransform;
            return this;
        }

        public Builder steps(int value) {
            this.steps = value;
            return this;
        }

        public Builder deltaTime(float value) {
            this.deltaTime = value;
            return this;
        }

        public Builder seed(long value) {
            this.seed = value;
            return this;
        }

        public DeterministicSimHarness build() {
            if (steps <= 0) {
                throw new IllegalArgumentException("steps must be > 0");
            }
            return new DeterministicSimHarness(this);
        }
    }
}
