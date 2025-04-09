package com.bupt.satviz.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hipparchus.util.FastMath;
import org.orekit.frames.Frame;
import org.orekit.orbits.KeplerianOrbit;
import org.orekit.orbits.PositionAngleType;
import org.orekit.time.AbsoluteDate;

/**
 * 轨道元素类
 * 封装轨道六根数和起始时间
 * 用于存储和转换轨道元素
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeplerianElements {
    private double semiMajorAxis;    // 半长轴 (m)
    private double eccentricity;     // 偏心率
    private double inclination;      // 倾角 (deg)
    private double raan;            // 升交点赤经 (deg)
    private double argPerigee;      // 近地点辐角 (deg)
    private double trueAnomaly;     // 真近点角 (deg)
    private AbsoluteDate epoch;     // 元(epoch)时刻：轨道元素的开始时间


    /** 转换为Orekit的 KeplerianOrbit 对象 */
    public KeplerianOrbit toOrbit(Frame inertialFrame, double mu) {
        return new KeplerianOrbit(
            semiMajorAxis,
            eccentricity,
            FastMath.toRadians(inclination),
            FastMath.toRadians(raan),
            FastMath.toRadians(argPerigee),
            FastMath.toRadians(trueAnomaly),
            PositionAngleType.TRUE,
            inertialFrame,
            epoch,
            mu
        );
    }
}
