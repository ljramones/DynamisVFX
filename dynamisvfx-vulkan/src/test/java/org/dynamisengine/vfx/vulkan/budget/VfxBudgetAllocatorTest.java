package org.dynamisengine.vfx.vulkan.budget;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class VfxBudgetAllocatorTest {

    private static final Consumer<Integer> NO_OP = id -> {};

    @Test
    void rejectPolicyDeniesWhenExhausted() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.REJECT);
        assertNotNull(alloc.allocate(100, NO_OP));
        assertNull(alloc.allocate(1, NO_OP));
    }

    @Test
    void rejectPolicyGrantsWhenAvailable() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(200, VfxBudgetPolicy.REJECT);
        VfxBudgetAllocation a = alloc.allocate(50, NO_OP);
        assertNotNull(a);
        assertEquals(50, a.allocatedParticles());
    }

    @Test
    void clampPolicyReducesToRemaining() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.CLAMP);
        alloc.allocate(80, NO_OP);
        VfxBudgetAllocation b = alloc.allocate(50, NO_OP);
        assertNotNull(b);
        assertEquals(20, b.allocatedParticles());
    }

    @Test
    void clampPolicyGrantsFullWhenAvailable() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.CLAMP);
        VfxBudgetAllocation a = alloc.allocate(30, NO_OP);
        assertNotNull(a);
        assertEquals(30, a.allocatedParticles());
    }

    @Test
    void evictOldestFreesOldestAllocation() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.EVICT_OLDEST);
        List<Integer> evicted = new ArrayList<>();
        Consumer<Integer> callback = evicted::add;

        VfxBudgetAllocation first = alloc.allocate(60, callback);
        alloc.allocate(40, callback);
        assertNotNull(first);

        VfxBudgetAllocation third = alloc.allocate(70, callback);
        assertNotNull(third);
        assertTrue(evicted.contains(first.allocationId()));
    }

    @Test
    void releaseFreesParticles() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.REJECT);
        VfxBudgetAllocation a = alloc.allocate(100, NO_OP);
        assertNotNull(a);
        assertNull(alloc.allocate(1, NO_OP));

        alloc.release(a.allocationId());
        VfxBudgetAllocation b = alloc.allocate(50, NO_OP);
        assertNotNull(b);
        assertEquals(50, b.allocatedParticles());
    }

    @Test
    void statsTrackRejections() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(10, VfxBudgetPolicy.REJECT);
        alloc.allocate(10, NO_OP);
        alloc.allocate(5, NO_OP);
        assertEquals(1, alloc.stats().rejectedThisFrame());
    }

    @Test
    void statsTrackClamps() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.CLAMP);
        alloc.allocate(80, NO_OP);
        alloc.allocate(50, NO_OP);
        assertEquals(1, alloc.stats().clampedThisFrame());
    }

    @Test
    void statsTrackEvictions() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.EVICT_OLDEST);
        List<Integer> evicted = new ArrayList<>();
        alloc.allocate(60, evicted::add);
        alloc.allocate(40, evicted::add);
        alloc.allocate(70, evicted::add);
        assertTrue(alloc.stats().evictedThisFrame() >= 1);
    }

    @Test
    void zeroBudgetRejectsAll() {
        assertThrows(IllegalArgumentException.class,
            () -> new VfxBudgetAllocator(0, VfxBudgetPolicy.REJECT));
    }

    @Test
    void multipleAllocationsTrackCorrectly() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.REJECT);
        alloc.allocate(30, NO_OP);
        alloc.allocate(40, NO_OP);
        alloc.allocate(20, NO_OP);

        VfxBudgetStats stats = alloc.stats();
        assertEquals(90, stats.usedBudget());
        assertEquals(10, stats.remainingBudget());
        assertEquals(3, stats.activeEffectCount());
    }

    @Test
    void releaseNonexistentIdIsNoOp() {
        VfxBudgetAllocator alloc = new VfxBudgetAllocator(100, VfxBudgetPolicy.REJECT);
        alloc.release(999);
        assertEquals(0, alloc.stats().usedBudget());
    }
}
