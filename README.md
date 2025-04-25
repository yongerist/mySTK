
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
│   │   ├── OrekitConfig.java         // 集中初始化 Orekit（加载 orekit-data 数据）
│   │   └── DataPreparation.java      //  处理仿真数据准备（卫星、地面站、时间等）
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
│   ├── preprocessing/ 
│   │   └── EphemerisGeneratorService.java  // 生成卫星星历
│   ├── output/
│   │   └── ConsoleResultPrinter.java // 格式化输出仿真结果到控制台
│   └── visibility/
│       ├── GroundStationVisibilityAnalyzer.java // 地面站可见性计算模块
│       └── InterSatelliteVisibilityAnalyzer.java  // 卫星间可见性计算模块
└── src/main/resources/
    └── simulation_scenario_?.yaml    // YAML 配置文件，定义仿真参数（卫星、地面站、仿真时间）

```

## 使用说明

### 1. 依赖要求

- **Java 8** 或更高版本
- **Orekit 库**：用于轨道传播和天文计算
- **SnakeYAML**：用于解析 YAML 配置文件
- 构建工具：**Maven** 

确保在 `pom.xml`  中已经正确添加相关依赖。


### 2. 配置
目前，仿真参数主要在 `src/main/java/com/bupt/satviz/config/DataPreparation.java` 文件中 **硬编码** 定义。您可以修改此文件来更改星座、地面站和仿真时间。

项目也通过 `src/main/java/com/bupt/satviz/config/SimulationConfig.java` 支持 **YAML 配置**，使用类似 `src/main/resources/simulation_scenario_1.yaml` 的文件。但是，这部分代码目前在 `SatVizApplication.java` 中被 **注释掉了**。要启用它，您需要：

1.  在 `SatVizApplication.java` 中取消注释 `SimulationConfig` 加载代码。
2.  在 `SatVizApplication.java` 中注释掉 `DataPreparation` 代码。
3.  确保在 `src/main/resources/` 中存在一个有效的 YAML 文件（例如 `simulation_scenario_1.yaml`）。


### 3. 运行

直接从您的 IDE 运行`SatVizApplication`类。
该类会自动加载配置文件，初始化 Orekit，并开始仿真计算。
仿真结果将输出到控制台。包括：
- 初始化消息。
- 星历生成和可见性计算期间的进度指示。
- 仿真结束时计算出的卫星状态。
- 每个卫星的详细可见性窗口（对地面站和其他卫星）。
- 总仿真运行时间。

## 代码概览

- **SatVizApplication:** 主驱动类。
- **config:** 处理参数加载（YAML 或硬编码）和 Orekit 设置。
- **preprocessing:** 生成星历（预计算的轨迹）。
- **calculation:** 计算特定时间的卫星状态。
- **concurrent:** 管理可见性任务的并行执行。
- **visibility:** 包含使用 Orekit 事件进行星地和星间可见性检测的算法。
- **model:** 数据类（轨道、站点、结果等）。
- **output:** 格式化结果以供控制台打印。



## 贡献指南

- 修改代码前请编写或更新相应单元测试，确保更改不会引入回归问题。
- 遵循项目的模块化设计，将业务逻辑、配置加载、以及输出模块保持分离，便于后续维护与扩展。
- 提交代码前请确保代码风格一致，遵循 Java 编码规范。

## 联系方式

如果对本项目有任何疑问或建议，欢迎在项目仓库中提交 Issue 或直接联系维护者。

