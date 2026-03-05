package com.dataconversion.writer;

import com.dataconversion.model.GeometryType;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * Builds GeoTools SimpleFeatureType schema for shapefiles.
 * Handles geometry type selection and attribute field creation.
 */
public class SchemaBuilder {
    private static final Logger logger = LoggerFactory.getLogger(SchemaBuilder.class);
    /** DBF 字段名最大字节数（规范限制 10 字节） */
    private static final int MAX_FIELD_NAME_BYTES = 10;
    /** 与 ShapefileWriter 保持一致的编码 */
    private static final Charset DBF_CHARSET = ShapefileWriter.DBF_CHARSET;

    /**
     * Builds a SimpleFeatureType schema for the shapefile.
     *
     * @param typeName Name of the feature type (usually based on sheet name)
     * @param geometryType Type of geometry (POINT or LINESTRING)
     * @param columnNames Array of column names from Excel
     * @param crsCode CRS code (e.g., "EPSG:4326")
     * @return SimpleFeatureType schema
     * @throws FactoryException If CRS cannot be created
     */
    public SimpleFeatureType buildSchema(String typeName, GeometryType geometryType,
                                         String[] columnNames, String crsCode)
            throws FactoryException {

        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(sanitizeTypeName(typeName));

        // Set CRS
        CoordinateReferenceSystem crs = CRS.decode(crsCode);
        builder.setCRS(crs);

        // Add geometry field based on type
        if (geometryType == GeometryType.POINT) {
            builder.add("the_geom", Point.class);
            logger.debug("添加Point几何字段");
        } else if (geometryType == GeometryType.LINESTRING) {
            builder.add("the_geom", LineString.class);
            logger.debug("添加LineString几何字段");
        }

        // Add attribute fields for each Excel column (skip Column* placeholders)
        for (String columnName : columnNames) {
            if (columnName.startsWith("Column")) continue;
            String fieldName = sanitizeFieldName(columnName);
            builder.add(fieldName, String.class);
            logger.debug("添加属性字段: {} (原名: {})", fieldName, columnName);
        }

        SimpleFeatureType schema = builder.buildFeatureType();
        logger.info("Schema构建完成: {}, 几何类型: {}, 属性数: {}",
            typeName, geometryType, columnNames.length);

        return schema;
    }

    /**
     * Sanitizes type name for shapefile compatibility.
     */
    private String sanitizeTypeName(String typeName) {
        if (typeName == null || typeName.trim().isEmpty()) {
            return "features";
        }

        // Remove invalid characters and replace with underscore
        String sanitized = typeName.replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "_");

        // Ensure it starts with a letter or underscore
        if (!sanitized.matches("^[a-zA-Z_\\u4e00-\\u9fa5].*")) {
            sanitized = "_" + sanitized;
        }

        return sanitized;
    }

    /**
     * Sanitizes field name for shapefile compatibility.
     * Preserves original name including Chinese characters.
     * Truncates by byte length (GBK) to satisfy the 10-byte DBF field name limit.
     */
    private String sanitizeFieldName(String fieldName) {
        if (fieldName == null || fieldName.trim().isEmpty()) {
            return "field";
        }

        String sanitized = fieldName.trim();

        // Remove control characters
        sanitized = sanitized.replaceAll("[\\x00-\\x1F\\x7F]", "_");

        // Ensure starts with a letter, underscore, or Chinese character
        if (!sanitized.matches("^[a-zA-Z_\\u4e00-\\u9fa5].*")) {
            sanitized = "_" + sanitized;
        }

        // Truncate by byte count in GBK (DBF field name ≤ 10 bytes)
        if (sanitized.getBytes(DBF_CHARSET).length > MAX_FIELD_NAME_BYTES) {
            StringBuilder sb = new StringBuilder();
            int byteCount = 0;
            for (char c : sanitized.toCharArray()) {
                int charBytes = String.valueOf(c).getBytes(DBF_CHARSET).length;
                if (byteCount + charBytes > MAX_FIELD_NAME_BYTES) break;
                sb.append(c);
                byteCount += charBytes;
            }
            String truncated = sb.toString();
            logger.debug("字段名截断: {} -> {}", fieldName, truncated);
            sanitized = truncated;
        }

        return sanitized;
    }

    /**
     * Gets the sanitized field name for a given column name.
     * Useful for mapping original column names to shapefile field names.
     */
    public String getSanitizedFieldName(String columnName) {
        return sanitizeFieldName(columnName);
    }
}
