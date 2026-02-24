package org.dynamisvfx.test.mock;

import org.dynamisvfx.api.DebrisSpawnEvent;
import org.dynamisvfx.api.PhysicsHandoff;

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
