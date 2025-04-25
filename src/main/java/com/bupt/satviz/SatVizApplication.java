package com.bupt.satviz;

import com.bupt.satviz.calculation.SatelliteStateCalculator;
import com.bupt.satviz.concurrent.ParallelVisibilityExecutor;
import com.bupt.satviz.config.DataPreparation;
import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.config.OrekitConfig;
import com.bupt.satviz.config.SimulationConfig;
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.SatelliteState;
import com.bupt.satviz.output.ConsoleResultPrinter;
import com.bupt.satviz.model.GroundStation;
import org.orekit.errors.OrekitException;
import org.orekit.time.AbsoluteDate;
import com.bupt.satviz.preprocessing.EphemerisGeneratorService;
import org.orekit.propagation.BoundedPropagator;

import java.util.Collections;
import java.util.List;

public class SatVizApplication {
    public static void main(String[] args) {
        // 记录仿真开始时间戳
        long startTimeMillis = System.currentTimeMillis();
        System.out.println("开始仿真...");
        List<KeplerianElements> orbitsElements = null;
        AbsoluteDate startDate = null;
        long ephemerisEndTimeMillis = 0;
        long ephemerisStartTimeMillis = 0;
        long visibilityEndTimeMillis = 0;
        long visibilityStartTimeMillis = 0;
        try {
            // 1. 初始化 Orekit（加载 orekit-data 数据）
//            System.out.println("步骤 1: 初始化 Orekit...");
            OrekitConfig.initialize();
            System.out.println("Orekit 初始化完成.");
//            2. 调用数据准备模块，获取模拟参数(硬编码)
//            DataPreparation.SimulationParameters simParams = DataPreparation.prepareSimulationData();
//            orbitsElements = simParams.satelliteOrbits;
//            List<GroundStation> groundStations = simParams.groundStations;
//            startDate = simParams.startDate;
//            AbsoluteDate endDate = simParams.endDate;

            // 2. 通过 SimulationConfig 加载仿真参数（YAML 文件在 src/main/resources 下）
            System.out.println("步骤 2: 加载仿真配置 ...");
            SimulationConfig config = new SimulationConfig("simulation_scenario_1.yaml");
            orbitsElements = config.getSatelliteOrbits();
            List<GroundStation> groundStations = config.getGroundStations();
            startDate = config.getStartDate();
            AbsoluteDate endDate = config.getEndDate();
            if (orbitsElements == null || groundStations == null || startDate == null || endDate == null) {
                throw new RuntimeException("仿真参数加载失败！");
            }
            System.out.println("仿真配置加载完成. 开始时间: " + startDate + ", 结束时间: " + endDate);
            System.out.println("卫星数量: " + orbitsElements.size() + ", 地面站数量: " + groundStations.size());


            // --- 步骤 2.5: 调用新模块生成星历 ---
            List<BoundedPropagator> allEphemerides = EphemerisGeneratorService.generateEphemerides(
                    orbitsElements, startDate, endDate);
            // --- 星历生成结束 ---


            // 3. 调用并行计算模块，计算所有卫星的可见性结果
            List<SatResult> allResults = ParallelVisibilityExecutor.computeAllVisibilities(
                    allEphemerides, groundStations, startDate, endDate);
            // --- 可见性计算结束 ---


            // 4. 计算并打印各卫星状态
            System.out.println("步骤 4: 计算卫星最终状态 ...");
            List<SatelliteState> states = SatelliteStateCalculator.computeSatelliteStates(orbitsElements, endDate);
            SatelliteStateCalculator.printSatelliteStates(states);
            // --- 卫星状态计算结束 ---


            // 5. 按ID查询特定卫星的状态
            int satelliteIdToQuery = 0;
            SatelliteState singleState = SatelliteStateCalculator.getSatelliteStateByIdAndTime(
                    satelliteIdToQuery, endDate, orbitsElements);
            System.out.println("查询结果:");
            // 用列表包装打印
            SatelliteStateCalculator.printSatelliteStates(Collections.singletonList(singleState));


            // 5. 打印可见性结果
            ConsoleResultPrinter.printResults(allResults, orbitsElements, startDate);
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
