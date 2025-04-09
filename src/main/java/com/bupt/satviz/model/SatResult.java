package com.bupt.satviz.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class SatResult {
    private int satId;   // 卫星编号（索引）
    // 卫星对地面站的可见性结果：键为地面站标识，值为可见性窗口列表
    private Map<String, List<VisibilityWindow>> groundStationResults = new HashMap<>();
    // 卫星对其他卫星的可见性结果：键为另一卫星的编号，值为可见性窗口列表
    private Map<Integer, List<VisibilityWindow>> interSatelliteResults = new HashMap<>();

    public SatResult(int satId) {
        this.satId = satId;
    }

    public void addGroundStationResult(String gsIdentifier, List<VisibilityWindow> windows) {
        groundStationResults.put(gsIdentifier, windows);
    }
    public void addInterSatelliteResult(int otherSatId, List<VisibilityWindow> windows) {
        interSatelliteResults.put(otherSatId, windows);
    }
}
