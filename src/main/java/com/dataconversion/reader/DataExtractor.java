package com.dataconversion.reader;

import com.dataconversion.model.ExcelData;
import com.dataconversion.util.InputValidationException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Extracts data from Excel sheets after header detection.
 * Converts Excel rows into structured data maps with automatic type conversion.
 */
public class DataExtractor {
    private static final Logger logger = LoggerFactory.getLogger(DataExtractor.class);

    /**
     * Extracts all data rows from a sheet starting from the specified row index.
     *
     * @param sheet The Excel sheet
     * @param headerNames The header column names
     * @param dataStartRowIndex The row index where data starts (after headers)
     * @return ExcelData containing headers and data rows
     * @throws InputValidationException if no data is found
     */
    public ExcelData extractData(Sheet sheet, String[] headerNames, int dataStartRowIndex)
            throws InputValidationException {
        if (headerNames == null || headerNames.length == 0) {
            throw new InputValidationException("未提供列名");
        }

        List<Map<String, Object>> dataRows = new ArrayList<>();
        int validRowCount = 0;
        int invalidRowCount = 0;

        for (int rowIndex = dataStartRowIndex; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
            Row row = sheet.getRow(rowIndex);
            if (row == null) {
                continue;
            }

            Map<String, Object> rowData = extractRowData(row, headerNames);

            if (!isEmptyRow(rowData)) {
                dataRows.add(rowData);
                validRowCount++;
            } else {
                invalidRowCount++;
            }
        }

        if (dataRows.isEmpty()) {
            throw new InputValidationException("未找到任何有效数据行");
        }

        logger.info("提取数据完成: {} 行有效数据, {} 行空行", validRowCount, invalidRowCount);

        ExcelData excelData = new ExcelData(headerNames, dataRows);
        excelData.setSheetName(sheet.getSheetName());
        excelData.setHeaderRowIndex(dataStartRowIndex - 1);
        excelData.setTotalRows(validRowCount);

        return excelData;
    }

    /**
     * Extracts data from a single row into a map structure.
     */
    private Map<String, Object> extractRowData(Row row, String[] headerNames) {
        Map<String, Object> rowData = new LinkedHashMap<>();

        for (int colIndex = 0; colIndex < headerNames.length; colIndex++) {
            if (headerNames[colIndex].startsWith("Column")) continue;
            Cell cell = row.getCell(colIndex);
            Object value = getCellValue(cell);
            rowData.put(headerNames[colIndex], value);
        }

        return rowData;
    }

    /**
     * Gets the typed value of a cell.
     */
    private Object getCellValue(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return cell.getNumericCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }

    /**
     * Checks if a row is effectively empty (all values are null/blank).
     */
    private boolean isEmptyRow(Map<String, Object> rowData) {
        return rowData.values().stream()
                .allMatch(v -> v == null || (v instanceof String && ((String) v).trim().isEmpty()));
    }
}