package org.dynamisvfx.vulkan.budget;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class VfxBudgetAllocator {
    public static final int DEFAULT_GLOBAL_BUDGET = 1 << 20;

    private final int totalBudget;
    private final VfxBudgetPolicy policy;
    private final AtomicInteger usedBudget = new AtomicInteger(0);
    private final AtomicInteger nextId = new AtomicInteger(1);
    private final LinkedHashMap<Integer, VfxBudgetAllocation> activeAllocations = new LinkedHashMap<>();

    private final AtomicInteger rejectedThisFrame = new AtomicInteger(0);
    private final AtomicInteger clampedThisFrame = new AtomicInteger(0);
    private final AtomicInteger evictedThisFrame = new AtomicInteger(0);

    public VfxBudgetAllocator(int totalBudget, VfxBudgetPolicy policy) {
        if (totalBudget <= 0) {
            throw new IllegalArgumentException("totalBudget must be > 0");
        }
        this.totalBudget = totalBudget;
        this.policy = Objects.requireNonNull(policy, "policy");
    }

    public synchronized VfxBudgetAllocation allocate(
        int requestedParticles,
        Consumer<Integer> evictionCallback
    ) {
        if (requestedParticles <= 0) {
            rejectedThisFrame.incrementAndGet();
            return null;
        }
        Objects.requireNonNull(evictionCallback, "evictionCallback");

        int remaining = totalBudget - usedBudget.get();
        if (remaining <= 0) {
            return onExhausted(requestedParticles, evictionCallback);
        }

        Integer granted = switch (policy) {
            case REJECT -> requestedParticles <= remaining ? requestedParticles : null;
            case CLAMP -> Math.min(requestedParticles, remaining);
            case EVICT_OLDEST -> requestedParticles <= remaining
                ? requestedParticles
                : allocateWithEviction(requestedParticles, evictionCallback);
        };

        if (granted == null || granted <= 0) {
            rejectedThisFrame.incrementAndGet();
            return null;
        }
        if (granted < requestedParticles) {
            clampedThisFrame.incrementAndGet();
        }

        usedBudget.addAndGet(granted);
        VfxBudgetAllocation allocation = new VfxBudgetAllocation(granted, nextId.getAndIncrement());
        activeAllocations.put(allocation.allocationId(), allocation);
        return allocation;
    }

    public synchronized void release(int allocationId) {
        VfxBudgetAllocation alloc = activeAllocations.remove(allocationId);
        if (alloc != null) {
            usedBudget.addAndGet(-alloc.allocatedParticles());
        }
    }

    public synchronized VfxBudgetStats stats() {
        return new VfxBudgetStats(
            totalBudget,
            usedBudget.get(),
            totalBudget - usedBudget.get(),
            activeAllocations.size(),
            rejectedThisFrame.get(),
            clampedThisFrame.get(),
            evictedThisFrame.get()
        );
    }

    public int totalBudget() {
        return totalBudget;
    }

    public VfxBudgetPolicy policy() {
        return policy;
    }

    private VfxBudgetAllocation onExhausted(int requestedParticles, Consumer<Integer> evictionCallback) {
        return switch (policy) {
            case REJECT -> {
                rejectedThisFrame.incrementAndGet();
                yield null;
            }
            case CLAMP -> {
                rejectedThisFrame.incrementAndGet();
                yield null;
            }
            case EVICT_OLDEST -> {
                if (activeAllocations.isEmpty()) {
                    rejectedThisFrame.incrementAndGet();
                    yield null;
                }
                evictOldest(evictionCallback);
                yield allocate(requestedParticles, evictionCallback);
            }
        };
    }

    private Integer allocateWithEviction(int requestedParticles, Consumer<Integer> evictionCallback) {
        int remaining = totalBudget - usedBudget.get();
        while (remaining < requestedParticles && !activeAllocations.isEmpty()) {
            evictOldest(evictionCallback);
            remaining = totalBudget - usedBudget.get();
        }
        return requestedParticles <= remaining ? requestedParticles : null;
    }

    private void evictOldest(Consumer<Integer> evictionCallback) {
        if (activeAllocations.isEmpty()) {
            return;
        }
        Map.Entry<Integer, VfxBudgetAllocation> oldest = activeAllocations.entrySet().iterator().next();
        int allocationId = oldest.getKey();
        evictedThisFrame.incrementAndGet();
        evictionCallback.accept(allocationId);
        release(allocationId);
    }
}
