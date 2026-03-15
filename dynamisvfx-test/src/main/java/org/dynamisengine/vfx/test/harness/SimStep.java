package org.dynamisengine.vfx.test.harness;

import org.dynamisengine.vfx.api.VfxStats;

public record SimStep(int stepIndex, float deltaTime, VfxStats stats) {
}
