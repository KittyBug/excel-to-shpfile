package com.dataconversion.analyzer;

import com.dataconversion.model.ColumnMapping;
import com.dataconversion.model.GeometryType;
import com.dataconversion.util.CoordinateMatchingException;
import com.dataconversion.util.CoordinatePatterns;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Matches coordinate columns using 3-tier pattern matching algorithm.
 * Priority 1: Exact match
 * Priority 2: Contains match
 * Priority 3: Regex match
 */
public class CoordinateColumnMatcher {
    private static final Logger logger = LoggerFactory.getLogger(CoordinateColumnMatcher.class);

    /**
     * Matches coordinate columns based on geometry type.
     *
     * @param columnNames Array of column names
     * @param geometryType The detected geometry type
     * @return ColumnMapping with matched column indices
     * @throws CoordinateMatchingException If required columns cannot be found
     */
    public ColumnMapping matchCoordinateColumns(String[] columnNames, GeometryType geometryType)
            throws CoordinateMatchingException {

        ColumnMapping mapping = new ColumnMapping(columnNames, geometryType);

        if (geometryType == GeometryType.POINT) {
            matchPointColumns(columnNames, mapping);
        } else if (geometryType == GeometryType.LINESTRING) {
            matchLineStringColumns(columnNames, mapping);
        }

        if (!mapping.isValid()) {
            throw new CoordinateMatchingException(
                "无法找到所有必需的坐标列。可用列: " + Arrays.toString(columnNames) +
                ". 几何类型: " + geometryType
            );
        }

        logger.info("坐标列匹配成功: {}", mapping);
        return mapping;
    }

    /**
     * Match columns for POINT geometry (single X/Y pair).
     */
    private void matchPointColumns(String[] columnNames, ColumnMapping mapping)
            throws CoordinateMatchingException {

        Integer xIndex = findColumn(columnNames,
            CoordinatePatterns.EXACT_X_PATTERNS,
            CoordinatePatterns.CONTAINS_X_KEYWORDS,
            CoordinatePatterns.REGEX_X_PATTERN);

        Integer yIndex = findColumn(columnNames,
            CoordinatePatterns.EXACT_Y_PATTERNS,
            CoordinatePatterns.CONTAINS_Y_KEYWORDS,
            CoordinatePatterns.REGEX_Y_PATTERN);

        if (xIndex == null) {
            throw new CoordinateMatchingException(
                "无法找到X/经度列。可用列: " + Arrays.toString(columnNames)
            );
        }

        if (yIndex == null) {
            throw new CoordinateMatchingException(
                "无法找到Y/纬度列。可用列: " + Arrays.toString(columnNames)
            );
        }

        mapping.setPointCoordinates(xIndex, yIndex);
    }

    /**
     * Match columns for LINESTRING geometry (start X/Y and end X/Y).
     */
    private void matchLineStringColumns(String[] columnNames, ColumnMapping mapping)
            throws CoordinateMatchingException {

        Integer startXIndex = findColumn(columnNames,
            CoordinatePatterns.EXACT_START_X_PATTERNS,
            CoordinatePatterns.CONTAINS_START_KEYWORDS,
            CoordinatePatterns.REGEX_START_PATTERN,
            true, true);

        Integer startYIndex = findColumn(columnNames,
            CoordinatePatterns.EXACT_START_Y_PATTERNS,
            CoordinatePatterns.CONTAINS_START_KEYWORDS,
            CoordinatePatterns.REGEX_START_PATTERN,
            true, false);

        Integer endXIndex = findColumn(columnNames,
            CoordinatePatterns.EXACT_END_X_PATTERNS,
            CoordinatePatterns.CONTAINS_END_KEYWORDS,
            CoordinatePatterns.REGEX_END_PATTERN,
            false, true);

        Integer endYIndex = findColumn(columnNames,
            CoordinatePatterns.EXACT_END_Y_PATTERNS,
            CoordinatePatterns.CONTAINS_END_KEYWORDS,
            CoordinatePatterns.REGEX_END_PATTERN,
            false, false);

        if (startXIndex == null || startYIndex == null || endXIndex == null || endYIndex == null) {
            throw new CoordinateMatchingException(
                "无法找到起止点坐标列（需要：起点X, 起点Y, 终点X, 终点Y）。可用列: " + Arrays.toString(columnNames)
            );
        }

        mapping.setLineStringCoordinates(startXIndex, startYIndex, endXIndex, endYIndex);
    }

    /**
     * Find column index using 3-tier matching (for POINT mode).
     */
    private Integer findColumn(String[] columnNames, String[] exactPatterns,
                               String[] containsKeywords, java.util.regex.Pattern regexPattern) {
        return findColumn(columnNames, exactPatterns, containsKeywords, regexPattern, false, false);
    }

    /**
     * Find column index using 3-tier matching with additional filters for LINESTRING mode.
     *
     * @param needsStartKeyword If true, column must contain start keyword
     * @param needsXKeyword If true, column must contain X/lon keyword; if false, must contain Y/lat keyword
     */
    private Integer findColumn(String[] columnNames, String[] exactPatterns,
                               String[] containsKeywords, java.util.regex.Pattern regexPattern,
                               boolean needsStartKeyword, boolean needsXKeyword) {

        // Priority 1: Exact match
        for (int i = 0; i < columnNames.length; i++) {
            if (isColumnPlaceholder(columnNames[i])) continue;
            if (CoordinatePatterns.matchesExact(columnNames[i], exactPatterns)) {
                return i;
            }
        }

        // Priority 2: Contains match
        for (int i = 0; i < columnNames.length; i++) {
            if (isColumnPlaceholder(columnNames[i])) continue;
            String normalized = columnNames[i].trim().toLowerCase();

            // Check if contains required keywords
            if (CoordinatePatterns.containsKeyword(columnNames[i], containsKeywords)) {
                // For LINESTRING mode, also check X/Y keyword
                if (needsStartKeyword || needsXKeyword) {
                    boolean hasXKeyword = containsXKeyword(normalized);
                    boolean hasYKeyword = containsYKeyword(normalized);

                    if (needsXKeyword && hasXKeyword) {
                        return i;
                    } else if (!needsXKeyword && hasYKeyword) {
                        return i;
                    }
                } else {
                    return i;
                }
            }
        }

        // Priority 3: Regex match
        for (int i = 0; i < columnNames.length; i++) {
            if (isColumnPlaceholder(columnNames[i])) continue;
            if (CoordinatePatterns.matchesRegex(columnNames[i], regexPattern)) {
                return i;
            }
        }

        return null;
    }

    /**
     * Returns true if column name is a placeholder generated for merged/empty cells (e.g., "Column0").
     */
    private boolean isColumnPlaceholder(String columnName) {
        return columnName != null && columnName.startsWith("Column");
    }

    /**
     * Check if column name contains X/longitude keywords.
     */
    private boolean containsXKeyword(String normalizedColumnName) {
        return normalizedColumnName.contains("x") ||
               normalizedColumnName.contains("lon") ||
               normalizedColumnName.contains("lng") ||
               normalizedColumnName.contains("经");
    }

    /**
     * Check if column name contains Y/latitude keywords.
     */
    private boolean containsYKeyword(String normalizedColumnName) {
        return normalizedColumnName.contains("y") ||
               normalizedColumnName.contains("lat") ||
               normalizedColumnName.contains("纬");
    }
}
