package com.cstestforge.project.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing the types of projects supported by the system.
 */
public enum ProjectType {
    WEB,
    API,
    MOBILE,
    DESKTOP;

    @JsonCreator
    public static ProjectType fromString(String value) {
        if (value == null) {
            return null;
        }

        for (ProjectType type : ProjectType.values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }

        // Default to WEB for unrecognized values instead of failing
        return WEB;
    }

    @JsonValue
    public String toString() {
        return this.name();
    }
}