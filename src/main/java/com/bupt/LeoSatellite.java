package com.bupt;
/*
* @param GEOToLEODistanceConstrain
* @param LEOToLEODistanceConstrain
* @param LEOToPhysicalNodeSensorConeHalfAngle
* */
// 低轨卫星
public class LeoSatellite  extends Satellite {
    private double GEOToLEODistanceConstrain; // GEO到LEO的距离约束
    private double LEOToLEODistanceConstrain; // LEO到LEO的距离约束
    private double LEOToPhysicalNodeSensorConeHalfAngle; // LEO到物理节点的传感器锥角
    // 构造器
    public LeoSatellite(String name, double semimajorAxis, double eccentricity, double inclination, double raan, double trueAnomaly, int color, double GEOToLEODistanceConstrain, double LEOToLEODistanceConstrain, double LEOToPhysicalNodeSensorConeHalfAngle) {
        super(name, semimajorAxis, eccentricity, inclination, raan, trueAnomaly);
        this.GEOToLEODistanceConstrain = GEOToLEODistanceConstrain;
        this.LEOToLEODistanceConstrain = LEOToLEODistanceConstrain;
        this.LEOToPhysicalNodeSensorConeHalfAngle = LEOToPhysicalNodeSensorConeHalfAngle;
    }
    public LeoSatellite() {
    }

    @Override
    public String toString() {
        return "LeoSatellite{" +
                "GEOToLEODistanceConstrain=" + GEOToLEODistanceConstrain +
                ", LEOToLEODistanceConstrain=" + LEOToLEODistanceConstrain +
                ", LEOToPhysicalNodeSensorConeHalfAngle=" + LEOToPhysicalNodeSensorConeHalfAngle +
                "} " + super.toString();
    }
}
