package com.bupt.satviz.model;

import lombok.Getter;
import org.orekit.bodies.GeodeticPoint;

@Getter
public class SatelliteState {
    private final int satelliteId;  // 卫星编号
    private final GeodeticPoint position;  // 地理位置（纬度/经度/高度），纬度和经度以弧度表示
    private final double latRateDegPerSec;
    private final double lonRateDegPerSec;
    private final double altRateMetersPerSec;

    public SatelliteState(int satelliteId, GeodeticPoint position,
                          double latRate, double lonRate, double altRate) {
        this.satelliteId = satelliteId;
        this.position = position;
        this.latRateDegPerSec = latRate;
        this.lonRateDegPerSec = lonRate;
        this.altRateMetersPerSec = altRate;
    }

}
