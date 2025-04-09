
---

# 卫星可见性模拟器

本项目实现了一个基于 Orekit 库的卫星可见性模拟器，用于计算卫星与地面站以及卫星间的可见性窗口。项目采用外部 YAML 配置文件加载仿真参数，并使用并发技术优化可见性计算，以便支持大规模星座的仿真。

## 主要特点

- **高精度轨道传播：** 使用 Orekit 库实现轨道传播与姿态计算。
- **灵活的仿真配置：** 利用 YAML 配置文件（`simulation.yaml`）加载仿真参数，包括卫星轨道数据和地面站坐标，无需修改源码即可切换仿真场景。
- **集中化初始化：** 通过 `OrekitConfig` 类统一管理 Orekit 数据加载与初始化，保证所有模块都能正常访问必要的天文及地理数据。
- **并发计算：** 使用并发执行器对各颗卫星的可见性计算进行并行处理，提高仿真效率。
- **模块化设计：** 项目结构清晰，功能模块分离，便于后续扩展与维护。

## 项目结构

```
satviz/
├── pom.xml                           // 构建配置文件（Maven）
├── orekit-data/                      // Orekit 所需数据目录（如天文、地理数据）
├── src/main/java/com/bupt/satviz/
│   ├── SatVizApplication.java        // 应用主入口，驱动整个仿真流程
│   ├── config/
│   │   ├── SimulationConfig.java     // 从 YAML 文件加载仿真参数
│   │   └── OrekitConfig.java         // 集中初始化 Orekit（加载 orekit-data 数据）
│   ├── calculation/
│   │   ├── OrbitPropagator.java      // 使用 Orekit 进行轨道传播计算
│   │   └── SatelliteStateCalculator.java // 计算卫星状态（位置、变化率）
│   ├── concurrent/
│   │   ├── ParallelVisibilityExecutor.java // 并发执行卫星可见性计算任务
│   │   └── SatelliteVisibilityTask.java     // 单颗卫星的可见性计算任务
│   ├── model/
│   │   ├── KeplerianElements.java    // 卫星轨道元素封装（转换为 Orekit 轨道对象）
│   │   ├── GroundStation.java        // 地面站坐标模型
│   │   ├── PropagationResult.java    // 轨道传播结果
│   │   ├── VisibilityWindow.java     // 可见性窗口的数据结构
│   │   ├── SatResult.java            // 卫星可见性结果（包含地面站/卫星间窗口）
│   │   └── SatelliteState.java       // 卫星状态（位置与运动变化率）
│   ├── output/
│   │   └── ConsoleResultPrinter.java // 格式化输出仿真结果到控制台
│   └── visibility/
│       ├── GroundStationVisibilityAnalyzer.java // 地面站可见性计算模块
│       └── InterSatelliteVisibilityAnalyzer.java  // 卫星间可见性计算模块
├── src/main/resources/
│   ├── simulation_scenario_?.yaml    // YAML 配置文件，定义仿真参数（卫星、地面站、仿真时间）
│   └── logback.xml                   // Logback 日志配置文件
└── src/test/java/                    // 测试代码目录
    └── ...                           // 各模块单元测试及集成测试（与 src/main/java 镜像结构）
```

## 使用说明

### 1. 依赖要求

- **Java 8** 或更高版本
- **Orekit 库**：用于轨道传播和天文计算
- **SnakeYAML**：用于解析 YAML 配置文件
- 构建工具：**Maven** 

确保在 `pom.xml`  中已经正确添加相关依赖。

### 2. 配置仿真场景

- 修改 `src/main/resources/simulation_scenario_?.yaml` 文件：
    - 可设置仿真起始时间与持续时长。
    - 定义卫星星座参数（本示例支持 12 个轨道平面，每个轨道平面内包含3颗卫星）。
    - 定义地面站的坐标（如 10 个地面站）。

如需要切换场景，可创建多个配置文件或在单个文件中增加场景识别标识，在 `SatVizApplication` 中选择加载不同场景。

### 3. 初始化 Orekit

- 项目启动时，在主程序入口中调用 `OrekitConfig.initialize()`，该方法加载 `orekit-data` 数据目录中的文件，并完成必要的 Orekit 初始化。

### 4. 构建与运行

#### 使用 Maven

构建项目：

```bash
mvn clean install
```

运行项目：

```bash
mvn exec:java -Dexec.mainClass="com.bupt.satviz.SatVizApplication"
```

### 5. 测试

测试代码位于 `src/test/java`。可通过命令行运行所有测试：

```bash
mvn test
```

或

```bash
gradle test
```

确保所有测试用例顺利通过，以验证各模块功能的正确性。

### 6. 日志配置

- 日志配置文件 `logback.xml` 放置在 `src/main/resources` 中。
- 修改该文件可调整日志级别、格式以及输出方式（例如控制台或文件）。
- 运行程序时，日志输出会根据该配置显示相关信息，便于调试和问题排查。

## 贡献指南

- 修改代码前请编写或更新相应单元测试，确保更改不会引入回归问题。
- 遵循项目的模块化设计，将业务逻辑、配置加载、以及输出模块保持分离，便于后续维护与扩展。
- 提交代码前请确保代码风格一致，遵循 Java 编码规范。

## 联系方式

如果对本项目有任何疑问或建议，欢迎在项目仓库中提交 Issue 或直接联系维护者。

---

以上即为本项目的基本介绍及使用说明，欢迎大家一起参与，共同完善这个卫星可见性模拟器项目。