package com.dataconversion.reader;

import com.dataconversion.util.InputValidationException;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Reads Excel files using Apache POI.
 * Handles .xlsx format workbooks.
 */
public class ExcelReader {
    private static final Logger logger = LoggerFactory.getLogger(ExcelReader.class);
    private Workbook workbook;
    private FileInputStream fileInputStream;

    /**
     * Opens an Excel file and loads the workbook.
     *
     * @param filePath Path to the Excel file
     * @throws InputValidationException If file not found or invalid format
     */
    public void openWorkbook(String filePath) throws InputValidationException {
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                throw new InputValidationException("Excel文件未找到: " + filePath);
            }

            String name = file.getName().toLowerCase();
            if (!name.endsWith(".xlsx") && !name.endsWith(".xls")) {
                throw new InputValidationException("不支持的文件格式，请使用 .xlsx 或 .xls 文件");
            }

            fileInputStream = new FileInputStream(file);
            workbook = WorkbookFactory.create(fileInputStream);
            logger.info("Successfully opened workbook: {}", filePath);
        } catch (IOException e) {
            throw new InputValidationException("无法打开Excel文件: " + filePath, e);
        }
    }

    /**
     * Gets a sheet by index.
     *
     * @param sheetIndex Index of the sheet
     * @return The requested sheet
     * @throws InputValidationException If sheet index is out of bounds
     */
    public Sheet getSheet(int sheetIndex) throws InputValidationException {
        if (workbook == null) {
            throw new InputValidationException("工作簿未打开");
        }

        if (sheetIndex >= workbook.getNumberOfSheets() || sheetIndex < 0) {
            throw new InputValidationException("Sheet索引超出范围: " + sheetIndex);
        }

        return workbook.getSheetAt(sheetIndex);
    }

    /**
     * Gets a sheet by name.
     *
     * @param sheetName Name of the sheet
     * @return The requested sheet
     * @throws InputValidationException If sheet name not found
     */
    public Sheet getSheetByName(String sheetName) throws InputValidationException {
        if (workbook == null) {
            throw new InputValidationException("工作簿未打开");
        }

        Sheet sheet = workbook.getSheet(sheetName);
        if (sheet == null) {
            throw new InputValidationException("未找到Sheet: " + sheetName);
        }

        return sheet;
    }

    /**
     * Gets the number of sheets in the workbook.
     *
     * @return Number of sheets
     */
    public int getSheetCount() {
        if (workbook == null) {
            return 0;
        }
        return workbook.getNumberOfSheets();
    }

    /**
     * Gets all sheet names.
     *
     * @return Array of sheet names
     */
    public String[] getSheetNames() {
        if (workbook == null) {
            return new String[0];
        }

        String[] names = new String[workbook.getNumberOfSheets()];
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            names[i] = workbook.getSheetAt(i).getSheetName();
        }
        return names;
    }

    /**
     * Closes the workbook and releases resources.
     */
    public void close() {
        try {
            if (workbook != null) {
                workbook.close();
                logger.info("Workbook closed");
            }
            if (fileInputStream != null) {
                fileInputStream.close();
            }
        } catch (IOException e) {
            logger.error("Error closing workbook", e);
        }
    }
}