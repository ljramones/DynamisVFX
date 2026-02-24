package org.dynamisvfx.core.serial;

import org.dynamisvfx.api.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class EffectBinarySerializer {
    private static final int MAGIC = 0x56465831; // VFX1
    private static final short VERSION = 1;

    private EffectBinarySerializer() {
    }

    public static byte[] toBytes(ParticleEmitterDescriptor descriptor) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            DataOutputStream out = new DataOutputStream(bos);

            out.writeInt(MAGIC);
            out.writeShort(VERSION);

            writeString(out, descriptor.id());
            writeShape(out, descriptor.shape());
            writeRate(out, descriptor.rate());
            writeInit(out, descriptor.init());
            writeForces(out, descriptor.forces());
            writeRenderer(out, descriptor.renderer());
            writeLod(out, descriptor.lod());
            writePhysics(out, descriptor.physics());

            out.flush();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to serialize descriptor to bytes", e);
        }
    }

    public static ParticleEmitterDescriptor fromBytes(byte[] bytes) {
        try {
            DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));
            int magic = in.readInt();
            if (magic != MAGIC) {
                throw new IllegalArgumentException("Invalid binary descriptor magic");
            }
            short version = in.readShort();
            if (version != VERSION) {
                throw new IllegalArgumentException("Unsupported binary descriptor version: " + version);
            }

            String id = readString(in);
            EmitterShapeDescriptor shape = readShape(in);
            EmissionRateDescriptor rate = readRate(in);
            ParticleInitDescriptor init = readInit(in);
            List<ForceDescriptor> forces = readForces(in);
            RendererDescriptor renderer = readRenderer(in);
            LodDescriptor lod = readLod(in);
            PhysicsHandoffDescriptor physics = readPhysics(in);

            return new ParticleEmitterDescriptor(id, shape, rate, init, forces, renderer, lod, physics);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to deserialize descriptor from bytes", e);
        }
    }

    private static void writeShape(DataOutputStream out, EmitterShapeDescriptor shape) throws IOException {
        out.writeInt(shape.type().ordinal());
        writeFloatArray(out, shape.dimensions());
        writeString(out, shape.sourceMeshId());
        writeString(out, shape.sourceSplineId());
    }

    private static EmitterShapeDescriptor readShape(DataInputStream in) throws IOException {
        EmitterShapeType type = EmitterShapeType.values()[in.readInt()];
        float[] dimensions = readFloatArray(in);
        String meshId = readString(in);
        String splineId = readString(in);
        return new EmitterShapeDescriptor(type, dimensions, meshId, splineId);
    }

    private static void writeRate(DataOutputStream out, EmissionRateDescriptor rate) throws IOException {
        out.writeInt(rate.mode().ordinal());
        out.writeFloat(rate.particlesPerSecond());
        out.writeInt(rate.burstCount());
        writeString(out, rate.eventKey());
    }

    private static EmissionRateDescriptor readRate(DataInputStream in) throws IOException {
        EmissionMode mode = EmissionMode.values()[in.readInt()];
        float pps = in.readFloat();
        int burstCount = in.readInt();
        String eventKey = readString(in);
        return new EmissionRateDescriptor(mode, pps, burstCount, eventKey);
    }

    private static void writeInit(DataOutputStream out, ParticleInitDescriptor init) throws IOException {
        out.writeFloat(init.lifetimeMinSeconds());
        out.writeFloat(init.lifetimeMaxSeconds());
        out.writeFloat(init.speedMin());
        out.writeFloat(init.speedMax());
        out.writeFloat(init.sizeMin());
        out.writeFloat(init.sizeMax());
        writeFloatArray(out, init.initialDirection());
        writeFloatArray(out, init.colorRgb());
        out.writeFloat(init.alpha());
    }

    private static ParticleInitDescriptor readInit(DataInputStream in) throws IOException {
        float lifeMin = in.readFloat();
        float lifeMax = in.readFloat();
        float speedMin = in.readFloat();
        float speedMax = in.readFloat();
        float sizeMin = in.readFloat();
        float sizeMax = in.readFloat();
        float[] direction = readFloatArray(in);
        float[] color = readFloatArray(in);
        float alpha = in.readFloat();
        return new ParticleInitDescriptor(lifeMin, lifeMax, speedMin, speedMax, sizeMin, sizeMax, direction, color, alpha);
    }

    private static void writeForces(DataOutputStream out, List<ForceDescriptor> forces) throws IOException {
        out.writeInt(forces.size());
        for (ForceDescriptor force : forces) {
            out.writeInt(force.type().ordinal());
            out.writeFloat(force.strength());
            writeFloatArray(out, force.direction());
            writeNoiseConfig(out, force.noiseConfig());
        }
    }

    private static List<ForceDescriptor> readForces(DataInputStream in) throws IOException {
        int size = in.readInt();
        List<ForceDescriptor> forces = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ForceType type = ForceType.values()[in.readInt()];
            float strength = in.readFloat();
            float[] direction = readFloatArray(in);
            NoiseForceConfig config = readNoiseConfig(in);
            forces.add(new ForceDescriptor(type, strength, direction, config));
        }
        return forces;
    }

    private static void writeRenderer(DataOutputStream out, RendererDescriptor renderer) throws IOException {
        out.writeInt(renderer.type().ordinal());
        out.writeInt(renderer.blendMode().ordinal());
        writeString(out, renderer.textureAtlasId());
        out.writeInt(renderer.frameCount());
        out.writeBoolean(renderer.softParticles());
        out.writeBoolean(renderer.lightEmitting());
    }

    private static RendererDescriptor readRenderer(DataInputStream in) throws IOException {
        RendererType type = RendererType.values()[in.readInt()];
        BlendMode blend = BlendMode.values()[in.readInt()];
        String textureAtlasId = readString(in);
        int frameCount = in.readInt();
        boolean softParticles = in.readBoolean();
        boolean lightEmitting = in.readBoolean();
        return new RendererDescriptor(type, blend, textureAtlasId, frameCount, softParticles, lightEmitting);
    }

    private static void writeLod(DataOutputStream out, LodDescriptor lod) throws IOException {
        if (lod == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        List<LodTier> tiers = lod.tiers();
        out.writeInt(tiers.size());
        for (LodTier tier : tiers) {
            out.writeFloat(tier.minDistance());
            out.writeFloat(tier.maxDistance());
            out.writeFloat(tier.simulationScale());
            out.writeFloat(tier.emissionScale());
        }
        out.writeBoolean(lod.allowSleeping());
        out.writeFloat(lod.sleepingDistance());
    }

    private static LodDescriptor readLod(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        int tierCount = in.readInt();
        List<LodTier> tiers = new ArrayList<>(tierCount);
        for (int i = 0; i < tierCount; i++) {
            tiers.add(new LodTier(in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat()));
        }
        boolean allowSleeping = in.readBoolean();
        float sleepingDistance = in.readFloat();
        return new LodDescriptor(tiers, allowSleeping, sleepingDistance);
    }

    private static void writePhysics(DataOutputStream out, PhysicsHandoffDescriptor physics) throws IOException {
        if (physics == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeBoolean(physics.enabled());
        out.writeFloat(physics.speedThreshold());
        writeString(out, physics.meshId());
        writeString(out, physics.materialTag());
        out.writeFloat(physics.mass());
    }

    private static PhysicsHandoffDescriptor readPhysics(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        boolean enabled = in.readBoolean();
        float speedThreshold = in.readFloat();
        String meshId = readString(in);
        String materialTag = readString(in);
        float mass = in.readFloat();
        return new PhysicsHandoffDescriptor(enabled, speedThreshold, meshId, materialTag, mass);
    }

    private static void writeNoiseConfig(DataOutputStream out, NoiseForceConfig config) throws IOException {
        if (config == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeFloat(config.frequency());
        out.writeFloat(config.amplitude());
        out.writeInt(config.octaves());
        out.writeFloat(config.lacunarity());
        out.writeFloat(config.gain());
        out.writeFloat(config.timeScale());
        out.writeInt(config.seed());
    }

    private static NoiseForceConfig readNoiseConfig(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        float frequency = in.readFloat();
        float amplitude = in.readFloat();
        int octaves = in.readInt();
        float lacunarity = in.readFloat();
        float gain = in.readFloat();
        float timeScale = in.readFloat();
        int seed = in.readInt();
        return new NoiseForceConfig(frequency, amplitude, octaves, lacunarity, gain, timeScale, seed);
    }

    private static void writeString(DataOutputStream out, String value) throws IOException {
        if (value == null) {
            out.writeBoolean(false);
            return;
        }
        out.writeBoolean(true);
        out.writeUTF(value);
    }

    private static String readString(DataInputStream in) throws IOException {
        if (!in.readBoolean()) {
            return null;
        }
        return in.readUTF();
    }

    private static void writeFloatArray(DataOutputStream out, float[] values) throws IOException {
        if (values == null) {
            out.writeInt(-1);
            return;
        }
        out.writeInt(values.length);
        for (float value : values) {
            out.writeFloat(value);
        }
    }

    private static float[] readFloatArray(DataInputStream in) throws IOException {
        int length = in.readInt();
        if (length < 0) {
            return null;
        }
        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            values[i] = in.readFloat();
        }
        return values;
    }
}
