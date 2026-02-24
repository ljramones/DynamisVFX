package org.dynamisvfx.vulkan.descriptor;

import org.lwjgl.vulkan.VK10;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

public final class VulkanVfxDescriptorSetLayout {
    public record DescriptorBinding(int binding, int descriptorType, int stageFlags) {}

    private static final AtomicLong HANDLE_IDS = new AtomicLong(1000L);

    private final long set0Layout;
    private final long set1Layout;
    private final long set2Layout;
    private final long set3Layout;

    private final List<DescriptorBinding> set0Bindings;
    private final List<DescriptorBinding> set1Bindings;
    private final List<DescriptorBinding> set2Bindings;
    private final List<DescriptorBinding> set3Bindings;

    private VulkanVfxDescriptorSetLayout(
        long set0Layout,
        long set1Layout,
        long set2Layout,
        long set3Layout,
        List<DescriptorBinding> set0Bindings,
        List<DescriptorBinding> set1Bindings,
        List<DescriptorBinding> set2Bindings,
        List<DescriptorBinding> set3Bindings
    ) {
        this.set0Layout = set0Layout;
        this.set1Layout = set1Layout;
        this.set2Layout = set2Layout;
        this.set3Layout = set3Layout;
        this.set0Bindings = set0Bindings;
        this.set1Bindings = set1Bindings;
        this.set2Bindings = set2Bindings;
        this.set3Bindings = set3Bindings;
    }

    public static VulkanVfxDescriptorSetLayout create(long device) {
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }

        List<DescriptorBinding> set0 = List.of(
            new DescriptorBinding(
                0,
                VK10.VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
                VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT | VK10.VK_SHADER_STAGE_FRAGMENT_BIT
            )
        );

        List<DescriptorBinding> set1 = List.of(
            new DescriptorBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT)
        );

        List<DescriptorBinding> set2 = List.of(
            new DescriptorBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT),
            new DescriptorBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT),
            new DescriptorBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT),
            new DescriptorBinding(3, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT),
            new DescriptorBinding(4, VK10.VK_DESCRIPTOR_TYPE_STORAGE_IMAGE, VK10.VK_SHADER_STAGE_COMPUTE_BIT)
        );

        List<DescriptorBinding> set3 = List.of(
            new DescriptorBinding(0, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(1, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(2, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT),
            new DescriptorBinding(3, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK10.VK_SHADER_STAGE_FRAGMENT_BIT),
            new DescriptorBinding(4, VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK10.VK_SHADER_STAGE_FRAGMENT_BIT),
            new DescriptorBinding(5, VK10.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER, VK10.VK_SHADER_STAGE_COMPUTE_BIT | VK10.VK_SHADER_STAGE_VERTEX_BIT)
        );

        return new VulkanVfxDescriptorSetLayout(
            HANDLE_IDS.getAndIncrement(),
            HANDLE_IDS.getAndIncrement(),
            HANDLE_IDS.getAndIncrement(),
            HANDLE_IDS.getAndIncrement(),
            set0,
            set1,
            set2,
            set3
        );
    }

    public void destroy(long device) {
        long ignored = device;
        if (ignored == Long.MIN_VALUE) {
            throw new IllegalArgumentException("Invalid device handle");
        }
    }

    public long set0Layout() {
        return set0Layout;
    }

    public long set1Layout() {
        return set1Layout;
    }

    public long set2Layout() {
        return set2Layout;
    }

    public long set3Layout() {
        return set3Layout;
    }

    public List<DescriptorBinding> set0Bindings() {
        return set0Bindings;
    }

    public List<DescriptorBinding> set1Bindings() {
        return set1Bindings;
    }

    public List<DescriptorBinding> set2Bindings() {
        return set2Bindings;
    }

    public List<DescriptorBinding> set3Bindings() {
        return set3Bindings;
    }
}
