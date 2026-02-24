package org.dynamisvfx.core.serial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.dynamisvfx.api.ParticleEmitterDescriptor;

public final class EffectSerializer {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private EffectSerializer() {
    }

    public static String toJson(ParticleEmitterDescriptor descriptor) {
        try {
            return MAPPER.writeValueAsString(descriptor);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to serialize descriptor to JSON", e);
        }
    }

    public static ParticleEmitterDescriptor fromJson(String json) {
        try {
            return MAPPER.readValue(json, ParticleEmitterDescriptor.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Unable to deserialize descriptor from JSON", e);
        }
    }
}
