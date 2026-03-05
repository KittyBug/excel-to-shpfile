一个基于 Java 17 的命令行工具，将 Excel 文件（.xlsx / .xls）智能转换为 Shapefile 格式，支持中英文列名识别、多行表头合并、合并单元格处理及多 Sheet 批量导出。

  ---
  功能特性

  - 双格式支持：兼容 .xlsx 和 .xls 两种 Excel 格式
  - 智能表头检测：自动评分定位表头行，处理多行表头与合并单元格
  - 坐标列自动匹配：三级优先级匹配策略，支持中英文坐标列名
  - 几何类型自动推断：根据列名模式自动识别 POINT 或 LINESTRING 类型
  - 多 Sheet 批量处理：每个 Sheet 独立生成对应的 Shapefile
  - 坐标范围校验：经度 [-180, 180]，纬度 [-90, 90]
  - 默认坐标系：EPSG:4326（WGS84）

  ---
  环境要求

  ┌────────────┬───────┐
  │    依赖    │ 版本  │
  ├────────────┼───────┤
  │ Java       │ 17+   │
  ├────────────┼───────┤
  │ Maven      │ 3.6+  │
  ├────────────┼───────┤
  │ Apache POI │ 5.2.5 │
  ├────────────┼───────┤
  │ GeoTools   │ 30.1  │
  └────────────┴───────┘

  ---
  快速开始

  构建项目

  mvn clean compile
  mvn clean package

  基本用法

  java -jar target/excel-to-shapefile-1.0.0.jar input.xlsx output.shp

  完整参数

  java -jar target/excel-to-shapefile-1.0.0.jar \
    --input  data.xlsx \
    --output result.shp \
    --header-row 1 \
    --x-column "经度" \
    --y-column "纬度" \
    --crs "EPSG:4326" \
    --strict

  ┌──────────────┬───────────────────────────────────────────┐
  │     参数     │                   说明                    │
  ├──────────────┼───────────────────────────────────────────┤
  │ --input      │ 输入 Excel 文件路径（.xlsx 或 .xls）      │
  ├──────────────┼───────────────────────────────────────────┤
  │ --output     │ 输出 Shapefile 路径                       │
  ├──────────────┼───────────────────────────────────────────┤
  │ --header-row │ 手动指定表头行（从 1 开始），默认自动检测 │
  ├──────────────┼───────────────────────────────────────────┤
  │ --x-column   │ 手动指定经度列名                          │
  ├──────────────┼───────────────────────────────────────────┤
  │ --y-column   │ 手动指定纬度列名                          │
  ├──────────────┼───────────────────────────────────────────┤
  │ --crs        │ 坐标参考系，默认 EPSG:4326                │
  ├──────────────┼───────────────────────────────────────────┤
  │ --strict     │ 严格模式，遇到无效坐标时终止转换          │
  └──────────────┴───────────────────────────────────────────┘

  ---
  坐标列名识别规则

  工具按三级优先级自动匹配坐标列：

  第一级 — 精确匹配
  - 经度：经度 longitude lon lng x
  - 纬度：纬度 latitude lat y
  - 起终点：起点经度 start_lon / 终点经度 end_lon 等

  第二级 — 包含匹配：起点 终点 start end x坐标 y坐标 东经 北纬

  第三级 — 正则匹配：.*[xX].* / .*lon.* / .*经.* 等，全部忽略大小写。

  ---
  几何类型说明

  ┌────────────┬────────────────────────────┬────────┐
  │    类型    │          触发条件          │  输出  │
  ├────────────┼────────────────────────────┼────────┤
  │ POINT      │ 存在单组经纬度列           │ 点要素 │
  ├────────────┼────────────────────────────┼────────┤
  │ LINESTRING │ 存在起点和终点两组经纬度列 │ 线要素 │
  └────────────┴────────────────────────────┴────────┘

  ---
  输出文件

  每个 Sheet 生成一组 Shapefile，以 Sheet 名称命名：

  output/
  ├── Sheet1.shp    # 几何数据
  ├── Sheet1.shx    # 空间索引
  ├── Sheet1.dbf    # 属性数据
  └── Sheet1.prj    # 坐标系信息

  注意：Shapefile 属性字段名最长 10 个字符，超出部分将自动截断。

  ---
  项目结构

  src/main/java/com/dataconversion/
  ├── ExcelToShapefileConverter.java   # 主入口
  ├── reader/
  │   ├── ExcelReader.java             # Excel 读取（支持 .xlsx/.xls）
  │   ├── HeaderDetector.java          # 表头自动检测
  │   └── DataExtractor.java           # 数据行提取
  ├── analyzer/
  │   ├── GeometryTypeDetector.java    # 几何类型推断
  │   ├── CoordinateColumnMatcher.java # 坐标列匹配
  │   └── DataValidator.java           # 坐标数据校验
  ├── writer/
  │   ├── SchemaBuilder.java           # 要素类型构建
  │   └── ShapefileWriter.java         # Shapefile 写出
  ├── model/
  │   ├── ConversionConfig.java
  │   ├── GeometryType.java
  │   ├── ColumnMapping.java
  │   └── ExcelData.java
  └── util/
      ├── CoordinatePatterns.java
      ├── ConversionException.java
      ├── InputValidationException.java
      ├── HeaderDetectionException.java
      └── CoordinateMatchingException.java

  ---
  运行测试

  mvn test
  mvn test -Dtest=HeaderDetectorTest
  mvn test -Dtest=HeaderDetectorTest#testDetectHeaderAtRowZero

  ---
  许可证

  MIT License

  ---
