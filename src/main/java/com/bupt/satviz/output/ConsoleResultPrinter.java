package com.bupt.satviz.output;

import com.bupt.satviz.model.KeplerianElements; // 导入
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.VisibilityWindow;
import org.orekit.time.AbsoluteDate; // 导入

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ConsoleResultPrinter {

    /**
     * 打印所有卫星的可见性结果，并包含卫星的初始轨道根数信息。
     *
     * @param allResults          所有卫星的可见性结果列表。
     * @param initialOrbits       所有卫星的初始轨道根数列表 (与 allResults 顺序对应)。
     * @param simulationStartDate 仿真开始时间 (用于参考历元是否一致)。
     */
    public static void printResults(List<SatResult> allResults,
                                    List<KeplerianElements> initialOrbits, // 新增参数
                                    AbsoluteDate simulationStartDate) {   // 新增参数

        if (allResults == null || initialOrbits == null || allResults.size() != initialOrbits.size()) {
            System.err.println("错误：结果列表与初始轨道列表不匹配或为 null！");
            return;
        }

        System.out.println("\n========================================");
        System.out.println("        可见性仿真结果输出");
        System.out.println("========================================");

        // 按卫星编号遍历结果
        for (int i = 0; i < allResults.size(); i++) {
            SatResult res = allResults.get(i);
            KeplerianElements initialOrbit = initialOrbits.get(i); // 获取对应的初始轨道根数
            int satId = res.getSatId();

            // 校验一下 ID 是否匹配，以防万一
            if (satId != i) {
                System.err.println("警告：结果列表中的卫星 ID (" + satId + ") 与其索引 (" + i + ") 不匹配！");
            }

            // 检查是否有任何可见性事件，如果没有则可以选择跳过打印或仅打印基本信息
            boolean hasVisibility = !res.getGroundStationResults().isEmpty() || !res.getInterSatelliteResults().isEmpty();

            // --- 打印卫星初始信息 ---
//            System.out.println("\n==== 卫星 #" + satId + " (初始轨道信息) ====");
//            System.out.println(formatKeplerianElements(initialOrbit, simulationStartDate)); // 调用辅助方法格式化输出

            if (!hasVisibility) {
                System.out.println("  该卫星在仿真期间内无可见性事件。");
                continue; // 跳过打印空的可见性窗口部分
            }

            System.out.println("---- 卫星 #" + satId + " 可见性窗口 ----");

            // 1. 打印与地面站的可见性
            boolean gsHeaderPrinted = false;
            // 按 地面站 ID 排序后再打印，使输出更规整
            List<Map.Entry<Integer, List<VisibilityWindow>>> sortedGsEntries = new ArrayList<>(res.getGroundStationResults().entrySet());
            sortedGsEntries.sort(Map.Entry.comparingByKey());
            for (Map.Entry<Integer, List<VisibilityWindow>> entry : sortedGsEntries) {
                int stationId = entry.getKey(); // 地面站标识符
                List<VisibilityWindow> windows = entry.getValue();
                if (windows == null || windows.isEmpty()) continue; // 跳过无窗口的

                if (!gsHeaderPrinted) {
                    System.out.println("  --- 与地面站可见性 ---");
                    gsHeaderPrinted = true;
                }
                System.out.println("    地面站 #" + stationId + " 窗口数: " + windows.size());
                printVisibilityWindows(windows); // 调用辅助方法打印窗口
            }

            // 2. 打印与其他卫星的可见性
            boolean isHeaderPrinted = false;
            // 为了更容易查找，按 otherSatId 排序后再打印
            List<Map.Entry<Integer, List<VisibilityWindow>>> sortedEntries = new ArrayList<>(res.getInterSatelliteResults().entrySet());
            sortedEntries.sort(Map.Entry.comparingByKey());
            // for (Map.Entry<Integer, List<VisibilityWindow>> entry : sortedEntries) {
            for (Map.Entry<Integer, List<VisibilityWindow>> entry : sortedEntries) {
                int otherSatId = entry.getKey();
                List<VisibilityWindow> windows = entry.getValue();
                if (windows == null || windows.isEmpty()) continue; // 跳过无窗口的
                if (!isHeaderPrinted) {
                    System.out.println("  --- 与其他卫星可见性 ---");
                    isHeaderPrinted = true;
                }
                System.out.println("    与卫星 #" + otherSatId + " 窗口数: " + windows.size());
                printVisibilityWindows(windows); // 调用辅助方法打印窗口
            }
            System.out.println("---- 卫星 #" + satId + " 结果结束 ----");
        }
        System.out.println("\n========================================");
        System.out.println("             结果输出结束");
        System.out.println("========================================");
    }

    /**
     * 辅助方法：格式化开普勒轨道根数以便打印。
     */
    private static String formatKeplerianElements(KeplerianElements ke, AbsoluteDate simulationStart) {
        if (ke == null) return "  轨道信息: null";
        // 检查历元是否与仿真开始时间一致
        String epochInfo = ke.getEpoch().equals(simulationStart) ? " (与仿真开始时间一致)" : " (注意: 历元与仿真开始时间不一致!)";
        return String.format(
                "  轨道信息: a=%.1f km, e=%.4f, i=%.2f°, RAAN=%.2f°, ω=%.2f°, ν=%.2f° @ %s%s",
                ke.getSemiMajorAxis() / 1000.0, // 转为千米
                ke.getEccentricity(),
                ke.getInclination(),
                ke.getRaan(),
                ke.getArgPerigee(),
                ke.getTrueAnomaly(),
                ke.getEpoch(),
                epochInfo
        );
    }

    /**
     * 辅助方法：打印可见性窗口列表。
     */
    private static void printVisibilityWindows(List<VisibilityWindow> windows) {
        if (windows == null) return;
        for (VisibilityWindow win : windows) {
            String endTimeStr = (win.getEndTime() != null) ? win.getEndTime().toString() : "仍可见";
            System.out.println(String.format(
                    "      开始: %s, 结束: %s, 持续: %.1f 秒", // 持续时间保留一位小数
                    win.getStartTime(),
                    endTimeStr,
                    win.getDurationSeconds()
            ));
        }
    }
}