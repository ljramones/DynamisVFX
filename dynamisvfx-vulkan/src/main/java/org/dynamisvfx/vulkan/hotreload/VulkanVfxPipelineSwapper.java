package org.dynamisvfx.vulkan.hotreload;

import org.dynamisvfx.api.BlendMode;
import org.dynamisvfx.api.RendererDescriptor;
import org.dynamisvfx.vulkan.descriptor.VulkanVfxDescriptorSetLayout;
import org.dynamisvfx.vulkan.renderer.VulkanVfxRendererPipelineUtil;
import org.dynamisvfx.vulkan.resources.VulkanVfxEffectResources;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class VulkanVfxPipelineSwapper {
    private final long device;
    private final long renderPass;
    private final VulkanVfxDescriptorSetLayout layout;

    private final ExecutorService backgroundThread =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "vfx-pipeline-rebuild");
            t.setDaemon(true);
            return t;
        });

    private final Map<Integer, Future<VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles>> pendingByEffect =
        new ConcurrentHashMap<>();
    private final Map<Integer, VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles> activeByEffect =
        new ConcurrentHashMap<>();
    private final Map<Integer, Long> pendingDestroyFrameByEffect = new ConcurrentHashMap<>();

    public VulkanVfxPipelineSwapper(long device, long renderPass, VulkanVfxDescriptorSetLayout layout) {
        this.device = device;
        this.renderPass = renderPass;
        this.layout = Objects.requireNonNull(layout, "layout");
    }

    public Future<VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles> submitRebuild(
        int effectId,
        RendererDescriptor newRenderer
    ) {
        Objects.requireNonNull(newRenderer, "newRenderer");
        Future<VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles> future = backgroundThread.submit(() -> {
            long[] setLayouts = new long[] {
                layout.set0Layout(),
                layout.set1Layout(),
                layout.set2Layout(),
                layout.set3Layout()
            };
            BlendMode blendMode = newRenderer.blendMode() == null ? BlendMode.ALPHA : newRenderer.blendMode();
            // Placeholder shader strings until renderer-specific hot-swap routing is fully wired.
            return VulkanVfxRendererPipelineUtil.create(
                device,
                renderPass,
                "#version 450\nvoid main(){gl_Position=vec4(0.0);}",
                "#version 450\nlayout(location=0) out vec4 outColor; void main(){outColor=vec4(1.0);}",
                setLayouts,
                0,
                blendMode
            );
        });
        pendingByEffect.put(effectId, future);
        return future;
    }

    public void pollAndSwap(
        VulkanVfxEffectResources resources,
        long frameIndex,
        int framesInFlight
    ) {
        Objects.requireNonNull(resources, "resources");
        if (framesInFlight <= 0) {
            throw new IllegalArgumentException("framesInFlight must be > 0");
        }
        int effectId = resources.handle().id();

        Future<VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles> pending = pendingByEffect.get(effectId);
        if (pending != null && pending.isDone()) {
            try {
                VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles rebuilt = pending.get();
                VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles old = activeByEffect.put(effectId, rebuilt);
                if (old != null) {
                    pendingDestroyFrameByEffect.put(effectId, frameIndex + framesInFlight);
                }
                pendingByEffect.remove(effectId);
            } catch (Exception ex) {
                pendingByEffect.remove(effectId);
            }
        }

        Long destroyAt = pendingDestroyFrameByEffect.get(effectId);
        if (destroyAt != null && frameIndex >= destroyAt) {
            VulkanVfxRendererPipelineUtil.VulkanRendererPipelineHandles old = activeByEffect.get(effectId);
            if (old != null) {
                VulkanVfxRendererPipelineUtil.destroy(device, old);
            }
            pendingDestroyFrameByEffect.remove(effectId);
        }
    }

    public void shutdown() {
        backgroundThread.shutdown();
    }
}
