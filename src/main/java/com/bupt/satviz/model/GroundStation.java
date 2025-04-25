package com.bupt.satviz.model;

import lombok.Getter;

public class GroundStation {
    @Getter
    public int id;      // 地面站 ID (在列表中的索引)
    public double lat;  // 纬度（度）
    public double lon;  // 经度（度）
    public double alt;  // 高度（米）
    public GroundStation(int id, double lat, double lon, double alt) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }

    @Override
    public String toString() {
        return String.format("GroundStation[id=%d, Lat=%.2f°, Lon=%.2f°, Alt=%.1f m]", id, lat, lon, alt);
    }
}
