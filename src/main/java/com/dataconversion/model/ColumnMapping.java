package com.dataconversion.model;

/**
 * Mapping of coordinate columns for both POINT and LINESTRING geometries.
 * Supports single point mode (x, y) and start-end point mode (startX, startY, endX, endY).
 */
public class ColumnMapping {
    private final GeometryType geometryType;
    private final String[] columnNames;

    // For POINT geometry
    private Integer xColumnIndex;
    private Integer yColumnIndex;

    // For LINESTRING geometry
    private Integer startXColumnIndex;
    private Integer startYColumnIndex;
    private Integer endXColumnIndex;
    private Integer endYColumnIndex;

    public ColumnMapping(String[] columnNames, GeometryType geometryType) {
        this.columnNames = columnNames;
        this.geometryType = geometryType;
    }

    // POINT mode setters
    public void setPointCoordinates(int xIndex, int yIndex) {
        this.xColumnIndex = xIndex;
        this.yColumnIndex = yIndex;
    }

    // LINESTRING mode setters
    public void setLineStringCoordinates(int startXIndex, int startYIndex, int endXIndex, int endYIndex) {
        this.startXColumnIndex = startXIndex;
        this.startYColumnIndex = startYIndex;
        this.endXColumnIndex = endXIndex;
        this.endYColumnIndex = endYIndex;
    }

    public GeometryType getGeometryType() {
        return geometryType;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    // POINT mode getters
    public Integer getXColumnIndex() {
        return xColumnIndex;
    }

    public Integer getYColumnIndex() {
        return yColumnIndex;
    }

    public String getXColumnName() {
        return xColumnIndex != null ? columnNames[xColumnIndex] : null;
    }

    public String getYColumnName() {
        return yColumnIndex != null ? columnNames[yColumnIndex] : null;
    }

    // LINESTRING mode getters
    public Integer getStartXColumnIndex() {
        return startXColumnIndex;
    }

    public Integer getStartYColumnIndex() {
        return startYColumnIndex;
    }

    public Integer getEndXColumnIndex() {
        return endXColumnIndex;
    }

    public Integer getEndYColumnIndex() {
        return endYColumnIndex;
    }

    public String getStartXColumnName() {
        return startXColumnIndex != null ? columnNames[startXColumnIndex] : null;
    }

    public String getStartYColumnName() {
        return startYColumnIndex != null ? columnNames[startYColumnIndex] : null;
    }

    public String getEndXColumnName() {
        return endXColumnIndex != null ? columnNames[endXColumnIndex] : null;
    }

    public String getEndYColumnName() {
        return endYColumnIndex != null ? columnNames[endYColumnIndex] : null;
    }

    public boolean isValid() {
        if (geometryType == GeometryType.POINT) {
            return xColumnIndex != null && yColumnIndex != null;
        } else if (geometryType == GeometryType.LINESTRING) {
            return startXColumnIndex != null && startYColumnIndex != null
                && endXColumnIndex != null && endYColumnIndex != null;
        }
        return false;
    }

    @Override
    public String toString() {
        if (geometryType == GeometryType.POINT) {
            return "ColumnMapping{" +
                    "geometryType=" + geometryType +
                    ", x=" + getXColumnName() + " (index=" + xColumnIndex + ")" +
                    ", y=" + getYColumnName() + " (index=" + yColumnIndex + ")" +
                    '}';
        } else {
            return "ColumnMapping{" +
                    "geometryType=" + geometryType +
                    ", startX=" + getStartXColumnName() + " (index=" + startXColumnIndex + ")" +
                    ", startY=" + getStartYColumnName() + " (index=" + startYColumnIndex + ")" +
                    ", endX=" + getEndXColumnName() + " (index=" + endXColumnIndex + ")" +
                    ", endY=" + getEndYColumnName() + " (index=" + endYColumnIndex + ")" +
                    '}';
        }
    }
}
