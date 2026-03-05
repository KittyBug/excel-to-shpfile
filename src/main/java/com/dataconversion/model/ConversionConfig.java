package com.dataconversion.model;

/**
 * Configuration options for Excel to Shapefile conversion.
 */
public class ConversionConfig {
    private String inputFilePath;
    private String outputDirPath;
    private Integer sheetIndex;
    private Integer headerRowIndex;
    private String xColumnName;
    private String yColumnName;
    private String crs;
    private boolean strictMode;
    private boolean skipInvalidRows;

    public ConversionConfig(String inputFilePath, String outputDirPath) {
        this.inputFilePath = inputFilePath;
        this.outputDirPath = outputDirPath;
        this.sheetIndex = 0;
        this.crs = "EPSG:4326";
        this.strictMode = false;
        this.skipInvalidRows = true;
    }

    // Getters and setters
    public String getInputFilePath() {
        return inputFilePath;
    }

    public void setInputFilePath(String inputFilePath) {
        this.inputFilePath = inputFilePath;
    }

    public String getOutputDirPath() {
        return outputDirPath;
    }

    public void setOutputDirPath(String outputDirPath) {
        this.outputDirPath = outputDirPath;
    }

    public Integer getSheetIndex() {
        return sheetIndex;
    }

    public void setSheetIndex(Integer sheetIndex) {
        this.sheetIndex = sheetIndex;
    }

    public Integer getHeaderRowIndex() {
        return headerRowIndex;
    }

    public void setHeaderRowIndex(Integer headerRowIndex) {
        this.headerRowIndex = headerRowIndex;
    }

    public String getXColumnName() {
        return xColumnName;
    }

    public void setXColumnName(String xColumnName) {
        this.xColumnName = xColumnName;
    }

    public String getYColumnName() {
        return yColumnName;
    }

    public void setYColumnName(String yColumnName) {
        this.yColumnName = yColumnName;
    }

    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    public boolean isSkipInvalidRows() {
        return skipInvalidRows;
    }

    public void setSkipInvalidRows(boolean skipInvalidRows) {
        this.skipInvalidRows = skipInvalidRows;
    }

    @Override
    public String toString() {
        return "ConversionConfig{" +
                "inputFilePath='" + inputFilePath + '\'' +
                ", outputDirPath='" + outputDirPath + '\'' +
                ", sheetIndex=" + sheetIndex +
                ", headerRowIndex=" + headerRowIndex +
                ", xColumnName='" + xColumnName + '\'' +
                ", yColumnName='" + yColumnName + '\'' +
                ", crs='" + crs + '\'' +
                ", strictMode=" + strictMode +
                ", skipInvalidRows=" + skipInvalidRows +
                '}';
    }
}