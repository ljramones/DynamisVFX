package org.dynamisengine.vfx.test.mock;

import org.dynamisengine.vfx.api.DebrisSpawnEvent;
import org.dynamisengine.vfx.api.PhysicsHandoff;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MockPhysicsHandoff implements PhysicsHandoff {
    private final List<DebrisSpawnEvent> events = new ArrayList<>();

    @Override
    public void onDebrisSpawn(DebrisSpawnEvent event) {
        events.add(event);
    }

    public List<DebrisSpawnEvent> events() {
        return Collections.unmodifiableList(events);
    }

    public int eventCount() {
        return events.size();
    }

    public void reset() {
        events.clear();
    }
}
