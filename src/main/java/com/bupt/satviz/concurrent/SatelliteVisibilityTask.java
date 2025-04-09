package com.bupt.satviz.concurrent;

import com.bupt.satviz.model.GroundStation;
import com.bupt.satviz.model.KeplerianElements;
import com.bupt.satviz.model.SatResult;
import com.bupt.satviz.model.VisibilityWindow;
import com.bupt.satviz.visibility.GroundStationVisibilityAnalyzer;
import com.bupt.satviz.visibility.InterSatelliteVisibilityAnalyzer;
import org.orekit.time.AbsoluteDate;

import java.util.List;
import java.util.concurrent.Callable;

public class SatelliteVisibilityTask implements Callable<SatResult> {
    private int satId;
    private KeplerianElements satOrbit;
    private List<GroundStation> groundStations;
    private List<KeplerianElements> allSatOrbits;
    private AbsoluteDate startDate;
    private AbsoluteDate endDate;

    public SatelliteVisibilityTask(int satId,
                                   KeplerianElements satOrbit,
                                   List<GroundStation> groundStations,
                                   List<KeplerianElements> allSatOrbits,
                                   AbsoluteDate startDate,
                                   AbsoluteDate endDate) {
        this.satId = satId;
        this.satOrbit = satOrbit;
        this.groundStations = groundStations;
        this.allSatOrbits = allSatOrbits;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    @Override
    public SatResult call() {
        SatResult result = new SatResult(satId);
        // 1. 计算该卫星对每个地面站的可见性窗口
        GroundStationVisibilityAnalyzer gsAnalyzer = new GroundStationVisibilityAnalyzer(0.0, 45.0);
        for (GroundStation gs : groundStations) {
            try {
                List<VisibilityWindow> windows = gsAnalyzer.computeVisibility(
                        satOrbit, startDate, endDate,
                        gs.lat, gs.lon, gs.alt);
                result.addGroundStationResult(gs.toString(), windows);
            } catch (Exception e) {
                System.err.println("卫星#" + satId + " 与地面站 " + gs.toString() +
                        " 的可见性计算出错: " + e.getMessage());
            }
        }
        // 2. 计算该卫星与其它卫星的可见性窗口（只计算编号比当前大的，以避免重复）
        InterSatelliteVisibilityAnalyzer interSatAnalyzer = new InterSatelliteVisibilityAnalyzer(5_000_000.0);
        for (int otherId = satId + 1; otherId < allSatOrbits.size(); otherId++) {
            try {
                List<VisibilityWindow> windows = interSatAnalyzer.computeVisibility(
                        satOrbit, allSatOrbits.get(otherId),
                        startDate, endDate);
                result.addInterSatelliteResult(otherId, windows);
            } catch (Exception e) {
                System.err.println("卫星#" + satId + " 与卫星#" + otherId +
                        " 的可见性计算出错: " + e.getMessage());
            }
        }
        return result;
    }
}
