package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.ParticleInitDescriptor;

import java.util.Arrays;

public final class ParticleInitBuilder {
    private float lifetimeMinSeconds = 1.0f;
    private float lifetimeMaxSeconds = 1.0f;
    private float speedMin = 0.0f;
    private float speedMax = 0.0f;
    private float sizeMin = 1.0f;
    private float sizeMax = 1.0f;
    private float[] initialDirection = new float[] {0.0f, 1.0f, 0.0f};
    private float[] colorRgb = new float[] {1.0f, 1.0f, 1.0f};
    private float alpha = 1.0f;

    public ParticleInitBuilder lifetime(float minSeconds, float maxSeconds) {
        this.lifetimeMinSeconds = minSeconds;
        this.lifetimeMaxSeconds = maxSeconds;
        return this;
    }

    public ParticleInitBuilder velocityRange(float minSpeed, float maxSpeed) {
        this.speedMin = minSpeed;
        this.speedMax = maxSpeed;
        return this;
    }

    public ParticleInitBuilder sizeRange(float minSize, float maxSize) {
        this.sizeMin = minSize;
        this.sizeMax = maxSize;
        return this;
    }

    public ParticleInitBuilder initialDirection(float x, float y, float z) {
        this.initialDirection = new float[] {x, y, z};
        return this;
    }

    public ParticleInitBuilder color(float r, float g, float b, float a) {
        this.colorRgb = new float[] {r, g, b};
        this.alpha = a;
        return this;
    }

    public ParticleInitDescriptor build() {
        return new ParticleInitDescriptor(
            lifetimeMinSeconds,
            lifetimeMaxSeconds,
            speedMin,
            speedMax,
            sizeMin,
            sizeMax,
            Arrays.copyOf(initialDirection, initialDirection.length),
            Arrays.copyOf(colorRgb, colorRgb.length),
            alpha
        );
    }
}
