package org.dynamisengine.vfx.test.harness;

import org.dynamisengine.vfx.api.VfxHandle;

import java.util.List;
import java.util.Map;

public record SimResult(List<SimStep> steps, Map<VfxHandle, Boolean> finalHandleStates) {
}
