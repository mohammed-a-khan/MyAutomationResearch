package com.cstestforge.project.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Enum representing project statuses
 */
public enum ProjectStatus {
    ACTIVE,
    ARCHIVED,
    DRAFT,
    DELETED;

    @JsonCreator
    public static ProjectStatus fromString(String value) {
        if (value == null) {
            return null;
        }

        for (ProjectStatus status : ProjectStatus.values()) {
            if (status.name().equalsIgnoreCase(value)) {
                return status;
            }
        }

        // Default to ACTIVE for unrecognized values instead of failing
        return ACTIVE;
    }

    @JsonValue
    public String toString() {
        return this.name();
    }
}