package com.dataconversion;

import com.dataconversion.analyzer.CoordinateColumnMatcher;
import com.dataconversion.analyzer.DataValidator;
import com.dataconversion.analyzer.GeometryTypeDetector;
import com.dataconversion.model.ColumnMapping;
import com.dataconversion.model.ConversionConfig;
import com.dataconversion.model.ExcelData;
import com.dataconversion.model.GeometryType;
import com.dataconversion.reader.DataExtractor;
import com.dataconversion.reader.ExcelReader;
import com.dataconversion.reader.HeaderDetector;
import com.dataconversion.writer.ShapefileWriter;
import org.apache.poi.ss.usermodel.Sheet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Main entry point for Excel to Shapefile conversion.
 * Handles multi-sheet workbooks with automatic geometry type detection.
 */
public class ExcelToShapefileConverter {
    private static final Logger logger = LoggerFactory.getLogger(ExcelToShapefileConverter.class);

    public static void main(String[] args) {
        if (args.length < 2) {
            printUsage();
            System.exit(1);
        }

        try {
            // Parse command line arguments
            ConversionConfig config = parseArguments(args);
//            logger.info("开始转换: {}", config);
//            ConversionConfig config = new ConversionConfig(
//                "D:\\Projects\\DataConversion\\data\\excels\\2.2023年度浙江省海洋空间要素保护与利用保障基本数据整理和更新调查项目（嵊泗县）-秋季航次-海洋生物生态-数据报表.xls",
//                "D:\\Projects\\DataConversion\\data\\output3"
//            );

            // Run conversion
            List<SheetConversionResult> results = convertExcelToShapefile(config);

            // Print summary
            printSummary(results);

            logger.info("转换完成!");
            System.exit(0);

        } catch (Exception e) {
            logger.error("转换失败: {}", e.getMessage(), e);
            System.err.println("错误: " + e.getMessage());
            System.exit(1);
        }
    }

    /**
     * Converts Excel file to Shapefile(s).
     * Processes all sheets in the workbook.
     */
    public static List<SheetConversionResult> convertExcelToShapefile(ConversionConfig config)
            throws Exception {

        List<SheetConversionResult> results = new ArrayList<>();
        ExcelReader excelReader = new ExcelReader();

        try {
            // Open Excel workbook
            excelReader.openWorkbook(config.getInputFilePath());
            int sheetCount = excelReader.getSheetCount();
            logger.info("工作簿包含 {} 个sheet", sheetCount);

            // Process each sheet
            for (int sheetIndex = 0; sheetIndex < sheetCount; sheetIndex++) {
                String sheetName = excelReader.getSheet(sheetIndex).getSheetName();
                if ("分析方法索引".equals(sheetName) || "sheet".equals(sheetName.toLowerCase(Locale.ROOT))) {
                    logger.info("跳过Sheet: {} (索引: {})", sheetName, sheetIndex);
                    continue;
                }
                try {
                    SheetConversionResult result = convertSheet(excelReader, sheetIndex, config);
                    results.add(result);
                } catch (Exception e) {
                    logger.error("Sheet {} 转换失败: {}", sheetIndex, e.getMessage(), e);
                    results.add(new SheetConversionResult(
                        excelReader.getSheet(sheetIndex).getSheetName(),
                        sheetIndex,
                        false,
                        0,
                        e.getMessage()
                    ));
                }
            }

        } finally {
            excelReader.close();
        }

        return results;
    }

    /**
     * Converts a single sheet to shapefile.
     */
    private static SheetConversionResult convertSheet(ExcelReader excelReader, int sheetIndex,
                                                      ConversionConfig config) throws Exception {

        Sheet sheet = excelReader.getSheet(sheetIndex);
        String sheetName = sheet.getSheetName();
        logger.info("========================================");
        logger.info("处理Sheet: {} (索引: {})", sheetName, sheetIndex);

        // 1. Detect header
        HeaderDetector headerDetector = new HeaderDetector();
        HeaderDetector.HeaderInfo headerInfo = headerDetector.detectHeader(sheet);
        String[] columnNames = headerInfo.getColumnNames();
        int headerRowIndex = headerInfo.getHeaderRowIndex();
        logger.info("检测到表头行: {}, 列数: {}", headerRowIndex, columnNames.length);

        // 2. Extract data
        DataExtractor dataExtractor = new DataExtractor();
        ExcelData excelData = dataExtractor.extractData(sheet, columnNames, headerRowIndex + 1);
        logger.info("提取数据: {} 行", excelData.getTotalRows());

        // 3. Detect geometry type
        GeometryTypeDetector geometryDetector = new GeometryTypeDetector();
        GeometryType geometryType = geometryDetector.detectGeometryType(columnNames);

        // 4. Match coordinate columns
        CoordinateColumnMatcher columnMatcher = new CoordinateColumnMatcher();
        ColumnMapping columnMapping = columnMatcher.matchCoordinateColumns(columnNames, geometryType);

        // 5. Validate data
        DataValidator validator = new DataValidator(config.isStrictMode(), config.isSkipInvalidRows());
        DataValidator.ValidationResult validationResult = validator.validate(
            excelData.getDataRows(),
            columnMapping
        );

        if (validationResult.hasErrors()) {
            logger.warn("验证发现 {} 个无效行", validationResult.getInvalidRowCount());
        }

        // 6. Write shapefile
        String outputPath = generateOutputPath(config.getOutputDirPath(), sheetName, sheetIndex);
        ShapefileWriter writer = new ShapefileWriter();
        int featureCount = writer.writeShapefile(
            outputPath,
            sheetName,
            columnMapping,
            validationResult.getValidRows(),
            config.getCrs()
        );

        logger.info("Sheet {} 转换完成: {} 个要素写入到 {}", sheetName, featureCount, outputPath);

        return new SheetConversionResult(
            sheetName,
            sheetIndex,
            true,
            featureCount,
            outputPath
        );
    }

    /**
     * Generates output file path for a sheet.
     */
    private static String generateOutputPath(String outputDir, String sheetName, int sheetIndex) {
        // Sanitize sheet name for file system
        String sanitizedName = sheetName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");
        if (sanitizedName.isEmpty()) {
            sanitizedName = "sheet_" + sheetIndex;
        }

        File outputDirFile = new File(outputDir);
        if (!outputDirFile.exists()) {
            outputDirFile.mkdirs();
        }

        return new File(outputDirFile, sanitizedName + ".shp").getAbsolutePath();
    }

    /**
     * Parses command line arguments into ConversionConfig.
     */
    private static ConversionConfig parseArguments(String[] args) {
        String inputPath = args[0];
        String outputDir = args[1];

        ConversionConfig config = new ConversionConfig(inputPath, outputDir);

        // Parse optional arguments
        for (int i = 2; i < args.length; i++) {
            String arg = args[i];

            if (arg.equals("--crs") && i + 1 < args.length) {
                config.setCrs(args[++i]);
            } else if (arg.equals("--strict")) {
                config.setStrictMode(true);
            } else if (arg.equals("--header-row") && i + 1 < args.length) {
                config.setHeaderRowIndex(Integer.parseInt(args[++i]));
            } else if (arg.equals("--x-column") && i + 1 < args.length) {
                config.setXColumnName(args[++i]);
            } else if (arg.equals("--y-column") && i + 1 < args.length) {
                config.setYColumnName(args[++i]);
            }
        }

        return config;
    }

    /**
     * Prints usage information.
     */
    private static void printUsage() {
        System.out.println("Excel to Shapefile Converter");
        System.out.println();
        System.out.println("用法:");
        System.out.println("  java -jar excel-to-shapefile.jar <input.xlsx> <output_dir> [options]");
        System.out.println();
        System.out.println("参数:");
        System.out.println("  input.xlsx    输入Excel文件路径");
        System.out.println("  output_dir    输出目录路径");
        System.out.println();
        System.out.println("选项:");
        System.out.println("  --crs <code>        CRS代码 (默认: EPSG:4326)");
        System.out.println("  --strict            严格模式，遇到无效数据时失败");
        System.out.println("  --header-row <n>    手动指定表头行索引");
        System.out.println("  --x-column <name>   手动指定X/经度列名");
        System.out.println("  --y-column <name>   手动指定Y/纬度列名");
        System.out.println();
        System.out.println("示例:");
        System.out.println("  java -jar excel-to-shapefile.jar data.xlsx output");
        System.out.println("  java -jar excel-to-shapefile.jar data.xlsx output --crs EPSG:4326 --strict");
    }

    /**
     * Prints conversion summary.
     */
    private static void printSummary(List<SheetConversionResult> results) {
        System.out.println();
        System.out.println("========================================");
        System.out.println("转换摘要");
        System.out.println("========================================");

        int successCount = 0;
        int failureCount = 0;
        int totalFeatures = 0;

        for (SheetConversionResult result : results) {
            if (result.isSuccess()) {
                successCount++;
                totalFeatures += result.getFeatureCount();
                System.out.println(String.format("✓ %s: %d 个要素 -> %s",
                    result.getSheetName(), result.getFeatureCount(), result.getOutputPath()));
            } else {
                failureCount++;
                System.out.println(String.format("✗ %s: 失败 - %s",
                    result.getSheetName(), result.getErrorMessage()));
            }
        }

        System.out.println("========================================");
        System.out.println(String.format("总计: %d 个sheet, %d 成功, %d 失败, %d 个要素",
            results.size(), successCount, failureCount, totalFeatures));
        System.out.println("========================================");
    }

    /**
     * Result of converting a single sheet.
     */
    public static class SheetConversionResult {
        private final String sheetName;
        private final int sheetIndex;
        private final boolean success;
        private final int featureCount;
        private final String outputPath;
        private final String errorMessage;

        public SheetConversionResult(String sheetName, int sheetIndex, boolean success,
                                     int featureCount, String outputPathOrError) {
            this.sheetName = sheetName;
            this.sheetIndex = sheetIndex;
            this.success = success;
            this.featureCount = featureCount;

            if (success) {
                this.outputPath = outputPathOrError;
                this.errorMessage = null;
            } else {
                this.outputPath = null;
                this.errorMessage = outputPathOrError;
            }
        }

        public String getSheetName() {
            return sheetName;
        }

        public int getSheetIndex() {
            return sheetIndex;
        }

        public boolean isSuccess() {
            return success;
        }

        public int getFeatureCount() {
            return featureCount;
        }

        public String getOutputPath() {
            return outputPath;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }
}
