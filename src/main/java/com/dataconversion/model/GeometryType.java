package com.dataconversion.model;

/**
 * Enum for geometry types supported by the converter.
 */
public enum GeometryType {
    POINT("Point geometry - single coordinate pair"),
    LINESTRING("LineString geometry - start and end point pairs");

    private final String description;

    GeometryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}