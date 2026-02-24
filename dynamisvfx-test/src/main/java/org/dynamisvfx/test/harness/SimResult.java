package org.dynamisvfx.test.harness;

import org.dynamisvfx.api.VfxHandle;

import java.util.List;
import java.util.Map;

public record SimResult(List<SimStep> steps, Map<VfxHandle, Boolean> finalHandleStates) {
}
