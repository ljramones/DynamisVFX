package org.dynamisvfx.api;

import java.util.Objects;

public final class VfxHandle {
    private final int id;
    private final int generation;
    private final String effectId;

    private VfxHandle(int id, int generation, String effectId) {
        this.id = id;
        this.generation = generation;
        this.effectId = Objects.requireNonNull(effectId, "effectId");
    }

    public static VfxHandle create(int id, int generation, String effectId) {
        return new VfxHandle(id, generation, effectId);
    }

    public int id() {
        return id;
    }

    public int generation() {
        return generation;
    }

    public String effectId() {
        return effectId;
    }
}
