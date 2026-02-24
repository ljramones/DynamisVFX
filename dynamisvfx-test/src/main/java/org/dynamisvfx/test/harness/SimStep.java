package org.dynamisvfx.test.harness;

import org.dynamisvfx.api.VfxStats;

public record SimStep(int stepIndex, float deltaTime, VfxStats stats) {
}
