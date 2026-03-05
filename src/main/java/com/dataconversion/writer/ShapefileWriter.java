package com.dataconversion.writer;

import com.dataconversion.model.ColumnMapping;
import com.dataconversion.model.GeometryType;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Writes data to Shapefile format using GeoTools.
 * Supports both Point and LineString geometries.
 */
public class ShapefileWriter {
    private static final Logger logger = LoggerFactory.getLogger(ShapefileWriter.class);
    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final SchemaBuilder schemaBuilder= new SchemaBuilder();

    /** DBF 编码：GBK 兼容中文 Windows 及主流国产 GIS 工具 */
    static final Charset DBF_CHARSET = Charset.forName("GBK");

    /**
     * Writes data to a shapefile.
     *
     * @param outputPath Path to output shapefile (e.g., "output.shp")
     * @param typeName Name for the feature type
     * @param columnMapping Column mapping with coordinate indices
     * @param dataRows List of data rows
     * @param crsCode CRS code (e.g., "EPSG:4326")
     * @return Number of features written
     * @throws Exception If writing fails
     */
    public int writeShapefile(String outputPath, String typeName, ColumnMapping columnMapping,
                              List<Map<String, Object>> dataRows, String crsCode) throws Exception {

        File shapeFile = new File(outputPath);
        File parentDir = shapeFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }

        // Build schema
        SimpleFeatureType schema = schemaBuilder.buildSchema(
            typeName,
            columnMapping.getGeometryType(),
            columnMapping.getColumnNames(),
            crsCode
        );

        // Create shapefile data store
        ShapefileDataStoreFactory dataStoreFactory = new ShapefileDataStoreFactory();
        Map<String, Serializable> params = new HashMap<>();
        params.put("url", shapeFile.toURI().toURL());
        params.put("create spatial index", Boolean.TRUE);

        ShapefileDataStore dataStore = (ShapefileDataStore) dataStoreFactory.createNewDataStore(params);
        dataStore.setCharset(DBF_CHARSET);
        dataStore.createSchema(schema);

        // Build features
        DefaultFeatureCollection featureCollection = new DefaultFeatureCollection();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(schema);

        int featureCount = 0;
        for (Map<String, Object> row : dataRows) {
            try {
                SimpleFeature feature = buildFeature(featureBuilder, row, columnMapping);
                if (feature != null) {
                    featureCollection.add(feature);
                    featureCount++;
                }
            } catch (Exception e) {
                logger.warn("跳过无效行: {}", e.getMessage());
            }
        }

        // Write features to shapefile
        Transaction transaction = new DefaultTransaction("create");
        try {
            String typeName2 = dataStore.getTypeNames()[0];
            SimpleFeatureSource featureSource = dataStore.getFeatureSource(typeName2);

            if (featureSource instanceof SimpleFeatureStore) {
                SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
                featureStore.setTransaction(transaction);
                featureStore.addFeatures(featureCollection);
                transaction.commit();

                logger.info("成功写入 {} 个要素到: {}", featureCount, outputPath);
            } else {
                throw new IOException("无法写入shapefile: " + typeName2);
            }
        } catch (Exception e) {
            transaction.rollback();
            throw e;
        } finally {
            transaction.close();
            dataStore.dispose();
        }

        return featureCount;
    }

    /**
     * Builds a single feature from a data row.
     */
    private SimpleFeature buildFeature(SimpleFeatureBuilder builder, Map<String, Object> row,
                                       ColumnMapping mapping) {

        builder.reset();

        // Create geometry based on type
        if (mapping.getGeometryType() == GeometryType.POINT) {
            Point point = createPointGeometry(row, mapping);
            if (point == null) {
                return null;
            }
            builder.add(point);
        } else if (mapping.getGeometryType() == GeometryType.LINESTRING) {
            LineString lineString = createLineStringGeometry(row, mapping);
            if (lineString == null) {
                return null;
            }
            builder.add(lineString);
        }

        // Add all attribute values (skip Column* placeholders)
        for (String columnName : mapping.getColumnNames()) {
            if (columnName.startsWith("Column")) continue;
            Object value = row.get(columnName);
            String strValue = value != null ? value.toString() : "";
            builder.add(strValue);
        }

        return builder.buildFeature(null);
    }

    /**
     * Creates Point geometry from row data.
     */
    private Point createPointGeometry(Map<String, Object> row, ColumnMapping mapping) {
        try {
            String xColumnName = mapping.getXColumnName();
            String yColumnName = mapping.getYColumnName();

            Object xValue = row.get(xColumnName);
            Object yValue = row.get(yColumnName);

            double x = parseDouble(xValue);
            double y = parseDouble(yValue);

            Coordinate coord = new Coordinate(x, y);
            return geometryFactory.createPoint(coord);
        } catch (Exception e) {
            logger.warn("无法创建Point几何: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Creates LineString geometry from row data.
     */
    private LineString createLineStringGeometry(Map<String, Object> row, ColumnMapping mapping) {
        try {
            String startXName = mapping.getStartXColumnName();
            String startYName = mapping.getStartYColumnName();
            String endXName = mapping.getEndXColumnName();
            String endYName = mapping.getEndYColumnName();

            Object startXValue = row.get(startXName);
            Object startYValue = row.get(startYName);
            Object endXValue = row.get(endXName);
            Object endYValue = row.get(endYName);

            double startX = parseDouble(startXValue);
            double startY = parseDouble(startYValue);
            double endX = parseDouble(endXValue);
            double endY = parseDouble(endYValue);

            Coordinate[] coords = new Coordinate[]{
                new Coordinate(startX, startY),
                new Coordinate(endX, endY)
            };

            return geometryFactory.createLineString(coords);
        } catch (Exception e) {
            logger.warn("无法创建LineString几何: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Parses a double value from an object.
     */
    private double parseDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            return Double.parseDouble(((String) value).trim());
        }
        throw new IllegalArgumentException("无法解析为数字: " + value);
    }
}
