package com.bupt.satviz;

import com.bupt.satviz.calculation.SatelliteStateCalculator;
import com.bupt.satviz.concurrent.ParallelVisibilityExecutor;
import com.bupt.satviz.config.DataPreparation;
import com.bupt.satviz.config.OrekitConfig;
import com.bupt.satviz.config.SimulationConfig;
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.SatelliteState;
import com.bupt.satviz.output.ConsoleResultPrinter;
import com.bupt.satviz.model.GroundStation;
import org.orekit.time.AbsoluteDate;
import com.bupt.satviz.model.KeplerianElements;

import java.util.List;

public class SatVizApplication {
    public static void main(String[] args) {
        // 记录仿真开始时间戳
        long startTimeMillis = System.currentTimeMillis();
        try {
            // 1. 初始化 Orekit（加载 orekit-data 数据）
            OrekitConfig.initialize();
//            2. 调用数据准备模块，获取模拟参数(硬编码)
//            DataPreparation.SimulationParameters simParams = DataPreparation.prepareSimulationData();
//            List<KeplerianElements> orbits = simParams.satelliteOrbits;
//            List<GroundStation> groundStations = simParams.groundStations;
//            AbsoluteDate startDate = simParams.startDate;
//            AbsoluteDate endDate   = simParams.endDate;

            // 2. 通过 SimulationConfig 加载仿真参数（YAML 文件在 src/main/resources 下）
            SimulationConfig config = new SimulationConfig("simulation_scenario_2.yaml");
            List<KeplerianElements> orbits = config.getSatelliteOrbits();
            List<GroundStation> groundStations = config.getGroundStations();
            AbsoluteDate startDate = config.getStartDate();
            AbsoluteDate endDate = config.getEndDate();

            // 3. 调用并行计算模块，计算所有卫星的可见性结果
            List<SatResult> allResults = ParallelVisibilityExecutor.computeAllVisibilities(
                    orbits, groundStations, startDate, endDate);

            // 4. 计算并打印各卫星状态
            List<SatelliteState> states = SatelliteStateCalculator.computeSatelliteStates(orbits, endDate);
            SatelliteStateCalculator.printSatelliteStates(states);


            // 5. 打印可见性结果
            ConsoleResultPrinter.printResults(allResults);
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 5. 记录仿真结束时间并打印运行总耗时
        long endTimeMillis = System.currentTimeMillis();
        System.out.println("程序总运行时间：" + (endTimeMillis - startTimeMillis) + " 毫秒");
    }
}
