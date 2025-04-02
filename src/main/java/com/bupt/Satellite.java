package com.bupt;
//成员变量：
//String name：卫星名称。
//double semimajorAxis：半长轴。
//double eccentricity：偏心率。
//double inclination：倾角。
//double raan：升交点赤经。
//double trueAnomaly：真近点角。

//成员方法：


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 普通卫星类
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Satellite {
    private String name; //卫星名称 如LEO01
    private double semimajorAxis; //半长轴 单位为m，大于 6378137.0（地球半径）
    private double eccentricity; //偏心率
    private double inclination; //倾角 单位为度，要求 >= 0 and <= 180.0 degrees
    private double raan; //升交点赤经 单位为度，要求 >= -180.0 and <= 360.0 degrees
    private double trueAnomaly; //真近点角 单位为度，要求 >= -180.0 and <= 360.0 degrees
//    private int color; //轨道颜色（没用)


    @Override
    public String toString() {
        return "Satellite{" +
                "name='" + name + '\'' +
                ", semimajorAxis=" + semimajorAxis +
                ", eccentricity=" + eccentricity +
                ", inclination=" + inclination +
                ", raan=" + raan +
                ", trueAnomaly=" + trueAnomaly +
                '}';
    }
}
