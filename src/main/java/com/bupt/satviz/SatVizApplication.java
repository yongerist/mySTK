package com.bupt.satviz;

//import com.bupt.satviz.calculation.SatelliteStateCalculator;
import com.bupt.satviz.calculation.SatelliteStateCalculator;
import com.bupt.satviz.concurrent.ParallelVisibilityExecutor;
import com.bupt.satviz.config.OrekitConfig;
import com.bupt.satviz.config.SimulationConfig;
import com.bupt.satviz.model.SatResult;
//import com.bupt.satviz.model.SatelliteState;
import com.bupt.satviz.model.SatelliteState;
import com.bupt.satviz.output.ConsoleResultPrinter;
import com.bupt.satviz.model.GroundStation;
import org.orekit.time.AbsoluteDate;
import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.preprocessing.EphemerisGeneratorService; // 导入新的服务类
import org.orekit.propagation.BoundedPropagator; // 导入 BoundedPropagator

import java.util.List;

public class SatVizApplication {
    public static void main(String[] args) {
        // 记录仿真开始时间戳
        long startTimeMillis = System.currentTimeMillis();
        System.out.println("开始仿真...");

        try {
            // 1. 初始化 Orekit（加载 orekit-data 数据）
            System.out.println("步骤 1: 初始化 Orekit...");
            OrekitConfig.initialize();
            System.out.println("Orekit 初始化完成.");
//            2. 调用数据准备模块，获取模拟参数(硬编码)
//            DataPreparation.SimulationParameters simParams = DataPreparation.prepareSimulationData();
//            List<KeplerianElements> orbits = simParams.satelliteOrbits;
//            List<GroundStation> groundStations = simParams.groundStations;
//            AbsoluteDate startDate = simParams.startDate;
//            AbsoluteDate endDate   = simParams.endDate;

            // 2. 通过 SimulationConfig 加载仿真参数（YAML 文件在 src/main/resources 下）
            System.out.println("步骤 2: 加载仿真配置 ...");
            SimulationConfig config = new SimulationConfig("simulation_scenario_2.yaml");
            List<KeplerianElements> orbitsElements = config.getSatelliteOrbits();
            List<GroundStation> groundStations = config.getGroundStations();
            AbsoluteDate startDate = config.getStartDate();
            AbsoluteDate endDate = config.getEndDate();
            System.out.println("仿真配置加载完成. 开始时间: " + startDate + ", 结束时间: " + endDate);
            System.out.println("卫星数量: " + orbitsElements.size() + ", 地面站数量: " + groundStations.size());

            // --- 步骤 2.5: 调用新模块生成星历 ---
            System.out.println("步骤 2.5: 生成卫星星历...");
            List<BoundedPropagator> allEphemerides = EphemerisGeneratorService.generateEphemerides(
                    orbitsElements, startDate, endDate);
            System.out.println("所有卫星星历生成完毕.");
            // --- 星历生成结束 ---


            // 3. 调用并行计算模块，计算所有卫星的可见性结果(传入星历列表)
            System.out.println("步骤 3: 并行计算可见性...");
            List<SatResult> allResults = ParallelVisibilityExecutor.computeAllVisibilities(
                    allEphemerides, groundStations, startDate, endDate);
            System.out.println("可见性计算完成.");


            // 4. 计算并打印各卫星状态
            // TODO：需要修改以适应星历，因为它当前使用原始轨道根数。
            System.out.println("步骤 4: 计算卫星最终状态 ...（目前忽略）");
//            List<SatelliteState> states = SatelliteStateCalculator.computeSatelliteStates(orbitsElements, endDate);
//            SatelliteStateCalculator.printSatelliteStates(states);


            // 5. 打印可见性结果
            System.out.println("步骤 5: 打印可见性结果...");
            ConsoleResultPrinter.printResults(allResults);

        } catch (Exception e) { // 捕获所有可能的异常，包括 OrekitException 和并发异常
            System.err.println("仿真过程中发生严重错误:");
            e.printStackTrace();
        } finally {
            // 6. 记录仿真结束时间并打印运行总耗时
            long endTimeMillis = System.currentTimeMillis();
            System.out.println("仿真结束.");
            System.out.println("程序总运行时间：" + (endTimeMillis - startTimeMillis) + " 毫秒");
        }
    }
}
