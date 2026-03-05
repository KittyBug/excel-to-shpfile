package com.dataconversion.reader;

import com.dataconversion.util.HeaderDetectionException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Detects header rows in Excel sheets using a scoring algorithm.
 * Handles multi-row headers and merged cells.
 */
public class HeaderDetector {
    private static final Logger logger = LoggerFactory.getLogger(HeaderDetector.class);
    private static final int DEFAULT_SCAN_ROWS = 10;

    /**
     * Detects the header row using a scoring algorithm.
     *
     * @param sheet The Excel sheet to scan
     * @param maxScanRows Maximum number of rows to scan for header
     * @return HeaderInfo containing header row index and column names
     * @throws HeaderDetectionException If header cannot be detected
     */
    public HeaderInfo detectHeader(Sheet sheet, int maxScanRows) throws HeaderDetectionException {
        if (sheet == null || sheet.getPhysicalNumberOfRows() == 0) {
            throw new HeaderDetectionException("Sheet is empty");
        }

        maxScanRows = Math.min(maxScanRows, Math.min(DEFAULT_SCAN_ROWS, sheet.getPhysicalNumberOfRows()));
        int bestScore = Integer.MIN_VALUE;
        int bestRowIndex = -1;

        // Score each row
        for (int i = 0; i < maxScanRows; i++) {
            Row row = sheet.getRow(i);
            if (row == null) {
                continue;
            }

            int score = scoreRow(row);
            logger.debug("Row {} score: {}", i, score);

            if (score > bestScore) {
                bestScore = score;
                bestRowIndex = i;
            }
        }

        if (bestRowIndex == -1) {
            throw new HeaderDetectionException("Could not detect header row. Please specify with --header-row");
        }

        Row headerRow = sheet.getRow(bestRowIndex);
        String[] columnNames = extractColumnNames(sheet, bestRowIndex);

        logger.info("Detected header at row {}: {}", bestRowIndex, Arrays.toString(columnNames));

        return new HeaderInfo(bestRowIndex, columnNames);
    }

    /**
     * Detects the header row with default scan rows.
     */
    public HeaderInfo detectHeader(Sheet sheet) throws HeaderDetectionException {
        return detectHeader(sheet, DEFAULT_SCAN_ROWS);
    }

    /**
     * Scores a row based on content characteristics.
     * - +10 if row index is 0
     * - +5 per text cell
     * - -3 per numeric cell
     * - +8 if no duplicate values
     */
    private int scoreRow(Row row) {
        int score = 0;

        // Bonus for first row
        if (row.getRowNum() == 0) {
            score += 10;
        }

        int textCells = 0;
        int numericCells = 0;
        Set<String> uniqueValues = new HashSet<>();

        for (int i = 0; i < row.getLastCellNum(); i++) {
            Cell cell = row.getCell(i);
            if (cell == null) {
                continue;
            }

            String cellValue = getCellStringValue(cell).trim();
            if (cellValue.isEmpty()) {
                continue;
            }

            // Check if numeric
            try {
                Double.parseDouble(cellValue);
                numericCells++;
            } catch (NumberFormatException e) {
                textCells++;
                uniqueValues.add(cellValue.toLowerCase());
            }
        }

        score += textCells * 5;
        score -= numericCells * 3;

        // Bonus if no duplicates
        int totalCells = textCells + numericCells;
        if (totalCells > 0 && uniqueValues.size() == textCells) {
            score += 8;
        }

        return score;
    }

    /**
     * Extracts column names from the header row, handling merged cells.
     */
    private String[] extractColumnNames(Sheet sheet, int headerRowIndex) throws HeaderDetectionException {
        Row headerRow = sheet.getRow(headerRowIndex);
        if (headerRow == null) {
            throw new HeaderDetectionException("Header row is null at index " + headerRowIndex);
        }

        List<String> columnNames = new ArrayList<>();
        int lastCellNum = headerRow.getLastCellNum();

        for (int i = 0; i < lastCellNum; i++) {
            Cell cell = headerRow.getCell(i);
            String columnName = getCellStringValue(cell).trim();

            // Handle empty cells from merged cells
            if (columnName.isEmpty()) {
                columnName = "Column" + i;
            }

            columnNames.add(columnName);
        }

        if (columnNames.isEmpty()) {
            throw new HeaderDetectionException("No column names found in header row");
        }

        return columnNames.toArray(new String[0]);
    }

    /**
     * Gets cell value as string, handling different cell types.
     */
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    /**
     * Container for header detection results.
     */
    public static class HeaderInfo {
        private final int headerRowIndex;
        private final String[] columnNames;

        public HeaderInfo(int headerRowIndex, String[] columnNames) {
            this.headerRowIndex = headerRowIndex;
            this.columnNames = columnNames;
        }

        public int getHeaderRowIndex() {
            return headerRowIndex;
        }

        public String[] getColumnNames() {
            return columnNames;
        }

        @Override
        public String toString() {
            return "HeaderInfo{" +
                    "headerRowIndex=" + headerRowIndex +
                    ", columnCount=" + columnNames.length +
                    '}';
        }
    }
}