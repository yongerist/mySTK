package com.bupt.satviz.concurrent;

import com.bupt.satviz.model.GroundStation;
import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.VisibilityWindow;
import com.bupt.satviz.visibility.GroundStationVisibilityAnalyzer;
import com.bupt.satviz.visibility.InterSatelliteVisibilityAnalyzer;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.BoundedPropagator;
import org.orekit.time.AbsoluteDate;

import java.util.List;
import java.util.concurrent.Callable;

public class SatelliteVisibilityTask implements Callable<SatResult> {
    private final int satId;
    private final BoundedPropagator primarySatEphemeris; // 存储主卫星星历
    private final List<GroundStation> groundStations;
    private final List<BoundedPropagator> allEphemerides; // 存储所有星历
    private final AbsoluteDate startDate;
    private final AbsoluteDate endDate;

    public SatelliteVisibilityTask(int satId,
                                   BoundedPropagator primarySatEphemeris,
                                   List<GroundStation> groundStations,
                                   List<BoundedPropagator> allEphemerides,
                                   AbsoluteDate startDate,
                                   AbsoluteDate endDate) {
        this.satId = satId;
        this.primarySatEphemeris = primarySatEphemeris;
        this.groundStations = groundStations;
        this.allEphemerides = allEphemerides;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public SatResult call() throws OrekitException{
        //System.out.println("  任务 #" + satId + ": 开始计算...");
        SatResult result = new SatResult(satId);
        // 1. 计算该卫星对每个地面站的可见性窗口
        GroundStationVisibilityAnalyzer gsAnalyzer = new GroundStationVisibilityAnalyzer(0.0, 45.0);
        for (GroundStation gs : groundStations) {
            try {
                // 将主卫星的星历传递给分析器
                List<VisibilityWindow> windows = gsAnalyzer.computeVisibility(
                        primarySatEphemeris, startDate, endDate,
                        gs.lat, gs.lon, gs.alt);
                // 只添加包含窗口的结果，避免空的列表项
                // TODO：确保 gs.toString() 提供唯一且合适的标识符。地面站需要序号
                if (windows != null && !windows.isEmpty()) {
                    result.addGroundStationResult(gs.toString(), windows);
                }
            } catch (OrekitException e) {
                // 捕获并记录 Orekit 相关的错误
                System.err.println("任务 #" + satId + ": 计算与地面站 " + gs.toString() +
                        " 可见性时发生 Orekit 错误: " + e.getLocalizedMessage());
                e.printStackTrace(); // 调试时使用
            }
        }
        //System.out.println("  任务 #" + satId + ": 星地计算完成.");


        // 2. 计算该卫星与其它卫星的可见性窗口（只计算编号比当前大的，以避免重复）
        InterSatelliteVisibilityAnalyzer interSatAnalyzer = new InterSatelliteVisibilityAnalyzer(5_000_000.0);
        // 优化循环，避免重复计算 (A->B 和 B->A) 以及自身计算 (A->A)
        for (int otherId = satId + 1; otherId < allEphemerides.size(); otherId++) {
            try {
                // 将两个卫星的星历都传递给分析器
                List<VisibilityWindow> windows = interSatAnalyzer.computeVisibility(
                        primarySatEphemeris,
                        allEphemerides.get(otherId),
                        startDate,
                        endDate
                );
                // 只添加包含窗口的结果
                if (windows != null && !windows.isEmpty()) {
                    result.addInterSatelliteResult(otherId, windows);
                }
            } catch (Exception e) {
                System.err.println("卫星#" + satId + " 与卫星#" + otherId +
                        " 的可见性计算出错: " + e.getMessage());
            }
        }
        //System.out.println("  任务 #" + satId + ": 星间计算完成.");
        return result;
    }
}
