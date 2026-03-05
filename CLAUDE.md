# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Excel to Shapefile converter - a Java 17 tool that converts Excel files (.xlsx) to Shapefile format with intelligent header detection, coordinate column matching, multi-row header merging, merged cell handling, and multi-sheet workbook support. Each sheet represents a data type and generates a corresponding Shapefile.

## Build and Development Commands

### Maven Commands
```bash
# Compile the project
mvn clean compile

# Run tests
mvn test

# Package as JAR
mvn clean package

# Run a single test class
mvn test -Dtest=HeaderDetectorTest

# Run a specific test method
mvn test -Dtest=HeaderDetectorTest#testDetectHeaderAtRowZero
```

### Running the Application
```bash
# Basic usage
java -jar target/excel-to-shapefile-1.0.0.jar input.xlsx output.shp

# With options
java -jar target/excel-to-shapefile-1.0.0.jar \
  --input data.xlsx \
  --output result.shp \
  --header-row 1 \
  --x-column "经度" \
  --y-column "纬度" \
  --crs "EPSG:4326" \
  --strict
```

## Architecture

### Core Data Flow
1. **Excel Reading** (reader package) → Opens workbook, iterates through sheets
2. **Header Detection** (reader/HeaderDetector) → Scores rows to find header, handles multi-row headers and merged cells
3. **Data Extraction** (reader/DataExtractor) → Reads data rows until empty row encountered
4. **Geometry Type Detection** (analyzer package) → Determines POINT vs LINESTRING based on column patterns
5. **Coordinate Matching** (analyzer package) → Matches coordinate columns using 3-tier pattern matching
6. **Data Validation** (analyzer package) → Validates coordinate ranges and data types
7. **Shapefile Writing** (writer package) → Generates .shp, .shx, .dbf, .prj files using GeoTools

### Package Structure
- `com.dataconversion.reader` - Excel file reading and data extraction
- `com.dataconversion.analyzer` - Coordinate detection, geometry type detection, validation (TO BE IMPLEMENTED)
- `com.dataconversion.writer` - Shapefile generation using GeoTools (TO BE IMPLEMENTED)
- `com.dataconversion.model` - Data models and configuration
- `com.dataconversion.util` - Exception classes and coordinate patterns

### Key Algorithms

#### Header Detection Scoring (HeaderDetector.java)
- Scans first N rows (default 10)
- Scoring: +10 for row 0, +5 per text cell, -3 per numeric cell, +8 if no duplicates
- Highest score wins as header row
- Handles merged cells by filling empty cells with "Column{index}"

#### Coordinate Column Matching (3-tier priority)
**Priority 1 - Exact match:**
- Single point: "经度", "longitude", "lon", "lng", "x" / "纬度", "latitude", "lat", "y"
- Start point: "起点经度", "start_lon", "start_x" / "起点纬度", "start_lat", "start_y"
- End point: "终点经度", "end_lon", "end_x" / "终点纬度", "end_lat", "end_y"

**Priority 2 - Contains match:**
- Keywords: "起点", "start", "终点", "end", "x坐标", "y坐标", "东经", "北纬"

**Priority 3 - Regex:**
- X: `.*[xX].*|.*lon.*|.*经.*`
- Y: `.*[yY].*|.*lat.*|.*纬.*`
- Start: `.*起.*点.*|.*start.*|.*from.*`
- End: `.*终.*点.*|.*end.*|.*to.*`

All matching is case-insensitive.

#### Geometry Type Detection
- **POINT mode**: Single X/Y coordinate pair → generates Point geometry
- **LINESTRING mode**: Four coordinate fields (start X/Y, end X/Y) with keywords "起点"/"终点"/"start"/"end" → generates LineString geometry

### Multi-Sheet Processing
Each sheet in the Excel workbook is processed independently:
1. Detect header and extract data for the sheet
2. Determine geometry type (POINT or LINESTRING) based on column patterns
3. Match coordinate columns according to geometry type
4. Generate separate Shapefile named after the sheet
5. Each sheet's output is completely independent

## Implementation Status

### Completed Components
- ✅ Maven project setup with dependencies (Apache POI 5.2.5, GeoTools 30.1)
- ✅ Model classes: ConversionConfig, GeometryType enum
- ✅ Exception hierarchy: ConversionException, InputValidationException, HeaderDetectionException, CoordinateMatchingException
- ✅ ExcelReader: Opens .xlsx workbooks, provides sheet access
- ✅ HeaderDetector: Scoring algorithm for header detection
- ✅ DataExtractor: Extracts data rows with type conversion, stops at empty rows

### To Be Implemented (阶段4-6)
- ⏳ `util/CoordinatePatterns.java` - Pattern definitions for coordinate matching
- ⏳ `analyzer/GeometryTypeDetector.java` - Detects POINT vs LINESTRING mode
- ⏳ `analyzer/CoordinateColumnMatcher.java` - 3-tier matching algorithm
- ⏳ `analyzer/DataValidator.java` - Coordinate range validation (lon: [-180,180], lat: [-90,90])
- ⏳ `model/ColumnMapping.java` - Column index mapping for coordinates
- ⏳ `model/ExcelData.java` - Data container with headers and rows
- ⏳ `writer/SchemaBuilder.java` - GeoTools SimpleFeatureType builder
- ⏳ `writer/ShapefileWriter.java` - Shapefile generation (supports Point and LineString)
- ⏳ `ExcelToShapefileConverter.java` - Main entry point with multi-sheet loop

## Important Implementation Notes

### Exception Visibility
The exception classes in `util/ConversionException.java` use package-private visibility (no `public` modifier). When importing:
- `InputValidationException` is in `com.dataconversion.util` package
- `HeaderDetectionException` is in `com.dataconversion.util` package
- `CoordinateMatchingException` is in `com.dataconversion.util` package

These are already imported correctly in existing classes.

### Chinese Language Support
- Error messages use Chinese (e.g., "Excel文件未找到", "工作簿未打开")
- Column names support both Chinese and English
- Ensure UTF-8 encoding throughout (already configured in pom.xml)

### Shapefile Limitations
- Attribute names limited to 10 characters - implement smart truncation in SchemaBuilder
- Default CRS is EPSG:4326 (WGS84) for geographic coordinates

### Resource Management
- Always close ExcelReader after use (calls workbook.close() and fileInputStream.close())
- GeoTools DataStore must be properly disposed after writing

### Data Extraction Behavior
- DataExtractor reads ALL rows until the end of the sheet
- Empty rows are skipped but don't stop extraction (current implementation)
- Note: Implementation plan specifies stopping at first empty row - may need adjustment

## Testing Strategy

### Unit Tests to Create
- `HeaderDetectorTest` - Test scoring with various row layouts (all text, all numeric, mixed, row 0 bonus)
- `CoordinateColumnMatcherTest` - Test all 3 tiers of pattern matching with Chinese/English names
- `DataValidatorTest` - Test coordinate range boundaries (±180, ±90)
- `GeometryTypeDetectorTest` - Test POINT vs LINESTRING detection

### Integration Tests
- End-to-end conversion with sample Excel files
- Test cases: Chinese headers, English headers, multi-row headers, merged cells, POINT mode, LINESTRING mode, multi-sheet workbooks

## Dependencies

### Core Libraries
- **Apache POI 5.2.5** (poi-ooxml) - Excel reading
- **GeoTools 30.1** (gt-shapefile, gt-epsg-hsql) - Shapefile writing and CRS support
- **SLF4J 2.0.9 + Logback 1.4.14** - Logging
- **JUnit Jupiter 5.10.1** - Testing

### Repository Configuration
OSGeo repository required for GeoTools: https://repo.osgeo.org/repository/release/

## Main Entry Point
When implementing `ExcelToShapefileConverter.java`:
- Main class: `com.dataconversion.ExcelToShapefileConverter` (configured in pom.xml)
- Parse command-line arguments for input/output paths and options
- Loop through all sheets in workbook
- For each sheet: detect header → determine geometry type → match coordinates → validate → write shapefile
- Generate conversion summary with row counts and output file paths
