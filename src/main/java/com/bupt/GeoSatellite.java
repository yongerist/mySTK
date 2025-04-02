package com.bupt;

/*
 * @param GEOToPhysicalNodeSensorConeHalfAngle 高轨卫星连接地面站传感器的圆锥半角
* */
// 高轨卫星
public class GeoSatellite extends Satellite{
    private double GEOToPhysicalNodeSensorConeHalfAngle; // 高轨卫星连接地面站传感器的圆锥半角
    // 构造器
    public GeoSatellite(String name, double semimajorAxis, double eccentricity, double inclination, double raan, double trueAnomaly, int color, double GEOToPhysicalNodeSensorConeHalfAngle) {
        super(name, semimajorAxis, eccentricity, inclination, raan, trueAnomaly);
        this.GEOToPhysicalNodeSensorConeHalfAngle = GEOToPhysicalNodeSensorConeHalfAngle;
    }
    public GeoSatellite(){
    }


    @Override
    public String toString() {
        return "GeoSatellite{" +
                "GEOToPhysicalNodeSensorConeHalfAngle=" + GEOToPhysicalNodeSensorConeHalfAngle +
                "} " + super.toString();
    }
}
