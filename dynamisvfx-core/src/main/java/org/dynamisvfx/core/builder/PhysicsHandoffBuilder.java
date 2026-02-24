package org.dynamisvfx.core.builder;

import org.dynamisvfx.api.PhysicsHandoffDescriptor;

public final class PhysicsHandoffBuilder {
    private boolean enabled;
    private float speedThreshold;
    private String meshId;
    private String materialTag;
    private float mass = 1.0f;

    public PhysicsHandoffBuilder enabled(boolean value) {
        this.enabled = value;
        return this;
    }

    public PhysicsHandoffBuilder speedThreshold(float value) {
        this.speedThreshold = value;
        return this;
    }

    public PhysicsHandoffBuilder meshId(String value) {
        this.meshId = value;
        return this;
    }

    public PhysicsHandoffBuilder materialTag(String value) {
        this.materialTag = value;
        return this;
    }

    public PhysicsHandoffBuilder mass(float value) {
        this.mass = value;
        return this;
    }

    public PhysicsHandoffDescriptor build() {
        return new PhysicsHandoffDescriptor(enabled, speedThreshold, meshId, materialTag, mass);
    }
}
