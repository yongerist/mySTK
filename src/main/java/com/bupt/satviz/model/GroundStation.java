package com.bupt.satviz.model;

public class GroundStation {
    public double lat;  // 纬度（度）
    public double lon;  // 经度（度）
    public double alt;  // 高度（米）
    public GroundStation(double lat, double lon, double alt) {
        this.lat = lat;
        this.lon = lon;
        this.alt = alt;
    }
    @Override
    public String toString() {
        return String.format("Lat: %.2f°, Lon: %.2f°, Alt: %.1f m", lat, lon, alt);
    }
}
