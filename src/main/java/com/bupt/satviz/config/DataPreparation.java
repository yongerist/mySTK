package com.bupt.satviz.config;

import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.GroundStation;
import org.orekit.time.AbsoluteDate;
import org.orekit.time.TimeScalesFactory;


import java.util.ArrayList;
import java.util.List;

public class DataPreparation {
    /**
     * 生成新的仿真场景：10轨道，每轨12星，方向交替，相位偏移。
     */
    public static SimulationParameters prepareSimulationData() {
        // 1. 初始化仿真起止时间 (2小时)
        AbsoluteDate startDate = new AbsoluteDate(2025, 1, 1, 4, 0, 0.0, TimeScalesFactory.getUTC());
        AbsoluteDate endDate   = startDate.shiftedBy(3600.0);  // 仿真持续1小时

        // 2. 星座参数
        int numOrbits = 10;       // 10条轨道
        int satsPerOrbit = 12;    // 每条轨道12颗卫星
        double semiMajorAxis = 6878.14e3; // ~500 km 高度
        double eccentricity  = 0.0;       // 圆轨道
        double inclination   = 90.0;      // 极轨道
        double argPerigee    = 0.0;       // 对圆轨道无具体意义，设为0
        // 3. 地面站参数
        int numGroundStations = 50;
        // 轨道参数列表
        List<KeplerianElements> allSatOrbits = new ArrayList<>();

        // 计算轨道平面间的 RAAN 间隔
        double raanSeparation = 360.0 / numOrbits; // 36度
        // 计算轨道内卫星的真近点角间隔
        double anomalySeparation = 360.0 / satsPerOrbit; // 30度
        // 为同向轨道设置不同的基础相位偏移（例如，每条轨道偏移15度）
        double basePhaseOffsetStep = 15.0;

        System.out.println("生成星座构型：");
        System.out.println("  轨道数: " + numOrbits);
        System.out.println("  每轨道卫星数: " + satsPerOrbit);
        System.out.println("  轨道倾角: " + inclination + " 度");
        System.out.println("  轨道间 RAAN 间隔: " + raanSeparation + " 度");
        System.out.println("  轨道内卫星角度间隔: " + anomalySeparation + " 度");


        for (int orbitIndex = 0; orbitIndex < numOrbits; orbitIndex++) {
            // 计算当前轨道的 RAAN
            double raan = raanSeparation * orbitIndex;
            // 判断轨道方向 (偶数顺行 Prograde - true, 奇数逆行 Retrograde - false)
            boolean isPrograde = (orbitIndex % 2 == 0);
            // 计算当前轨道的基础相位偏移
            double orbitPhaseOffset = basePhaseOffsetStep * orbitIndex;

//            String direction = isPrograde ? "顺行(Prograde)" : "逆行(Retrograde)";
//            System.out.println("  轨道 #" + orbitIndex + ": RAAN=" + String.format("%.1f", raan)
//                    + ", 方向=" + direction + ", 相位偏移=" + String.format("%.1f", orbitPhaseOffset));

            for (int satIndex = 0; satIndex < satsPerOrbit; satIndex++) {

                // 1. 计算相对于轨道相位偏移的、基础的、逆时针分布的角度
                double baseTrueAnomalyDeg = orbitPhaseOffset + (anomalySeparation * satIndex);

                // 2. 将角度归一化到 [0, 360) 度
                // 取模
                baseTrueAnomalyDeg = baseTrueAnomalyDeg % 360.0;
                if (baseTrueAnomalyDeg < 0) {
                    baseTrueAnomalyDeg += 360.0;
                }

                // 3. 根据轨道方向调整最终的初始真近点角
                double initialTrueAnomalyDeg;
                if (isPrograde) {
                    // 顺行轨道：直接使用归一化后的基础角度
                    initialTrueAnomalyDeg = baseTrueAnomalyDeg;
                } else {
                    // 逆行轨道：翻转角度 (相对于 0 度)
                    initialTrueAnomalyDeg = (baseTrueAnomalyDeg == 0.0) ? 0.0 : (360.0 - baseTrueAnomalyDeg);
                    // 再次确保在 [0, 360)
                    if (initialTrueAnomalyDeg >= 360.0) initialTrueAnomalyDeg -= 360.0;
                    if (initialTrueAnomalyDeg < 0.0) initialTrueAnomalyDeg += 360.0;
                }
                // 创建 KeplerianElements 对象
                KeplerianElements ke = new KeplerianElements(
                        semiMajorAxis, eccentricity, inclination,
                        raan, argPerigee,
                        initialTrueAnomalyDeg, // 使用最终计算出的初始真近点角
                        startDate); // 历元设为仿真开始时间
                allSatOrbits.add(ke);
            }
        }

        // 3. 生成地面站列表
        List<GroundStation> groundStations = new ArrayList<>();
        System.out.println("生成地面站：");
        for (int i = 0; i < numGroundStations; i++) {
            double lat = 0;
            double lon = (360.0 / numGroundStations) * i; // 均匀分布经度
            double alt = 0;
            GroundStation gs = new GroundStation(lat, lon, alt);
            groundStations.add(gs);
//            System.out.println("  地面站 #" + i + ": " + gs.toString());
        }

        // 封装并返回所有参数
        return new SimulationParameters(allSatOrbits, groundStations, startDate, endDate);
    }

    /**
     * 封装仿真参数
     */
    public static class SimulationParameters {
        public final List<KeplerianElements> satelliteOrbits;
        public final List<GroundStation> groundStations;
        public final AbsoluteDate startDate;
        public final AbsoluteDate endDate;
        public SimulationParameters(List<KeplerianElements> orbits,
                                    List<GroundStation> stations,
                                    AbsoluteDate start, AbsoluteDate end) {
            this.satelliteOrbits = orbits;
            this.groundStations  = stations;
            this.startDate       = start;
            this.endDate         = end;
        }
    }
}