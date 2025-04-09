package com.bupt.satviz.output;

import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.VisibilityWindow;

import java.util.List;
import java.util.Map;

public class ConsoleResultPrinter {
    /**
     * 打印所有卫星的可见性结果。
     */
    public static void printResults(List<SatResult> allResults) {
        // 按卫星编号升序遍历结果列表
        for (SatResult res : allResults) {
            int satId = res.getSatId();
            // 如果该卫星没有任何可见窗口，则可选择跳过（忽略无窗口情况）
            if (res.getGroundStationResults().isEmpty() && res.getInterSatelliteResults().isEmpty()) {
                continue;
            }
            System.out.println("==== 卫星 #" + satId + " 可见性结果 ====");
            // 1. 打印该卫星与各地面站的可见性窗口
            //    按地面站编号或标识排序，只打印有窗口的地面站条目
            for (Map.Entry<String, List<VisibilityWindow>> entry
                    : res.getGroundStationResults().entrySet()) {
                String stationId = entry.getKey();
                List<VisibilityWindow> windows = entry.getValue();
                if (windows.isEmpty()) continue;  // 跳过无窗口的地面站
                System.out.println("  地面站 " + stationId + " 窗口数: " + windows.size());
                for (VisibilityWindow win : windows) {
                    System.out.println(String.format(
                        "    开始: %s, 结束: %s, 持续: %.0f 秒",
                        win.getStartTime(),
                        (win.getEndTime() != null ? win.getEndTime() : "仍可见"),
                        win.getDurationSeconds()
                    ));
                }
            }
            // 2. 打印该卫星与其它卫星的可见性窗口
            //    按另一卫星编号升序，只打印有窗口的条目
            for (Map.Entry<Integer, List<VisibilityWindow>> entry 
                    : res.getInterSatelliteResults().entrySet()) {
                int otherSatId = entry.getKey();
                List<VisibilityWindow> windows = entry.getValue();
                if (windows.isEmpty()) continue;  // 跳过无窗口的卫星对
                System.out.println("  与卫星 #" + otherSatId + " 窗口数: " + windows.size());
                for (VisibilityWindow win : windows) {
                    System.out.println(String.format(
                        "    开始: %s, 结束: %s, 持续: %.0f 秒",
                        win.getStartTime(),
                        (win.getEndTime() != null ? win.getEndTime() : "仍可见"),
                        win.getDurationSeconds()
                    ));
                }
            }
            System.out.println();  // 空行分隔不同卫星的结果
        }
    }
}
