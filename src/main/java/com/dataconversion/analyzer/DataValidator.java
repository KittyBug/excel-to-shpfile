package com.dataconversion.analyzer;

import com.dataconversion.model.ColumnMapping;
import com.dataconversion.model.GeometryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Validates coordinate data including type checking and range validation.
 * Supports strict mode (fail fast) and lenient mode (skip invalid rows).
 */
public class DataValidator {
    private static final Logger logger = LoggerFactory.getLogger(DataValidator.class);

    private static final double MIN_LONGITUDE = -180.0;
    private static final double MAX_LONGITUDE = 180.0;
    private static final double MIN_LATITUDE = -90.0;
    private static final double MAX_LATITUDE = 90.0;

    private final boolean strictMode;
    private final boolean skipInvalidRows;

    public DataValidator(boolean strictMode, boolean skipInvalidRows) {
        this.strictMode = strictMode;
        this.skipInvalidRows = skipInvalidRows;
    }

    /**
     * Validates all data rows and returns validation result.
     *
     * @param dataRows List of data rows
     * @param mapping Column mapping with coordinate indices
     * @return ValidationResult with valid rows and statistics
     */
    public ValidationResult validate(List<Map<String, Object>> dataRows, ColumnMapping mapping) {
        List<Map<String, Object>> validRows = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int invalidRowCount = 0;

        for (int i = 0; i < dataRows.size(); i++) {
            Map<String, Object> row = dataRows.get(i);
            int rowNumber = i + 1;

            try {
                if (mapping.getGeometryType() == GeometryType.POINT) {
                    validatePointRow(row, mapping, rowNumber);
                } else if (mapping.getGeometryType() == GeometryType.LINESTRING) {
                    validateLineStringRow(row, mapping, rowNumber);
                }
                validRows.add(row);
            } catch (ValidationException e) {
                invalidRowCount++;
                String errorMsg = "行 " + rowNumber + ": " + e.getMessage();
                errors.add(errorMsg);

                if (strictMode) {
                    logger.error(errorMsg);
                    throw new RuntimeException(errorMsg, e);
                } else {
                    logger.warn(errorMsg);
                    if (!skipInvalidRows) {
                        validRows.add(row);
                    }
                }
            }
        }

        logger.info("数据验证完成: {} 行有效, {} 行无效", validRows.size(), invalidRowCount);

        return new ValidationResult(validRows, invalidRowCount, errors);
    }

    /**
     * Validates a single POINT row.
     */
    private void validatePointRow(Map<String, Object> row, ColumnMapping mapping, int rowNumber)
            throws ValidationException {

        String xColumnName = mapping.getXColumnName();
        String yColumnName = mapping.getYColumnName();

        Object xValue = row.get(xColumnName);
        Object yValue = row.get(yColumnName);

        double x = parseCoordinate(xValue, xColumnName, rowNumber);
        double y = parseCoordinate(yValue, yColumnName, rowNumber);

        validateLongitude(x, xColumnName, rowNumber);
        validateLatitude(y, yColumnName, rowNumber);
    }

    /**
     * Validates a single LINESTRING row.
     */
    private void validateLineStringRow(Map<String, Object> row, ColumnMapping mapping, int rowNumber)
            throws ValidationException {

        String startXName = mapping.getStartXColumnName();
        String startYName = mapping.getStartYColumnName();
        String endXName = mapping.getEndXColumnName();
        String endYName = mapping.getEndYColumnName();

        Object startXValue = row.get(startXName);
        Object startYValue = row.get(startYName);
        Object endXValue = row.get(endXName);
        Object endYValue = row.get(endYName);

        double startX = parseCoordinate(startXValue, startXName, rowNumber);
        double startY = parseCoordinate(startYValue, startYName, rowNumber);
        double endX = parseCoordinate(endXValue, endXName, rowNumber);
        double endY = parseCoordinate(endYValue, endYName, rowNumber);

        validateLongitude(startX, startXName, rowNumber);
        validateLatitude(startY, startYName, rowNumber);
        validateLongitude(endX, endXName, rowNumber);
        validateLatitude(endY, endYName, rowNumber);
    }

    /**
     * Parses coordinate value from cell data.
     */
    private double parseCoordinate(Object value, String columnName, int rowNumber)
            throws ValidationException {

        if (value == null) {
            throw new ValidationException("列 '" + columnName + "' 值为空");
        }

        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }

        if (value instanceof String) {
            String strValue = ((String) value).trim();
            if (strValue.isEmpty()) {
                throw new ValidationException("列 '" + columnName + "' 值为空");
            }

            try {
                return Double.parseDouble(strValue);
            } catch (NumberFormatException e) {
                throw new ValidationException("列 '" + columnName + "' 不是有效的数字: " + strValue);
            }
        }

        throw new ValidationException("列 '" + columnName + "' 类型无效: " + value.getClass().getSimpleName());
    }

    /**
     * Validates longitude is within valid range.
     */
    private void validateLongitude(double value, String columnName, int rowNumber)
            throws ValidationException {

        if (value < MIN_LONGITUDE || value > MAX_LONGITUDE) {
            throw new ValidationException(
                "经度超出范围 [" + MIN_LONGITUDE + ", " + MAX_LONGITUDE + "]: " +
                columnName + " = " + value
            );
        }
    }

    /**
     * Validates latitude is within valid range.
     */
    private void validateLatitude(double value, String columnName, int rowNumber)
            throws ValidationException {

        if (value < MIN_LATITUDE || value > MAX_LATITUDE) {
            throw new ValidationException(
                "纬度超出范围 [" + MIN_LATITUDE + ", " + MAX_LATITUDE + "]: " +
                columnName + " = " + value
            );
        }
    }

    /**
     * Validation exception for coordinate errors.
     */
    private static class ValidationException extends Exception {
        public ValidationException(String message) {
            super(message);
        }
    }

    /**
     * Result of validation process.
     */
    public static class ValidationResult {
        private final List<Map<String, Object>> validRows;
        private final int invalidRowCount;
        private final List<String> errors;

        public ValidationResult(List<Map<String, Object>> validRows, int invalidRowCount, List<String> errors) {
            this.validRows = validRows;
            this.invalidRowCount = invalidRowCount;
            this.errors = errors;
        }

        public List<Map<String, Object>> getValidRows() {
            return validRows;
        }

        public int getInvalidRowCount() {
            return invalidRowCount;
        }

        public List<String> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return invalidRowCount > 0;
        }
    }
}
