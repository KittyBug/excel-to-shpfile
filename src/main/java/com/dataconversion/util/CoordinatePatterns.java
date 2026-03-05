package com.dataconversion.util;

import java.util.regex.Pattern;

/**
 * Coordinate column name patterns for 3-tier matching algorithm.
 * Supports both Chinese and English column names.
 */
public class CoordinatePatterns {

    // Priority 1: Exact match patterns (case-insensitive)
    public static final String[] EXACT_X_PATTERNS = {
        "经度", "longitude", "lon", "lng", "x"
    };

    public static final String[] EXACT_Y_PATTERNS = {
        "纬度", "latitude", "lat", "y"
    };

    public static final String[] EXACT_START_X_PATTERNS = {
        "起点经度", "起点lon", "起点lng", "起点x",
        "start_lon", "start_lng", "start_x",
        "startlon", "startlng", "startx",
        "from_lon", "from_lng", "from_x"
    };

    public static final String[] EXACT_START_Y_PATTERNS = {
        "起点纬度", "起点lat", "起点y",
        "start_lat", "start_y",
        "startlat", "starty",
        "from_lat", "from_y"
    };

    public static final String[] EXACT_END_X_PATTERNS = {
        "终点经度", "终点lon", "终点lng", "终点x",
        "end_lon", "end_lng", "end_x",
        "endlon", "endlng", "endx",
        "to_lon", "to_lng", "to_x"
    };

    public static final String[] EXACT_END_Y_PATTERNS = {
        "终点纬度", "终点lat", "终点y",
        "end_lat", "end_y",
        "endlat", "endy",
        "to_lat", "to_y"
    };

    // Priority 2: Contains match keywords
    public static final String[] CONTAINS_X_KEYWORDS = {
        "x坐标", "x_coord", "x coord", "东经", "经度"
    };

    public static final String[] CONTAINS_Y_KEYWORDS = {
        "y坐标", "y_coord", "y coord", "北纬", "纬度"
    };

    public static final String[] CONTAINS_START_KEYWORDS = {
        "起点", "start", "起始", "from", "始"
    };

    public static final String[] CONTAINS_END_KEYWORDS = {
        "终点", "end", "结束", "to", "终"
    };

    // Priority 3: Regex patterns
    public static final Pattern REGEX_X_PATTERN = Pattern.compile(
        ".*[xX].*|.*lon.*|.*lng.*|.*经.*",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern REGEX_Y_PATTERN = Pattern.compile(
        ".*[yY].*|.*lat.*|.*纬.*",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern REGEX_START_PATTERN = Pattern.compile(
        ".*起.*点.*|.*start.*|.*from.*|.*始.*",
        Pattern.CASE_INSENSITIVE
    );

    public static final Pattern REGEX_END_PATTERN = Pattern.compile(
        ".*终.*点.*|.*end.*|.*to.*|.*终.*",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Check if column name matches exact pattern (case-insensitive).
     */
    public static boolean matchesExact(String columnName, String[] patterns) {
        String normalized = columnName.trim().toLowerCase();
        for (String pattern : patterns) {
            if (normalized.equals(pattern.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if column name contains any keyword (case-insensitive).
     */
    public static boolean containsKeyword(String columnName, String[] keywords) {
        String normalized = columnName.trim().toLowerCase();
        for (String keyword : keywords) {
            if (normalized.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Check if column name matches regex pattern.
     */
    public static boolean matchesRegex(String columnName, Pattern pattern) {
        return pattern.matcher(columnName.trim()).matches();
    }
}
