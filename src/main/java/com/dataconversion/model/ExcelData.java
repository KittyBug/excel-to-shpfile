package com.dataconversion.model;

import java.util.List;
import java.util.Map;

/**
 * Container for Excel data including headers and data rows.
 */
public class ExcelData {
    private final String[] headerNames;
    private final List<Map<String, Object>> dataRows;
    private String sheetName;
    private int headerRowIndex;
    private int totalRows;

    public ExcelData(String[] headerNames, List<Map<String, Object>> dataRows) {
        this.headerNames = headerNames;
        this.dataRows = dataRows;
    }

    public String[] getHeaderNames() {
        return headerNames;
    }

    public List<Map<String, Object>> getDataRows() {
        return dataRows;
    }

    public String getSheetName() {
        return sheetName;
    }

    public void setSheetName(String sheetName) {
        this.sheetName = sheetName;
    }

    public int getHeaderRowIndex() {
        return headerRowIndex;
    }

    public void setHeaderRowIndex(int headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    @Override
    public String toString() {
        return "ExcelData{" +
                "sheetName='" + sheetName + '\'' +
                ", headerRowIndex=" + headerRowIndex +
                ", totalRows=" + totalRows +
                ", columnCount=" + headerNames.length +
                '}';
    }
}
