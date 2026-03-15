package org.dynamisengine.vfx.core.validate;

public record ValidationError(String field, String message, Severity severity) {
    public enum Severity {
        WARN,
        ERROR
    }
}
