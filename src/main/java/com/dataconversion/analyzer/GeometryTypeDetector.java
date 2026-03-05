package com.dataconversion.analyzer;

import com.dataconversion.model.GeometryType;
import com.dataconversion.util.CoordinatePatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Detects geometry type (POINT or LINESTRING) based on column name patterns.
 * LINESTRING mode is detected when start/end point keywords are found.
 */
public class GeometryTypeDetector {
    private static final Logger logger = LoggerFactory.getLogger(GeometryTypeDetector.class);

    /**
     * Detects the geometry type based on column names.
     *
     * @param columnNames Array of column names from Excel header
     * @return GeometryType.LINESTRING if start/end keywords found, otherwise GeometryType.POINT
     */
    public GeometryType detectGeometryType(String[] columnNames) {
        boolean hasStartKeyword = false;
        boolean hasEndKeyword = false;

        for (String columnName : columnNames) {
            if (columnName == null || columnName.trim().isEmpty()) {
                continue;
            }

            String normalized = columnName.trim().toLowerCase();

            // Check for start point keywords
            if (containsStartKeyword(normalized)) {
                hasStartKeyword = true;
            }

            // Check for end point keywords
            if (containsEndKeyword(normalized)) {
                hasEndKeyword = true;
            }

            // Early exit if both found
            if (hasStartKeyword && hasEndKeyword) {
                break;
            }
        }

        GeometryType detectedType;
        if (hasStartKeyword && hasEndKeyword) {
            detectedType = GeometryType.LINESTRING;
            logger.info("检测到LineString几何类型（起止点模式）");
        } else {
            detectedType = GeometryType.POINT;
            logger.info("检测到Point几何类型（单点模式）");
        }

        return detectedType;
    }

    /**
     * Check if column name contains start point keywords.
     */
    private boolean containsStartKeyword(String normalizedColumnName) {
        // Check exact patterns
        for (String pattern : CoordinatePatterns.EXACT_START_X_PATTERNS) {
            if (normalizedColumnName.equals(pattern.toLowerCase())) {
                return true;
            }
        }
        for (String pattern : CoordinatePatterns.EXACT_START_Y_PATTERNS) {
            if (normalizedColumnName.equals(pattern.toLowerCase())) {
                return true;
            }
        }

        // Check contains keywords
        for (String keyword : CoordinatePatterns.CONTAINS_START_KEYWORDS) {
            if (normalizedColumnName.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // Check regex
        return CoordinatePatterns.REGEX_START_PATTERN.matcher(normalizedColumnName).matches();
    }

    /**
     * Check if column name contains end point keywords.
     */
    private boolean containsEndKeyword(String normalizedColumnName) {
        // Check exact patterns
        for (String pattern : CoordinatePatterns.EXACT_END_X_PATTERNS) {
            if (normalizedColumnName.equals(pattern.toLowerCase())) {
                return true;
            }
        }
        for (String pattern : CoordinatePatterns.EXACT_END_Y_PATTERNS) {
            if (normalizedColumnName.equals(pattern.toLowerCase())) {
                return true;
            }
        }

        // Check contains keywords
        for (String keyword : CoordinatePatterns.CONTAINS_END_KEYWORDS) {
            if (normalizedColumnName.contains(keyword.toLowerCase())) {
                return true;
            }
        }

        // Check regex
        return CoordinatePatterns.REGEX_END_PATTERN.matcher(normalizedColumnName).matches();
    }
}
